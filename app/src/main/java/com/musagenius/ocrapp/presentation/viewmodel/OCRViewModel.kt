package com.musagenius.ocrapp.presentation.viewmodel

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.musagenius.ocrapp.domain.model.OCRConfig
import com.musagenius.ocrapp.domain.model.ScanResult
import com.musagenius.ocrapp.domain.repository.ScanRepository
import com.musagenius.ocrapp.domain.service.OCRService
import com.musagenius.ocrapp.domain.usecase.ProcessImageUseCase
import com.musagenius.ocrapp.presentation.ui.ocr.OCREvent
import com.musagenius.ocrapp.presentation.ui.ocr.OCRUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Date
import javax.inject.Inject

/**
 * ViewModel for OCR processing screen
 */
@HiltViewModel
class OCRViewModel @Inject constructor(
    private val processImageUseCase: ProcessImageUseCase,
    private val scanRepository: ScanRepository,
    private val ocrService: OCRService
) : ViewModel() {

    private val _uiState = MutableStateFlow(OCRUiState())
    val uiState: StateFlow<OCRUiState> = _uiState.asStateFlow()

    // Track current OCR job to prevent concurrent processing
    private var currentJob: Job? = null

    companion object {
        private const val TAG = "OCRViewModel"
    }

    /**
     * Handle OCR events
     */
    fun onEvent(event: OCREvent) {
        when (event) {
            is OCREvent.ProcessImage -> processImage(event.imageUri, event.language)
            is OCREvent.RetryProcessing -> retryProcessing()
            is OCREvent.SaveToHistory -> saveToHistory()
            is OCREvent.CopyText -> {
                // Copying will be handled by UI layer
                Log.d(TAG, "Copy text requested")
            }
            is OCREvent.ShareText -> {
                // Sharing will be handled by UI layer
                Log.d(TAG, "Share text requested")
            }
            is OCREvent.DismissError -> dismissError()
            is OCREvent.NavigateBack -> {
                // Navigation will be handled by UI layer
            }
        }
    }

    /**
     * Starts OCR processing for the given image and updates the ViewModel UI state with progress and results.
     *
     * Cancels any currently running OCR job, stops the OCR service before starting, and launches a coroutine to perform OCR.
     * On success the UI state is updated with extracted text, confidence, and processing time; on failure the UI state is updated with an error.
     * Cancellation is handled to stop the OCR service and clear processing state without surfacing an error to the user.
     *
     * @param imageUri The Uri of the image to process.
     * @param language The OCR language code to use (e.g., "eng").
     */
    private fun processImage(imageUri: Uri, language: String) {
        // Cancel any ongoing OCR job to prevent concurrent processing
        currentJob?.cancel()

        // Stop any ongoing OCR processing in Tesseract
        try {
            ocrService.stop()
        } catch (e: Exception) {
            Log.w(TAG, "Error stopping previous OCR job", e)
        }

        currentJob = viewModelScope.launch {
            try {
                _uiState.update {
                    it.copy(
                        imageUri = imageUri,
                        isProcessing = true,
                        error = null,
                        language = language,
                        extractedText = "",
                        confidenceScore = 0f
                    )
                }

                val startTime = System.currentTimeMillis()

                // Create OCR config
                val config = OCRConfig(
                    language = language,
                    preprocessImage = true,
                    maxImageDimension = 2048
                )

                // Process image using the use case
                processImageUseCase(imageUri, config).collect { result ->
                    result.fold(
                        onSuccess = { ocrResult ->
                            val processingTime = System.currentTimeMillis() - startTime

                            _uiState.update {
                                it.copy(
                                    isProcessing = false,
                                    extractedText = ocrResult.text,
                                    confidenceScore = ocrResult.confidence,
                                    processingTimeMs = processingTime,
                                    error = null
                                )
                            }

                            Log.d(TAG, "OCR completed in ${processingTime}ms with confidence: ${ocrResult.confidence}")
                        },
                        onFailure = { error ->
                            Log.e(TAG, "OCR processing failed", error)
                            _uiState.update {
                                it.copy(
                                    isProcessing = false,
                                    error = "Failed to process image: ${error.message}"
                                )
                            }
                        }
                    )
                }
            } catch (e: CancellationException) {
                // Job was cancelled - stop OCR processing and clean up
                Log.d(TAG, "OCR job cancelled, cleaning up resources")
                try {
                    ocrService.stop()
                } catch (ex: Exception) {
                    Log.w(TAG, "Error stopping OCR service after cancellation", ex)
                }

                _uiState.update {
                    it.copy(
                        isProcessing = false,
                        error = null // Don't show error for user-initiated cancellation
                    )
                }

                // Re-throw to properly propagate cancellation
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Error during OCR processing", e)
                _uiState.update {
                    it.copy(
                        isProcessing = false,
                        error = "An error occurred: ${e.localizedMessage}"
                    )
                }
            }
        }

        // Add completion handler to ensure cleanup happens
        currentJob?.invokeOnCompletion { throwable ->
            if (throwable is CancellationException) {
                Log.d(TAG, "OCR job completion handler: Job was cancelled")
            } else if (throwable != null) {
                Log.e(TAG, "OCR job completion handler: Job failed with error", throwable)
            } else {
                Log.d(TAG, "OCR job completion handler: Job completed successfully")
            }
        }
    }

    /**
     * Retry processing with same image
     */
    private fun retryProcessing() {
        val imageUri = _uiState.value.imageUri
        val language = _uiState.value.language

        if (imageUri != null) {
            processImage(imageUri, language)
        }
    }

    /**
     * Save scan result to history
     */
    private fun saveToHistory() {
        viewModelScope.launch {
            try {
                val state = _uiState.value
                val imageUri = state.imageUri ?: return@launch
                val text = state.extractedText

                if (text.isEmpty()) {
                    Log.w(TAG, "Cannot save empty text to history")
                    return@launch
                }

                // Create scan result
                val scanResult = ScanResult(
                    id = 0, // Will be auto-generated by database
                    timestamp = Date(),
                    imageUri = imageUri,
                    extractedText = text,
                    language = state.language,
                    confidenceScore = state.confidenceScore,
                    title = generateTitle(text),
                    tags = emptyList(),
                    notes = "",
                    isFavorite = false,
                    modifiedTimestamp = Date()
                )

                // Save to repository
                val result = scanRepository.insert(scanResult)

                result.fold(
                    onSuccess = { id ->
                        _uiState.update { it.copy(isSaved = true) }
                        Log.d(TAG, "Scan saved to history with ID: $id")
                    },
                    onFailure = { error ->
                        Log.e(TAG, "Failed to save scan to history", error)
                        _uiState.update {
                            it.copy(error = "Failed to save: ${error.message}")
                        }
                    }
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error saving to history", e)
                _uiState.update {
                    it.copy(error = "Error saving: ${e.localizedMessage}")
                }
            }
        }
    }

    /**
     * Generate a title from extracted text
     */
    private fun generateTitle(text: String): String {
        // Take first line or first 50 characters
        val firstLine = text.lines().firstOrNull()?.trim() ?: ""
        return if (firstLine.length > 50) {
            firstLine.take(47) + "..."
        } else {
            firstLine.ifEmpty { "Untitled Scan" }
        }
    }

    /**
     * Dismiss error message
     */
    private fun dismissError() {
        _uiState.update { it.copy(error = null) }
    }

    /**
     * Trigger OCR processing for the provided image URI.
     *
     * @param uri The image Uri to process.
     * @param language The OCR language code to use; defaults to "eng".
     */
    fun setImageUri(uri: Uri, language: String = "eng") {
        processImage(uri, language)
    }

    /**
     * Cancels any ongoing OCR work, stops the OCR service, and updates the UI to reflect cancellation.
     *
     * After invocation the ViewModel stops the OCR engine (if running) and updates UI state setting `isProcessing` to `false` and `error` to `"Processing cancelled"`. Safe to call when no processing is active.
     */
    fun cancelProcessing() {
        Log.d(TAG, "Cancelling OCR processing")

        // Cancel the current job
        currentJob?.cancel()
        currentJob = null

        // Stop Tesseract processing
        try {
            ocrService.stop()
            Log.d(TAG, "OCR processing stopped successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping OCR service", e)
        }

        // Update UI state
        _uiState.update {
            it.copy(
                isProcessing = false,
                error = "Processing cancelled"
            )
        }
    }

    /**
     * Perform final cleanup when the ViewModel is destroyed.
     *
     * Cancels any ongoing OCR processing job and requests the OCR service to stop; failures while
     * stopping the OCR service are caught and logged.
     */
    override fun onCleared() {
        super.onCleared()
        Log.d(TAG, "ViewModel being cleared, cleaning up resources")

        // Cancel any ongoing processing
        currentJob?.cancel()
        currentJob = null

        // Clean up OCR service resources
        // Note: We don't call cleanup() here as OCRService is a singleton
        // and might be used by other components. We just stop any ongoing processing.
        try {
            ocrService.stop()
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping OCR service during cleanup", e)
        }
    }
}