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
     * Determines whether the specified OCR language is available.
     *
     * @param languageCode The language code to check (e.g., "eng", "fra").
     * @return `true` if the language is available, `false` otherwise.
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
     * Determine availability for each provided language code.
     *
     * @param languageCodes List of language codes to check.
     * @return A map from each language code to `true` if that language is available, `false` otherwise.
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
     * Determines whether all specified languages are available.
     *
     * @param languageCodes List of language codes to check.
     * @return `true` if all specified languages are available, `false` otherwise.
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
     * Determine whether any of the specified language codes is available.
     *
     * @param languageCodes The language codes to check.
     * @return `true` if at least one of the specified languages is available, `false` otherwise.
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