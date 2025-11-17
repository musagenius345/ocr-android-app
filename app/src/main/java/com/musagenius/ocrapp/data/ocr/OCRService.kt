package com.musagenius.ocrapp.data.ocr

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.googlecode.tesseract.android.TessBaseAPI
import com.musagenius.ocrapp.domain.model.OCRConfig
import com.musagenius.ocrapp.domain.model.OCRResult
import com.musagenius.ocrapp.domain.model.Result
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Service for performing OCR using Tesseract
 */
@Singleton
class OCRService @Inject constructor(
    @ApplicationContext private val context: Context,
    private val imagePreprocessor: ImagePreprocessor
) {
    private var tessBaseAPI: TessBaseAPI? = null
    private var currentLanguage: String = ""
    private val tessdataPath: String by lazy {
        context.getExternalFilesDir(null)?.absolutePath + "/tessdata/"
    }

    companion object {
        private const val TAG = "OCRService"
        private const val TESSDATA_FOLDER = "tessdata"
    }

    /**
     * Initialize Tesseract with the specified configuration
     */
    suspend fun initialize(config: OCRConfig = OCRConfig()): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // Ensure tessdata directory exists
            val tessdataDir = File(tessdataPath)
            if (!tessdataDir.exists()) {
                tessdataDir.mkdirs()
            }

            // Check if language file exists, if not, copy from assets
            val trainedDataFile = File(tessdataDir, config.getTessdataFileName())
            if (!trainedDataFile.exists()) {
                val result = copyTrainedDataFromAssets(config.language)
                if (result is Result.Error) {
                    return@withContext result
                }
            }

            // Initialize Tesseract
            if (tessBaseAPI == null || currentLanguage != config.language) {
                tessBaseAPI?.end()
                tessBaseAPI = TessBaseAPI()

                val success = tessBaseAPI?.init(tessdataPath, config.language) ?: false
                if (!success) {
                    return@withContext Result.error(
                        Exception("Failed to initialize Tesseract"),
                        "Could not initialize OCR engine for language: ${config.language}"
                    )
                }

                // Set page segmentation mode
                tessBaseAPI?.pageSegMode = config.pageSegmentationMode.value

                currentLanguage = config.language
                Log.d(TAG, "Tesseract initialized successfully for language: ${config.language}")
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing Tesseract", e)
            Result.error(e, "Failed to initialize OCR engine: ${e.message}")
        }
    }

    /**
     * Perform OCR on the given bitmap
     */
    suspend fun recognizeText(
        bitmap: Bitmap,
        config: OCRConfig = OCRConfig()
    ): Result<OCRResult> = withContext(Dispatchers.Default) {
        try {
            val startTime = System.currentTimeMillis()

            // Initialize if needed
            val initResult = initialize(config)
            if (initResult is Result.Error) {
                return@withContext Result.error(
                    initResult.exception,
                    "Failed to initialize OCR: ${initResult.message}"
                )
            }

            // Preprocess image if enabled
            val processedBitmap = if (config.preprocessImage) {
                imagePreprocessor.preprocessImage(bitmap, config.maxImageDimension)
            } else {
                bitmap
            }

            // Set image for recognition
            tessBaseAPI?.setImage(processedBitmap)

            // Get text
            val text = tessBaseAPI?.utF8Text ?: ""

            // Get confidence (0-100, convert to 0-1)
            val confidence = (tessBaseAPI?.meanConfidence() ?: 0) / 100f

            val processingTime = System.currentTimeMillis() - startTime

            val result = OCRResult(
                text = text.trim(),
                confidence = confidence,
                processingTimeMs = processingTime,
                language = config.language
            )

            Log.d(TAG, "OCR completed in ${processingTime}ms with confidence: ${result.getConfidencePercentage()}")

            Result.success(result)
        } catch (e: Exception) {
            Log.e(TAG, "Error during OCR", e)
            Result.error(e, "Failed to recognize text: ${e.message}")
        }
    }

    /**
     * Copy trained data file from assets to external storage
     */
    private fun copyTrainedDataFromAssets(language: String): Result<Unit> {
        try {
            val fileName = "$language.traineddata"
            val assetPath = "$TESSDATA_FOLDER/$fileName"
            val outputFile = File(tessdataPath, fileName)

            // Check if file exists in assets
            val assetList = context.assets.list(TESSDATA_FOLDER) ?: emptyArray()
            if (!assetList.contains(fileName)) {
                return Result.error(
                    Exception("Language file not found"),
                    "Language file '$fileName' not found in assets. Please download it separately."
                )
            }

            // Copy file from assets
            context.assets.open(assetPath).use { input ->
                FileOutputStream(outputFile).use { output ->
                    input.copyTo(output)
                }
            }

            Log.d(TAG, "Copied $fileName from assets to $tessdataPath")
            return Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error copying trained data", e)
            return Result.error(e, "Failed to copy language data: ${e.message}")
        }
    }

    /**
     * Check if a language is available
     */
    fun isLanguageAvailable(language: String): Boolean {
        val fileName = "$language.traineddata"
        val file = File(tessdataPath, fileName)
        return file.exists()
    }

    /**
     * Get list of available languages
     */
    fun getAvailableLanguages(): List<String> {
        val tessdataDir = File(tessdataPath)
        if (!tessdataDir.exists()) return emptyList()

        return tessdataDir.listFiles()
            ?.filter { it.name.endsWith(".traineddata") }
            ?.map { it.name.removeSuffix(".traineddata") }
            ?: emptyList()
    }

    /**
     * Stop OCR processing (if running)
     */
    fun stop() {
        tessBaseAPI?.stop()
    }

    /**
     * Clean up resources
     */
    fun cleanup() {
        tessBaseAPI?.end()
        tessBaseAPI = null
        currentLanguage = ""
        Log.d(TAG, "Tesseract cleaned up")
    }

    /**
     * Get Tesseract version
     */
    fun getVersion(): String {
        return TessBaseAPI.getVersion()
    }
}
