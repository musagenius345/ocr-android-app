package com.musagenius.ocrapp.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.musagenius.ocrapp.domain.model.Result
import com.musagenius.ocrapp.domain.model.TesseractLanguage
import com.musagenius.ocrapp.domain.usecase.DeleteLanguageUseCase
import com.musagenius.ocrapp.domain.usecase.DownloadLanguageUseCase
import com.musagenius.ocrapp.domain.usecase.GetAvailableLanguagesUseCase
import com.musagenius.ocrapp.domain.usecase.GetLanguageStorageUseCase
import com.musagenius.ocrapp.presentation.ui.language.LanguageState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for Language Management screen
 */
@HiltViewModel
class LanguageViewModel @Inject constructor(
    private val getAvailableLanguagesUseCase: GetAvailableLanguagesUseCase,
    private val downloadLanguageUseCase: DownloadLanguageUseCase,
    private val deleteLanguageUseCase: DeleteLanguageUseCase,
    private val getLanguageStorageUseCase: GetLanguageStorageUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(LanguageState())
    val state: StateFlow<LanguageState> = _state.asStateFlow()

    private var downloadJob: Job? = null

    init {
        loadLanguages()
    }

    /**
     * Load available languages
     */
    fun loadLanguages() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            when (val result = getAvailableLanguagesUseCase()) {
                is Result.Success -> {
                    val storageUsed = getLanguageStorageUseCase()
                    // Map Language to TesseractLanguage
                    val tesseractLanguages = result.data.map { lang ->
                        TesseractLanguage(
                            code = lang.code,
                            name = lang.displayName,
                            downloadUrl = "", // URL would come from a different source
                            fileSizeBytes = lang.fileSize,
                            isInstalled = lang.isInstalled
                        )
                    }
                    _state.update {
                        it.copy(
                            languages = tesseractLanguages,
                            totalStorageUsed = storageUsed,
                            isLoading = false,
                            error = null
                        )
                    }
                }
                is Result.Error -> {
                    _state.update {
                        it.copy(
                            isLoading = false,
                            error = result.exception.message ?: "Failed to load languages"
                        )
                    }
                }
                else -> {
                    _state.update { it.copy(isLoading = false) }
                }
            }
        }
    }

    /**
     * Download a language
     */
    fun downloadLanguage(languageCode: String) {
        // Cancel any existing download
        downloadJob?.cancel()

        downloadJob = viewModelScope.launch {
            _state.update {
                it.copy(
                    downloadingLanguageCode = languageCode,
                    downloadProgress = 0f,
                    error = null
                )
            }

            downloadLanguageUseCase(languageCode).collect { result ->
                when (result) {
                    is Result.Success -> {
                        val progress = result.data
                        _state.update { it.copy(downloadProgress = progress) }

                        // Download complete
                        if (progress >= 1.0f) {
                            _state.update {
                                it.copy(
                                    downloadingLanguageCode = null,
                                    downloadProgress = 0f,
                                    snackbarMessage = "Language downloaded successfully"
                                )
                            }
                            // Reload languages to update installation status
                            loadLanguages()
                        }
                    }
                    is Result.Error -> {
                        _state.update {
                            it.copy(
                                downloadingLanguageCode = null,
                                downloadProgress = 0f,
                                error = result.exception.message,
                                snackbarMessage = "Download failed: ${result.exception.message}"
                            )
                        }
                    }
                    else -> { /* No-op */ }
                }
            }
        }
    }

    /**
     * Cancel current download
     */
    fun cancelDownload() {
        downloadJob?.cancel()
        _state.update {
            it.copy(
                downloadingLanguageCode = null,
                downloadProgress = 0f,
                snackbarMessage = "Download cancelled"
            )
        }
    }

    /**
     * Show delete confirmation dialog
     */
    fun showDeleteConfirmation(languageCode: String) {
        _state.update { it.copy(showDeleteConfirmation = languageCode) }
    }

    /**
     * Hide delete confirmation dialog
     */
    fun hideDeleteConfirmation() {
        _state.update { it.copy(showDeleteConfirmation = null) }
    }

    /**
     * Delete a language
     */
    fun deleteLanguage(languageCode: String) {
        viewModelScope.launch {
            _state.update { it.copy(showDeleteConfirmation = null) }

            when (val result = deleteLanguageUseCase(languageCode)) {
                is Result.Success -> {
                    _state.update {
                        it.copy(snackbarMessage = "Language deleted successfully")
                    }
                    // Reload languages to update installation status
                    loadLanguages()
                }
                is Result.Error -> {
                    _state.update {
                        it.copy(
                            error = result.exception.message,
                            snackbarMessage = "Failed to delete language"
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
