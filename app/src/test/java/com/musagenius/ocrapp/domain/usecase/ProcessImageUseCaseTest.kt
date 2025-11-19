package com.musagenius.ocrapp.domain.usecase

import android.graphics.Bitmap
import android.net.Uri
import com.musagenius.ocrapp.data.utils.ImageCompressor
import com.musagenius.ocrapp.domain.model.OCRConfig
import com.musagenius.ocrapp.domain.model.OCRResult
import com.musagenius.ocrapp.domain.model.Result
import com.musagenius.ocrapp.domain.service.OCRService
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever

/**
 * Comprehensive unit tests for [ProcessImageUseCase].
 *
 * This test suite validates OCR image processing functionality including:
 * - Bitmap processing with Flow emission (Loading → Success/Error)
 * - URI processing with image loading and compression
 * - Direct execution methods (execute) for non-Flow processing
 * - Success and error handling from OCRService
 * - Resource cleanup verification (bitmap recycling)
 * - Compression handling for URI inputs
 * - OCR configuration propagation to service layer
 *
 * Tests verify proper Flow emissions, error propagation,
 * resource management, and service interaction patterns.
 *
 * @see ProcessImageUseCase
 * @see OCRService
 * @see ImageCompressor
 */
class ProcessImageUseCaseTest {

    /** Mock OCR service for text recognition */
    @Mock
    private lateinit var ocrService: OCRService

    /** Mock image compressor for URI-based image processing */
    @Mock
    private lateinit var imageCompressor: ImageCompressor

    /** Mock bitmap for testing bitmap processing path */
    @Mock
    private lateinit var mockBitmap: Bitmap

    /** System under test */
    private lateinit var useCase: ProcessImageUseCase

    /** Test OCR result with standard confidence and processing time */
    private val testOCRResult = OCRResult(
        text = "Test extracted text",
        confidence = 0.95f,
        processingTimeMs = 1500L,
        language = "eng"
    )

    /** Test OCR configuration with English language and preprocessing enabled */
    private val testConfig = OCRConfig(
        language = "eng",
        preprocessImage = true,
        maxImageDimension = 2048
    )

