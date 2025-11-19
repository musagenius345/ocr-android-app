package com.musagenius.ocrapp.presentation.viewmodel

import android.net.Uri
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.camera.view.PreviewView
import androidx.lifecycle.LifecycleOwner
import app.cash.turbine.test
import com.musagenius.ocrapp.data.camera.CameraManager
import com.musagenius.ocrapp.data.camera.LowLightDetector
import com.musagenius.ocrapp.data.utils.ImageCompressor
import com.musagenius.ocrapp.data.utils.StorageManager
import com.musagenius.ocrapp.domain.model.Result
import com.musagenius.ocrapp.presentation.ui.camera.CameraEvent
import com.musagenius.ocrapp.presentation.ui.camera.CameraFacing
import com.musagenius.ocrapp.presentation.ui.camera.CameraResolution
import com.musagenius.ocrapp.presentation.ui.camera.FlashMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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

/**
 * Comprehensive unit tests for CameraViewModel
 */
@OptIn(ExperimentalCoroutinesApi::class)
class CameraViewModelTest {

    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()

    @Mock
    private lateinit var cameraManager: CameraManager

    @Mock
    private lateinit var imageCompressor: ImageCompressor

    @Mock
    private lateinit var storageManager: StorageManager

    @Mock
    private lateinit var lifecycleOwner: LifecycleOwner

    @Mock
    private lateinit var previewView: PreviewView

    private lateinit var viewModel: CameraViewModel

    private val testCapturedUri = Uri.parse("content://test/captured.jpg")
    private val testCompressedUri = Uri.parse("content://test/compressed.jpg")

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        Dispatchers.setMain(testDispatcher)
        viewModel = CameraViewModel(cameraManager, imageCompressor, storageManager)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ============ Initial State Tests ============

    @Test
    fun `initial state should have default values`() = runTest {
        viewModel.uiState.test {
            val state = awaitItem()
            assertFalse(state.isLoading)
            assertNull(state.capturedImageUri)
            assertNull(state.error)
            assertEquals(FlashMode.OFF, state.flashMode)
            assertFalse(state.isProcessing)
            assertEquals(1f, state.zoomRatio)
            assertEquals(CameraFacing.BACK, state.cameraFacing)
            assertFalse(state.showGridOverlay)
            assertEquals(0, state.exposureCompensation)
            assertEquals(CameraResolution.HD, state.resolution)
            assertFalse(state.showResolutionDialog)
        }
    }

    // ============ Image Capture Tests ============

