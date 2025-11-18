package com.musagenius.ocrapp.presentation.ui.editor

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.musagenius.ocrapp.presentation.ui.components.rememberHapticFeedback
import com.musagenius.ocrapp.presentation.viewmodel.ImageEditorViewModel

/**
 * Image editor screen for rotating images before OCR
 *
 * @param sourceImageUri URI of the image to edit
 * @param onImageSaved Callback when edited image is saved
 * @param onCancel Callback when editing is cancelled
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageEditorScreen(
    sourceImageUri: Uri,
    onImageSaved: (Uri) -> Unit,
    onCancel: () -> Unit,
    viewModel: ImageEditorViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val haptic = rememberHapticFeedback()

    // Initialize with source image
    LaunchedEffect(sourceImageUri) {
        viewModel.initializeImage(
            uri = sourceImageUri,
            onSaved = onImageSaved,
            onCancelled = onCancel
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit Image") },
                navigationIcon = {
                    IconButton(onClick = {
                        haptic.performLightTap()
                        viewModel.onEvent(ImageEditorEvent.Cancel)
                    }) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cancel"
                        )
                    }
                },
                actions = {
                    // Reset rotation button
                    IconButton(
                        onClick = {
                            haptic.performLightTap()
                            viewModel.onEvent(ImageEditorEvent.ResetRotation)
                        },
                        enabled = uiState.rotationDegrees != 0f && !uiState.isProcessing
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Reset rotation"
                        )
                    }

                    // Save button
                    TextButton(
                        onClick = {
                            haptic.performMediumTap()
                            viewModel.onEvent(ImageEditorEvent.SaveImage)
                        },
                        enabled = !uiState.isProcessing
                    ) {
                        Text("Save")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color.Black)
        ) {
            // Image preview with rotation
            uiState.sourceImageUri?.let { uri ->
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(uri)
                        .crossfade(true)
                        .build(),
                    contentDescription = "Image to edit",
                    modifier = Modifier
                        .fillMaxSize()
                        .rotate(uiState.rotationDegrees),
                    contentScale = ContentScale.Fit
                )
            }

            // Loading indicator
            if (uiState.isProcessing) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.7f)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Processing image...",
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color.White
                        )
                    }
                }
            }

            // Rotation controls at bottom
            if (!uiState.isProcessing) {
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Rotate counter-clockwise
                    FilledIconButton(
                        onClick = {
                            haptic.performLightTap()
                            viewModel.onEvent(ImageEditorEvent.RotateCounterClockwise)
                        },
                        modifier = Modifier.size(64.dp),
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.RotateLeft,
                            contentDescription = "Rotate counter-clockwise",
                            modifier = Modifier.size(32.dp),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }

                    // Rotation indicator
                    Surface(
                        shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                        tonalElevation = 2.dp
                    ) {
                        Text(
                            text = "${uiState.rotationDegrees.toInt()}°",
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // Rotate clockwise
                    FilledIconButton(
                        onClick = {
                            haptic.performLightTap()
                            viewModel.onEvent(ImageEditorEvent.RotateClockwise)
                        },
                        modifier = Modifier.size(64.dp),
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.RotateRight,
                            contentDescription = "Rotate clockwise",
                            modifier = Modifier.size(32.dp),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
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
                        TextButton(onClick = {
                            viewModel.onEvent(ImageEditorEvent.DismissError)
                        }) {
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
