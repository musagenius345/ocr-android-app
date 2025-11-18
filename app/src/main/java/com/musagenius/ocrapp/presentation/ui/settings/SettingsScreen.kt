package com.musagenius.ocrapp.presentation.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.musagenius.ocrapp.domain.model.AppTheme
import com.musagenius.ocrapp.domain.model.DefaultCamera
import com.musagenius.ocrapp.domain.model.ImageQuality
import com.musagenius.ocrapp.presentation.viewmodel.SettingsViewModel
import kotlin.math.roundToInt

/**
 * Settings screen
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToLanguageManagement: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val preferences by viewModel.preferences.collectAsState()
    var showThemeDialog by remember { mutableStateOf(false) }
    var showCameraDialog by remember { mutableStateOf(false) }
    var showQualityDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Settings",
                        style = MaterialTheme.typography.headlineMedium
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Appearance Section
            SectionHeader(title = "Appearance")
            Spacer(modifier = Modifier.height(8.dp))

            PreferenceItem(
                title = "Theme",
                subtitle = preferences.theme.getDisplayName(),
                onClick = { showThemeDialog = true }
            )

            SwitchPreferenceItem(
                title = "Dynamic Color",
                subtitle = "Use colors from your wallpaper",
                checked = preferences.useDynamicColor,
                onCheckedChange = viewModel::updateDynamicColor
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Camera Section
            SectionHeader(title = "Camera")
            Spacer(modifier = Modifier.height(8.dp))

            PreferenceItem(
                title = "Default Camera",
                subtitle = preferences.defaultCamera.getDisplayName(),
                onClick = { showCameraDialog = true }
            )

            PreferenceItem(
                title = "Image Quality",
                subtitle = preferences.imageQuality.getDisplayName(),
                onClick = { showQualityDialog = true }
            )

            SwitchPreferenceItem(
                title = "Auto-Focus",
                subtitle = "Automatically focus when capturing",
                checked = preferences.autoFocus,
                onCheckedChange = viewModel::updateAutoFocus
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Language Section
            SectionHeader(title = "Language")
            Spacer(modifier = Modifier.height(8.dp))

            PreferenceItem(
                title = "OCR Languages",
                subtitle = "Download and manage OCR language files",
                onClick = onNavigateToLanguageManagement
            )

            Spacer(modifier = Modifier.height(24.dp))

            // General Section
            SectionHeader(title = "General")
            Spacer(modifier = Modifier.height(8.dp))

            SwitchPreferenceItem(
                title = "Auto-save to History",
                subtitle = "Automatically save scans to history",
                checked = preferences.autoSaveToHistory,
                onCheckedChange = viewModel::updateAutoSaveToHistory
            )

            SwitchPreferenceItem(
                title = "Auto-delete Old Scans",
                subtitle = "Automatically delete scans older than ${preferences.autoDeleteDays} days",
                checked = preferences.autoDeleteOldScans,
                onCheckedChange = viewModel::updateAutoDeleteOldScans
            )

            if (preferences.autoDeleteOldScans) {
                SliderPreferenceItem(
                    title = "Auto-delete Days",
                    value = preferences.autoDeleteDays,
                    valueRange = 7f..365f,
                    steps = 51,
                    onValueChange = { viewModel.updateAutoDeleteDays(it.roundToInt()) }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // About Section
            SectionHeader(title = "About")
            Spacer(modifier = Modifier.height(8.dp))

            PreferenceItem(
                title = "Version",
                subtitle = "1.0.0",
                onClick = {}
            )
        }
    }

    // Theme selection dialog
    if (showThemeDialog) {
        SelectionDialog(
            title = "Theme",
            options = AppTheme.entries,
            selectedOption = preferences.theme,
            onOptionSelected = {
                viewModel.updateTheme(it)
                showThemeDialog = false
            },
            onDismiss = { showThemeDialog = false },
            optionLabel = { it.getDisplayName() }
        )
    }

    // Camera selection dialog
    if (showCameraDialog) {
        SelectionDialog(
            title = "Default Camera",
            options = DefaultCamera.entries,
            selectedOption = preferences.defaultCamera,
            onOptionSelected = {
                viewModel.updateDefaultCamera(it)
                showCameraDialog = false
            },
            onDismiss = { showCameraDialog = false },
            optionLabel = { it.getDisplayName() }
        )
    }

    // Quality selection dialog
    if (showQualityDialog) {
        SelectionDialog(
            title = "Image Quality",
            options = ImageQuality.entries,
            selectedOption = preferences.imageQuality,
            onOptionSelected = {
                viewModel.updateImageQuality(it)
                showQualityDialog = false
            },
            onDismiss = { showQualityDialog = false },
            optionLabel = { it.getDisplayName() }
        )
    }
}

/**
 * Section header
 */
@Composable
private fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier
    )
}

/**
 * Preference item
 */
@Composable
private fun PreferenceItem(
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Switch preference item
 */
@Composable
private fun SwitchPreferenceItem(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange
            )
        }
    }
}

/**
 * Slider preference item
 */
@Composable
private fun SliderPreferenceItem(
    title: String,
    value: Int,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "$value days",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Slider(
                value = value.toFloat(),
                onValueChange = onValueChange,
                valueRange = valueRange,
                steps = steps
            )
        }
    }
}

/**
 * Selection dialog with radio buttons
 */
@Composable
private fun <T> SelectionDialog(
    title: String,
    options: List<T>,
    selectedOption: T,
    onOptionSelected: (T) -> Unit,
    onDismiss: () -> Unit,
    optionLabel: (T) -> String
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Column {
                options.forEach { option ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOptionSelected(option) }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = option == selectedOption,
                            onClick = { onOptionSelected(option) }
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = optionLabel(option),
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                    if (option != options.last()) {
                        HorizontalDivider()
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

/**
 * Extension functions for display names
 */
private fun AppTheme.getDisplayName(): String {
    return when (this) {
        AppTheme.LIGHT -> "Light"
        AppTheme.DARK -> "Dark"
        AppTheme.SYSTEM -> "System Default"
    }
}

private fun DefaultCamera.getDisplayName(): String {
    return when (this) {
        DefaultCamera.BACK -> "Back Camera"
        DefaultCamera.FRONT -> "Front Camera"
    }
}

private fun ImageQuality.getDisplayName(): String {
    return when (this) {
        ImageQuality.LOW -> "Low (Faster)"
        ImageQuality.MEDIUM -> "Medium"
        ImageQuality.HIGH -> "High (Best Quality)"
    }
}
