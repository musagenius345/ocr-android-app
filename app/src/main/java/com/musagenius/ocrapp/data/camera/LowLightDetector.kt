package com.musagenius.ocrapp.data.camera

import android.graphics.ImageFormat
import androidx.camera.core.ImageProxy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Detects low-light conditions from camera frames
 * Uses luminance analysis to determine if lighting is sufficient for OCR
 */
@Singleton
class LowLightDetector @Inject constructor() {

    companion object {
        // Brightness thresholds (0-255 scale)
        private const val VERY_LOW_LIGHT_THRESHOLD = 30f
        private const val LOW_LIGHT_THRESHOLD = 60f

        // Sample rate - analyze every Nth pixel for performance
        private const val SAMPLE_RATE = 8
    }

    /**
     * Lighting condition levels
     */
    enum class LightingCondition {
        GOOD,       // Sufficient lighting for OCR
        LOW,        // Suboptimal lighting - flash recommended
        VERY_LOW    // Very poor lighting - flash strongly recommended
    }

    /**
     * Analyze image for lighting conditions
     * @param imageProxy The camera frame to analyze
     * @return Lighting condition assessment
     */
    suspend fun detectLightingCondition(imageProxy: ImageProxy): LightingCondition = withContext(Dispatchers.Default) {
        try {
            val brightness = calculateAverageBrightness(imageProxy)

            when {
                brightness < VERY_LOW_LIGHT_THRESHOLD -> LightingCondition.VERY_LOW
                brightness < LOW_LIGHT_THRESHOLD -> LightingCondition.LOW
                else -> LightingCondition.GOOD
            }
        } catch (e: Exception) {
            // On error, assume good lighting to avoid false warnings
            LightingCondition.GOOD
        }
    }

    /**
     * Calculate average brightness of the image
     * Uses Y plane (luminance) from YUV format for efficiency
     */
    private fun calculateAverageBrightness(imageProxy: ImageProxy): Float {
        // Only process YUV_420_888 format (standard for camera preview)
        if (imageProxy.format != ImageFormat.YUV_420_888) {
            return LOW_LIGHT_THRESHOLD + 1 // Return safe value for unsupported formats
        }

        val yPlane = imageProxy.planes[0]
        val yBuffer = yPlane.buffer
        val ySize = yBuffer.remaining()
        val pixelStride = yPlane.pixelStride
        val rowStride = yPlane.rowStride

        val width = imageProxy.width
        val height = imageProxy.height

        var sum = 0L
        var count = 0

        // Sample pixels at regular intervals for performance
        for (y in 0 until height step SAMPLE_RATE) {
            for (x in 0 until width step SAMPLE_RATE) {
                val index = y * rowStride + x * pixelStride
                if (index < ySize) {
                    val luminance = yBuffer[index].toInt() and 0xFF
                    sum += luminance
                    count++
                }
            }
        }

        return if (count > 0) {
            sum.toFloat() / count
        } else {
            LOW_LIGHT_THRESHOLD + 1 // Return safe value if no samples
        }
    }
}
