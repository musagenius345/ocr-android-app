package com.musagenius.ocrapp.presentation.ui.history

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenu
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SheetState
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.musagenius.ocrapp.domain.model.DateRangeOption
import com.musagenius.ocrapp.domain.model.FilterOptions

/**
 * Bottom sheet for filtering scans
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterBottomSheet(
    currentFilters: FilterOptions,
    availableLanguages: List<String>,
    onApplyFilters: (FilterOptions) -> Unit,
    onDismiss: () -> Unit,
    sheetState: SheetState = androidx.compose.material3.rememberModalBottomSheetState()
) {
    // Use remember with currentFilters as key to reset when filters change
    var selectedLanguage by remember(currentFilters) { mutableStateOf(currentFilters.language) }
    var selectedDateRange by remember(currentFilters) { mutableStateOf(DateRangeOption.fromDateRange(currentFilters.dateRange)) }
    var minConfidence by remember(currentFilters) { mutableFloatStateOf(currentFilters.minConfidence ?: 0f) }
    var favoritesOnly by remember(currentFilters) { mutableStateOf(currentFilters.favoritesOnly) }
    var showLanguageDropdown by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header
            Text(
                text = "Filter Scans",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Language filter
            Text(
                text = "Language",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            ExposedDropdownMenuBox(
                expanded = showLanguageDropdown,
                onExpandedChange = { showLanguageDropdown = it }
            ) {
                OutlinedTextField(
                    value = selectedLanguage?.uppercase() ?: "All Languages",
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = showLanguageDropdown)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                    colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                )

                ExposedDropdownMenu(
                    expanded = showLanguageDropdown,
                    onDismissRequest = { showLanguageDropdown = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("All Languages") },
                        onClick = {
                            selectedLanguage = null
                            showLanguageDropdown = false
                        }
                    )
                    availableLanguages.forEach { language ->
                        DropdownMenuItem(
                            text = { Text(language.uppercase()) },
                            onClick = {
                                selectedLanguage = language
                                showLanguageDropdown = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Date range filter
            Text(
                text = "Date Range",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                DateRangeOption.entries.filter { it != DateRangeOption.CUSTOM }.forEach { option ->
                    FilterChip(
                        selected = selectedDateRange == option,
                        onClick = { selectedDateRange = option },
                        label = { Text(option.displayName) },
                        leadingIcon = if (selectedDateRange == option) {
                            {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Selected",
                                    modifier = Modifier.padding(end = 4.dp)
                                )
                            }
                        } else null,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(16.dp))

            // Minimum confidence filter
            Text(
                text = "Minimum Confidence: ${(minConfidence * 100).toInt()}%",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Slider(
                value = minConfidence,
                onValueChange = { minConfidence = it },
                valueRange = 0f..1f,
                steps = 9,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Favorites only filter
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { favoritesOnly = !favoritesOnly }
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = favoritesOnly,
                    onCheckedChange = { favoritesOnly = it }
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Favorites only",
                    style = MaterialTheme.typography.bodyLarge
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        selectedLanguage = null
                        selectedDateRange = DateRangeOption.ALL_TIME
                        minConfidence = 0f
                        favoritesOnly = false
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Clear All")
                }

                Button(
                    onClick = {
                        val filters = FilterOptions(
                            language = selectedLanguage,
                            dateRange = selectedDateRange.toDateRange(),
                            minConfidence = if (minConfidence > 0f) minConfidence else null,
                            favoritesOnly = favoritesOnly
                        )
                        onApplyFilters(filters)
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Apply")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
