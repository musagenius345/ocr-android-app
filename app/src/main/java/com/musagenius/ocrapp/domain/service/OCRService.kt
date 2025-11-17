package com.musagenius.ocrapp.domain.service

import android.graphics.Bitmap
import com.musagenius.ocrapp.domain.model.OCRConfig
import com.musagenius.ocrapp.domain.model.OCRProgress
import com.musagenius.ocrapp.domain.model.OCRResult
import com.musagenius.ocrapp.domain.model.Result
import kotlinx.coroutines.flow.Flow

/**
 * Domain interface for OCR service
 * Defines the contract for OCR operations without exposing implementation details
 */
interface OCRService {
    /**
     * Initialize Tesseract with the specified configuration
     */
    suspend fun initialize(config: OCRConfig = OCRConfig()): Result<Unit>

    /**
     * Recognizes text contained in the provided bitmap image.
     *
     * @param bitmap The image to perform OCR on.
     * @param config Optional OCR configuration to use for this recognition; defaults to the service default.
     * @return A `Result` wrapping an `OCRResult` on success, or a failure describing the error. 
     */
    suspend fun recognizeText(
        bitmap: Bitmap,
        config: OCRConfig = OCRConfig()
    ): Result<OCRResult>

    /**
     * Performs OCR on the provided bitmap while emitting progress updates.
     *
     * @param bitmap The image to run OCR on.
     * @param config Optional OCR configuration; defaults to a new OCRConfig instance.
     * @return A Flow that emits `Result<OCRProgress>` values representing intermediate progress updates and a final `Result` containing the completed OCR result or an error.
     */
    fun recognizeTextWithProgress(
        bitmap: Bitmap,
        config: OCRConfig = OCRConfig()
    ): Flow<Result<OCRProgress>>

    /**
     * Check if a language is available
     */
    fun isLanguageAvailable(language: String): Boolean

    /**
     * Get list of available languages
     */
    fun getAvailableLanguages(): List<String>

    /**
     * Stop OCR processing (if running)
     */
    fun stop()

    /**
     * Clean up resources
     */
    fun cleanup()

    /**
     * Get Tesseract version
     */
    fun getVersion(): String
}