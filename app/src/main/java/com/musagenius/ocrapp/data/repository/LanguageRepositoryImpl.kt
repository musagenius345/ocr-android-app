package com.musagenius.ocrapp.data.repository

import android.content.Context
import com.musagenius.ocrapp.domain.model.Result
import com.musagenius.ocrapp.domain.model.TesseractLanguage
import com.musagenius.ocrapp.domain.repository.LanguageRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of LanguageRepository for managing Tesseract language files
 */
@Singleton
class LanguageRepositoryImpl @Inject constructor(
    private val context: Context
) : LanguageRepository {

    /**
     * Get tessdata directory path
     */
    override fun getTessdataPath(): String {
        val tessdataDir = File(context.filesDir, "tessdata")
        if (!tessdataDir.exists()) {
            tessdataDir.mkdirs()
        }
        return tessdataDir.absolutePath
    }

    /**
     * Get list of all available languages with installation status
     */
    override suspend fun getAvailableLanguages(): Result<List<TesseractLanguage>> {
        return withContext(Dispatchers.IO) {
            try {
                val tessdataPath = getTessdataPath()
                val installedLanguages = File(tessdataPath).listFiles()
                    ?.filter { it.name.endsWith(".traineddata") }
                    ?.map { it.nameWithoutExtension }
                    ?.toSet() ?: emptySet()

                val languages = TesseractLanguage.getSupportedLanguages().map { lang ->
                    lang.copy(isInstalled = installedLanguages.contains(lang.code))
                }

                Result.Success(languages)
            } catch (e: Exception) {
                Result.Error(e)
            }
        }
    }

    /**
     * Download a language file with progress tracking
     */
    override fun downloadLanguage(languageCode: String): Flow<Result<Float>> = flow {
        try {
            val language = TesseractLanguage.getSupportedLanguages()
                .find { it.code == languageCode }
                ?: throw IllegalArgumentException("Language not found: $languageCode")

            val tessdataPath = getTessdataPath()
            val outputFile = File(tessdataPath, "${languageCode}.traineddata")

            // Create temporary file for download
            val tempFile = File(tessdataPath, "${languageCode}.tmp")

            val url = URL(language.downloadUrl)
            val connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = 30000
            connection.readTimeout = 30000

            try {
                connection.connect()

                if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                    throw Exception("Server returned HTTP ${connection.responseCode}")
                }

                val fileLength = connection.contentLength
                val buffer = ByteArray(8192)
                var totalBytesRead = 0L

                connection.inputStream.use { input ->
                    tempFile.outputStream().use { output ->
                        var bytesRead: Int
                        while (input.read(buffer).also { bytesRead = it } != -1) {
                            output.write(buffer, 0, bytesRead)
                            totalBytesRead += bytesRead

                            // Emit progress
                            val progress = if (fileLength > 0) {
                                totalBytesRead.toFloat() / fileLength.toFloat()
                            } else {
                                0f
                            }
                            emit(Result.Success(progress))
                        }
                    }
                }

                // Rename temp file to final file
                if (tempFile.renameTo(outputFile)) {
                    emit(Result.Success(1.0f))
                } else {
                    throw Exception("Failed to rename downloaded file")
                }
            } finally {
                connection.disconnect()
                // Clean up temp file if it still exists
                if (tempFile.exists()) {
                    tempFile.delete()
                }
            }
        } catch (e: CancellationException) {
            // Rethrow CancellationException to preserve cooperative cancellation
            throw e
        } catch (e: Exception) {
            emit(Result.Error(e))
        }
    }.flowOn(Dispatchers.IO)

    /**
     * Delete a language file
     */
    override suspend fun deleteLanguage(languageCode: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val tessdataPath = getTessdataPath()
                val file = File(tessdataPath, "${languageCode}.traineddata")

                if (file.exists()) {
                    if (file.delete()) {
                        Result.Success(Unit)
                    } else {
                        Result.Error(Exception("Failed to delete language file"))
                    }
                } else {
                    Result.Error(Exception("Language file not found"))
                }
            } catch (e: Exception) {
                Result.Error(e)
            }
        }
    }

    /**
     * Check if a language is installed
     */
    override suspend fun isLanguageInstalled(languageCode: String): Boolean {
        return withContext(Dispatchers.IO) {
            val tessdataPath = getTessdataPath()
            val file = File(tessdataPath, "${languageCode}.traineddata")
            file.exists()
        }
    }

    /**
     * Get total storage used by language files
     */
    override suspend fun getTotalStorageUsed(): Long {
        return withContext(Dispatchers.IO) {
            try {
                val tessdataPath = getTessdataPath()
                val tessdataDir = File(tessdataPath)

                tessdataDir.listFiles()
                    ?.filter { it.name.endsWith(".traineddata") }
                    ?.sumOf { it.length() } ?: 0L
            } catch (e: Exception) {
                0L
            }
        }
    }
}
