package com.musagenius.ocrapp.presentation.ui.camera

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import com.musagenius.ocrapp.presentation.ui.components.GridOverlay
import com.musagenius.ocrapp.presentation.ui.components.ShutterAnimation
import com.musagenius.ocrapp.presentation.ui.components.rememberHapticFeedback
import com.musagenius.ocrapp.presentation.viewmodel.CameraViewModel
import java.util.Locale

/**
 * Camera screen for capturing images
 * Includes accessibility features from the start (shift-left approach)
 */
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun CameraScreen(
    onImageCaptured: (Uri) -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: CameraViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val uiState by viewModel.uiState.collectAsState()
    val haptic = rememberHapticFeedback()

    // Shutter animation trigger
    var showShutterAnimation by remember { mutableStateOf(false) }

    // Camera permission state
    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)

    // Gallery picker launcher
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { onImageCaptured(it) }
    }

    // Permission rationale dialog state
    var showRationaleDialog by remember { mutableStateOf(false) }
    var showDeniedDialog by remember { mutableStateOf(false) }

    // Handle captured image
    LaunchedEffect(uiState.capturedImageUri) {
        uiState.capturedImageUri?.let { uri ->
            onImageCaptured(uri)
            viewModel.clearCapturedImage()
        }
    }

    // Request permission on first launch
    LaunchedEffect(Unit) {
        if (!cameraPermissionState.status.isGranted) {
            if (cameraPermissionState.status.shouldShowRationale) {
                showRationaleDialog = true
            } else {
                cameraPermissionState.launchPermissionRequest()
            }
        }
    }

    // Show permission dialogs
    if (showRationaleDialog) {
        PermissionRationaleDialog(
            onDismiss = {
                showRationaleDialog = false
                onNavigateBack()
            },
            onConfirm = {
                showRationaleDialog = false
                cameraPermissionState.launchPermissionRequest()
            }
        )
    }

    if (showDeniedDialog) {
        PermissionDeniedDialog(
            onDismiss = {
                showDeniedDialog = false
                onNavigateBack()
            },
            onOpenSettings = {
                showDeniedDialog = false
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", context.packageName, null)
                }
                context.startActivity(intent)
            }
        )
    }

    // Main content
    Scaffold(
        topBar = {
            CameraTopBar(
                onNavigateBack = onNavigateBack,
                flashMode = uiState.flashMode,
                onToggleFlash = { viewModel.onEvent(CameraEvent.ToggleFlash) }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                !cameraPermissionState.status.isGranted -> {
                    // Permission not granted - show fallback UI
                    CameraPermissionRequired(
                        onRequestPermission = {
                            if (cameraPermissionState.status.shouldShowRationale) {
                                showRationaleDialog = true
                            } else {
                                showDeniedDialog = true
                            }
                        },
                        onPickFromGallery = {
                            galleryLauncher.launch("image/*")
                        }
                    )
                }
                uiState.isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .semantics {
                                contentDescription = "Loading camera"
                            }
                    )
                }
                else -> {
                    // Camera preview
                    var previewView: androidx.camera.view.PreviewView? by remember { mutableStateOf(null) }

                    LaunchedEffect(previewView, cameraPermissionState.status.isGranted) {
                        if (cameraPermissionState.status.isGranted && previewView != null) {
                            viewModel.startCamera(lifecycleOwner, previewView!!)
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(Unit) {
                                // Pinch-to-zoom gesture
                                detectTransformGestures { _, _, zoom, _ ->
                                    val newZoom = uiState.zoomRatio * zoom
                                    viewModel.onEvent(CameraEvent.SetZoom(newZoom))
                                }
                            }
                            .pointerInput(Unit) {
                                // Tap-to-focus gesture
                                detectTapGestures { offset ->
                                    val x = offset.x / size.width
                                    val y = offset.y / size.height
                                    viewModel.onEvent(CameraEvent.TapToFocus(x, y))
                                    haptic.performLightTap()
                                }
                            }
                    ) {
                        CameraPreview(
                            modifier = Modifier.fillMaxSize(),
                            onPreviewViewCreated = { previewView = it }
                        )

                        // Grid overlay
                        if (uiState.showGridOverlay) {
                            GridOverlay()
                        }

                        // Shutter animation overlay
                        ShutterAnimation(
                            trigger = showShutterAnimation,
                            onAnimationComplete = { showShutterAnimation = false }
                        )

                        // Top controls (zoom, grid, flip)
                        CameraTopControls(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(16.dp),
                            zoomRatio = uiState.zoomRatio,
                            showGridOverlay = uiState.showGridOverlay,
                            cameraFacing = uiState.cameraFacing,
                            onZoomChange = { viewModel.onEvent(CameraEvent.SetZoom(it)) },
                            onToggleGrid = {
                                haptic.performLightTap()
                                viewModel.onEvent(CameraEvent.ToggleGridOverlay)
                            },
                            onFlipCamera = {
                                haptic.performLightTap()
                                viewModel.onEvent(CameraEvent.FlipCamera)
                            }
                        )

                        // Camera controls overlay
                        CameraControls(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 32.dp),
                            isProcessing = uiState.isProcessing,
                            onCaptureClick = {
                                haptic.performCaptureFeedback()
                                showShutterAnimation = true
                                viewModel.onEvent(CameraEvent.CaptureImage)
                            },
                            onGalleryClick = {
                                haptic.performLightTap()
                                galleryLauncher.launch("image/*")
                            }
                        )
                    }
                }
            }

            // Error snackbar
            uiState.error?.let { error ->
                Snackbar(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp),
                    action = {
                        TextButton(onClick = { viewModel.onEvent(CameraEvent.DismissError) }) {
                            Text("Dismiss")
                        }
                    }
                ) {
                    Text(error)
                }
            }
        }
    }
}

