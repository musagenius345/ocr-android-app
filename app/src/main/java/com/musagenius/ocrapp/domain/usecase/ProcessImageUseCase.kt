package com.musagenius.ocrapp.domain.usecase

import android.graphics.Bitmap
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
    private val ocrService: OCRService
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
}
