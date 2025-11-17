package com.musagenius.ocrapp.domain.model

/**
 * Result of OCR processing
 */
data class OCRResult(
    val text: String,
    val confidence: Float, // 0.0 to 1.0
    val processingTimeMs: Long,
    val language: String,
    val wordCount: Int = if (text.trim().isEmpty()) 0 else text.trim().split("\\s+".toRegex()).size,
    val characterCount: Int = text.length
) {
    /**
     * Check if the result has acceptable confidence
     */
    fun hasAcceptableConfidence(threshold: Float = 0.5f): Boolean {
        return confidence >= threshold
    }

    /**
     * Check if text was found
     */
    fun hasText(): Boolean {
        return text.isNotBlank()
    }

    /**
     * Get confidence as percentage string
     */
    fun getConfidencePercentage(): String {
        return "${(confidence * 100).toInt()}%"
    }

    /**
     * Get processing time in seconds
     */
    fun getProcessingTimeSeconds(): Double {
        return processingTimeMs / 1000.0
    }

    companion object {
        /**
         * Create an empty result for error cases
         */
        fun empty(language: String = "eng"): OCRResult {
            return OCRResult(
                text = "",
                confidence = 0f,
                processingTimeMs = 0,
                language = language,
                wordCount = 0,
                characterCount = 0
            )
        }
    }
}

/**
 * Image quality assessment result
 */
data class ImageQuality(
    val isAcceptable: Boolean,
    val blurScore: Float, // 0.0 (blurry) to 1.0 (sharp)
    val brightnessScore: Float, // 0.0 (too dark) to 1.0 (good)
    val resolution: Int, // in pixels (width * height)
    val warnings: List<String> = emptyList()
) {
    /**
     * Check if image needs preprocessing
     */
    fun needsPreprocessing(): Boolean {
        return brightnessScore < 0.5f || blurScore < 0.6f
    }

    /**
     * Get quality description
     */
    fun getQualityDescription(): String {
        return when {
            !isAcceptable -> "Poor quality - may affect OCR accuracy"
            blurScore < 0.6f -> "Image appears blurry"
            brightnessScore < 0.5f -> "Image is too dark"
            else -> "Good quality"
        }
    }
}
