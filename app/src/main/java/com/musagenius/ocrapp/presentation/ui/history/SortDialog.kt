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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.musagenius.ocrapp.R
import com.musagenius.ocrapp.domain.model.SortBy
import com.musagenius.ocrapp.domain.model.getAllSortOptions

/**
 * Get display name for sort option (UI layer)
 * Uses string resources for localization support
 */
@Composable
private fun SortBy.getDisplayName(): String {
    return when (this) {
        SortBy.DATE_DESC -> stringResource(R.string.sort_date_newest)
        SortBy.DATE_ASC -> stringResource(R.string.sort_date_oldest)
        SortBy.TITLE_ASC -> stringResource(R.string.sort_title_az)
        SortBy.TITLE_DESC -> stringResource(R.string.sort_title_za)
        SortBy.CONFIDENCE_DESC -> stringResource(R.string.sort_confidence_high)
        SortBy.CONFIDENCE_ASC -> stringResource(R.string.sort_confidence_low)
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
                text = stringResource(R.string.sort_scans),
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
                Text(stringResource(R.string.apply))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}
