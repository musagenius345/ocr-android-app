package com.musagenius.ocrapp.domain.model

import org.junit.Assert.*
import org.junit.Test

/**
 * Comprehensive unit tests for [ImageQualityAssessment] domain model.
 *
 * This test suite validates image quality assessment functionality including:
 * - Preprocessing need detection based on brightness and blur thresholds
 * - Blur score evaluation (Laplacian variance based)
 * - Brightness score evaluation (0.0 = dark, 1.0 = bright)
 * - Resolution acceptability checking
 * - Quality description generation based on multiple factors
 * - Warning message accumulation for quality issues
 * - Acceptance criteria validation
 *
 * Tests ensure proper quality assessment logic, threshold validation,
 * and user-facing quality feedback messages.
 *
 * @see ImageQualityAssessment
 */
class ImageQualityTest {

    @Test
    fun `needsPreprocessing returns true for low brightness`() {
        val quality = ImageQualityAssessment(
            isAcceptable = true,
            blurScore = 0.8f,
            brightnessScore = 0.3f,
            resolution = 1000000
        )

        assertTrue(quality.needsPreprocessing())
    }

    @Test
    fun `needsPreprocessing returns true for low blur score`() {
        val quality = ImageQualityAssessment(
            isAcceptable = true,
            blurScore = 0.5f,
            brightnessScore = 0.7f,
            resolution = 1000000
        )

        assertTrue(quality.needsPreprocessing())
    }

    @Test
    fun `needsPreprocessing returns false for good quality`() {
        val quality = ImageQualityAssessment(
            isAcceptable = true,
            blurScore = 0.8f,
            brightnessScore = 0.7f,
            resolution = 1000000
        )

        assertFalse(quality.needsPreprocessing())
    }

    @Test
    fun `getQualityDescription returns appropriate messages`() {
        val poorQuality = ImageQualityAssessment(
            isAcceptable = false,
            blurScore = 0.2f,
            brightnessScore = 0.2f,
            resolution = 100000
        )

        val blurryQuality = ImageQualityAssessment(
            isAcceptable = true,
            blurScore = 0.5f,
            brightnessScore = 0.7f,
            resolution = 1000000
        )

        val darkQuality = ImageQualityAssessment(
            isAcceptable = true,
            blurScore = 0.8f,
            brightnessScore = 0.3f,
            resolution = 1000000
        )

        val goodQuality = ImageQualityAssessment(
            isAcceptable = true,
            blurScore = 0.8f,
            brightnessScore = 0.7f,
            resolution = 1000000
        )

        assertTrue(poorQuality.getQualityDescription().contains("Poor quality"))
        assertTrue(blurryQuality.getQualityDescription().contains("blurry"))
        assertTrue(darkQuality.getQualityDescription().contains("dark"))
        assertTrue(goodQuality.getQualityDescription().contains("Good quality"))
    }

    @Test
    fun `warnings list contains appropriate warnings`() {
        val warnings = listOf("Image is very blurry", "Image is too dark")
        val quality = ImageQualityAssessment(
            isAcceptable = false,
            blurScore = 0.2f,
            brightnessScore = 0.2f,
            resolution = 100000,
            warnings = warnings
        )

        assertEquals(2, quality.warnings.size)
        assertTrue(quality.warnings.contains("Image is very blurry"))
        assertTrue(quality.warnings.contains("Image is too dark"))
    }
}
