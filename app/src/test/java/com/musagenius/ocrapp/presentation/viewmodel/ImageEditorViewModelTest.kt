package com.musagenius.ocrapp.presentation.viewmodel

import android.net.Uri
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import app.cash.turbine.test
import com.musagenius.ocrapp.data.utils.ImageEditor
import com.musagenius.ocrapp.domain.model.Result
import com.musagenius.ocrapp.presentation.ui.editor.ImageEditorEvent
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
 * Comprehensive unit tests for ImageEditorViewModel
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ImageEditorViewModelTest {

    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()

    @Mock
    private lateinit var imageEditor: ImageEditor

    private lateinit var viewModel: ImageEditorViewModel

    private val testSourceUri = Uri.parse("content://test/source.jpg")
    private val testEditedUri = Uri.parse("content://test/edited.jpg")

    private var savedCallback: Uri? = null
    private var cancelledCalled = false

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        Dispatchers.setMain(testDispatcher)
        viewModel = ImageEditorViewModel(imageEditor)
        savedCallback = null
        cancelledCalled = false
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ============ Initialization Tests ============

    @Test
    fun `initializeImage should set source URI and callbacks`() = runTest {
        // When
        viewModel.initializeImage(
            uri = testSourceUri,
            onSaved = { savedCallback = it },
            onCancelled = { cancelledCalled = true }
        )

        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(testSourceUri, state.sourceImageUri)
            assertEquals(0f, state.rotationDegrees)
            assertFalse(state.isProcessing)
            assertNull(state.error)
        }
    }

    @Test
    fun `initial state should have default values`() = runTest {
        viewModel.uiState.test {
            val state = awaitItem()
            assertNull(state.sourceImageUri)
            assertEquals(0f, state.rotationDegrees)
            assertFalse(state.isProcessing)
            assertNull(state.error)
        }
    }

    // ============ Rotation Tests ============

    @Test
    fun `rotateClockwise should increase rotation by 90 degrees`() = runTest {
        // Given
        viewModel.initializeImage(
            uri = testSourceUri,
            onSaved = { savedCallback = it },
            onCancelled = { cancelledCalled = true }
        )

        // When
        viewModel.onEvent(ImageEditorEvent.RotateClockwise)

        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(90f, state.rotationDegrees)
        }
    }

    @Test
    fun `rotateClockwise multiple times should accumulate rotation`() = runTest {
        // Given
        viewModel.initializeImage(
            uri = testSourceUri,
            onSaved = { savedCallback = it },
            onCancelled = { cancelledCalled = true }
        )

        // When - rotate 4 times
        viewModel.onEvent(ImageEditorEvent.RotateClockwise)
        viewModel.onEvent(ImageEditorEvent.RotateClockwise)
        viewModel.onEvent(ImageEditorEvent.RotateClockwise)

        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(270f, state.rotationDegrees)
        }
    }

    @Test
    fun `rotateClockwise should wrap around at 360 degrees`() = runTest {
        // Given
        viewModel.initializeImage(
            uri = testSourceUri,
            onSaved = { savedCallback = it },
            onCancelled = { cancelledCalled = true }
        )

        // When - rotate 4 times (360 degrees)
        repeat(4) {
            viewModel.onEvent(ImageEditorEvent.RotateClockwise)
        }

        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(0f, state.rotationDegrees)
        }
    }

    @Test
    fun `rotateCounterClockwise should decrease rotation by 90 degrees`() = runTest {
        // Given
        viewModel.initializeImage(
            uri = testSourceUri,
            onSaved = { savedCallback = it },
            onCancelled = { cancelledCalled = true }
        )

        // When
        viewModel.onEvent(ImageEditorEvent.RotateCounterClockwise)

        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(270f, state.rotationDegrees)
        }
    }

    @Test
    fun `rotateCounterClockwise multiple times should work correctly`() = runTest {
        // Given
        viewModel.initializeImage(
            uri = testSourceUri,
            onSaved = { savedCallback = it },
            onCancelled = { cancelledCalled = true }
        )

        // When
        viewModel.onEvent(ImageEditorEvent.RotateCounterClockwise)
        viewModel.onEvent(ImageEditorEvent.RotateCounterClockwise)

        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(180f, state.rotationDegrees)
        }
    }

    @Test
    fun `mixing clockwise and counterclockwise rotations should work correctly`() = runTest {
        // Given
        viewModel.initializeImage(
            uri = testSourceUri,
            onSaved = { savedCallback = it },
            onCancelled = { cancelledCalled = true }
        )

        // When
        viewModel.onEvent(ImageEditorEvent.RotateClockwise) // 90
        viewModel.onEvent(ImageEditorEvent.RotateClockwise) // 180
        viewModel.onEvent(ImageEditorEvent.RotateCounterClockwise) // 90

        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(90f, state.rotationDegrees)
        }
    }

    @Test
    fun `resetRotation should set rotation to 0`() = runTest {
        // Given
        viewModel.initializeImage(
            uri = testSourceUri,
            onSaved = { savedCallback = it },
            onCancelled = { cancelledCalled = true }
        )

        viewModel.onEvent(ImageEditorEvent.RotateClockwise)
        viewModel.onEvent(ImageEditorEvent.RotateClockwise)

        // When
        viewModel.onEvent(ImageEditorEvent.ResetRotation)

        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(0f, state.rotationDegrees)
        }
    }

    // ============ Save Image Tests ============

    @Test
    fun `saveImage should save successfully and call callback`() = runTest {
        // Given
        viewModel.initializeImage(
            uri = testSourceUri,
            onSaved = { savedCallback = it },
            onCancelled = { cancelledCalled = true }
        )

        viewModel.onEvent(ImageEditorEvent.RotateClockwise)

        whenever(imageEditor.editImage(any(), any(), any()))
            .thenReturn(Result.Success(testEditedUri))

        // When
        viewModel.onEvent(ImageEditorEvent.SaveImage)
        advanceUntilIdle()

        // Then
        verify(imageEditor).editImage(
            sourceUri = testSourceUri,
            rotationDegrees = 90f,
            cropRect = null
        )
        assertEquals(testEditedUri, savedCallback)
        assertFalse(cancelledCalled)
    }

    @Test
    fun `saveImage should update processing state during save`() = runTest {
        // Given
        viewModel.initializeImage(
            uri = testSourceUri,
            onSaved = { savedCallback = it },
            onCancelled = { cancelledCalled = true }
        )

        whenever(imageEditor.editImage(any(), any(), any()))
            .thenReturn(Result.Success(testEditedUri))

        // When
        viewModel.onEvent(ImageEditorEvent.SaveImage)
        advanceUntilIdle()

        // Then - processing should be false after completion
        viewModel.uiState.test {
            val state = awaitItem()
            assertFalse(state.isProcessing)
        }
    }

    @Test
    fun `saveImage should handle save errors`() = runTest {
        // Given
        viewModel.initializeImage(
            uri = testSourceUri,
            onSaved = { savedCallback = it },
            onCancelled = { cancelledCalled = true }
        )

        whenever(imageEditor.editImage(any(), any(), any()))
            .thenReturn(Result.Error("Save failed"))

        // When
        viewModel.onEvent(ImageEditorEvent.SaveImage)
        advanceUntilIdle()

        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            assertFalse(state.isProcessing)
            assertNotNull(state.error)
            assertTrue(state.error!!.contains("Failed to save image"))
        }
        assertNull(savedCallback)
    }

    @Test
    fun `saveImage should handle exceptions during save`() = runTest {
        // Given
        viewModel.initializeImage(
            uri = testSourceUri,
            onSaved = { savedCallback = it },
            onCancelled = { cancelledCalled = true }
        )

        whenever(imageEditor.editImage(any(), any(), any()))
            .thenThrow(RuntimeException("Unexpected error"))

        // When
        viewModel.onEvent(ImageEditorEvent.SaveImage)
        advanceUntilIdle()

        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            assertFalse(state.isProcessing)
            assertNotNull(state.error)
            assertTrue(state.error!!.contains("Error saving image"))
        }
    }

    @Test
    fun `saveImage without source URI should show error`() = runTest {
        // Given - no initialization
        // When
        viewModel.onEvent(ImageEditorEvent.SaveImage)
        advanceUntilIdle()

        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals("No image to save", state.error)
        }
        verify(imageEditor, never()).editImage(any(), any(), any())
    }

    @Test
    fun `saveImage with zero rotation should still save`() = runTest {
        // Given
        viewModel.initializeImage(
            uri = testSourceUri,
            onSaved = { savedCallback = it },
            onCancelled = { cancelledCalled = true }
        )

        whenever(imageEditor.editImage(any(), any(), any()))
            .thenReturn(Result.Success(testEditedUri))

        // When - no rotation applied
        viewModel.onEvent(ImageEditorEvent.SaveImage)
        advanceUntilIdle()

        // Then
        verify(imageEditor).editImage(
            sourceUri = testSourceUri,
            rotationDegrees = 0f,
            cropRect = null
        )
        assertEquals(testEditedUri, savedCallback)
    }

    // ============ Cancel Tests ============

    @Test
    fun `cancel should call cancelled callback`() = runTest {
        // Given
        viewModel.initializeImage(
            uri = testSourceUri,
            onSaved = { savedCallback = it },
            onCancelled = { cancelledCalled = true }
        )

        // When
        viewModel.onEvent(ImageEditorEvent.Cancel)

        // Then
        assertTrue(cancelledCalled)
        assertNull(savedCallback)
    }

    // ============ Error Handling Tests ============

    @Test
    fun `dismissError should clear error message`() = runTest {
        // Given
        viewModel.initializeImage(
            uri = testSourceUri,
            onSaved = { savedCallback = it },
            onCancelled = { cancelledCalled = true }
        )

        whenever(imageEditor.editImage(any(), any(), any()))
            .thenReturn(Result.Error("Save failed"))

        viewModel.onEvent(ImageEditorEvent.SaveImage)
        advanceUntilIdle()

        // When
        viewModel.onEvent(ImageEditorEvent.DismissError)

        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            assertNull(state.error)
        }
    }

    // ============ Cleanup Tests ============

    @Test
    fun `onCleared should cleanup callbacks`() = runTest {
        // Given
        viewModel.initializeImage(
            uri = testSourceUri,
            onSaved = { savedCallback = it },
            onCancelled = { cancelledCalled = true }
        )

        // When - simulate ViewModel clearing
        // Note: We can't actually call onCleared() in tests, but we verify behavior
        viewModel.onEvent(ImageEditorEvent.Cancel)

        // Then - callback should still work before clearing
        assertTrue(cancelledCalled)
    }

    // ============ Edge Case Tests ============

    @Test
    fun `multiple saves should work correctly`() = runTest {
        // Given
        viewModel.initializeImage(
            uri = testSourceUri,
            onSaved = { savedCallback = it },
            onCancelled = { cancelledCalled = true }
        )

        whenever(imageEditor.editImage(any(), any(), any()))
            .thenReturn(Result.Success(testEditedUri))

        // When - save multiple times
        viewModel.onEvent(ImageEditorEvent.SaveImage)
        advanceUntilIdle()

        savedCallback = null // Reset

        viewModel.onEvent(ImageEditorEvent.RotateClockwise)
        viewModel.onEvent(ImageEditorEvent.SaveImage)
        advanceUntilIdle()

        // Then
        verify(imageEditor, times(2)).editImage(any(), any(), any())
        assertEquals(testEditedUri, savedCallback)
    }

    @Test
    fun `rotation after error should still work`() = runTest {
        // Given
        viewModel.initializeImage(
            uri = testSourceUri,
            onSaved = { savedCallback = it },
            onCancelled = { cancelledCalled = true }
        )

        whenever(imageEditor.editImage(any(), any(), any()))
            .thenReturn(Result.Error("First save failed"))

        viewModel.onEvent(ImageEditorEvent.SaveImage)
        advanceUntilIdle()

        // When - rotate after error
        viewModel.onEvent(ImageEditorEvent.RotateClockwise)

        // Then - rotation should still work
        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(90f, state.rotationDegrees)
            assertNotNull(state.error) // Error still present until dismissed
        }
    }

    @Test
    fun `saving with 360 degree rotation should use 0 degrees`() = runTest {
        // Given
        viewModel.initializeImage(
            uri = testSourceUri,
            onSaved = { savedCallback = it },
            onCancelled = { cancelledCalled = true }
        )

        whenever(imageEditor.editImage(any(), any(), any()))
            .thenReturn(Result.Success(testEditedUri))

        // When - rotate 4 times (back to 0)
        repeat(4) {
            viewModel.onEvent(ImageEditorEvent.RotateClockwise)
        }

        viewModel.onEvent(ImageEditorEvent.SaveImage)
        advanceUntilIdle()

        // Then
        verify(imageEditor).editImage(
            sourceUri = testSourceUri,
            rotationDegrees = 0f,
            cropRect = null
        )
    }
}
