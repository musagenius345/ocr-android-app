package com.musagenius.ocrapp.domain.usecase

import android.graphics.Bitmap
import android.net.Uri
import com.musagenius.ocrapp.data.utils.ImageCompressor
import com.musagenius.ocrapp.domain.model.OCRConfig
import com.musagenius.ocrapp.domain.model.OCRResult
import com.musagenius.ocrapp.domain.model.Result
import com.musagenius.ocrapp.domain.service.OCRService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

/**
 * Use case for processing an image with OCR
 */
class ProcessImageUseCase @Inject constructor(
    private val ocrService: OCRService,
    private val imageCompressor: ImageCompressor
) {
    /**
     * Process an image and extract text
     * @param bitmap The image to process
     * @param config OCR configuration
     * @return Flow emitting loading, success, or error states
     */
    operator fun invoke(
        bitmap: Bitmap,
        config: OCRConfig = OCRConfig()
    ): Flow<Result<OCRResult>> = flow {
        emit(Result.loading())

        val result = ocrService.recognizeText(bitmap, config)
        emit(result)
    }

    /**
     * Performs OCR on the provided bitmap using the given configuration.
     *
     * @param bitmap The bitmap to process.
     * @param config OCR configuration to control recognition parameters; defaults to a new OCRConfig.
     * @return A Result wrapping the OCR outcome: `Result.success` contains the OCRResult on success, `Result.failure` contains the error on failure.
     */
    suspend fun execute(
        bitmap: Bitmap,
        config: OCRConfig = OCRConfig()
    ): Result<OCRResult> {
        return ocrService.recognizeText(bitmap, config)
    }

    /**
     * Processes the image at the given URI and extracts text using OCR.
     *
     * Loads and prepares a bitmap from the provided URI, runs OCR with the given configuration,
     * and emits processing states and the final result.
     *
     * @param imageUri The URI of the image to process.
     * @param config OCR configuration; its `maxImageDimension` is used when loading the bitmap.
     * @return A Flow that emits a loading state, then a `Result` containing the `OCRResult` on success,
     * or a failure `Result` if bitmap loading or OCR fails.
     */
    operator fun invoke(
        imageUri: Uri,
        config: OCRConfig = OCRConfig()
    ): Flow<Result<OCRResult>> = flow {
        emit(Result.loading())

        // Load bitmap from URI with automatic orientation fixing and efficient sampling
        val bitmap = imageCompressor.loadBitmapFromUri(
            uri = imageUri,
            maxSize = config.maxImageDimension
        )

        if (bitmap == null) {
            emit(Result.failure(Exception("Failed to load bitmap from URI")))
            return@flow
        }

        try {
            // Process with OCR
            val result = ocrService.recognizeText(bitmap, config)
            emit(result)
        } finally {
            // Clean up bitmap resources
            bitmap.recycle()
        }
    }

    /**
     * Processes an image at the given URI and performs OCR on it.
     *
     * Loads a bitmap from the provided URI (resized to config.maxImageDimension), runs text recognition, and recycles the bitmap before returning.
     *
     * @param imageUri The URI of the image to process.
     * @param config OCR configuration options; defaults to a new OCRConfig.
     * @return A Result containing an OCRResult on success, or a failure Result with an exception if bitmap loading or recognition fails.
     */
    suspend fun execute(
        imageUri: Uri,
        config: OCRConfig = OCRConfig()
    ): Result<OCRResult> {
        // Load bitmap from URI with automatic orientation fixing and efficient sampling
        val bitmap = imageCompressor.loadBitmapFromUri(
            uri = imageUri,
            maxSize = config.maxImageDimension
        ) ?: return Result.failure(Exception("Failed to load bitmap from URI"))

        return try {
            ocrService.recognizeText(bitmap, config)
        } finally {
            // Clean up bitmap resources
            bitmap.recycle()
        }
    }
}