    /**
     * Sets up the test environment before each test.
     * Initializes mocks and creates the use case instance.
     */
    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        useCase = ProcessImageUseCase(ocrService, imageCompressor)
    }

    // ============ Bitmap Processing Tests ============

    @Test
    fun `invoke with bitmap should emit loading then success`() = runTest {
        // Given
        whenever(ocrService.recognizeText(any(), any()))
            .thenReturn(Result.Success(testOCRResult))

        // When
        val results = useCase.invoke(mockBitmap, testConfig).toList()

        // Then
        assertEquals(2, results.size)
        assertTrue(results[0] is Result.Loading)
        assertTrue(results[1] is Result.Success)
        assertEquals("Test extracted text", (results[1] as Result.Success).data.text)
    }

    @Test
    fun `invoke with bitmap should emit loading then error`() = runTest {
        // Given
        whenever(ocrService.recognizeText(any(), any()))
            .thenReturn(Result.Error(Exception("OCR failed")))

        // When
        val results = useCase.invoke(mockBitmap, testConfig).toList()

        // Then
        assertEquals(2, results.size)
        assertTrue(results[0] is Result.Loading)
        assertTrue(results[1] is Result.Error)
        assertEquals("OCR failed", (results[1] as Result.Error).exception.message)
    }

    @Test
    fun `execute with bitmap should return OCR result`() = runTest {
        // Given
        whenever(ocrService.recognizeText(any(), any()))
            .thenReturn(Result.Success(testOCRResult))

        // When
        val result = useCase.execute(mockBitmap, testConfig)

        // Then
        assertTrue(result is Result.Success)
        assertEquals("Test extracted text", (result as Result.Success).data.text)
        verify(ocrService).recognizeText(mockBitmap, testConfig)
    }

    @Test
    fun `execute with bitmap should handle OCR service errors`() = runTest {
        // Given
        whenever(ocrService.recognizeText(any(), any()))
            .thenReturn(Result.Error(Exception("Processing failed")))

        // When
        val result = useCase.execute(mockBitmap, testConfig)

        // Then
        assertTrue(result is Result.Error)
        assertEquals("Processing failed", (result as Result.Error).exception.message)
    }

    @Test
    fun `invoke with bitmap should use default config when not provided`() = runTest {
        // Given
        whenever(ocrService.recognizeText(any(), any()))
            .thenReturn(Result.Success(testOCRResult))

        // When
        useCase.invoke(mockBitmap).toList()

        // Then
        verify(ocrService).recognizeText(eq(mockBitmap), argThat { config ->
            config.language == "eng" &&
            config.preprocessImage &&
            config.maxImageDimension == 2000
        })
    }

    // ============ URI Processing Tests ============

    @Test
    fun `invoke with URI should load bitmap and process`() = runTest {
        // Given
        val testUri = Uri.parse("content://test/image.jpg")
        whenever(imageCompressor.loadBitmapFromUri(any(), any()))
            .thenReturn(mockBitmap)
        whenever(ocrService.recognizeText(any(), any()))
            .thenReturn(Result.Success(testOCRResult))

        // When
        val results = useCase.invoke(testUri, testConfig).toList()

        // Then
        assertEquals(2, results.size)
        assertTrue(results[0] is Result.Loading)
        assertTrue(results[1] is Result.Success)
        verify(imageCompressor).loadBitmapFromUri(testUri, 2048)
        verify(ocrService).recognizeText(mockBitmap, testConfig)
        verify(mockBitmap).recycle() // Should clean up
    }

    @Test
    fun `invoke with URI should fail when bitmap loading fails`() = runTest {
        // Given
        val testUri = Uri.parse("content://test/image.jpg")
        whenever(imageCompressor.loadBitmapFromUri(any(), any()))
            .thenReturn(null)

        // When
        val results = useCase.invoke(testUri, testConfig).toList()

        // Then
        assertEquals(2, results.size)
        assertTrue(results[0] is Result.Loading)
        assertTrue(results[1] is Result.Error)
        val error = results[1] as Result.Error
        assertTrue(error.exception.message?.contains("Failed to load bitmap") == true)
        verify(ocrService, never()).recognizeText(any(), any())
    }

    @Test
    fun `invoke with URI should recycle bitmap even when OCR fails`() = runTest {
        // Given
        val testUri = Uri.parse("content://test/image.jpg")
        whenever(imageCompressor.loadBitmapFromUri(any(), any()))
            .thenReturn(mockBitmap)
        whenever(ocrService.recognizeText(any(), any()))
            .thenReturn(Result.Error(Exception("OCR failed")))

        // When
        useCase.invoke(testUri, testConfig).toList()

        // Then
        verify(mockBitmap).recycle() // Should still clean up
    }

    @Test
    fun `execute with URI should return OCR result`() = runTest {
        // Given
        val testUri = Uri.parse("content://test/image.jpg")
        whenever(imageCompressor.loadBitmapFromUri(any(), any()))
            .thenReturn(mockBitmap)
        whenever(ocrService.recognizeText(any(), any()))
            .thenReturn(Result.Success(testOCRResult))

        // When
        val result = useCase.execute(testUri, testConfig)

        // Then
        assertTrue(result is Result.Success)
        assertEquals("Test extracted text", (result as Result.Success).data.text)
        verify(mockBitmap).recycle()
    }

    @Test
    fun `execute with URI should fail when bitmap loading fails`() = runTest {
        // Given
        val testUri = Uri.parse("content://test/image.jpg")
        whenever(imageCompressor.loadBitmapFromUri(any(), any()))
            .thenReturn(null)

        // When
        val result = useCase.execute(testUri, testConfig)

        // Then
        assertTrue(result is Result.Error)
        val error = result as Result.Error
        assertTrue(error.exception.message?.contains("Failed to load bitmap") == true)
    }

    @Test
    fun `execute with URI should recycle bitmap even when OCR fails`() = runTest {
        // Given
        val testUri = Uri.parse("content://test/image.jpg")
        whenever(imageCompressor.loadBitmapFromUri(any(), any()))
            .thenReturn(mockBitmap)
        whenever(ocrService.recognizeText(any(), any()))
            .thenReturn(Result.Error(Exception("OCR failed")))

        // When
        useCase.execute(testUri, testConfig)

        // Then
        verify(mockBitmap).recycle()
    }

    @Test
    fun `invoke with URI should use max image dimension from config`() = runTest {
        // Given
        val testUri = Uri.parse("content://test/image.jpg")
        val customConfig = testConfig.copy(maxImageDimension = 4096)
        whenever(imageCompressor.loadBitmapFromUri(any(), any()))
            .thenReturn(mockBitmap)
        whenever(ocrService.recognizeText(any(), any()))
            .thenReturn(Result.Success(testOCRResult))

        // When
        useCase.invoke(testUri, customConfig).toList()

        // Then
        verify(imageCompressor).loadBitmapFromUri(testUri, 4096)
    }

    // ============ Configuration Tests ============

    @Test
    fun `invoke should pass correct config to OCR service`() = runTest {
        // Given
        val customConfig = OCRConfig(
            language = "spa",
            preprocessImage = false,
            maxImageDimension = 1024
        )
        whenever(ocrService.recognizeText(any(), any()))
            .thenReturn(Result.Success(testOCRResult))

        // When
        useCase.invoke(mockBitmap, customConfig).toList()

        // Then
        verify(ocrService).recognizeText(mockBitmap, customConfig)
    }
}
