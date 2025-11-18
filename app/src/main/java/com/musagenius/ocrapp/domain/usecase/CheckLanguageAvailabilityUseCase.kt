package com.musagenius.ocrapp.domain.usecase

import com.musagenius.ocrapp.domain.model.Result
import com.musagenius.ocrapp.domain.service.OCRService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Use case for checking if a language is available for OCR
 */
class CheckLanguageAvailabilityUseCase @Inject constructor(
    private val ocrService: OCRService
) {
    /**
     * Check if a language is available
     * @param languageCode The language code to check (e.g., "eng", "fra")
     * @return Result containing true if language is available, false otherwise
     */
    suspend operator fun invoke(languageCode: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val isAvailable = ocrService.isLanguageAvailable(languageCode)
            Result.success(isAvailable)
        } catch (e: Exception) {
            Result.error(e, "Failed to check language availability: ${e.message}")
        }
    }

    /**
     * Check if multiple languages are available
     * @param languageCodes List of language codes to check
     * @return Result containing map of language codes to availability status
     */
    suspend fun checkMultiple(languageCodes: List<String>): Result<Map<String, Boolean>> = withContext(Dispatchers.IO) {
        try {
            val availability = languageCodes.associateWith { code ->
                ocrService.isLanguageAvailable(code)
            }
            Result.success(availability)
        } catch (e: Exception) {
            Result.error(e, "Failed to check multiple languages: ${e.message}")
        }
    }

    /**
     * Check if all specified languages are available
     * @param languageCodes List of language codes to check
     * @return Result containing true only if ALL languages are available
     */
    suspend fun areAllAvailable(languageCodes: List<String>): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val allAvailable = languageCodes.all { code ->
                ocrService.isLanguageAvailable(code)
            }
            Result.success(allAvailable)
        } catch (e: Exception) {
            Result.error(e, "Failed to check if all languages are available: ${e.message}")
        }
    }

    /**
     * Check if at least one of the specified languages is available
     * @param languageCodes List of language codes to check
     * @return Result containing true if at least one language is available
     */
    suspend fun isAnyAvailable(languageCodes: List<String>): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val anyAvailable = languageCodes.any { code ->
                ocrService.isLanguageAvailable(code)
            }
            Result.success(anyAvailable)
        } catch (e: Exception) {
            Result.error(e, "Failed to check if any language is available: ${e.message}")
        }
    }
}
