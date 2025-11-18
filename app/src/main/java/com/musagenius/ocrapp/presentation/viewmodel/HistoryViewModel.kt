package com.musagenius.ocrapp.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.musagenius.ocrapp.domain.model.FilterOptions
import com.musagenius.ocrapp.domain.model.Result
import com.musagenius.ocrapp.domain.model.ScanResult
import com.musagenius.ocrapp.domain.model.SortBy
import com.musagenius.ocrapp.domain.usecase.DeleteScanUseCase
import com.musagenius.ocrapp.domain.usecase.GetAllScansUseCase
import com.musagenius.ocrapp.domain.usecase.GetScansByDateRangeUseCase
import com.musagenius.ocrapp.domain.usecase.GetScansByLanguageUseCase
import com.musagenius.ocrapp.domain.usecase.SearchScansUseCase
import com.musagenius.ocrapp.presentation.ui.history.HistoryState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the History screen
 * Manages scan history, search, and deletion
 */
@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val getAllScansUseCase: GetAllScansUseCase,
    private val searchScansUseCase: SearchScansUseCase,
    private val deleteScanUseCase: DeleteScanUseCase,
    private val getScansByLanguageUseCase: GetScansByLanguageUseCase,
    private val getScansByDateRangeUseCase: GetScansByDateRangeUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(HistoryState())
    val state: StateFlow<HistoryState> = _state.asStateFlow()

    private var searchJob: Job? = null
    private var recentlyDeletedScan: ScanResult? = null

    init {
        loadScans()
    }

    /**
     * Load all scans from the repository
     */
    fun loadScans() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            getAllScansUseCase().collect { result ->
                when (result) {
                    is Result.Success -> {
                        _state.update {
                            it.copy(
                                scans = result.data,
                                isLoading = false,
                                error = null
                            )
                        }
                    }
                    is Result.Error -> {
                        _state.update {
                            it.copy(
                                isLoading = false,
                                error = result.exception.message ?: "Failed to load scans"
                            )
                        }
                    }
                    is Result.Loading -> {
                        _state.update { it.copy(isLoading = true) }
                    }
                }
            }
        }
    }

    /**
     * Search scans with debouncing
     */
    fun searchScans(query: String) {
        _state.update { it.copy(searchQuery = query) }

        // Cancel previous search job
        searchJob?.cancel()

        // If query is empty, load all scans
        if (query.isBlank()) {
            loadScans()
            return
        }

        // Debounce search
        searchJob = viewModelScope.launch {
            delay(300) // Wait 300ms before searching

            searchScansUseCase(query).collect { result ->
                when (result) {
                    is Result.Success -> {
                        _state.update {
                            it.copy(
                                scans = result.data,
                                isLoading = false,
                                error = null
                            )
                        }
                    }
                    is Result.Error -> {
                        _state.update {
                            it.copy(
                                isLoading = false,
                                error = result.exception.message ?: "Search failed"
                            )
                        }
                    }
                    is Result.Loading -> {
                        _state.update { it.copy(isLoading = true) }
                    }
                }
            }
        }
    }

    /**
     * Delete a scan
     */
    fun deleteScan(scan: ScanResult) {
        viewModelScope.launch {
            recentlyDeletedScan = scan

            when (val result = deleteScanUseCase(scan.id)) {
                is Result.Success -> {
                    // Show snackbar with undo option
                    _state.update {
                        it.copy(
                            snackbarMessage = "Scan deleted",
                            showUndoDelete = true
                        )
                    }
                }
                is Result.Error -> {
                    _state.update {
                        it.copy(
                            error = result.exception.message ?: "Failed to delete scan",
                            snackbarMessage = "Failed to delete scan"
                        )
                    }
                }
                else -> { /* No-op */ }
            }
        }
    }

    /**
     * Undo delete operation
     */
    fun undoDelete() {
        // TODO: Implement undo delete by re-inserting the scan
        // For now, just refresh the list
        recentlyDeletedScan = null
        _state.update {
            it.copy(
                showUndoDelete = false,
                snackbarMessage = null
            )
        }
        loadScans()
    }

    /**
     * Clear snackbar message
     */
    fun clearSnackbar() {
        _state.update {
            it.copy(
                snackbarMessage = null,
                showUndoDelete = false
            )
        }
    }

    /**
     * Clear error message
     */
    fun clearError() {
        _state.update { it.copy(error = null) }
    }

    /**
     * Apply filters to scans
     */
    fun applyFilter(filterOptions: FilterOptions) {
        _state.update { it.copy(filterOptions = filterOptions, showFilterSheet = false) }
        loadFilteredScans()
    }

    /**
     * Clear all filters
     */
    fun clearFilters() {
        _state.update { it.copy(filterOptions = FilterOptions()) }
        loadScans()
    }

    /**
     * Change sort order
     */
    fun changeSortOrder(sortBy: SortBy) {
        _state.update { it.copy(sortBy = sortBy, showSortDialog = false) }
        applySortingToCurrentScans()
    }

    /**
     * Toggle filter sheet visibility
     */
    fun toggleFilterSheet() {
        _state.update { it.copy(showFilterSheet = !it.showFilterSheet) }
    }

    /**
     * Toggle sort dialog visibility
     */
    fun toggleSortDialog() {
        _state.update { it.copy(showSortDialog = !it.showSortDialog) }
    }

    /**
     * Load scans with applied filters
     */
    private fun loadFilteredScans() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            val currentState = _state.value
            val filters = currentState.filterOptions

            // Determine which flow to use based on filters
            val scansFlow = when {
                filters.language != null -> getScansByLanguageUseCase(filters.language)
                filters.dateRange != null -> getScansByDateRangeUseCase(filters.dateRange)
                else -> getAllScansUseCase()
            }

            scansFlow.collect { result ->
                when (result) {
                    is Result.Success -> {
                        var filteredScans = result.data

                        // Apply additional filters in memory
                        if (filters.minConfidence != null) {
                            filteredScans = filteredScans.filter { it.confidenceScore >= filters.minConfidence }
                        }
                        if (filters.favoritesOnly) {
                            filteredScans = filteredScans.filter { it.isFavorite }
                        }

                        // Apply current search query if active
                        if (currentState.searchQuery.isNotBlank()) {
                            filteredScans = filteredScans.filter {
                                it.extractedText.contains(currentState.searchQuery, ignoreCase = true) ||
                                it.title.contains(currentState.searchQuery, ignoreCase = true)
                            }
                        }

                        // Apply sorting
                        filteredScans = applySorting(filteredScans, currentState.sortBy)

                        // Extract unique languages for filter options
                        val languages = result.data.map { it.language }.distinct().sorted()

                        _state.update {
                            it.copy(
                                scans = filteredScans,
                                availableLanguages = languages,
                                isLoading = false,
                                error = null
                            )
                        }
                    }
                    is Result.Error -> {
                        _state.update {
                            it.copy(
                                isLoading = false,
                                error = result.exception.message ?: "Failed to load scans"
                            )
                        }
                    }
                    is Result.Loading -> {
                        _state.update { it.copy(isLoading = true) }
                    }
                }
            }
        }
    }

    /**
     * Apply sorting to current scans in memory
     */
    private fun applySortingToCurrentScans() {
        _state.update {
            it.copy(scans = applySorting(it.scans, it.sortBy))
        }
    }

    /**
     * Apply sorting to a list of scans
     */
    private fun applySorting(scans: List<ScanResult>, sortBy: SortBy): List<ScanResult> {
        return when (sortBy) {
            SortBy.DATE_DESC -> scans.sortedByDescending { it.timestamp }
            SortBy.DATE_ASC -> scans.sortedBy { it.timestamp }
            SortBy.TITLE_ASC -> scans.sortedBy { it.title.ifEmpty { it.extractedText.take(50) } }
            SortBy.TITLE_DESC -> scans.sortedByDescending { it.title.ifEmpty { it.extractedText.take(50) } }
            SortBy.CONFIDENCE_DESC -> scans.sortedByDescending { it.confidenceScore }
            SortBy.CONFIDENCE_ASC -> scans.sortedBy { it.confidenceScore }
        }
    }
}
