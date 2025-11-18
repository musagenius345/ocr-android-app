package com.musagenius.ocrapp.presentation.ui.detail

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.musagenius.ocrapp.presentation.viewmodel.ScanDetailViewModel
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Scan Detail screen showing full scan information and edit capabilities
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanDetailScreen(
    onNavigateBack: () -> Unit,
    viewModel: ScanDetailViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    // Handle snackbar messages
    LaunchedEffect(state.snackbarMessage) {
        state.snackbarMessage?.let { message ->
            snackbarHostState.showSnackbar(
                message = message,
                duration = SnackbarDuration.Short
            )
            viewModel.clearSnackbar()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Scan Details") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    // Favorite button
                    IconButton(onClick = viewModel::toggleFavorite) {
                        Icon(
                            imageVector = if (state.scan?.isFavorite == true) {
                                Icons.Default.Favorite
                            } else {
                                Icons.Default.FavoriteBorder
                            },
                            contentDescription = if (state.scan?.isFavorite == true) {
                                "Remove from favorites"
                            } else {
                                "Add to favorites"
                            },
                            tint = if (state.scan?.isFavorite == true) {
                                MaterialTheme.colorScheme.error
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            }
                        )
                    }

                    // Share button
                    IconButton(
                        onClick = {
                            state.scan?.let { scan ->
                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, scan.extractedText)
                                    putExtra(Intent.EXTRA_SUBJECT, scan.title.ifEmpty { "OCR Scan" })
                                }
                                context.startActivity(Intent.createChooser(shareIntent, "Share text"))
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share"
                        )
                    }

                    // Delete button
                    IconButton(onClick = viewModel::showDeleteConfirmation) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete"
                        )
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                state.isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                state.error != null -> {
                    ErrorState(
                        error = state.error!!,
                        onRetry = viewModel::reloadScan,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                state.scan != null -> {
                    ScanDetailContent(
                        state = state,
                        onStartEditingText = viewModel::startEditingText,
                        onUpdateEditedText = viewModel::updateEditedText,
                        onSaveEditedText = viewModel::saveEditedText,
                        onCancelEditingText = viewModel::cancelEditingText,
                        onStartEditingTitleNotes = viewModel::startEditingTitleNotes,
                        onUpdateEditedTitle = viewModel::updateEditedTitle,
                        onUpdateEditedNotes = viewModel::updateEditedNotes,
                        onSaveEditedTitleNotes = viewModel::saveEditedTitleNotes,
                        onCancelEditingTitleNotes = viewModel::cancelEditingTitleNotes
                    )
                }
            }
        }

        // Delete confirmation dialog
        if (state.showDeleteConfirmation) {
            DeleteConfirmationDialog(
                onConfirm = {
                    viewModel.deleteScan(onDeleted = onNavigateBack)
                },
                onDismiss = viewModel::hideDeleteConfirmation
            )
        }
    }
}

/**
 * Scan detail content
 */
@Composable
private fun ScanDetailContent(
    state: ScanDetailState,
    onStartEditingText: () -> Unit,
    onUpdateEditedText: (String) -> Unit,
    onSaveEditedText: () -> Unit,
    onCancelEditingText: () -> Unit,
    onStartEditingTitleNotes: () -> Unit,
    onUpdateEditedTitle: (String) -> Unit,
    onUpdateEditedNotes: (String) -> Unit,
    onSaveEditedTitleNotes: () -> Unit,
    onCancelEditingTitleNotes: () -> Unit
) {
    val scan = state.scan ?: return

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Image preview
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            AsyncImage(
                model = scan.imageUri,
                contentDescription = "Scanned image",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Fit
            )
        }

        // Title and Notes Section
        TitleNotesSection(
            state = state,
            onStartEditing = onStartEditingTitleNotes,
            onUpdateTitle = onUpdateEditedTitle,
            onUpdateNotes = onUpdateEditedNotes,
            onSave = onSaveEditedTitleNotes,
            onCancel = onCancelEditingTitleNotes
        )

        // Metadata Section
        MetadataSection(scan = scan)

        // Extracted Text Section
        ExtractedTextSection(
            state = state,
            onStartEditing = onStartEditingText,
            onUpdateText = onUpdateEditedText,
            onSave = onSaveEditedText,
            onCancel = onCancelEditingText
        )
    }
}

/**
 * Title and Notes Section
 */
@Composable
private fun TitleNotesSection(
    state: ScanDetailState,
    onStartEditing: () -> Unit,
    onUpdateTitle: (String) -> Unit,
    onUpdateNotes: (String) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Title & Notes",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                if (!state.isEditingTitleNotes) {
                    IconButton(onClick = onStartEditing) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit title and notes"
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (state.isEditingTitleNotes) {
                OutlinedTextField(
                    value = state.editedTitle,
                    onValueChange = onUpdateTitle,
                    label = { Text("Title") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = state.editedNotes,
                    onValueChange = onUpdateNotes,
                    label = { Text("Notes") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TextButton(
                        onClick = onCancel,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel")
                    }
                    FilledTonalButton(
                        onClick = onSave,
                        modifier = Modifier.weight(1f),
                        enabled = !state.isSaving
                    ) {
                        if (state.isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("Save")
                        }
                    }
                }
            } else {
                if (state.scan?.title?.isNotEmpty() == true) {
                    Text(
                        text = state.scan.title,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                Text(
                    text = state.scan?.notes?.ifEmpty { "No notes" } ?: "No notes",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (state.scan?.notes?.isEmpty() == true) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }
                )
            }
        }
    }
}

/**
 * Metadata Section
 */
@Composable
private fun MetadataSection(scan: com.musagenius.ocrapp.domain.model.ScanResult) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Information",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            MetadataRow("Date", SimpleDateFormat("MMM dd, yyyy 'at' hh:mm a", Locale.getDefault()).format(scan.timestamp))
            MetadataRow("Language", scan.language.uppercase())
            MetadataRow("Confidence", scan.getConfidencePercentage())
            MetadataRow("Word Count", "${scan.getWordCount()} words")
        }
    }
}

/**
 * Metadata Row
 */
@Composable
private fun MetadataRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
    }
}

/**
 * Extracted Text Section
 */
@Composable
private fun ExtractedTextSection(
    state: ScanDetailState,
    onStartEditing: () -> Unit,
    onUpdateText: (String) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Extracted Text",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                if (!state.isEditingText) {
                    IconButton(onClick = onStartEditing) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit text"
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (state.isEditingText) {
                OutlinedTextField(
                    value = state.editedText,
                    onValueChange = onUpdateText,
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 10
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TextButton(
                        onClick = onCancel,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel")
                    }
                    FilledTonalButton(
                        onClick = onSave,
                        modifier = Modifier.weight(1f),
                        enabled = !state.isSaving
                    ) {
                        if (state.isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("Save")
                        }
                    }
                }
            } else {
                Text(
                    text = state.scan?.extractedText ?: "",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

/**
 * Error state display
 */
@Composable
private fun ErrorState(
    error: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Error",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.error
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = error,
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(modifier = Modifier.height(16.dp))
        TextButton(onClick = onRetry) {
            Text("Retry")
        }
    }
}

/**
 * Delete confirmation dialog
 */
@Composable
private fun DeleteConfirmationDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete Scan?") },
        text = { Text("This action cannot be undone. The scan will be permanently deleted.") },
        confirmButton = {
            TextButton(
                onClick = onConfirm
            ) {
                Text("Delete", color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
