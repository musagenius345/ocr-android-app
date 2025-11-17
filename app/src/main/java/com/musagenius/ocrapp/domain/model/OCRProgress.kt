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
 * Indicates whether processing is complete.
 *
 * @return `true` if `progress` is greater than or equal to 100, `false` otherwise.
 */
    fun isComplete(): Boolean = progress >= 100

    /**
 * Computes the current progress as a fraction between 0.0 and 1.0.
 *
 * @return The progress expressed as a Float where 0.0 represents 0% and 1.0 represents 100%.
 */
    fun getProgressFraction(): Float = progress / 100f

    /**
     * Produces a user-facing message describing the current OCR processing stage.
     *
     * For the RECOGNIZING stage the message includes the current progress percentage while progress is below 100% and shows "Finalizing results..." once progress reaches 100%.
     *
     * @return A stage-specific status message suitable for display to the user.
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
     * Format the estimated remaining time into a human-readable string.
     *
     * If the estimated time is unknown, this returns `null`. For known values:
     * - less than 1 second -> "Less than 1 second"
     * - less than 60 seconds -> "Y seconds"
     * - 60 seconds or more -> "Mm Ss" (minutes and seconds)
     *
     * @return A formatted remaining-time string, or `null` if the estimate is unavailable.
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
         * Creates an OCRProgress representing the initial OCR processing state.
         *
         * @return An OCRProgress with progress 0, stage INITIALIZING, unknown estimatedTimeRemainingMs (null), and elapsedTimeMs 0.
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
         * Creates an OCRProgress representing a completed operation.
         *
         * @param elapsedTimeMs Elapsed time in milliseconds for the completed operation.
         * @return An OCRProgress with progress = 100, stage = ProcessingStage.COMPLETED, estimatedTimeRemainingMs = 0, and the provided elapsedTimeMs.
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
         * Create an OCRProgress representing a cancelled processing run.
         *
         * @param elapsedTimeMs Total elapsed time of the run in milliseconds.
         * @return An OCRProgress with progress 0, stage CANCELLED, unknown estimated time remaining, and the provided elapsed time.
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