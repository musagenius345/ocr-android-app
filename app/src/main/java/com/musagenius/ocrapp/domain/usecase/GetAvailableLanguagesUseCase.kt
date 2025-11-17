package com.musagenius.ocrapp.domain.usecase

import android.content.Context
import com.musagenius.ocrapp.domain.model.Language
import com.musagenius.ocrapp.domain.model.Result
import com.musagenius.ocrapp.domain.service.OCRService
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

/**
 * Use case for getting list of available OCR languages
 */
class GetAvailableLanguagesUseCase @Inject constructor(
    private val ocrService: OCRService,
    @ApplicationContext private val context: Context
) {
    /**
     * Retrieve available OCR languages enriched with installation and file-size metadata.
     *
     * Obtains language codes from the OCR service, locates the app's tessdata directory under
     * external files, and maps each code to a Language object containing:
     * - `code`: the language code,
     * - `displayName`: derived from `Language.getDisplayName(code)`,
     * - `isInstalled`: `true` if a corresponding `<code>.traineddata` file exists in tessdata,
     * - `fileSize`: size of the traineddata file in bytes or `0` if not present.
     * The resulting list is sorted by `displayName`.
     *
     * @return A Result containing the list of Language objects ordered by displayName; on failure, a failure Result with the thrown exception.
     */
    suspend operator fun invoke(): Result<List<Language>> = withContext(Dispatchers.IO) {
        try {
            // Get language codes from OCR service
            val languageCodes = ocrService.getAvailableLanguages()

            // Get tessdata directory path
            val tessdataPath = File(context.getExternalFilesDir(null), "tessdata")

            // Map language codes to Language objects with file size info
            val languages = languageCodes.map { code ->
                val file = File(tessdataPath, "$code.traineddata")
                Language(
                    code = code,
                    displayName = Language.getDisplayName(code),
                    isInstalled = file.exists(),
                    fileSize = if (file.exists()) file.length() else 0L
                )
            }.sortedBy { it.displayName }

            Result.success(languages)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Retrieve the list of OCR languages that are currently installed.
     *
     * @return A `Result` containing the list of installed `Language` objects on success; a failed `Result` with the encountered exception on error.
     */
    suspend fun getInstalled(): Result<List<Language>> = withContext(Dispatchers.IO) {
        try {
            val allLanguagesResult = invoke()
            allLanguagesResult.fold(
                onSuccess = { languages ->
                    Result.success(languages.filter { it.isInstalled })
                },
                onFailure = { error ->
                    Result.failure(error)
                }
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}