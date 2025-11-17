package com.musagenius.ocrapp.presentation.ui.ocr

import android.net.Uri

/**
 * UI state for OCR result screen
 */
data class OCRUiState(
    val imageUri: Uri? = null,
    val extractedText: String = "",
    val isProcessing: Boolean = false,
    val processingProgress: Float = 0f,
    val error: String? = null,
    val confidenceScore: Float = 0f,
    val language: String = "eng",
    val processingTimeMs: Long = 0,
    val isSaved: Boolean = false
)

/**
 * OCR processing status
 */
sealed class OCRProcessingStatus {
    data object Idle : OCRProcessingStatus()
    data class Processing(val progress: Float = 0f, val stage: String = "") : OCRProcessingStatus()
    data class Success(val text: String, val confidence: Float) : OCRProcessingStatus()
    data class Error(val message: String) : OCRProcessingStatus()
}

/**
 * OCR events from UI
 */
sealed class OCREvent {
    data class ProcessImage(val imageUri: Uri, val language: String = "eng") : OCREvent()
    data object RetryProcessing : OCREvent()
    data object SaveToHistory : OCREvent()
    data class CopyText(val text: String) : OCREvent()
    data class ShareText(val text: String) : OCREvent()
    data object DismissError : OCREvent()
    data object NavigateBack : OCREvent()
}
