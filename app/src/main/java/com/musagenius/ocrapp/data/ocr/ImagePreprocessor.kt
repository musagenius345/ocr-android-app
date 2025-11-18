package com.musagenius.ocrapp.data.ocr

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Matrix
import androidx.core.graphics.createBitmap
import com.musagenius.ocrapp.domain.model.ImageQualityAssessment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlin.math.max
import kotlin.math.min

/**
 * Image preprocessing utilities for improving OCR accuracy
 */
class ImagePreprocessor @Inject constructor() {

    /**
     * Preprocess image for OCR
     * Applies grayscale conversion, scaling, and contrast enhancement
     */
    suspend fun preprocessImage(
        bitmap: Bitmap,
        maxDimension: Int = 2000
    ): Bitmap = withContext(Dispatchers.Default) {
        var processed = bitmap

        // 1. Scale if too large
        processed = scaleIfNeeded(processed, maxDimension)

        // 2. Convert to grayscale
        processed = convertToGrayscale(processed)

        // 3. Enhance contrast
        processed = enhanceContrast(processed)

        processed
    }

    /**
     * Scale image if it exceeds max dimension
     */
    private fun scaleIfNeeded(bitmap: Bitmap, maxDimension: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height

        if (width <= maxDimension && height <= maxDimension) {
            return bitmap
        }

        val scale = min(
            maxDimension.toFloat() / width,
            maxDimension.toFloat() / height
        )

        val matrix = Matrix().apply {
            postScale(scale, scale)
        }

        return Bitmap.createBitmap(bitmap, 0, 0, width, height, matrix, true)
    }

    /**
     * Convert bitmap to grayscale
     */
    private fun convertToGrayscale(bitmap: Bitmap): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val grayscale = createBitmap(width, height, Bitmap.Config.ARGB_8888)

        for (x in 0 until width) {
            for (y in 0 until height) {
                val pixel = bitmap.getPixel(x, y)
                val gray = (Color.red(pixel) * 0.299 +
                        Color.green(pixel) * 0.587 +
                        Color.blue(pixel) * 0.114).toInt()
                val grayPixel = Color.rgb(gray, gray, gray)
                grayscale.setPixel(x, y, grayPixel)
            }
        }

        return grayscale
    }

    /**
     * Enhance contrast using simple histogram stretching
     */
    private fun enhanceContrast(bitmap: Bitmap): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val enhanced = createBitmap(width, height, Bitmap.Config.ARGB_8888)

        // Find min and max values
        var minGray = 255
        var maxGray = 0

        for (x in 0 until width) {
            for (y in 0 until height) {
                val pixel = bitmap.getPixel(x, y)
                val gray = Color.red(pixel) // Already grayscale
                minGray = min(minGray, gray)
                maxGray = max(maxGray, gray)
            }
        }

        // Stretch histogram
        val range = maxGray - minGray
        if (range == 0) return bitmap // No contrast to enhance

        for (x in 0 until width) {
            for (y in 0 until height) {
                val pixel = bitmap.getPixel(x, y)
                val gray = Color.red(pixel)
                val stretched = ((gray - minGray) * 255 / range).coerceIn(0, 255)
                val stretchedPixel = Color.rgb(stretched, stretched, stretched)
                enhanced.setPixel(x, y, stretchedPixel)
            }
        }

        return enhanced
    }

    /**
     * Assess image quality for OCR
     */
    suspend fun assessImageQuality(bitmap: Bitmap): ImageQualityAssessment = withContext(Dispatchers.Default) {
        val blurScore = calculateSharpness(bitmap)
        val brightnessScore = calculateBrightness(bitmap)
        val resolution = bitmap.width * bitmap.height
        val warnings = mutableListOf<String>()

        // Check for issues
        if (blurScore < 0.3f) {
            warnings.add("Image is very blurry")
        } else if (blurScore < 0.6f) {
            warnings.add("Image may be slightly blurry")
        }

        if (brightnessScore < 0.3f) {
            warnings.add("Image is too dark")
        } else if (brightnessScore > 0.9f) {
            warnings.add("Image may be overexposed")
        }

        if (resolution < 500_000) {
            warnings.add("Image resolution is low")
        }

        val isAcceptable = blurScore >= 0.3f && brightnessScore in 0.2f..0.95f

        ImageQualityAssessment(
            isAcceptable = isAcceptable,
            blurScore = blurScore,
            brightnessScore = brightnessScore,
            resolution = resolution,
            warnings = warnings
        )
    }

    /**
     * Calculate image sharpness (blur detection)
     * Returns a score from 0.0 (blurry) to 1.0 (sharp)
     */
    private fun calculateSharpness(bitmap: Bitmap): Float {
        val sampleSize = 100 // Sample 100x100 pixels from center
        val centerX = bitmap.width / 2
        val centerY = bitmap.height / 2
        val halfSize = sampleSize / 2

        var laplacianSum = 0.0
        var count = 0

        // Sample center region
        for (x in max(0, centerX - halfSize) until min(bitmap.width, centerX + halfSize)) {
            for (y in max(0, centerY - halfSize) until min(bitmap.height, centerY + halfSize)) {
                if (x > 0 && x < bitmap.width - 1 && y > 0 && y < bitmap.height - 1) {
                    val center = getGrayValue(bitmap, x, y)
                    val top = getGrayValue(bitmap, x, y - 1)
                    val bottom = getGrayValue(bitmap, x, y + 1)
                    val left = getGrayValue(bitmap, x - 1, y)
                    val right = getGrayValue(bitmap, x + 1, y)

                    val laplacian = kotlin.math.abs(4 * center - top - bottom - left - right)
                    laplacianSum += laplacian
                    count++
                }
            }
        }

        val variance = if (count > 0) laplacianSum / count else 0.0
        // Normalize to 0-1 range (empirical values)
        return (variance / 50.0).coerceIn(0.0, 1.0).toFloat()
    }

    /**
     * Calculate average brightness
     * Returns a score from 0.0 (dark) to 1.0 (bright)
     */
    private fun calculateBrightness(bitmap: Bitmap): Float {
        var totalBrightness = 0L
        var count = 0

        // Sample every 10th pixel for performance
        for (x in 0 until bitmap.width step 10) {
            for (y in 0 until bitmap.height step 10) {
                totalBrightness += getGrayValue(bitmap, x, y)
                count++
            }
        }

        val avgBrightness = if (count > 0) totalBrightness / count else 128
        return avgBrightness / 255f
    }

    /**
     * Get grayscale value of a pixel
     */
    private fun getGrayValue(bitmap: Bitmap, x: Int, y: Int): Int {
        val pixel = bitmap.getPixel(x, y)
        return (Color.red(pixel) * 0.299 +
                Color.green(pixel) * 0.587 +
                Color.blue(pixel) * 0.114).toInt()
    }

    /**
     * Rotate bitmap by degrees
     */
    fun rotateBitmap(bitmap: Bitmap, degrees: Float): Bitmap {
        if (degrees == 0f) return bitmap

        val matrix = Matrix().apply {
            postRotate(degrees)
        }

        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }
}