    @Test
    fun `captureImage should capture and compress image successfully`() = runTest {
        // Given
        whenever(storageManager.hasAvailableStorage(any())).thenReturn(true)
        whenever(cameraManager.captureImage()).thenReturn(testCapturedUri)
        whenever(imageCompressor.compressImage(any(), any(), any()))
            .thenReturn(Result.Success(testCompressedUri))
        whenever(imageCompressor.getFileSizeKB(any())).thenReturn(500L)

        // When
        viewModel.onEvent(CameraEvent.CaptureImage)
        advanceUntilIdle()

        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(testCompressedUri, state.capturedImageUri)
            assertFalse(state.isProcessing)
            assertNull(state.error)
        }
        verify(storageManager).hasAvailableStorage(50L)
        verify(cameraManager).captureImage()
        verify(imageCompressor).compressImage(testCapturedUri, 85, 2048)
    }

    @Test
    fun `captureImage should fail when insufficient storage`() = runTest {
        // Given
        whenever(storageManager.hasAvailableStorage(any())).thenReturn(false)

        // When
        viewModel.onEvent(CameraEvent.CaptureImage)
        advanceUntilIdle()

        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            assertFalse(state.isProcessing)
            assertNotNull(state.error)
            assertTrue(state.error!!.contains("Insufficient storage"))
            assertNull(state.capturedImageUri)
        }
        verify(cameraManager, never()).captureImage()
    }

    @Test
    fun `captureImage should use raw image when compression fails`() = runTest {
        // Given
        whenever(storageManager.hasAvailableStorage(any())).thenReturn(true)
        whenever(cameraManager.captureImage()).thenReturn(testCapturedUri)
        whenever(imageCompressor.compressImage(any(), any(), any()))
            .thenReturn(Result.Error("Compression failed"))

        // When
        viewModel.onEvent(CameraEvent.CaptureImage)
        advanceUntilIdle()

        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(testCapturedUri, state.capturedImageUri)
            assertNotNull(state.error)
            assertTrue(state.error!!.contains("compression failed"))
        }
    }

    @Test
    fun `captureImage should handle capture errors`() = runTest {
        // Given
        whenever(storageManager.hasAvailableStorage(any())).thenReturn(true)
        whenever(cameraManager.captureImage()).thenThrow(RuntimeException("Camera error"))

        // When
        viewModel.onEvent(CameraEvent.CaptureImage)
        advanceUntilIdle()

        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            assertFalse(state.isProcessing)
            assertNotNull(state.error)
            assertTrue(state.error!!.contains("Failed to capture"))
            assertNull(state.capturedImageUri)
        }
    }

    // ============ Flash Mode Tests ============

    @Test
    fun `toggleFlash should cycle through flash modes`() = runTest {
        // Then - observe the full transition sequence
        viewModel.uiState.test {
            // Initial state - OFF
            val initialState = awaitItem()
            assertEquals(FlashMode.OFF, initialState.flashMode)

            // When - toggle from OFF to ON
            viewModel.onEvent(CameraEvent.ToggleFlash)
            val stateOn = awaitItem()
            assertEquals(FlashMode.ON, stateOn.flashMode)

            // When - toggle from ON to AUTO
            viewModel.onEvent(CameraEvent.ToggleFlash)
            val stateAuto = awaitItem()
            assertEquals(FlashMode.AUTO, stateAuto.flashMode)

            // When - toggle from AUTO back to OFF
            viewModel.onEvent(CameraEvent.ToggleFlash)
            val stateOff = awaitItem()
            assertEquals(FlashMode.OFF, stateOff.flashMode)
        }

        // Verify camera manager calls in order
        val inOrder = inOrder(cameraManager)
        inOrder.verify(cameraManager).setFlashMode(FlashMode.ON)
        inOrder.verify(cameraManager).setFlashMode(FlashMode.AUTO)
        inOrder.verify(cameraManager).setFlashMode(FlashMode.OFF)
    }

    // ============ Zoom Tests ============

    @Test
    fun `setZoom should update zoom ratio within valid range`() = runTest {
        // When
        viewModel.onEvent(CameraEvent.SetZoom(2.5f))

        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(2.5f, state.zoomRatio)
        }
        verify(cameraManager).setZoomRatio(2.5f)
    }

    @Test
    fun `setZoom should clamp zoom ratio to min value`() = runTest {
        // When - try to set zoom below min (1.0)
        viewModel.onEvent(CameraEvent.SetZoom(0.5f))

        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(1f, state.zoomRatio) // Clamped to min
        }
        verify(cameraManager).setZoomRatio(1f)
    }

    @Test
    fun `setZoom should clamp zoom ratio to max value`() = runTest {
        // Given - assuming max zoom is 1.0 by default
        // When - try to set zoom above max
        viewModel.onEvent(CameraEvent.SetZoom(10f))

        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            assertTrue(state.zoomRatio <= state.maxZoomRatio)
        }
    }

    // ============ Exposure Tests ============

    @Test
    fun `setExposure should update exposure compensation`() = runTest {
        // When
        viewModel.onEvent(CameraEvent.SetExposure(2))

        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(2, state.exposureCompensation)
        }
        verify(cameraManager).setExposureCompensation(2)
    }

    @Test
    fun `setExposure should clamp to min exposure`() = runTest {
        // When - try to set below min (0 by default)
        viewModel.onEvent(CameraEvent.SetExposure(-10))

        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(0, state.exposureCompensation) // Clamped to min
        }
    }

    @Test
    fun `setExposure should clamp to max exposure`() = runTest {
        // When - try to set above max (0 by default)
        viewModel.onEvent(CameraEvent.SetExposure(10))

        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(0, state.exposureCompensation) // Clamped to max
        }
    }

    // ============ Camera Flip Tests ============

    @Test
    fun `flipCamera should switch camera facing`() = runTest {
        // Given
        whenever(cameraManager.getCurrentCameraFacing()).thenReturn(CameraFacing.FRONT)

        // When
        viewModel.onEvent(CameraEvent.FlipCamera)
        advanceUntilIdle()

        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(CameraFacing.FRONT, state.cameraFacing)
            assertFalse(state.isLoading)
        }
        verify(cameraManager).flipCamera()
    }

    @Test
    fun `flipCamera should handle errors gracefully`() = runTest {
        // Given
        doThrow(RuntimeException("Flip failed")).whenever(cameraManager).flipCamera()

        // When
        viewModel.onEvent(CameraEvent.FlipCamera)
        advanceUntilIdle()

        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            assertFalse(state.isLoading)
            assertNotNull(state.error)
            assertTrue(state.error!!.contains("Failed to flip"))
        }
    }

    // ============ Grid Overlay Tests ============

    @Test
    fun `toggleGridOverlay should toggle grid visibility`() = runTest {
        // When - toggle on
        viewModel.onEvent(CameraEvent.ToggleGridOverlay)

        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            assertTrue(state.showGridOverlay)
        }

        // When - toggle off
        viewModel.onEvent(CameraEvent.ToggleGridOverlay)

        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            assertFalse(state.showGridOverlay)
        }
    }

    // ============ Lighting Condition Tests ============

    @Test
    fun `updateLightingCondition should update lighting state`() = runTest {
        // When
        viewModel.onEvent(CameraEvent.UpdateLightingCondition(LowLightDetector.LightingCondition.LOW_LIGHT))

        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(LowLightDetector.LightingCondition.LOW_LIGHT, state.lightingCondition)
        }
    }

    @Test
    fun `dismissLowLightWarning should hide warning`() = runTest {
        // When
        viewModel.onEvent(CameraEvent.DismissLowLightWarning)

        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            assertFalse(state.showLowLightWarning)
        }
    }

    // ============ Resolution Tests ============

    @Test
    fun `showResolutionDialog should display dialog`() = runTest {
        // When
        viewModel.onEvent(CameraEvent.ShowResolutionDialog)

        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            assertTrue(state.showResolutionDialog)
        }
    }

    @Test
    fun `dismissResolutionDialog should hide dialog`() = runTest {
        // Given - show dialog first
        viewModel.onEvent(CameraEvent.ShowResolutionDialog)

        // When
        viewModel.onEvent(CameraEvent.DismissResolutionDialog)

        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            assertFalse(state.showResolutionDialog)
        }
    }

    // ============ Error Handling Tests ============

    @Test
    fun `dismissError should clear error message`() = runTest {
        // Given - create error state
        whenever(storageManager.hasAvailableStorage(any())).thenReturn(false)
        viewModel.onEvent(CameraEvent.CaptureImage)
        advanceUntilIdle()

        // When
        viewModel.onEvent(CameraEvent.DismissError)

        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            assertNull(state.error)
        }
    }

    // ============ Tap to Focus Tests ============

    @Test
    fun `tapToFocus should call camera manager with coordinates`() = runTest {
        // When
        viewModel.onEvent(CameraEvent.TapToFocus(0.5f, 0.5f))
        advanceUntilIdle()

        // Then
        verify(cameraManager).focusOnPoint(0.5f, 0.5f, 1, 1)
    }

    @Test
    fun `tapToFocus should handle errors silently`() = runTest {
        // Given
        doThrow(RuntimeException("Focus failed"))
            .whenever(cameraManager).focusOnPoint(any(), any(), any(), any())

        // When - should not crash
        viewModel.onEvent(CameraEvent.TapToFocus(0.5f, 0.5f))
        advanceUntilIdle()

        // Then - error is logged but state unchanged
        viewModel.uiState.test {
            val state = awaitItem()
            // Should not have error in UI state for focus failures
            assertNull(state.error)
        }
    }
}
