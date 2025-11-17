package com.musagenius.ocrapp.domain.service

import android.graphics.Bitmap
import com.musagenius.ocrapp.domain.model.OCRConfig
import com.musagenius.ocrapp.domain.model.OCRResult
import com.musagenius.ocrapp.domain.model.Result

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
     * Perform OCR on the given bitmap
     */
    suspend fun recognizeText(
        bitmap: Bitmap,
        config: OCRConfig = OCRConfig()
    ): Result<OCRResult>

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