@Composable
fun CameraTopBar(
    onNavigateBack: () -> Unit,
    flashMode: FlashMode,
    onToggleFlash: () -> Unit
) {
    val haptic = rememberHapticFeedback()

    TopAppBar(
        title = { Text("Scan Document") },
        navigationIcon = {
            IconButton(
                onClick = {
                    haptic.performLightTap()
                    onNavigateBack()
                },
                modifier = Modifier.semantics {
                    contentDescription = "Navigate back"
                }
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back"
                )
            }
        },
        actions = {
            IconButton(
                onClick = {
                    haptic.performLightTap()
                    onToggleFlash()
                },
                modifier = Modifier
                    .size(48.dp) // Minimum touch target
                    .semantics {
                        contentDescription = flashMode.getContentDescription()
                    }
            ) {
                Icon(
                    imageVector = when (flashMode) {
                        FlashMode.OFF -> Icons.Default.FlashOff
                        FlashMode.ON -> Icons.Default.FlashOn
                        FlashMode.AUTO -> Icons.Default.FlashAuto
                    },
                    contentDescription = flashMode.getDisplayName()
                )
            }
        }
    )
}

@Composable
fun CameraControls(
    modifier: Modifier = Modifier,
    isProcessing: Boolean,
    onCaptureClick: () -> Unit,
    onGalleryClick: () -> Unit
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(32.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Gallery button
        IconButton(
            onClick = onGalleryClick,
            enabled = !isProcessing,
            modifier = Modifier
                .size(56.dp) // >48dp for better accessibility
                .semantics {
                    contentDescription = "Choose image from gallery"
                }
        ) {
            Icon(
                imageVector = Icons.Default.PhotoLibrary,
                contentDescription = "Gallery",
                modifier = Modifier.size(32.dp)
            )
        }

        // Capture button
        FloatingActionButton(
            onClick = onCaptureClick,
            enabled = !isProcessing,
            modifier = Modifier
                .size(72.dp) // Large, easy to tap
                .semantics {
                    contentDescription = if (isProcessing) {
                        "Processing image..."
                    } else {
                        "Capture image for text recognition"
                    }
                }
        ) {
            if (isProcessing) {
                CircularProgressIndicator(
                    modifier = Modifier.size(32.dp),
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            } else {
                Icon(
                    imageVector = Icons.Default.CameraAlt,
                    contentDescription = "Capture",
                    modifier = Modifier.size(36.dp)
                )
            }
        }

        // Placeholder for symmetry (or future feature)
        Spacer(modifier = Modifier.size(56.dp))
    }
}

@Composable
fun CameraPermissionRequired(
    onRequestPermission: () -> Unit,
    onPickFromGallery: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.CameraAlt,
            contentDescription = null,
            modifier = Modifier
                .size(80.dp)
                .padding(bottom = 16.dp),
            tint = MaterialTheme.colorScheme.primary
        )

        Text(
            text = "Camera Access Required",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Text(
            text = "This app needs camera access to scan documents and extract text.",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        Button(
            onClick = onRequestPermission,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp) // >48dp for accessibility
                .semantics {
                    contentDescription = "Grant camera permission"
                }
        ) {
            Text("Grant Permission")
        }

        OutlinedButton(
            onClick = onPickFromGallery,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(top = 8.dp)
                .semantics {
                    contentDescription = "Choose image from gallery instead"
                }
        ) {
            Text("Pick from Gallery")
        }
    }
}

@Composable
fun CameraTopControls(
    modifier: Modifier = Modifier,
    zoomRatio: Float,
    showGridOverlay: Boolean,
    cameraFacing: CameraFacing,
    onZoomChange: (Float) -> Unit,
    onToggleGrid: () -> Unit,
    onFlipCamera: () -> Unit
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Camera flip button
        FilledIconButton(
            onClick = onFlipCamera,
            modifier = Modifier
                .size(48.dp)
                .semantics {
                    contentDescription = "Flip camera to ${if (cameraFacing == CameraFacing.BACK) "front" else "back"}"
                },
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
            )
        ) {
            Icon(
                imageVector = Icons.Default.FlipCameraAndroid,
                contentDescription = "Flip Camera"
            )
        }

        // Grid overlay toggle button
        FilledIconButton(
            onClick = onToggleGrid,
            modifier = Modifier
                .size(48.dp)
                .semantics {
                    contentDescription = if (showGridOverlay) "Hide grid overlay" else "Show grid overlay"
                },
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = if (showGridOverlay) {
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f)
                } else {
                    MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
                }
            )
        ) {
            Icon(
                imageVector = Icons.Default.GridOn,
                contentDescription = "Grid",
                tint = if (showGridOverlay) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurface
                }
            )
        }

        // Zoom indicator and slider
        if (zoomRatio > 1.01f) {
            Surface(
                modifier = Modifier
                    .width(48.dp)
                    .height(120.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                shape = MaterialTheme.shapes.medium
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = String.format(Locale.US, "%.1fx", zoomRatio),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}
