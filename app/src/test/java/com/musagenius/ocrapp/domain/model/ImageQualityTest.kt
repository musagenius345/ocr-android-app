package com.musagenius.ocrapp.domain.model

import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for ImageQuality
 */
class ImageQualityTest {

    @Test
    fun `needsPreprocessing returns true for low brightness`() {
        val quality = ImageQuality(
            isAcceptable = true,
            blurScore = 0.8f,
            brightnessScore = 0.3f,
            resolution = 1000000
        )

        assertTrue(quality.needsPreprocessing())
    }

    @Test
    fun `needsPreprocessing returns true for low blur score`() {
        val quality = ImageQuality(
            isAcceptable = true,
            blurScore = 0.5f,
            brightnessScore = 0.7f,
            resolution = 1000000
        )

        assertTrue(quality.needsPreprocessing())
    }

    @Test
    fun `needsPreprocessing returns false for good quality`() {
        val quality = ImageQuality(
            isAcceptable = true,
            blurScore = 0.8f,
            brightnessScore = 0.7f,
            resolution = 1000000
        )

        assertFalse(quality.needsPreprocessing())
    }

    @Test
    fun `getQualityDescription returns appropriate messages`() {
        val poorQuality = ImageQuality(
            isAcceptable = false,
            blurScore = 0.2f,
            brightnessScore = 0.2f,
            resolution = 100000
        )

        val blurryQuality = ImageQuality(
            isAcceptable = true,
            blurScore = 0.5f,
            brightnessScore = 0.7f,
            resolution = 1000000
        )

        val darkQuality = ImageQuality(
            isAcceptable = true,
            blurScore = 0.8f,
            brightnessScore = 0.3f,
            resolution = 1000000
        )

        val goodQuality = ImageQuality(
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
        val quality = ImageQuality(
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
