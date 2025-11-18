package com.musagenius.ocrapp.presentation.ui.ocr

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.musagenius.ocrapp.R
import com.musagenius.ocrapp.presentation.ui.components.rememberHapticFeedback
import com.musagenius.ocrapp.presentation.viewmodel.OCRViewModel

/**
 * OCR Result screen showing extracted text and image
 * Includes accessibility features from the start
 */
@Composable
fun OCRResultScreen(
    imageUri: Uri,
    onNavigateBack: () -> Unit,
    viewModel: OCRViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val haptic = rememberHapticFeedback()
    val uiState by viewModel.uiState.collectAsState()

    // Start OCR processing when screen loads
    LaunchedEffect(imageUri) {
        viewModel.setImageUri(imageUri)
    }

    Scaffold(
        topBar = {
            OCRResultTopBar(
                onNavigateBack = {
                    haptic.performLightTap()
                    onNavigateBack()
                },
                isSaved = uiState.isSaved,
                onSave = {
                    haptic.performLightTap()
                    viewModel.onEvent(OCREvent.SaveToHistory)
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                uiState.isProcessing -> {
                    // Processing state
                    OCRProcessingView()
                }
                uiState.error != null -> {
                    // Error state
                    uiState.error.let { error ->
                        OCRErrorView(
                            error = error,
                            onRetry = {
                                haptic.performLightTap()
                                viewModel.onEvent(OCREvent.RetryProcessing)
                            },
                            onDismiss = {
                                viewModel.onEvent(OCREvent.DismissError)
                            }
                        )
                    }
                }
                uiState.extractedText.isNotEmpty() -> {
                    // Success state
                    OCRResultContent(
                        imageUri = imageUri,
                        extractedText = uiState.extractedText,
                        confidenceScore = uiState.confidenceScore,
                        processingTimeMs = uiState.processingTimeMs,
                        onCopyText = {
                            copyToClipboard(context, uiState.extractedText)
                            haptic.performSuccessFeedback()
                        },
                        onShareText = {
                            shareText(context, uiState.extractedText)
                            haptic.performLightTap()
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OCRResultTopBar(
    onNavigateBack: () -> Unit,
    isSaved: Boolean,
    onSave: () -> Unit
) {
    val haptic = rememberHapticFeedback()

    TopAppBar(
        title = { Text(stringResource(R.string.extracted_text)) },
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
            // Save button
            IconButton(
                onClick = onSave,
                enabled = !isSaved,
                modifier = Modifier.semantics {
                    contentDescription = if (isSaved) "Already saved to history" else "Save to history"
                }
            ) {
                Icon(
                    imageVector = if (isSaved) Icons.Default.Check else Icons.Default.Save,
                    contentDescription = if (isSaved) "Saved" else "Save",
                    tint = if (isSaved) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }
                )
            }
        }
    )
}

@Composable
fun OCRProcessingView() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator(
            modifier = Modifier
                .size(64.dp)
                .semantics {
                    contentDescription = "Processing image with OCR"
                }
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Extracting text from image...",
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "This may take a few seconds",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun OCRErrorView(
    error: String,
    onRetry: () -> Unit,
    onDismiss: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Error,
            contentDescription = null,
            modifier = Modifier
                .size(64.dp)
                .padding(bottom = 16.dp),
            tint = MaterialTheme.colorScheme.error
        )

        Text(
            text = "OCR Processing Failed",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.error,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = error,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onRetry,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .semantics {
                    contentDescription = "Retry OCR processing"
                }
        ) {
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = null,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(R.string.retry))
        }
    }
}

@Composable
fun OCRResultContent(
    imageUri: Uri,
    extractedText: String,
    confidenceScore: Float,
    processingTimeMs: Long,
    onCopyText: () -> Unit,
    onShareText: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        // Image preview
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .padding(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            AsyncImage(
                model = imageUri,
                contentDescription = "Scanned image",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }

        // Stats card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem(
                    label = "Confidence",
                    value = "${(confidenceScore * 100).toInt()}%"
                )
                StatItem(
                    label = "Time",
                    value = "${processingTimeMs}ms"
                )
                StatItem(
                    label = "Words",
                    value = extractedText.split("\\s+".toRegex()).size.toString()
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Action buttons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = onCopyText,
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp)
                    .semantics {
                        contentDescription = "Copy extracted text to clipboard"
                    }
            ) {
                Icon(
                    imageVector = Icons.Default.ContentCopy,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.copy_text))
            }

            OutlinedButton(
                onClick = onShareText,
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp)
                    .semantics {
                        contentDescription = "Share extracted text"
                    }
            ) {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.share_text))
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Extracted text
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = "Extracted Text",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(8.dp))

                SelectionContainer {
                    Text(
                        text = extractedText,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.semantics {
                            contentDescription = "Extracted text: $extractedText"
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun StatItem(label: String, value: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSecondaryContainer
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
        )
    }
}

/**
 * Copy text to clipboard
 */
private fun copyToClipboard(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText(
        context.getString(R.string.extracted_text_clipboard_label),
        text
    )
    clipboard.setPrimaryClip(clip)
}

/**
 * Share text via Android share sheet
 */
private fun shareText(context: Context, text: String) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    context.startActivity(Intent.createChooser(intent, "Share text"))
}
