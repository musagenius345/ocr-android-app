package com.musagenius.ocrapp.domain.usecase

import android.graphics.Bitmap
import com.musagenius.ocrapp.data.ocr.ImagePreprocessor
import com.musagenius.ocrapp.domain.model.ImageQualityAssessment
import com.musagenius.ocrapp.domain.model.Result
import javax.inject.Inject

/**
 * Use case for validating image quality before OCR
 */
class ValidateImageQualityUseCase @Inject constructor(
    private val imagePreprocessor: ImagePreprocessor
) {
    /**
     * Assess image quality
     * @param bitmap The image to validate
     * @return Result containing image quality assessment
     */
    suspend operator fun invoke(bitmap: Bitmap): Result<ImageQualityAssessment> {
        return try {
            val quality = imagePreprocessor.assessImageQuality(bitmap)
            Result.success(quality)
        } catch (e: Exception) {
            Result.error(e, "Failed to assess image quality: ${e.message}")
        }
    }
}
