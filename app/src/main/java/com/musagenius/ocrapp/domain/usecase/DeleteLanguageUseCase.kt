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
     * Get tessdata directory, returns null if external files dir is unavailable
     */
    private fun tessdataDir(): File? {
        val externalDir = context.getExternalFilesDir(null) ?: return null
        return File(externalDir, "tessdata")
    }

    /**
     * Delete a language data file
     * @param languageCode The language code to delete (e.g., "eng", "fra")
     * @return Result indicating success or failure
     */
    suspend operator fun invoke(languageCode: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // Don't allow deleting English as it's the default
            if (languageCode == "eng") {
                return@withContext Result.error(
                    IllegalArgumentException("Cannot delete default language (English)"),
                    "Cannot delete default language (English)"
                )
            }

            // Get tessdata directory path
            val tessdataPath = tessdataDir()
                ?: return@withContext Result.error(
                    IllegalStateException("External files directory not available"),
                    "External files directory not available"
                )

            val deleted = deleteLanguageFileInternal(tessdataPath, languageCode)
            if (deleted) {
                Result.success(Unit)
            } else {
                Result.error(
                    Exception("Failed to delete language file: $languageCode"),
                    "Failed to delete language file: $languageCode"
                )
            }
        } catch (e: Exception) {
            Result.error(e, e.message ?: "Failed to delete language file")
        }
    }

    /**
     * Delete a language file (non-suspending, assumes caller is on IO dispatcher)
     * @return true if deleted, false if not found or failed
     */
    private fun deleteLanguageFileInternal(tessdataPath: File, languageCode: String): Boolean {
        val languageFile = File(tessdataPath, "$languageCode.traineddata")

        // Check if file exists
        if (!languageFile.exists()) {
            throw IllegalArgumentException("Language file not found: $languageCode")
        }

        // Delete the file
        return languageFile.delete()
    }

    /**
     * Delete multiple language data files
     * @param languageCodes List of language codes to delete
     * @return Result containing map of language codes to deletion status (true if deleted)
     */
    suspend fun deleteMultiple(languageCodes: List<String>): Result<Map<String, Boolean>> {
        return try {
            // Let invoke() manage the IO dispatcher to avoid nested contexts
            val results = languageCodes.associateWith { code ->
                val result = invoke(code)
                result.isSuccess()
            }
            Result.success(results)
        } catch (e: Exception) {
            Result.error(e, e.message ?: "Failed to delete multiple language files")
        }
    }

    /**
     * Calculate total size of all language files
     * @return Result containing total size in bytes
     */
    suspend fun getTotalLanguageFilesSize(): Result<Long> = withContext(Dispatchers.IO) {
        try {
            val tessdataPath = tessdataDir()
                ?: return@withContext Result.success(0L)

            if (!tessdataPath.exists()) {
                return@withContext Result.success(0L)
            }

            val totalSize = tessdataPath.listFiles()
                ?.filter { it.extension == "traineddata" }
                ?.sumOf { it.length() } ?: 0L

            Result.success(totalSize)
        } catch (e: Exception) {
            Result.error(e, e.message ?: "Failed to calculate total language files size")
        }
    }

    /**
     * Get size of a specific language file
     * @param languageCode The language code to check
     * @return Result containing file size in bytes (0 if not found)
     */
    suspend fun getLanguageFileSize(languageCode: String): Result<Long> = withContext(Dispatchers.IO) {
        try {
            val tessdataPath = tessdataDir()
                ?: return@withContext Result.success(0L)

            val languageFile = File(tessdataPath, "$languageCode.traineddata")

            val size = if (languageFile.exists()) languageFile.length() else 0L
            Result.success(size)
        } catch (e: Exception) {
            Result.error(e, e.message ?: "Failed to get language file size")
        }
    }
}
