package com.musagenius.ocrapp.domain.model

/**
 * Represents OCR processing progress
 */
data class OCRProgress(
    /** Progress percentage (0-100) */
    val progress: Int,

    /** Current stage of processing */
    val stage: ProcessingStage,

    /** Estimated time remaining in milliseconds (null if unknown) */
    val estimatedTimeRemainingMs: Long? = null,

    /** Elapsed time in milliseconds */
    val elapsedTimeMs: Long = 0
) {
    /**
     * Check if processing is complete
     */
    fun isComplete(): Boolean = progress >= 100

    /**
     * Get progress as fraction (0.0 to 1.0)
     */
    fun getProgressFraction(): Float = progress / 100f

    /**
     * Get user-friendly status message
     */
    fun getStatusMessage(): String {
        return when (stage) {
            ProcessingStage.INITIALIZING -> "Initializing OCR engine..."
            ProcessingStage.PREPROCESSING -> "Preprocessing image..."
            ProcessingStage.RECOGNIZING -> {
                if (progress < 100) {
                    "Recognizing text... $progress%"
                } else {
                    "Finalizing results..."
                }
            }
            ProcessingStage.COMPLETED -> "Processing complete"
            ProcessingStage.FAILED -> "Processing failed"
            ProcessingStage.CANCELLED -> "Processing cancelled"
        }
    }

    /**
     * Get estimated time remaining as formatted string
     */
    fun getEstimatedTimeString(): String? {
        val remainingMs = estimatedTimeRemainingMs ?: return null
        val remainingSeconds = (remainingMs / 1000).toInt()

        return when {
            remainingSeconds < 1 -> "Less than 1 second"
            remainingSeconds < 60 -> "$remainingSeconds seconds"
            else -> {
                val minutes = remainingSeconds / 60
                val seconds = remainingSeconds % 60
                "${minutes}m ${seconds}s"
            }
        }
    }

    companion object {
        /**
         * Create initial progress state
         */
        fun initial(): OCRProgress {
            return OCRProgress(
                progress = 0,
                stage = ProcessingStage.INITIALIZING,
                estimatedTimeRemainingMs = null,
                elapsedTimeMs = 0
            )
        }

        /**
         * Create completed progress state
         */
        fun completed(elapsedTimeMs: Long): OCRProgress {
            return OCRProgress(
                progress = 100,
                stage = ProcessingStage.COMPLETED,
                estimatedTimeRemainingMs = 0,
                elapsedTimeMs = elapsedTimeMs
            )
        }

        /**
         * Create failed progress state
         */
        fun failed(elapsedTimeMs: Long): OCRProgress {
            return OCRProgress(
                progress = 0,
                stage = ProcessingStage.FAILED,
                estimatedTimeRemainingMs = null,
                elapsedTimeMs = elapsedTimeMs
            )
        }

        /**
         * Create cancelled progress state
         */
        fun cancelled(elapsedTimeMs: Long): OCRProgress {
            return OCRProgress(
                progress = 0,
                stage = ProcessingStage.CANCELLED,
                estimatedTimeRemainingMs = null,
                elapsedTimeMs = elapsedTimeMs
            )
        }
    }
}

/**
 * Stages of OCR processing
 */
enum class ProcessingStage {
    /** Initializing OCR engine and loading language data */
    INITIALIZING,

    /** Preprocessing image (grayscale, scaling, etc.) */
    PREPROCESSING,

    /** Performing text recognition */
    RECOGNIZING,

    /** Processing completed successfully */
    COMPLETED,

    /** Processing failed with error */
    FAILED,

    /** Processing was cancelled by user */
    CANCELLED
}
