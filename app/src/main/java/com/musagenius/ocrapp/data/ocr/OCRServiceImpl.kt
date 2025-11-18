package com.musagenius.ocrapp.data.ocr

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.googlecode.tesseract.android.TessBaseAPI
import com.musagenius.ocrapp.domain.model.OCRConfig
import com.musagenius.ocrapp.domain.model.OCRProgress
import com.musagenius.ocrapp.domain.model.OCRResult
import com.musagenius.ocrapp.domain.model.ProcessingStage
import com.musagenius.ocrapp.domain.model.Result
import com.musagenius.ocrapp.domain.service.OCRService
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of OCR service using Tesseract
 * Thread-safe singleton that serializes all tessBaseAPI access
 */
@Singleton
class OCRServiceImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val imagePreprocessor: ImagePreprocessor
) : OCRService {
    // Mutex to serialize all access to tessBaseAPI, preventing race conditions
    private val tessMutex = Mutex()

    private var tessBaseAPI: TessBaseAPI? = null
    private var currentLanguage: String = ""
    @Suppress("SpellCheckingInspection")
    private val tessdataPath: String by lazy {
        // Use external storage if available, fallback to internal storage
        val baseDir = context.getExternalFilesDir(null) ?: context.filesDir
        File(baseDir, "tessdata").absolutePath + File.separator
    }

    companion object {
        private const val TAG = "OCRService"
        @Suppress("SpellCheckingInspection")
        private const val TESSDATA_FOLDER = "tessdata"
    }

    /**
     * Internal initialization logic without lock
     * Must only be called from within a mutex.withLock block
     */
    private suspend fun initializeInternal(config: OCRConfig): Result<Unit> = withContext(Dispatchers.IO) {
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
     * Initialize Tesseract with the specified configuration
     */
    override suspend fun initialize(config: OCRConfig): Result<Unit> = tessMutex.withLock {
        initializeInternal(config)
    }

    /**
     * Perform OCR on the given bitmap
     */
    override suspend fun recognizeText(
        bitmap: Bitmap,
        config: OCRConfig
    ): Result<OCRResult> = tessMutex.withLock {
        withContext(Dispatchers.Default) {
            try {
                val startTime = System.currentTimeMillis()

                // Initialize if needed (call internal method to avoid deadlock)
                val initResult = initializeInternal(config)
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

                // Get text using the correct method call
                val text = tessBaseAPI?.getUTF8Text().orEmpty()

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
    }

    /**
     * Perform OCR on the given bitmap with progress tracking
     * Emits progress updates during processing
     */
    override fun recognizeTextWithProgress(
        bitmap: Bitmap,
        config: OCRConfig
    ): Flow<Result<OCRProgress>> = flow {
        val startTime = System.currentTimeMillis()

        try {
            // Stage 1: Initialization (0-20%)
            emit(Result.success(OCRProgress(
                progress = 0,
                stage = ProcessingStage.INITIALIZING,
                elapsedTimeMs = 0
            )))

            tessMutex.withLock {
                withContext(Dispatchers.Default) {
                    // Initialize if needed
                    val initResult = initializeInternal(config)
                    if (initResult is Result.Error) {
                        emit(Result.success(OCRProgress.failed(System.currentTimeMillis() - startTime)))
                        return@withContext
                    }

                    emit(Result.success(OCRProgress(
                        progress = 20,
                        stage = ProcessingStage.INITIALIZING,
                        elapsedTimeMs = System.currentTimeMillis() - startTime
                    )))

                    // Stage 2: Preprocessing (20-40%)
                    emit(Result.success(OCRProgress(
                        progress = 25,
                        stage = ProcessingStage.PREPROCESSING,
                        elapsedTimeMs = System.currentTimeMillis() - startTime
                    )))

                    val processedBitmap = if (config.preprocessImage) {
                        imagePreprocessor.preprocessImage(bitmap, config.maxImageDimension)
                    } else {
                        bitmap
                    }

                    emit(Result.success(OCRProgress(
                        progress = 40,
                        stage = ProcessingStage.PREPROCESSING,
                        elapsedTimeMs = System.currentTimeMillis() - startTime
                    )))

                    // Stage 3: Recognition (40-95%)
                    emit(Result.success(OCRProgress(
                        progress = 45,
                        stage = ProcessingStage.RECOGNIZING,
                        elapsedTimeMs = System.currentTimeMillis() - startTime
                    )))

                    // Set image for recognition
                    tessBaseAPI?.setImage(processedBitmap)

                    // Simulate progress during recognition
                    // Tesseract doesn't provide real-time progress, so we simulate it
                    for (progress in listOf(50, 60, 70, 80, 90, 95)) {
                        delay(50) // Small delay to show progress updates
                        val elapsedMs = System.currentTimeMillis() - startTime
                        val estimatedTotalMs = if (progress > 50) {
                            (elapsedMs * 100) / progress
                        } else {
                            null
                        }
                        val estimatedRemainingMs = estimatedTotalMs?.let { it - elapsedMs }

                        emit(Result.success(OCRProgress(
                            progress = progress,
                            stage = ProcessingStage.RECOGNIZING,
                            estimatedTimeRemainingMs = estimatedRemainingMs,
                            elapsedTimeMs = elapsedMs
                        )))
                    }

                    // Get text and confidence
                    val text = tessBaseAPI?.getUTF8Text().orEmpty()
                    val confidence = (tessBaseAPI?.meanConfidence() ?: 0) / 100f
                    val processingTime = System.currentTimeMillis() - startTime

                    // Stage 4: Completed (100%)
                    emit(Result.success(OCRProgress(
                        progress = 100,
                        stage = ProcessingStage.COMPLETED,
                        estimatedTimeRemainingMs = 0,
                        elapsedTimeMs = processingTime
                    )))

                    Log.d(TAG, "OCR completed in ${processingTime}ms with confidence: ${(confidence * 100).toInt()}%")
                }
            }
        } catch (e: CancellationException) {
            // Rethrow cancellation to allow proper coroutine cancellation
            Log.d(TAG, "OCR processing cancelled")
            throw e
        } catch (e: Exception) {
            // Handle other exceptions by emitting failed progress state
            Log.e(TAG, "Error during OCR with progress", e)
            val elapsedMs = System.currentTimeMillis() - startTime
            emit(Result.success(OCRProgress.failed(elapsedMs)))
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
    override fun isLanguageAvailable(language: String): Boolean {
        val fileName = "$language.traineddata"
        val file = File(tessdataPath, fileName)
        return file.exists()
    }

    /**
     * Get list of available languages
     */
    override fun getAvailableLanguages(): List<String> {
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
    override fun stop() {
        tessBaseAPI?.stop()
    }

    /**
     * Clean up resources
     */
    override fun cleanup() {
        tessBaseAPI?.end()
        tessBaseAPI = null
        currentLanguage = ""
        Log.d(TAG, "Tesseract cleaned up")
    }

    /**
     * Get Tesseract version
     */
    override fun getVersion(): String {
        return tessBaseAPI?.getVersion() ?: "Unknown"
    }
}
