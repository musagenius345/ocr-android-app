package com.musagenius.ocrapp.presentation.ui.history

import com.musagenius.ocrapp.domain.model.ScanResult

/**
 * UI state for the History screen
 */
data class HistoryState(
    val scans: List<ScanResult> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val searchQuery: String = "",
    val snackbarMessage: String? = null,
    val showUndoDelete: Boolean = false
) {
    /**
     * Check if the list is empty and not loading
     */
    val isEmpty: Boolean
        get() = scans.isEmpty() && !isLoading

    /**
     * Check if showing search results
     */
    val isSearching: Boolean
        get() = searchQuery.isNotBlank()
}
