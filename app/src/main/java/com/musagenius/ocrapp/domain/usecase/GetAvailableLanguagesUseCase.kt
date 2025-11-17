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
     * Get list of all available languages with details
     * Runs on IO dispatcher - do not call from withContext(Dispatchers.IO)
     * @return Result containing list of Language objects
     */
    suspend operator fun invoke(): Result<List<Language>> = withContext(Dispatchers.IO) {
        try {
            // Get language codes from OCR service
            val languageCodes = ocrService.getAvailableLanguages()

            // Get tessdata directory path, handling null case
            val externalFilesDir = context.getExternalFilesDir(null)
                ?: return@withContext Result.error(
                    IllegalStateException("External files directory not available"),
                    "Cannot access tessdata directory"
                )

            val tessdataPath = File(externalFilesDir, "tessdata")

            // Map language codes to Language objects with file size info
            val languages = languageCodes.map { code ->
                val file = File(tessdataPath, "$code.traineddata")
                // Cache exists() result to avoid duplicate IO
                val exists = file.exists()
                Language(
                    code = code,
                    displayName = Language.getDisplayName(code),
                    isInstalled = exists,
                    fileSize = if (exists) file.length() else 0L
                )
            }.sortedBy { it.displayName }

            Result.success(languages)
        } catch (e: Exception) {
            Result.error(e, "Failed to get available languages: ${e.message}")
        }
    }

    /**
     * Get only installed languages
     * Delegates to invoke() which already runs on IO dispatcher
     */
    suspend fun getInstalled(): Result<List<Language>> {
        return try {
            val allLanguagesResult = invoke()
            allLanguagesResult.fold(
                onSuccess = { languages ->
                    Result.success(languages.filter { it.isInstalled })
                },
                onFailure = { error ->
                    Result.error(error, "Failed to get installed languages")
                }
            )
        } catch (e: Exception) {
            Result.error(e, "Failed to get installed languages: ${e.message}")
        }
    }
}
