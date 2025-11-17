package com.musagenius.ocrapp.domain.usecase

import com.musagenius.ocrapp.domain.model.Result
import com.musagenius.ocrapp.domain.service.OCRService
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
    operator fun invoke(languageCode: String): Result<Boolean> {
        return try {
            val isAvailable = ocrService.isLanguageAvailable(languageCode)
            Result.success(isAvailable)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Check if multiple languages are available
     * @param languageCodes List of language codes to check
     * @return Result containing map of language codes to availability status
     */
    fun checkMultiple(languageCodes: List<String>): Result<Map<String, Boolean>> {
        return try {
            val availability = languageCodes.associateWith { code ->
                ocrService.isLanguageAvailable(code)
            }
            Result.success(availability)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Check if all specified languages are available
     * @param languageCodes List of language codes to check
     * @return Result containing true only if ALL languages are available
     */
    fun areAllAvailable(languageCodes: List<String>): Result<Boolean> {
        return try {
            val allAvailable = languageCodes.all { code ->
                ocrService.isLanguageAvailable(code)
            }
            Result.success(allAvailable)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Check if at least one of the specified languages is available
     * @param languageCodes List of language codes to check
     * @return Result containing true if at least one language is available
     */
    fun isAnyAvailable(languageCodes: List<String>): Result<Boolean> {
        return try {
            val anyAvailable = languageCodes.any { code ->
                ocrService.isLanguageAvailable(code)
            }
            Result.success(anyAvailable)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
