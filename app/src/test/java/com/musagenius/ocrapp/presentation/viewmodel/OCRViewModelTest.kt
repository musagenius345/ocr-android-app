package com.musagenius.ocrapp.presentation.viewmodel

import android.net.Uri
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import app.cash.turbine.test
import com.musagenius.ocrapp.domain.model.OCRConfig
import com.musagenius.ocrapp.domain.model.OCRResult
import com.musagenius.ocrapp.domain.model.Result
import com.musagenius.ocrapp.domain.model.ScanResult
import com.musagenius.ocrapp.domain.repository.ScanRepository
import com.musagenius.ocrapp.domain.service.OCRService
import com.musagenius.ocrapp.domain.usecase.ProcessImageUseCase
import com.musagenius.ocrapp.presentation.ui.ocr.OCREvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import java.util.Date

/**
 * Comprehensive unit tests for OCRViewModel
 */
@OptIn(ExperimentalCoroutinesApi::class)
class OCRViewModelTest {

    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()

    @Mock
    private lateinit var processImageUseCase: ProcessImageUseCase

    @Mock
    private lateinit var scanRepository: ScanRepository

    @Mock
    private lateinit var ocrService: OCRService

    private lateinit var viewModel: OCRViewModel

    private val testImageUri = Uri.parse("content://test/image.jpg")
    private val testOCRResult = OCRResult(
        text = "Test extracted text",
        confidence = 0.95f,
        processingTimeMs = 1500L
    )

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        Dispatchers.setMain(testDispatcher)
        viewModel = OCRViewModel(processImageUseCase, scanRepository, ocrService)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ============ Initial State Tests ============

    @Test
    fun `initial state should be empty`() = runTest {
        viewModel.uiState.test {
            val state = awaitItem()
            assertNull(state.imageUri)
            assertEquals("", state.extractedText)
            assertFalse(state.isProcessing)
            assertNull(state.error)
            assertEquals(0f, state.confidenceScore)
            assertEquals("eng", state.language)
            assertEquals(0L, state.processingTimeMs)
            assertFalse(state.isSaved)
        }
    }

    // ============ Process Image Tests ============

