package com.musagenius.ocrapp.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.musagenius.ocrapp.domain.model.Result
import com.musagenius.ocrapp.domain.usecase.DeleteScanUseCase
import com.musagenius.ocrapp.domain.usecase.GetScanByIdUseCase
import com.musagenius.ocrapp.domain.usecase.UpdateExtractedTextUseCase
import com.musagenius.ocrapp.domain.usecase.UpdateFavoriteStatusUseCase
import com.musagenius.ocrapp.domain.usecase.UpdateTitleAndNotesUseCase
import com.musagenius.ocrapp.presentation.ui.detail.ScanDetailState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the Scan Detail screen
 * Manages scan display, editing, and actions
 */
@HiltViewModel
class ScanDetailViewModel @Inject constructor(
    private val getScanByIdUseCase: GetScanByIdUseCase,
    private val updateExtractedTextUseCase: UpdateExtractedTextUseCase,
    private val updateTitleAndNotesUseCase: UpdateTitleAndNotesUseCase,
    private val updateFavoriteStatusUseCase: UpdateFavoriteStatusUseCase,
    private val deleteScanUseCase: DeleteScanUseCase,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val scanId: Long?

    private val _state = MutableStateFlow(ScanDetailState())
    val state: StateFlow<ScanDetailState> = _state.asStateFlow()

    init {
        scanId = savedStateHandle.get<String>("scanId")?.toLongOrNull()
        if (scanId == null) {
            _state.update {
                it.copy(
                    error = "Invalid scan ID",
                    isLoading = false
                )
            }
        } else {
            loadScan()
        }
    }

    /**
     * Load the scan details
     */
    private fun loadScan() {
        val id = scanId ?: return

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            getScanByIdUseCase(id).collect { result ->
                when (result) {
                    is Result.Success -> {
                        _state.update {
                            it.copy(
                                scan = result.data,
                                editedText = result.data.extractedText,
                                editedTitle = result.data.title,
                                editedNotes = result.data.notes,
                                isLoading = false,
                                error = null
                            )
                        }
                    }
                    is Result.Error -> {
                        _state.update {
                            it.copy(
                                isLoading = false,
                                error = result.exception.message ?: "Failed to load scan"
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
     * Reload the scan (for retry after error)
     */
    fun reloadScan() {
        loadScan()
    }

    /**
     * Start editing extracted text
     */
    fun startEditingText() {
        _state.update {
            it.copy(
                isEditingText = true,
                editedText = it.scan?.extractedText ?: ""
            )
        }
    }

    /**
     * Update the edited text
     */
    fun updateEditedText(text: String) {
        _state.update { it.copy(editedText = text) }
    }

    /**
     * Save the edited text
     */
    fun saveEditedText() {
        val currentScan = _state.value.scan ?: return
        val newText = _state.value.editedText

        viewModelScope.launch {
            _state.update { it.copy(isSaving = true) }

            when (val result = updateExtractedTextUseCase(currentScan.id, newText)) {
                is Result.Success -> {
                    // Reload scan to refresh state with updated text
                    loadScan()
                    _state.update {
                        it.copy(
                            isEditingText = false,
                            isSaving = false,
                            snackbarMessage = "Text updated"
                        )
                    }
                }
                is Result.Error -> {
                    _state.update {
                        it.copy(
                            isSaving = false,
                            snackbarMessage = "Failed to save text",
                            error = result.exception.message
                        )
                    }
                }
                else -> {
                    _state.update { it.copy(isSaving = false) }
                }
            }
        }
    }

    /**
     * Cancel editing text
     */
    fun cancelEditingText() {
        _state.update {
            it.copy(
                isEditingText = false,
                editedText = it.scan?.extractedText ?: ""
            )
        }
    }

    /**
     * Start editing title and notes
     */
    fun startEditingTitleNotes() {
        _state.update {
            it.copy(
                isEditingTitleNotes = true,
                editedTitle = it.scan?.title ?: "",
                editedNotes = it.scan?.notes ?: ""
            )
        }
    }

    /**
     * Update the edited title
     */
    fun updateEditedTitle(title: String) {
        _state.update { it.copy(editedTitle = title) }
    }

    /**
     * Update the edited notes
     */
    fun updateEditedNotes(notes: String) {
        _state.update { it.copy(editedNotes = notes) }
    }

    /**
     * Save the edited title and notes
     */
    fun saveEditedTitleNotes() {
        val currentScan = _state.value.scan ?: return
        val newTitle = _state.value.editedTitle
        val newNotes = _state.value.editedNotes

        viewModelScope.launch {
            _state.update { it.copy(isSaving = true) }

            when (val result = updateTitleAndNotesUseCase(currentScan.id, newTitle, newNotes)) {
                is Result.Success -> {
                    // Reload scan to refresh state with updated title/notes
                    loadScan()
                    _state.update {
                        it.copy(
                            isEditingTitleNotes = false,
                            isSaving = false,
                            snackbarMessage = "Title and notes updated"
                        )
                    }
                }
                is Result.Error -> {
                    _state.update {
                        it.copy(
                            isSaving = false,
                            snackbarMessage = "Failed to save changes",
                            error = result.exception.message
                        )
                    }
                }
                else -> {
                    _state.update { it.copy(isSaving = false) }
                }
            }
        }
    }

    /**
     * Cancel editing title and notes
     */
    fun cancelEditingTitleNotes() {
        _state.update {
            it.copy(
                isEditingTitleNotes = false,
                editedTitle = it.scan?.title ?: "",
                editedNotes = it.scan?.notes ?: ""
            )
        }
    }

    /**
     * Toggle favorite status
     */
    fun toggleFavorite() {
        val currentScan = _state.value.scan ?: return
        val newFavoriteStatus = !currentScan.isFavorite

        viewModelScope.launch {
            when (val result = updateFavoriteStatusUseCase(currentScan.id, newFavoriteStatus)) {
                is Result.Success -> {
                    // Reload scan to refresh state with updated favorite status
                    loadScan()
                    _state.update {
                        it.copy(
                            snackbarMessage = if (newFavoriteStatus) "Added to favorites" else "Removed from favorites"
                        )
                    }
                }
                is Result.Error -> {
                    _state.update {
                        it.copy(
                            snackbarMessage = "Failed to update favorite status",
                            error = result.exception.message
                        )
                    }
                }
                else -> { /* No-op */ }
            }
        }
    }

    /**
     * Show delete confirmation dialog
     */
    fun showDeleteConfirmation() {
        _state.update { it.copy(showDeleteConfirmation = true) }
    }

    /**
     * Hide delete confirmation dialog
     */
    fun hideDeleteConfirmation() {
        _state.update { it.copy(showDeleteConfirmation = false) }
    }

    /**
     * Delete the scan
     */
    fun deleteScan(onDeleted: () -> Unit) {
        val currentScan = _state.value.scan ?: return

        viewModelScope.launch {
            when (val result = deleteScanUseCase(currentScan.id)) {
                is Result.Success -> {
                    onDeleted()
                }
                is Result.Error -> {
                    _state.update {
                        it.copy(
                            showDeleteConfirmation = false,
                            snackbarMessage = "Failed to delete scan",
                            error = result.exception.message
                        )
                    }
                }
                else -> { /* No-op */ }
            }
        }
    }

    /**
     * Clear snackbar message
     */
    fun clearSnackbar() {
        _state.update { it.copy(snackbarMessage = null) }
    }

    /**
     * Clear error message
     */
    fun clearError() {
        _state.update { it.copy(error = null) }
    }
}
