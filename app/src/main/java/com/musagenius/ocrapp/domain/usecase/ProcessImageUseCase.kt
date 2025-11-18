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
     * Process an image synchronously
     */
    suspend fun execute(
        bitmap: Bitmap,
        config: OCRConfig = OCRConfig()
    ): Result<OCRResult> {
        return ocrService.recognizeText(bitmap, config)
    }

    /**
     * Process an image from URI and extract text
     * This overload handles Uri-to-Bitmap conversion with proper compression and orientation
     * @param imageUri The URI of the image to process
     * @param config OCR configuration
     * @return Flow emitting loading, success, or error states
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
     * Process an image from URI synchronously
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
