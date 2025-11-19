package com.musagenius.ocrapp.presentation.viewmodel

import android.net.Uri
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.musagenius.ocrapp.domain.model.Result
import com.musagenius.ocrapp.domain.model.ScanResult
import com.musagenius.ocrapp.domain.usecase.*
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
 * Comprehensive unit tests for ScanDetailViewModel
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ScanDetailViewModelTest {

    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()

    @Mock
    private lateinit var getScanByIdUseCase: GetScanByIdUseCase

    @Mock
    private lateinit var updateExtractedTextUseCase: UpdateExtractedTextUseCase

    @Mock
    private lateinit var updateTitleAndNotesUseCase: UpdateTitleAndNotesUseCase

    @Mock
    private lateinit var updateFavoriteStatusUseCase: UpdateFavoriteStatusUseCase

    @Mock
    private lateinit var deleteScanUseCase: DeleteScanUseCase

    private lateinit var savedStateHandle: SavedStateHandle
    private lateinit var viewModel: ScanDetailViewModel

    private val testScanResult = ScanResult(
        id = 1L,
        timestamp = Date(),
        imageUri = Uri.parse("content://test/image.jpg"),
        extractedText = "Original text",
        language = "eng",
        confidenceScore = 0.95f,
        title = "Original Title",
        tags = listOf("test"),
        notes = "Original notes",
        isFavorite = false,
        modifiedTimestamp = Date()
    )

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(scanId: String?) {
        savedStateHandle = SavedStateHandle(mapOf("scanId" to scanId))
        viewModel = ScanDetailViewModel(
            getScanByIdUseCase,
            updateExtractedTextUseCase,
            updateTitleAndNotesUseCase,
            updateFavoriteStatusUseCase,
            deleteScanUseCase,
            savedStateHandle
        )
    }

    // ============ Initial State Tests ============

    @Test
    fun `initial state with valid scanId should load scan`() = runTest {
        // Given
        whenever(getScanByIdUseCase.invoke(1L))
            .thenReturn(flowOf(Result.Success(testScanResult)))

        // When
        createViewModel("1")
        advanceUntilIdle()

        // Then
        viewModel.state.test {
            val state = awaitItem()
            assertEquals(testScanResult, state.scan)
            assertEquals("Original text", state.editedText)
            assertEquals("Original Title", state.editedTitle)
            assertEquals("Original notes", state.editedNotes)
            assertFalse(state.isLoading)
            assertNull(state.error)
        }
    }

    @Test
    fun `initial state with invalid scanId should show error`() = runTest {
        // When
        createViewModel("invalid")
        advanceUntilIdle()

        // Then
        viewModel.state.test {
            val state = awaitItem()
            assertNull(state.scan)
            assertFalse(state.isLoading)
            assertEquals("Invalid scan ID", state.error)
        }
        verify(getScanByIdUseCase, never()).invoke(any())
    }

    @Test
    fun `initial state with null scanId should show error`() = runTest {
        // When
        createViewModel(null)
        advanceUntilIdle()

        // Then
        viewModel.state.test {
            val state = awaitItem()
            assertNull(state.scan)
            assertFalse(state.isLoading)
            assertEquals("Invalid scan ID", state.error)
        }
    }

    @Test
    fun `initial load should handle scan not found error`() = runTest {
        // Given
        whenever(getScanByIdUseCase.invoke(999L))
            .thenReturn(flowOf(Result.Error("Scan not found")))

        // When
        createViewModel("999")
        advanceUntilIdle()

        // Then
        viewModel.state.test {
            val state = awaitItem()
            assertNull(state.scan)
            assertFalse(state.isLoading)
            assertEquals("Scan not found", state.error)
        }
    }

    // ============ Reload Scan Tests ============

    @Test
    fun `reloadScan should reload scan data`() = runTest {
        // Given
        whenever(getScanByIdUseCase.invoke(1L))
            .thenReturn(flowOf(Result.Success(testScanResult)))

        createViewModel("1")
        advanceUntilIdle()

        // When
        viewModel.reloadScan()
        advanceUntilIdle()

        // Then
        verify(getScanByIdUseCase, atLeast(2)).invoke(1L)
    }

    // ============ Edit Text Tests ============

    @Test
    fun `startEditingText should enable text editing mode`() = runTest {
        // Given
        whenever(getScanByIdUseCase.invoke(1L))
            .thenReturn(flowOf(Result.Success(testScanResult)))

        createViewModel("1")
        advanceUntilIdle()

        // When
        viewModel.startEditingText()

        // Then
        viewModel.state.test {
            val state = awaitItem()
            assertTrue(state.isEditingText)
            assertEquals("Original text", state.editedText)
        }
    }

    @Test
    fun `updateEditedText should update edited text`() = runTest {
        // Given
        whenever(getScanByIdUseCase.invoke(1L))
            .thenReturn(flowOf(Result.Success(testScanResult)))

        createViewModel("1")
        advanceUntilIdle()

        // When
        viewModel.updateEditedText("New edited text")

        // Then
        viewModel.state.test {
            val state = awaitItem()
            assertEquals("New edited text", state.editedText)
        }
    }

    @Test
    fun `saveEditedText should save text successfully`() = runTest {
        // Given
        whenever(getScanByIdUseCase.invoke(1L))
            .thenReturn(flowOf(Result.Success(testScanResult)))
        whenever(updateExtractedTextUseCase.invoke(1L, "Modified text"))
            .thenReturn(Result.Success(Unit))

        createViewModel("1")
        advanceUntilIdle()

        viewModel.startEditingText()
        viewModel.updateEditedText("Modified text")

        // When
        viewModel.saveEditedText()
        advanceUntilIdle()

        // Then
        verify(updateExtractedTextUseCase).invoke(1L, "Modified text")
        viewModel.state.test {
            val state = awaitItem()
            assertFalse(state.isEditingText)
            assertFalse(state.isSaving)
            assertEquals("Text updated", state.snackbarMessage)
        }
    }

    @Test
    fun `saveEditedText should handle save errors`() = runTest {
        // Given
        whenever(getScanByIdUseCase.invoke(1L))
            .thenReturn(flowOf(Result.Success(testScanResult)))
        whenever(updateExtractedTextUseCase.invoke(any(), any()))
            .thenReturn(Result.Error("Save failed"))

        createViewModel("1")
        advanceUntilIdle()

        viewModel.startEditingText()
        viewModel.updateEditedText("Modified text")

        // When
        viewModel.saveEditedText()
        advanceUntilIdle()

        // Then
        viewModel.state.test {
            val state = awaitItem()
            assertFalse(state.isSaving)
            assertEquals("Failed to save text", state.snackbarMessage)
            assertNotNull(state.error)
        }
    }

    @Test
    fun `cancelEditingText should restore original text`() = runTest {
        // Given
        whenever(getScanByIdUseCase.invoke(1L))
            .thenReturn(flowOf(Result.Success(testScanResult)))

        createViewModel("1")
        advanceUntilIdle()

        viewModel.startEditingText()
        viewModel.updateEditedText("Modified but cancelled")

        // When
        viewModel.cancelEditingText()

        // Then
        viewModel.state.test {
            val state = awaitItem()
            assertFalse(state.isEditingText)
            assertEquals("Original text", state.editedText)
        }
    }

    // ============ Edit Title and Notes Tests ============

    @Test
    fun `startEditingTitleNotes should enable editing mode`() = runTest {
        // Given
        whenever(getScanByIdUseCase.invoke(1L))
            .thenReturn(flowOf(Result.Success(testScanResult)))

        createViewModel("1")
        advanceUntilIdle()

        // When
        viewModel.startEditingTitleNotes()

        // Then
        viewModel.state.test {
            val state = awaitItem()
            assertTrue(state.isEditingTitleNotes)
            assertEquals("Original Title", state.editedTitle)
            assertEquals("Original notes", state.editedNotes)
        }
    }

    @Test
    fun `updateEditedTitle should update edited title`() = runTest {
        // Given
        whenever(getScanByIdUseCase.invoke(1L))
            .thenReturn(flowOf(Result.Success(testScanResult)))

        createViewModel("1")
        advanceUntilIdle()

        // When
        viewModel.updateEditedTitle("New Title")

        // Then
        viewModel.state.test {
            val state = awaitItem()
            assertEquals("New Title", state.editedTitle)
        }
    }

    @Test
    fun `updateEditedNotes should update edited notes`() = runTest {
        // Given
        whenever(getScanByIdUseCase.invoke(1L))
            .thenReturn(flowOf(Result.Success(testScanResult)))

        createViewModel("1")
        advanceUntilIdle()

        // When
        viewModel.updateEditedNotes("New notes")

        // Then
        viewModel.state.test {
            val state = awaitItem()
            assertEquals("New notes", state.editedNotes)
        }
    }

    @Test
    fun `saveEditedTitleNotes should save successfully`() = runTest {
        // Given
        whenever(getScanByIdUseCase.invoke(1L))
            .thenReturn(flowOf(Result.Success(testScanResult)))
        whenever(updateTitleAndNotesUseCase.invoke(1L, "New Title", "New notes"))
            .thenReturn(Result.Success(Unit))

        createViewModel("1")
        advanceUntilIdle()

        viewModel.startEditingTitleNotes()
        viewModel.updateEditedTitle("New Title")
        viewModel.updateEditedNotes("New notes")

        // When
        viewModel.saveEditedTitleNotes()
        advanceUntilIdle()

        // Then
        verify(updateTitleAndNotesUseCase).invoke(1L, "New Title", "New notes")
        viewModel.state.test {
            val state = awaitItem()
            assertFalse(state.isEditingTitleNotes)
            assertFalse(state.isSaving)
            assertEquals("Title and notes updated", state.snackbarMessage)
        }
    }

    @Test
    fun `saveEditedTitleNotes should handle save errors`() = runTest {
        // Given
        whenever(getScanByIdUseCase.invoke(1L))
            .thenReturn(flowOf(Result.Success(testScanResult)))
        whenever(updateTitleAndNotesUseCase.invoke(any(), any(), any()))
            .thenReturn(Result.Error("Save failed"))

        createViewModel("1")
        advanceUntilIdle()

        viewModel.startEditingTitleNotes()
        viewModel.updateEditedTitle("New Title")

        // When
        viewModel.saveEditedTitleNotes()
        advanceUntilIdle()

        // Then
        viewModel.state.test {
            val state = awaitItem()
            assertFalse(state.isSaving)
            assertEquals("Failed to save changes", state.snackbarMessage)
        }
    }

    @Test
    fun `cancelEditingTitleNotes should restore original values`() = runTest {
        // Given
        whenever(getScanByIdUseCase.invoke(1L))
            .thenReturn(flowOf(Result.Success(testScanResult)))

        createViewModel("1")
        advanceUntilIdle()

        viewModel.startEditingTitleNotes()
        viewModel.updateEditedTitle("Modified but cancelled")
        viewModel.updateEditedNotes("Modified notes but cancelled")

        // When
        viewModel.cancelEditingTitleNotes()

        // Then
        viewModel.state.test {
            val state = awaitItem()
            assertFalse(state.isEditingTitleNotes)
            assertEquals("Original Title", state.editedTitle)
            assertEquals("Original notes", state.editedNotes)
        }
    }

    // ============ Favorite Tests ============

    @Test
    fun `toggleFavorite should mark as favorite`() = runTest {
        // Given
        whenever(getScanByIdUseCase.invoke(1L))
            .thenReturn(flowOf(Result.Success(testScanResult)))
        whenever(updateFavoriteStatusUseCase.invoke(1L, true))
            .thenReturn(Result.Success(Unit))

        createViewModel("1")
        advanceUntilIdle()

        // When
        viewModel.toggleFavorite()
        advanceUntilIdle()

        // Then
        verify(updateFavoriteStatusUseCase).invoke(1L, true)
        viewModel.state.test {
            val state = awaitItem()
            assertEquals("Added to favorites", state.snackbarMessage)
        }
    }

    @Test
    fun `toggleFavorite should remove from favorites`() = runTest {
        // Given
        val favoriteScan = testScanResult.copy(isFavorite = true)
        whenever(getScanByIdUseCase.invoke(1L))
            .thenReturn(flowOf(Result.Success(favoriteScan)))
        whenever(updateFavoriteStatusUseCase.invoke(1L, false))
            .thenReturn(Result.Success(Unit))

        createViewModel("1")
        advanceUntilIdle()

        // When
        viewModel.toggleFavorite()
        advanceUntilIdle()

        // Then
        verify(updateFavoriteStatusUseCase).invoke(1L, false)
        viewModel.state.test {
            val state = awaitItem()
            assertEquals("Removed from favorites", state.snackbarMessage)
        }
    }

    @Test
    fun `toggleFavorite should handle errors`() = runTest {
        // Given
        whenever(getScanByIdUseCase.invoke(1L))
            .thenReturn(flowOf(Result.Success(testScanResult)))
        whenever(updateFavoriteStatusUseCase.invoke(any(), any()))
            .thenReturn(Result.Error("Update failed"))

        createViewModel("1")
        advanceUntilIdle()

        // When
        viewModel.toggleFavorite()
        advanceUntilIdle()

        // Then
        viewModel.state.test {
            val state = awaitItem()
            assertEquals("Failed to update favorite status", state.snackbarMessage)
        }
    }

    // ============ Delete Tests ============

    @Test
    fun `showDeleteConfirmation should show dialog`() = runTest {
        // Given
        whenever(getScanByIdUseCase.invoke(1L))
            .thenReturn(flowOf(Result.Success(testScanResult)))

        createViewModel("1")
        advanceUntilIdle()

        // When
        viewModel.showDeleteConfirmation()

        // Then
        viewModel.state.test {
            val state = awaitItem()
            assertTrue(state.showDeleteConfirmation)
        }
    }

    @Test
    fun `hideDeleteConfirmation should hide dialog`() = runTest {
        // Given
        whenever(getScanByIdUseCase.invoke(1L))
            .thenReturn(flowOf(Result.Success(testScanResult)))

        createViewModel("1")
        advanceUntilIdle()

        viewModel.showDeleteConfirmation()

        // When
        viewModel.hideDeleteConfirmation()

        // Then
        viewModel.state.test {
            val state = awaitItem()
            assertFalse(state.showDeleteConfirmation)
        }
    }

    @Test
    fun `deleteScan should delete successfully and call callback`() = runTest {
        // Given
        whenever(getScanByIdUseCase.invoke(1L))
            .thenReturn(flowOf(Result.Success(testScanResult)))
        whenever(deleteScanUseCase.invoke(1L))
            .thenReturn(Result.Success(Unit))

        createViewModel("1")
        advanceUntilIdle()

        var callbackCalled = false

        // When
        viewModel.deleteScan { callbackCalled = true }
        advanceUntilIdle()

        // Then
        verify(deleteScanUseCase).invoke(1L)
        assertTrue(callbackCalled)
    }

    @Test
    fun `deleteScan should handle deletion errors`() = runTest {
        // Given
        whenever(getScanByIdUseCase.invoke(1L))
            .thenReturn(flowOf(Result.Success(testScanResult)))
        whenever(deleteScanUseCase.invoke(1L))
            .thenReturn(Result.Error("Delete failed"))

        createViewModel("1")
        advanceUntilIdle()

        var callbackCalled = false

        // When
        viewModel.deleteScan { callbackCalled = true }
        advanceUntilIdle()

        // Then
        assertFalse(callbackCalled)
        viewModel.state.test {
            val state = awaitItem()
            assertFalse(state.showDeleteConfirmation)
            assertEquals("Failed to delete scan", state.snackbarMessage)
        }
    }

    // ============ Snackbar and Error Tests ============

    @Test
    fun `clearSnackbar should clear snackbar message`() = runTest {
        // Given
        whenever(getScanByIdUseCase.invoke(1L))
            .thenReturn(flowOf(Result.Success(testScanResult)))
        whenever(updateFavoriteStatusUseCase.invoke(any(), any()))
            .thenReturn(Result.Success(Unit))

        createViewModel("1")
        advanceUntilIdle()

        viewModel.toggleFavorite()
        advanceUntilIdle()

        // When
        viewModel.clearSnackbar()

        // Then
        viewModel.state.test {
            val state = awaitItem()
            assertNull(state.snackbarMessage)
        }
    }

    @Test
    fun `clearError should clear error message`() = runTest {
        // Given
        whenever(getScanByIdUseCase.invoke(1L))
            .thenReturn(flowOf(Result.Error("Load error")))

        createViewModel("1")
        advanceUntilIdle()

        // When
        viewModel.clearError()

        // Then
        viewModel.state.test {
            val state = awaitItem()
            assertNull(state.error)
        }
    }

    // ============ State Property Tests ============

    @Test
    fun `hasUnsavedChanges should be true when text is edited`() = runTest {
        // Given
        whenever(getScanByIdUseCase.invoke(1L))
            .thenReturn(flowOf(Result.Success(testScanResult)))

        createViewModel("1")
        advanceUntilIdle()

        // When
        viewModel.startEditingText()
        viewModel.updateEditedText("Modified text")

        // Then
        viewModel.state.test {
            val state = awaitItem()
            assertTrue(state.hasUnsavedChanges)
        }
    }

    @Test
    fun `hasUnsavedChanges should be true when title is edited`() = runTest {
        // Given
        whenever(getScanByIdUseCase.invoke(1L))
            .thenReturn(flowOf(Result.Success(testScanResult)))

        createViewModel("1")
        advanceUntilIdle()

        // When
        viewModel.startEditingTitleNotes()
        viewModel.updateEditedTitle("Modified Title")

        // Then
        viewModel.state.test {
            val state = awaitItem()
            assertTrue(state.hasUnsavedChanges)
        }
    }

    @Test
    fun `hasUnsavedChanges should be false when no edits are made`() = runTest {
        // Given
        whenever(getScanByIdUseCase.invoke(1L))
            .thenReturn(flowOf(Result.Success(testScanResult)))

        createViewModel("1")
        advanceUntilIdle()

        // Then
        viewModel.state.test {
            val state = awaitItem()
            assertFalse(state.hasUnsavedChanges)
        }
    }
}
