package com.musagenius.ocrapp.domain.usecase

import android.content.Context
import com.musagenius.ocrapp.domain.model.Result
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

/**
 * Use case for deleting language data files to free up storage
 */
class DeleteLanguageUseCase @Inject constructor(
    @ApplicationContext private val context: Context
) {
    /**
     * Delete a language data file
     * @param languageCode The language code to delete (e.g., "eng", "fra")
     * @return Result indicating success or failure
     */
    suspend operator fun invoke(languageCode: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // Don't allow deleting English as it's the default
            if (languageCode == "eng") {
                return@withContext Result.failure(
                    IllegalArgumentException("Cannot delete default language (English)")
                )
            }

            // Get tessdata directory path
            val tessdataPath = File(context.getExternalFilesDir(null), "tessdata")
            val languageFile = File(tessdataPath, "$languageCode.traineddata")

            // Check if file exists
            if (!languageFile.exists()) {
                return@withContext Result.failure(
                    IllegalArgumentException("Language file not found: $languageCode")
                )
            }

            // Delete the file
            val deleted = languageFile.delete()

            if (deleted) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to delete language file: $languageCode"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Delete multiple language data files
     * @param languageCodes List of language codes to delete
     * @return Result containing map of language codes to deletion status (true if deleted)
     */
    suspend fun deleteMultiple(languageCodes: List<String>): Result<Map<String, Boolean>> =
        withContext(Dispatchers.IO) {
            try {
                val results = languageCodes.associateWith { code ->
                    invoke(code).fold(
                        onSuccess = { true },
                        onFailure = { false }
                    )
                }
                Result.success(results)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    /**
     * Calculate total size of all language files
     * @return Result containing total size in bytes
     */
    suspend fun getTotalLanguageFilesSize(): Result<Long> = withContext(Dispatchers.IO) {
        try {
            val tessdataPath = File(context.getExternalFilesDir(null), "tessdata")

            if (!tessdataPath.exists()) {
                return@withContext Result.success(0L)
            }

            val totalSize = tessdataPath.listFiles()
                ?.filter { it.extension == "traineddata" }
                ?.sumOf { it.length() } ?: 0L

            Result.success(totalSize)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get size of a specific language file
     * @param languageCode The language code to check
     * @return Result containing file size in bytes (0 if not found)
     */
    suspend fun getLanguageFileSize(languageCode: String): Result<Long> = withContext(Dispatchers.IO) {
        try {
            val tessdataPath = File(context.getExternalFilesDir(null), "tessdata")
            val languageFile = File(tessdataPath, "$languageCode.traineddata")

            val size = if (languageFile.exists()) languageFile.length() else 0L
            Result.success(size)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
