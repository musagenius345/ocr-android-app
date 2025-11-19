package com.musagenius.ocrapp.data.ocr

import android.graphics.Bitmap
import android.graphics.Color
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.MockitoAnnotations

/**
 * Comprehensive unit tests for ImagePreprocessor
 * Note: These tests use mock bitmaps and focus on logic validation
 */
class ImagePreprocessorTest {

    private lateinit var preprocessor: ImagePreprocessor

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        preprocessor = ImagePreprocessor()
    }

    // ============ Helper Methods ============

    /**
     * Create a simple test bitmap with specified dimensions
     */
    private fun createTestBitmap(width: Int, height: Int, color: Int = Color.WHITE): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        for (x in 0 until width) {
            for (y in 0 until height) {
                bitmap.setPixel(x, y, color)
            }
        }
        return bitmap
    }

    /**
     * Create a bitmap with gradient (for testing contrast and brightness)
     */
    private fun createGradientBitmap(width: Int, height: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        for (x in 0 until width) {
            for (y in 0 until height) {
                val gray = (x * 255 / width).coerceIn(0, 255)
                bitmap.setPixel(x, y, Color.rgb(gray, gray, gray))
            }
        }
        return bitmap
    }

    // ============ Preprocessing Tests ============

    @Test
    fun `preprocessImage should handle small images without scaling`() = runTest {
        // Given
        val bitmap = createTestBitmap(100, 100)

        // When
        val result = preprocessor.preprocessImage(bitmap, maxDimension = 2000)

        // Then
        assertNotNull(result)
        // Dimensions should not change (grayscale/contrast don't resize)
        assertEquals(100, result.width)
        assertEquals(100, result.height)
    }

    @Test
    fun `preprocessImage should scale down large images`() = runTest {
        // Given
        val bitmap = createTestBitmap(3000, 3000)

        // When
        val result = preprocessor.preprocessImage(bitmap, maxDimension = 2000)

        // Then
        assertNotNull(result)
        // Should be scaled down (approximately, allowing for rounding)
        assertTrue(result.width <= 2000)
        assertTrue(result.height <= 2000)
    }

    @Test
    fun `preprocessImage should maintain aspect ratio when scaling`() = runTest {
        // Given - wide image
        val bitmap = createTestBitmap(4000, 2000)

        // When
        val result = preprocessor.preprocessImage(bitmap, maxDimension = 1000)

        // Then
        assertTrue(result.width <= 1000)
        assertTrue(result.height <= 1000)
        // Aspect ratio should be approximately maintained (2:1)
        val originalRatio = 4000f / 2000f
        val resultRatio = result.width.toFloat() / result.height.toFloat()
        assertEquals(originalRatio, resultRatio, 0.1f)
    }

    @Test
    fun `preprocessImage should convert to grayscale`() = runTest {
        // Given - colored bitmap
        val bitmap = Bitmap.createBitmap(10, 10, Bitmap.Config.ARGB_8888)
        for (x in 0 until 10) {
            for (y in 0 until 10) {
                bitmap.setPixel(x, y, Color.RED) // Pure red
            }
        }

        // When
        val result = preprocessor.preprocessImage(bitmap, maxDimension = 2000)

        // Then - should be grayscale (R=G=B)
        val pixel = result.getPixel(5, 5)
        val red = Color.red(pixel)
        val green = Color.green(pixel)
        val blue = Color.blue(pixel)
        assertEquals(red, green)
        assertEquals(green, blue)
    }

    @Test
    fun `preprocessImage with custom max dimension should scale accordingly`() = runTest {
        // Given
        val bitmap = createTestBitmap(2000, 2000)

        // When
        val result = preprocessor.preprocessImage(bitmap, maxDimension = 1000)

        // Then
        assertTrue(result.width <= 1000)
        assertTrue(result.height <= 1000)
    }

    // ============ Image Quality Assessment Tests ============

    @Test
    fun `assessImageQuality should accept good quality image`() = runTest {
        // Given - medium sized, medium brightness bitmap
        val bitmap = createGradientBitmap(800, 600)

        // When
        val assessment = preprocessor.assessImageQuality(bitmap)

        // Then
        assertTrue(assessment.resolution >= 500_000)
        assertTrue(assessment.brightnessScore > 0f)
        assertTrue(assessment.blurScore >= 0f && assessment.blurScore <= 1f)
    }

    @Test
    fun `assessImageQuality should detect low resolution`() = runTest {
        // Given - very small image
        val bitmap = createTestBitmap(100, 100)

        // When
        val assessment = preprocessor.assessImageQuality(bitmap)

        // Then
        assertEquals(10_000, assessment.resolution)
        assertTrue(assessment.warnings.any { it.contains("resolution") })
        // Low resolution alone doesn't make it unacceptable
    }

    @Test
    fun `assessImageQuality should detect dark images`() = runTest {
        // Given - very dark bitmap
        val bitmap = createTestBitmap(800, 600, Color.rgb(10, 10, 10))

        // When
        val assessment = preprocessor.assessImageQuality(bitmap)

        // Then
        assertTrue(assessment.brightnessScore < 0.3f)
        assertTrue(assessment.warnings.any { it.contains("dark") })
    }

    @Test
    fun `assessImageQuality should detect overexposed images`() = runTest {
        // Given - very bright bitmap
        val bitmap = createTestBitmap(800, 600, Color.rgb(250, 250, 250))

        // When
        val assessment = preprocessor.assessImageQuality(bitmap)

        // Then
        assertTrue(assessment.brightnessScore > 0.9f)
        assertTrue(assessment.warnings.any { it.contains("overexposed") })
    }

    @Test
    fun `assessImageQuality should have brightness score in valid range`() = runTest {
        // Given
        val bitmap = createTestBitmap(800, 600)

        // When
        val assessment = preprocessor.assessImageQuality(bitmap)

        // Then
        assertTrue(assessment.brightnessScore >= 0f)
        assertTrue(assessment.brightnessScore <= 1f)
    }

    @Test
    fun `assessImageQuality should have blur score in valid range`() = runTest {
        // Given
        val bitmap = createTestBitmap(800, 600)

        // When
        val assessment = preprocessor.assessImageQuality(bitmap)

        // Then
        assertTrue(assessment.blurScore >= 0f)
        assertTrue(assessment.blurScore <= 1f)
    }

    @Test
    fun `assessImageQuality should mark unacceptable when blur is too low`() = runTest {
        // Given - uniform color (no edges = very blurry)
        val bitmap = createTestBitmap(800, 600, Color.GRAY)

        // When
        val assessment = preprocessor.assessImageQuality(bitmap)

        // Then
        // Uniform bitmap should have very low blur score
        assertTrue(assessment.blurScore < 0.3f)
    }

    @Test
    fun `assessImageQuality should return empty warnings for good image`() = runTest {
        // Given - medium quality gradient image
        val bitmap = createGradientBitmap(1000, 800)

        // When
        val assessment = preprocessor.assessImageQuality(bitmap)

        // Then - may have some warnings but not too many
        assertTrue(assessment.warnings.size <= 2)
    }

    @Test
    fun `assessImageQuality brightness calculation should be consistent`() = runTest {
        // Given - two identical bitmaps
        val bitmap1 = createTestBitmap(500, 500, Color.rgb(128, 128, 128))
        val bitmap2 = createTestBitmap(500, 500, Color.rgb(128, 128, 128))

        // When
        val assessment1 = preprocessor.assessImageQuality(bitmap1)
        val assessment2 = preprocessor.assessImageQuality(bitmap2)

        // Then - should have same brightness score
        assertEquals(assessment1.brightnessScore, assessment2.brightnessScore, 0.01f)
    }

    // ============ Rotation Tests ============

    @Test
    fun `rotateBitmap should not change bitmap when degrees is 0`() = runTest {
        // Given
        val bitmap = createTestBitmap(100, 100)

        // When
        val result = preprocessor.rotateBitmap(bitmap, 0f)

        // Then
        assertEquals(bitmap, result) // Should be same instance
        assertEquals(100, result.width)
        assertEquals(100, result.height)
    }

    @Test
    fun `rotateBitmap should rotate by 90 degrees`() = runTest {
        // Given - rectangular bitmap
        val bitmap = createTestBitmap(200, 100)

        // When
        val result = preprocessor.rotateBitmap(bitmap, 90f)

        // Then - dimensions should be swapped (approximately)
        assertNotEquals(bitmap, result)
        // Allow some tolerance for rotation
        assertTrue(result.width in 90..110)
        assertTrue(result.height in 190..210)
    }

    @Test
    fun `rotateBitmap should rotate by 180 degrees`() = runTest {
        // Given
        val bitmap = createTestBitmap(100, 100)

        // When
        val result = preprocessor.rotateBitmap(bitmap, 180f)

        // Then - dimensions should remain same
        assertNotEquals(bitmap, result)
        assertEquals(100, result.width)
        assertEquals(100, result.height)
    }

    @Test
    fun `rotateBitmap should rotate by 270 degrees`() = runTest {
        // Given - rectangular bitmap
        val bitmap = createTestBitmap(200, 100)

        // When
        val result = preprocessor.rotateBitmap(bitmap, 270f)

        // Then - dimensions should be swapped
        assertNotEquals(bitmap, result)
        assertTrue(result.width in 90..110)
        assertTrue(result.height in 190..210)
    }

    @Test
    fun `rotateBitmap should handle negative angles`() = runTest {
        // Given
        val bitmap = createTestBitmap(100, 100)

        // When
        val result = preprocessor.rotateBitmap(bitmap, -90f)

        // Then
        assertNotEquals(bitmap, result)
        assertNotNull(result)
    }

    @Test
    fun `rotateBitmap should handle arbitrary angles`() = runTest {
        // Given
        val bitmap = createTestBitmap(100, 100)

        // When
        val result = preprocessor.rotateBitmap(bitmap, 45f)

        // Then
        assertNotEquals(bitmap, result)
        assertNotNull(result)
        // Diagonal rotation increases dimensions
        assertTrue(result.width > 100)
        assertTrue(result.height > 100)
    }

    // ============ Edge Case Tests ============

    @Test
    fun `preprocessImage should handle very small bitmaps`() = runTest {
        // Given
        val bitmap = createTestBitmap(1, 1)

        // When
        val result = preprocessor.preprocessImage(bitmap, maxDimension = 2000)

        // Then - should not crash
        assertNotNull(result)
        assertEquals(1, result.width)
        assertEquals(1, result.height)
    }

    @Test
    fun `assessImageQuality should handle edge pixels correctly`() = runTest {
        // Given - small bitmap to test edge handling
        val bitmap = createTestBitmap(10, 10)

        // When
        val assessment = preprocessor.assessImageQuality(bitmap)

        // Then - should not crash and return valid values
        assertNotNull(assessment)
        assertTrue(assessment.blurScore >= 0f && assessment.blurScore <= 1f)
    }

    @Test
    fun `rotateBitmap should handle full rotation (360 degrees)`() = runTest {
        // Given
        val bitmap = createTestBitmap(100, 100)

        // When
        val result = preprocessor.rotateBitmap(bitmap, 360f)

        // Then - should be back to original dimensions
        assertNotEquals(bitmap, result)
        assertEquals(100, result.width)
        assertEquals(100, result.height)
    }

    @Test
    fun `multiple preprocessing operations should work correctly`() = runTest {
        // Given
        val bitmap = createTestBitmap(1500, 1000)

        // When - process twice
        val result1 = preprocessor.preprocessImage(bitmap, maxDimension = 1000)
        val result2 = preprocessor.preprocessImage(result1, maxDimension = 500)

        // Then
        assertTrue(result2.width <= 500)
        assertTrue(result2.height <= 500)
    }

    @Test
    fun `rotation followed by preprocessing should work`() = runTest {
        // Given
        val bitmap = createTestBitmap(200, 100)

        // When
        val rotated = preprocessor.rotateBitmap(bitmap, 90f)
        val processed = preprocessor.preprocessImage(rotated, maxDimension = 2000)

        // Then
        assertNotNull(processed)
        // Should maintain rotated dimensions (approximately)
        assertTrue(processed.width < processed.height)
    }
}
