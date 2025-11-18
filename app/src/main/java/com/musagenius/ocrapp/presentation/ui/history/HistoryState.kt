package com.musagenius.ocrapp.presentation.ui.history

import com.musagenius.ocrapp.domain.model.FilterOptions
import com.musagenius.ocrapp.domain.model.ScanResult
import com.musagenius.ocrapp.domain.model.SortBy

/**
 * UI state for the History screen
 */
data class HistoryState(
    val scans: List<ScanResult> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val searchQuery: String = "",
    val sortBy: SortBy = SortBy.DATE_DESC,
    val filterOptions: FilterOptions = FilterOptions(),
    val snackbarMessage: String? = null,
    val showUndoDelete: Boolean = false,
    val showFilterSheet: Boolean = false,
    val showSortDialog: Boolean = false,
    val availableLanguages: List<String> = emptyList()
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

    /**
     * Check if any filters are active
     */
    val hasActiveFilters: Boolean
        get() = filterOptions.isActive()

    /**
     * Get total results count message
     */
    fun getResultsCountMessage(): String {
        val count = scans.size
        return when {
            isSearching && hasActiveFilters -> "$count result${if (count != 1) "s" else ""} for \"$searchQuery\" with filters"
            isSearching -> "$count result${if (count != 1) "s" else ""} for \"$searchQuery\""
            hasActiveFilters -> "$count scan${if (count != 1) "s" else ""} with filters applied"
            else -> "$count scan${if (count != 1) "s" else ""}"
        }
    }
}
