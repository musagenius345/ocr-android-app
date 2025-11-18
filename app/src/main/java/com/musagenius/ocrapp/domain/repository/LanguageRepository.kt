package com.musagenius.ocrapp.domain.repository

import com.musagenius.ocrapp.domain.model.Result
import com.musagenius.ocrapp.domain.model.TesseractLanguage
import kotlinx.coroutines.flow.Flow

/**
 * Repository for managing Tesseract language files
 */
interface LanguageRepository {
    /**
     * Get list of all available languages with installation status
     */
    suspend fun getAvailableLanguages(): Result<List<TesseractLanguage>>

    /**
     * Download a language file
     * @param languageCode The language code to download
     * @return Flow of download progress (0.0 to 1.0)
     */
    fun downloadLanguage(languageCode: String): Flow<Result<Float>>

    /**
     * Delete a language file
     * @param languageCode The language code to delete
     */
    suspend fun deleteLanguage(languageCode: String): Result<Unit>

    /**
     * Check if a language is installed
     */
    suspend fun isLanguageInstalled(languageCode: String): Boolean

    /**
     * Get total storage used by language files
     * @return Size in bytes
     */
    suspend fun getTotalStorageUsed(): Long

    /**
     * Get tessdata directory path
     */
    fun getTessdataPath(): String
}
