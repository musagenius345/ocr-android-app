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
     * Deletes the traineddata file for the given language code from the app's tessdata directory.
     *
     * @param languageCode The ISO-like language code identifying the traineddata file (e.g., "fra"). The default English code "eng" cannot be deleted.
     * @return A Result containing `Unit` on success, or a failure with an Exception describing why deletion did not occur (for example: default language protected, file not found, or deletion failure).
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
         * Deletes multiple language data files corresponding to the provided language codes.
         *
         * @param languageCodes The list of language codes whose `.traineddata` files should be deleted.
         * @return A Result containing a map from each language code to `true` if its file was deleted, `false` otherwise.
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
     * Compute the total size of all `.traineddata` language files in the app's `tessdata` external files directory.
     *
     * @return The total size in bytes of all `.traineddata` files (0 if none). On error, returns `Result.failure` with the encountered exception.
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
     * Retrieve the size of the traineddata file for the given language code.
     *
     * @param languageCode The language code corresponding to the traineddata file (e.g., "eng", "fra").
     * @return A `Result` containing the file size in bytes; contains `0` if the file does not exist. On error, returns a failure `Result` containing the caught exception.
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