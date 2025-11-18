package com.musagenius.ocrapp.presentation.ui.detail

import com.musagenius.ocrapp.domain.model.ScanResult

/**
 * UI state for the Scan Detail screen
 */
data class ScanDetailState(
    val scan: ScanResult? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val isEditingText: Boolean = false,
    val isEditingTitleNotes: Boolean = false,
    val editedText: String = "",
    val editedTitle: String = "",
    val editedNotes: String = "",
    val showDeleteConfirmation: Boolean = false,
    val snackbarMessage: String? = null,
    val isSaving: Boolean = false
) {
    /**
     * Check if any edits are pending
     */
    val hasUnsavedChanges: Boolean
        get() = scan?.let {
            (isEditingText && editedText != it.extractedText) ||
            (isEditingTitleNotes && (editedTitle != it.title || editedNotes != it.notes))
        } ?: false
}
