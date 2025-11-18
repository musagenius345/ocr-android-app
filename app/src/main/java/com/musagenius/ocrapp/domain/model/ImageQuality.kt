package com.musagenius.ocrapp.domain.model

/**
 * Image quality settings for captures
 */
enum class ImageQuality {
    LOW,
    MEDIUM,
    HIGH;

    /**
     * Get JPEG compression quality (0-100)
     */
    fun getCompressionQuality(): Int {
        return when (this) {
            LOW -> 70
            MEDIUM -> 85
            HIGH -> 95
        }
    }

    /**
     * Get maximum image dimension in pixels
     */
    fun getMaxDimension(): Int {
        return when (this) {
            LOW -> 1280
            MEDIUM -> 1920
            HIGH -> 3840
        }
    }
}
