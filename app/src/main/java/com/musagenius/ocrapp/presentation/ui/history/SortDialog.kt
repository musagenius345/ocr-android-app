package com.musagenius.ocrapp.presentation.ui.history

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.musagenius.ocrapp.domain.model.SortBy
import com.musagenius.ocrapp.domain.model.getAllSortOptions

/**
 * Get display name for sort option (UI layer)
 */
private fun SortBy.getDisplayName(): String {
    return when (this) {
        SortBy.DATE_DESC -> "Date (Newest First)"
        SortBy.DATE_ASC -> "Date (Oldest First)"
        SortBy.TITLE_ASC -> "Title (A-Z)"
        SortBy.TITLE_DESC -> "Title (Z-A)"
        SortBy.CONFIDENCE_DESC -> "Confidence (High to Low)"
        SortBy.CONFIDENCE_ASC -> "Confidence (Low to High)"
    }
}

/**
 * Dialog for selecting sort order
 */
@Composable
fun SortDialog(
    currentSortBy: SortBy,
    onSortSelected: (SortBy) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedSort by remember { mutableStateOf(currentSortBy) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Sort Scans",
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                getAllSortOptions().forEach { sortOption ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                selectedSort = sortOption
                            }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedSort == sortOption,
                            onClick = { selectedSort = sortOption }
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = sortOption.getDisplayName(),
                            style = MaterialTheme.typography.bodyLarge
                        )
                        if (selectedSort == sortOption) {
                            Spacer(modifier = Modifier.weight(1f))
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Selected",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onSortSelected(selectedSort)
                }
            ) {
                Text("Apply")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
