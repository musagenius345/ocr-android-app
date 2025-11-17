package com.musagenius.ocrapp.domain.usecase

import com.musagenius.ocrapp.domain.model.OCRConfig
import com.musagenius.ocrapp.domain.model.Result
import com.musagenius.ocrapp.domain.service.OCRService
import javax.inject.Inject

/**
 * Use case for initializing the OCR engine
 */
class InitializeOCRUseCase @Inject constructor(
    private val ocrService: OCRService
) {
    /**
     * Initialize OCR engine with configuration
     * @param config OCR configuration
     * @return Result indicating success or failure
     */
    suspend operator fun invoke(config: OCRConfig = OCRConfig()): Result<Unit> {
        return ocrService.initialize(config)
    }

    /**
     * Check if a language is available
     */
    fun isLanguageAvailable(language: String): Boolean {
        return ocrService.isLanguageAvailable(language)
    }

    /**
     * Get list of available languages
     */
    fun getAvailableLanguages(): List<String> {
        return ocrService.getAvailableLanguages()
    }
}