    @Test
    fun `processImage should update state to processing`() = runTest {
        // Given
        whenever(processImageUseCase.invoke(any(), any()))
            .thenReturn(flowOf(Result.Success(testOCRResult)))

        // When
        viewModel.onEvent(OCREvent.ProcessImage(testImageUri, "eng"))

        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            // State should eventually show processing or result
            assertTrue(state.imageUri == testImageUri || state.isProcessing || state.extractedText.isNotEmpty())
        }
    }

    @Test
    fun `processImage should extract text successfully`() = runTest {
        // Given
        whenever(processImageUseCase.invoke(any(), any()))
            .thenReturn(flowOf(Result.Success(testOCRResult)))

        // When
        viewModel.onEvent(OCREvent.ProcessImage(testImageUri, "eng"))
        advanceUntilIdle()

        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(testImageUri, state.imageUri)
            assertEquals("Test extracted text", state.extractedText)
            assertFalse(state.isProcessing)
            assertNull(state.error)
            assertEquals(0.95f, state.confidenceScore)
            assertEquals("eng", state.language)
        }
    }

    @Test
    fun `processImage should handle errors gracefully`() = runTest {
        // Given
        val errorMessage = "OCR processing failed"
        whenever(processImageUseCase.invoke(any(), any()))
            .thenReturn(flowOf(Result.Error(errorMessage)))

        // When
        viewModel.onEvent(OCREvent.ProcessImage(testImageUri, "eng"))
        advanceUntilIdle()

        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            assertFalse(state.isProcessing)
            assertNotNull(state.error)
            assertTrue(state.error!!.contains("Failed to process image"))
        }
    }

    @Test
    fun `processImage should handle exceptions during processing`() = runTest {
        // Given
        whenever(processImageUseCase.invoke(any(), any()))
            .thenThrow(RuntimeException("Unexpected error"))

        // When
        viewModel.onEvent(OCREvent.ProcessImage(testImageUri, "eng"))
        advanceUntilIdle()

        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            assertFalse(state.isProcessing)
            assertNotNull(state.error)
            assertTrue(state.error!!.contains("error occurred"))
        }
    }

    @Test
    fun `processImage should use correct OCR config`() = runTest {
        // Given
        whenever(processImageUseCase.invoke(any(), any()))
            .thenReturn(flowOf(Result.Success(testOCRResult)))

        // When
        viewModel.onEvent(OCREvent.ProcessImage(testImageUri, "spa"))
        advanceUntilIdle()

        // Then
        verify(processImageUseCase).invoke(
            eq(testImageUri),
            argThat { config ->
                config.language == "spa" &&
                config.preprocessImage &&
                config.maxImageDimension == 2048
            }
        )
    }

    @Test
    fun `concurrent processImage calls should cancel previous job`() = runTest {
        // Given
        val firstUri = Uri.parse("content://test/image1.jpg")
        val secondUri = Uri.parse("content://test/image2.jpg")

        whenever(processImageUseCase.invoke(any(), any()))
            .thenReturn(flowOf(Result.Success(testOCRResult)))

        // When
        viewModel.onEvent(OCREvent.ProcessImage(firstUri, "eng"))
        viewModel.onEvent(OCREvent.ProcessImage(secondUri, "eng"))
        advanceUntilIdle()

        // Then
        verify(ocrService, atLeast(1)).stop() // Should stop previous job
        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(secondUri, state.imageUri) // Should have second URI
        }
    }

    // ============ Retry Processing Tests ============

    @Test
    fun `retryProcessing should process same image again`() = runTest {
        // Given - first process an image
        whenever(processImageUseCase.invoke(any(), any()))
            .thenReturn(flowOf(Result.Success(testOCRResult)))

        viewModel.onEvent(OCREvent.ProcessImage(testImageUri, "eng"))
        advanceUntilIdle()

        // When - retry
        viewModel.onEvent(OCREvent.RetryProcessing)
        advanceUntilIdle()

        // Then - should process twice
        verify(processImageUseCase, times(2)).invoke(eq(testImageUri), any())
    }

    @Test
    fun `retryProcessing with no image should do nothing`() = runTest {
        // When - retry without processing any image first
        viewModel.onEvent(OCREvent.RetryProcessing)
        advanceUntilIdle()

        // Then - should not call use case
        verify(processImageUseCase, never()).invoke(any(), any())
    }

    // ============ Save to History Tests ============

    @Test
    fun `saveToHistory should save scan result`() = runTest {
        // Given - process image first
        whenever(processImageUseCase.invoke(any(), any()))
            .thenReturn(flowOf(Result.Success(testOCRResult)))
        whenever(scanRepository.insertScan(any()))
            .thenReturn(Result.Success(1L))

        viewModel.onEvent(OCREvent.ProcessImage(testImageUri, "eng"))
        advanceUntilIdle()

        // When
        viewModel.onEvent(OCREvent.SaveToHistory)
        advanceUntilIdle()

        // Then
        verify(scanRepository).insertScan(argThat { scanResult ->
            scanResult.imageUri == testImageUri &&
            scanResult.extractedText == "Test extracted text" &&
            scanResult.language == "eng" &&
            scanResult.confidenceScore == 0.95f
        })

        viewModel.uiState.test {
            val state = awaitItem()
            assertTrue(state.isSaved)
        }
    }

    @Test
    fun `saveToHistory with empty text should not save`() = runTest {
        // Given - no text extracted
        whenever(processImageUseCase.invoke(any(), any()))
            .thenReturn(flowOf(Result.Success(OCRResult("", 0f, 0L))))

        viewModel.onEvent(OCREvent.ProcessImage(testImageUri, "eng"))
        advanceUntilIdle()

        // When
        viewModel.onEvent(OCREvent.SaveToHistory)
        advanceUntilIdle()

        // Then
        verify(scanRepository, never()).insertScan(any())
    }

    @Test
    fun `saveToHistory should handle repository errors`() = runTest {
        // Given
        whenever(processImageUseCase.invoke(any(), any()))
            .thenReturn(flowOf(Result.Success(testOCRResult)))
        whenever(scanRepository.insertScan(any()))
            .thenReturn(Result.Error("Database error"))

        viewModel.onEvent(OCREvent.ProcessImage(testImageUri, "eng"))
        advanceUntilIdle()

        // When
        viewModel.onEvent(OCREvent.SaveToHistory)
        advanceUntilIdle()

        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            assertFalse(state.isSaved)
            assertNotNull(state.error)
            assertTrue(state.error!!.contains("Failed to save"))
        }
    }

    @Test
    fun `saveToHistory should generate appropriate title from text`() = runTest {
        // Given - long text
        val longText = "This is a very long text that should be truncated when used as a title for the scan"
        val longTextResult = OCRResult(longText, 0.9f, 1000L)

        whenever(processImageUseCase.invoke(any(), any()))
            .thenReturn(flowOf(Result.Success(longTextResult)))
        whenever(scanRepository.insertScan(any()))
            .thenReturn(Result.Success(1L))

        viewModel.onEvent(OCREvent.ProcessImage(testImageUri, "eng"))
        advanceUntilIdle()

        // When
        viewModel.onEvent(OCREvent.SaveToHistory)
        advanceUntilIdle()

        // Then
        verify(scanRepository).insertScan(argThat { scanResult ->
            scanResult.title.length <= 50 &&
            scanResult.title.startsWith("This is a very long text")
        })
    }

    // ============ Error Handling Tests ============

    @Test
    fun `dismissError should clear error message`() = runTest {
        // Given - create error state
        whenever(processImageUseCase.invoke(any(), any()))
            .thenReturn(flowOf(Result.Error("Test error")))

        viewModel.onEvent(OCREvent.ProcessImage(testImageUri, "eng"))
        advanceUntilIdle()

        // When
        viewModel.onEvent(OCREvent.DismissError)

        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            assertNull(state.error)
        }
    }

    // ============ Cancellation Tests ============

    @Test
    fun `cancelProcessing should stop OCR and update state`() = runTest {
        // Given
        whenever(processImageUseCase.invoke(any(), any()))
            .thenReturn(flowOf(Result.Success(testOCRResult)))

        viewModel.onEvent(OCREvent.ProcessImage(testImageUri, "eng"))

        // When
        viewModel.cancelProcessing()
        advanceUntilIdle()

        // Then
        verify(ocrService).stop()
        viewModel.uiState.test {
            val state = awaitItem()
            assertFalse(state.isProcessing)
            assertEquals("Processing cancelled", state.error)
        }
    }

    @Test
    fun `cancelProcessing should handle OCR service errors gracefully`() = runTest {
        // Given
        `when`(ocrService.stop()).thenThrow(RuntimeException("Stop failed"))

        whenever(processImageUseCase.invoke(any(), any()))
            .thenReturn(flowOf(Result.Success(testOCRResult)))

        viewModel.onEvent(OCREvent.ProcessImage(testImageUri, "eng"))

        // When - should not throw
        viewModel.cancelProcessing()
        advanceUntilIdle()

        // Then - state should still be updated
        viewModel.uiState.test {
            val state = awaitItem()
            assertFalse(state.isProcessing)
        }
    }

    // ============ SetImageUri Tests ============

    @Test
    fun `setImageUri should process image with default language`() = runTest {
        // Given
        whenever(processImageUseCase.invoke(any(), any()))
            .thenReturn(flowOf(Result.Success(testOCRResult)))

        // When
        viewModel.setImageUri(testImageUri)
        advanceUntilIdle()

        // Then
        verify(processImageUseCase).invoke(eq(testImageUri), argThat { it.language == "eng" })
    }

    @Test
    fun `setImageUri should process image with custom language`() = runTest {
        // Given
        whenever(processImageUseCase.invoke(any(), any()))
            .thenReturn(flowOf(Result.Success(testOCRResult)))

        // When
        viewModel.setImageUri(testImageUri, "fra")
        advanceUntilIdle()

        // Then
        verify(processImageUseCase).invoke(eq(testImageUri), argThat { it.language == "fra" })
    }

    // ============ Cleanup Tests ============

    @Test
    fun `onCleared should cleanup resources`() = runTest {
        // Given
        whenever(processImageUseCase.invoke(any(), any()))
            .thenReturn(flowOf(Result.Success(testOCRResult)))

        viewModel.onEvent(OCREvent.ProcessImage(testImageUri, "eng"))

        // When - simulate ViewModel being cleared
        // Note: We can't actually call onCleared() from test, but we can verify cleanup logic
        viewModel.cancelProcessing()
        advanceUntilIdle()

        // Then
        verify(ocrService, atLeastOnce()).stop()
    }
}
