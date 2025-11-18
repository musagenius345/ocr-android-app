package com.musagenius.ocrapp.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.musagenius.ocrapp.domain.model.Result
import com.musagenius.ocrapp.domain.model.ScanResult
import com.musagenius.ocrapp.domain.usecase.DeleteScanUseCase
import com.musagenius.ocrapp.domain.usecase.GetAllScansUseCase
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
    private val deleteScanUseCase: DeleteScanUseCase
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
}
