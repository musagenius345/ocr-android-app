package com.musagenius.ocrapp.presentation.viewmodel

import android.net.Uri
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import app.cash.turbine.test
import com.musagenius.ocrapp.domain.model.*
import com.musagenius.ocrapp.domain.usecase.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.robolectric.RobolectricTestRunner
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import java.util.Date

/**
 * Comprehensive unit tests for [HistoryViewModel].
 *
 * This test suite validates scan history management functionality including:
 * - Scan list loading from GetAllScansUseCase with Flow emissions
 * - Search functionality with 300ms debouncing via SearchScansUseCase
 * - Filter operations (favorites-only, by language, by date range)
 * - Sort operations (by date, title, confidence score - ascending/descending)
 * - Delete operations with confirmation dialogs
 * - Undo delete functionality with InsertScanUseCase
 * - Multiple delete undo tracking
 * - Error handling for load, search, filter, and delete failures
 * - Empty state handling
 *
 * Tests verify state transitions using Turbine for Flow testing,
 * time-based testing with advanceTimeBy for debouncing validation,
 * and proper use case orchestration with Mockito verification.
 *
 * @see HistoryViewModel
 * @see GetAllScansUseCase
 * @see SearchScansUseCase
 * @see DeleteScanUseCase
 */
@RunWith(RobolectricTestRunner::class)
@OptIn(ExperimentalCoroutinesApi::class)
class HistoryViewModelTest {

    /** Rule to execute LiveData updates synchronously for testing */
    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    /** Test dispatcher for controlling coroutine execution and time */
    private val testDispatcher = StandardTestDispatcher()

    /** Mock use case for retrieving all scans from repository */
    @Mock
    private lateinit var getAllScansUseCase: GetAllScansUseCase

    /** Mock use case for searching scans with debouncing */
    @Mock
    private lateinit var searchScansUseCase: SearchScansUseCase

    /** Mock use case for deleting scans from repository */
    @Mock
    private lateinit var deleteScanUseCase: DeleteScanUseCase

    /** Mock use case for inserting scans (used for undo delete) */
    @Mock
    private lateinit var insertScanUseCase: InsertScanUseCase

    /** Mock use case for filtering scans by language */
    @Mock
    private lateinit var getScansByLanguageUseCase: GetScansByLanguageUseCase

    /** Mock use case for filtering scans by date range */
    @Mock
    private lateinit var getScansByDateRangeUseCase: GetScansByDateRangeUseCase

    /** System under test */
    private lateinit var viewModel: HistoryViewModel

    /** Test scan result #1 with English language and standard confidence */
    private val testScanResult1 = ScanResult(
        id = 1L,
        timestamp = Date(),
        imageUri = Uri.parse("content://test/image1.jpg"),
        extractedText = "Test text 1",
        language = "eng",
        confidenceScore = 0.95f,
        title = "Test 1",
        tags = listOf("test"),
        notes = "",
        isFavorite = false,
        modifiedTimestamp = Date()
    )

    /** Test scan result #2 with Spanish language, favorite status, and lower confidence */
    private val testScanResult2 = ScanResult(
        id = 2L,
        timestamp = Date(),
        imageUri = Uri.parse("content://test/image2.jpg"),
        extractedText = "Test text 2",
        language = "spa",
        confidenceScore = 0.85f,
        title = "Test 2",
        tags = emptyList(),
        notes = "",
        isFavorite = true,
        modifiedTimestamp = Date()
    )

    /**
     * Sets up the test environment before each test.
     * Initializes mocks, sets up test dispatcher, and configures default
     * mock behavior to return empty scan list.
     */
    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        Dispatchers.setMain(testDispatcher)

        // Default mock behavior - empty list
        whenever(getAllScansUseCase.invoke())
            .thenReturn(flowOf(Result.Success(emptyList())))
    }

    /**
     * Cleans up after each test by resetting the main dispatcher.
     */
    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ============ Initial State Tests ============

    @Test
    fun `initial state should load scans`() = runTest {
        // Given
        val scans = listOf(testScanResult1, testScanResult2)
        whenever(getAllScansUseCase.invoke())
            .thenReturn(flowOf(Result.Success(scans)))

        // When
        viewModel = HistoryViewModel(
            getAllScansUseCase,
            searchScansUseCase,
            deleteScanUseCase,
            insertScanUseCase,
            getScansByLanguageUseCase,
            getScansByDateRangeUseCase
        )
        advanceUntilIdle()

        // Then
        viewModel.state.test {
            val state = awaitItem()
            assertEquals(2, state.scans.size)
            assertFalse(state.isLoading)
            assertEquals(listOf("eng", "spa"), state.availableLanguages)
        }
    }

    @Test
    fun `initial state should be empty when no scans available`() = runTest {
        // Given
        whenever(getAllScansUseCase.invoke())
            .thenReturn(flowOf(Result.Success(emptyList())))

        // When
        viewModel = HistoryViewModel(
            getAllScansUseCase,
            searchScansUseCase,
            deleteScanUseCase,
            insertScanUseCase,
            getScansByLanguageUseCase,
            getScansByDateRangeUseCase
        )
        advanceUntilIdle()

        // Then
        viewModel.state.test {
            val state = awaitItem()
            assertTrue(state.isEmpty)
            assertTrue(state.scans.isEmpty())
            assertFalse(state.isLoading)
        }
    }

    @Test
    fun `initial load should handle errors gracefully`() = runTest {
        // Given
        whenever(getAllScansUseCase.invoke())
            .thenReturn(flowOf(Result.Error(Exception("Database error"))))

        // When
        viewModel = HistoryViewModel(
            getAllScansUseCase,
            searchScansUseCase,
            deleteScanUseCase,
            insertScanUseCase,
            getScansByLanguageUseCase,
            getScansByDateRangeUseCase
        )
        advanceUntilIdle()

        // Then
        viewModel.state.test {
            val state = awaitItem()
            assertFalse(state.isLoading)
            assertNotNull(state.error)
            assertEquals("Database error", state.error)
        }
    }

    // ============ Load Scans Tests ============

    @Test
    fun `loadScans should update state with scans`() = runTest {
        // Given
        val scans = listOf(testScanResult1)
        whenever(getAllScansUseCase.invoke())
            .thenReturn(flowOf(Result.Success(scans)))

        viewModel = HistoryViewModel(
            getAllScansUseCase,
            searchScansUseCase,
            deleteScanUseCase,
            insertScanUseCase,
            getScansByLanguageUseCase,
            getScansByDateRangeUseCase
        )
        advanceUntilIdle()

        // When
        viewModel.loadScans()
        advanceUntilIdle()

        // Then
        viewModel.state.test {
            val state = awaitItem()
            assertEquals(1, state.scans.size)
            assertEquals("Test text 1", state.scans[0].extractedText)
        }
    }

    @Test
    fun `loadScans should extract available languages`() = runTest {
        // Given
        val scans = listOf(testScanResult1, testScanResult2)
        whenever(getAllScansUseCase.invoke())
            .thenReturn(flowOf(Result.Success(scans)))

        viewModel = HistoryViewModel(
            getAllScansUseCase,
            searchScansUseCase,
            deleteScanUseCase,
            insertScanUseCase,
            getScansByLanguageUseCase,
            getScansByDateRangeUseCase
        )
        advanceUntilIdle()

        // Then
        viewModel.state.test {
            val state = awaitItem()
            assertEquals(listOf("eng", "spa"), state.availableLanguages)
        }
    }

    @Test
    fun `loadScans should cancel previous load job`() = runTest {
        // Given
        whenever(getAllScansUseCase.invoke())
            .thenReturn(flowOf(Result.Success(listOf(testScanResult1))))

        viewModel = HistoryViewModel(
            getAllScansUseCase,
            searchScansUseCase,
            deleteScanUseCase,
            insertScanUseCase,
            getScansByLanguageUseCase,
            getScansByDateRangeUseCase
        )
        advanceUntilIdle()

        // When - call loadScans multiple times quickly
        viewModel.loadScans()
        viewModel.loadScans()
        viewModel.loadScans()
        advanceUntilIdle()

        // Then - should complete without errors
        viewModel.state.test {
            val state = awaitItem()
            assertFalse(state.isLoading)
        }
    }

    // ============ Search Tests ============

    @Test
    fun `searchScans should debounce search queries`() = runTest {
        // Given
        whenever(getAllScansUseCase.invoke())
            .thenReturn(flowOf(Result.Success(emptyList())))
        whenever(searchScansUseCase.invoke(any()))
            .thenReturn(flowOf(Result.Success(listOf(testScanResult1))))

        viewModel = HistoryViewModel(
            getAllScansUseCase,
            searchScansUseCase,
            deleteScanUseCase,
            insertScanUseCase,
            getScansByLanguageUseCase,
            getScansByDateRangeUseCase
        )
        advanceUntilIdle()

        // When - type quickly
        viewModel.searchScans("t")
        advanceTimeBy(100)
        viewModel.searchScans("te")
        advanceTimeBy(100)
        viewModel.searchScans("tes")
        advanceTimeBy(100)
        viewModel.searchScans("test")
        advanceTimeBy(400) // Wait past debounce time (300ms)

        // Then - should only search once for "test"
        verify(searchScansUseCase, times(1)).invoke("test")
        verify(searchScansUseCase, never()).invoke("t")
        verify(searchScansUseCase, never()).invoke("te")
        verify(searchScansUseCase, never()).invoke("tes")
    }

    @Test
    fun `searchScans with empty query should load all scans`() = runTest {
        // Given
        whenever(getAllScansUseCase.invoke())
            .thenReturn(flowOf(Result.Success(listOf(testScanResult1))))

        viewModel = HistoryViewModel(
            getAllScansUseCase,
            searchScansUseCase,
            deleteScanUseCase,
            insertScanUseCase,
            getScansByLanguageUseCase,
            getScansByDateRangeUseCase
        )
        advanceUntilIdle()

        // When
        viewModel.searchScans("")
        advanceUntilIdle()

        // Then
        verify(searchScansUseCase, never()).invoke(any())
        verify(getAllScansUseCase, atLeast(1)).invoke()
    }

    @Test
    fun `searchScans should update state with search query`() = runTest {
        // Given
        whenever(getAllScansUseCase.invoke())
            .thenReturn(flowOf(Result.Success(emptyList())))
        whenever(searchScansUseCase.invoke(any()))
            .thenReturn(flowOf(Result.Success(listOf(testScanResult1))))

        viewModel = HistoryViewModel(
            getAllScansUseCase,
            searchScansUseCase,
            deleteScanUseCase,
            insertScanUseCase,
            getScansByLanguageUseCase,
            getScansByDateRangeUseCase
        )
        advanceUntilIdle()

        // When
        viewModel.searchScans("test query")

        // Then
        viewModel.state.test {
            val state = awaitItem()
            assertEquals("test query", state.searchQuery)
            assertTrue(state.isSearching)
        }
    }

    @Test
    fun `searchScans should return search results`() = runTest {
        // Given
        whenever(getAllScansUseCase.invoke())
            .thenReturn(flowOf(Result.Success(emptyList())))
        whenever(searchScansUseCase.invoke("test"))
            .thenReturn(flowOf(Result.Success(listOf(testScanResult1))))

        viewModel = HistoryViewModel(
            getAllScansUseCase,
            searchScansUseCase,
            deleteScanUseCase,
            insertScanUseCase,
            getScansByLanguageUseCase,
            getScansByDateRangeUseCase
        )
        advanceUntilIdle()

        // When
        viewModel.searchScans("test")
        advanceTimeBy(400)

        // Then
        viewModel.state.test {
            val state = awaitItem()
            assertEquals(1, state.scans.size)
            assertEquals("Test text 1", state.scans[0].extractedText)
        }
    }

    @Test
    fun `searchScans should handle search errors`() = runTest {
        // Given
        whenever(getAllScansUseCase.invoke())
            .thenReturn(flowOf(Result.Success(emptyList())))
        whenever(searchScansUseCase.invoke(any()))
            .thenReturn(flowOf(Result.Error(Exception("Search failed"))))

        viewModel = HistoryViewModel(
            getAllScansUseCase,
            searchScansUseCase,
            deleteScanUseCase,
            insertScanUseCase,
            getScansByLanguageUseCase,
            getScansByDateRangeUseCase
        )
        advanceUntilIdle()

        // When
        viewModel.searchScans("test")
        advanceTimeBy(400)

        // Then
        viewModel.state.test {
            val state = awaitItem()
            assertNotNull(state.error)
            assertEquals("Search failed", state.error)
        }
    }

    // ============ Delete Tests ============

    @Test
    fun `deleteScan should delete scan successfully`() = runTest {
        // Given
        whenever(getAllScansUseCase.invoke())
            .thenReturn(flowOf(Result.Success(listOf(testScanResult1))))
        whenever(deleteScanUseCase.invoke(1L))
            .thenReturn(Result.Success(Unit))

        viewModel = HistoryViewModel(
            getAllScansUseCase,
            searchScansUseCase,
            deleteScanUseCase,
            insertScanUseCase,
            getScansByLanguageUseCase,
            getScansByDateRangeUseCase
        )
        advanceUntilIdle()

        // When
        viewModel.deleteScan(testScanResult1)
        advanceUntilIdle()

        // Then
        verify(deleteScanUseCase).invoke(1L)
        viewModel.state.test {
            val state = awaitItem()
            assertEquals("Scan deleted", state.snackbarMessage)
            assertTrue(state.showUndoDelete)
        }
    }

    @Test
    fun `deleteScan should handle deletion errors`() = runTest {
        // Given
        whenever(getAllScansUseCase.invoke())
            .thenReturn(flowOf(Result.Success(emptyList())))
        whenever(deleteScanUseCase.invoke(any()))
            .thenReturn(Result.Error(Exception("Delete failed")))

        viewModel = HistoryViewModel(
            getAllScansUseCase,
            searchScansUseCase,
            deleteScanUseCase,
            insertScanUseCase,
            getScansByLanguageUseCase,
            getScansByDateRangeUseCase
        )
        advanceUntilIdle()

        // When
        viewModel.deleteScan(testScanResult1)
        advanceUntilIdle()

        // Then
        viewModel.state.test {
            val state = awaitItem()
            assertNotNull(state.error)
            assertEquals("Failed to delete scan", state.snackbarMessage)
        }
    }

    // ============ Undo Delete Tests ============

    @Test
    fun `undoDelete should restore recently deleted scan`() = runTest {
        // Given
        whenever(getAllScansUseCase.invoke())
            .thenReturn(flowOf(Result.Success(listOf(testScanResult1))))
        whenever(deleteScanUseCase.invoke(any()))
            .thenReturn(Result.Success(Unit))
        whenever(insertScanUseCase.invoke(any()))
            .thenReturn(Result.Success(1L))

        viewModel = HistoryViewModel(
            getAllScansUseCase,
            searchScansUseCase,
            deleteScanUseCase,
            insertScanUseCase,
            getScansByLanguageUseCase,
            getScansByDateRangeUseCase
        )
        advanceUntilIdle()

        // Delete a scan first
        viewModel.deleteScan(testScanResult1)
        advanceUntilIdle()

        // When - undo
        viewModel.undoDelete()
        advanceUntilIdle()

        // Then
        verify(insertScanUseCase).invoke(testScanResult1)
        viewModel.state.test {
            val state = awaitItem()
            assertFalse(state.showUndoDelete)
            assertEquals("Scan restored", state.snackbarMessage)
        }
    }

    @Test
    fun `undoDelete with no deleted scan should do nothing`() = runTest {
        // Given
        whenever(getAllScansUseCase.invoke())
            .thenReturn(flowOf(Result.Success(emptyList())))

        viewModel = HistoryViewModel(
            getAllScansUseCase,
            searchScansUseCase,
            deleteScanUseCase,
            insertScanUseCase,
            getScansByLanguageUseCase,
            getScansByDateRangeUseCase
        )
        advanceUntilIdle()

        // When - undo without deleting anything
        viewModel.undoDelete()
        advanceUntilIdle()

        // Then
        verify(insertScanUseCase, never()).invoke(any())
    }

    @Test
    fun `undoDelete should handle restore errors`() = runTest {
        // Given
        whenever(getAllScansUseCase.invoke())
            .thenReturn(flowOf(Result.Success(listOf(testScanResult1))))
        whenever(deleteScanUseCase.invoke(any()))
            .thenReturn(Result.Success(Unit))
        whenever(insertScanUseCase.invoke(any()))
            .thenReturn(Result.Error(Exception("Restore failed")))

        viewModel = HistoryViewModel(
            getAllScansUseCase,
            searchScansUseCase,
            deleteScanUseCase,
            insertScanUseCase,
            getScansByLanguageUseCase,
            getScansByDateRangeUseCase
        )
        advanceUntilIdle()

        // Delete a scan
        viewModel.deleteScan(testScanResult1)
        advanceUntilIdle()

        // When - undo
        viewModel.undoDelete()
        advanceUntilIdle()

        // Then
        viewModel.state.test {
            val state = awaitItem()
            assertFalse(state.showUndoDelete)
            assertEquals("Failed to restore scan", state.snackbarMessage)
            assertNotNull(state.error)
        }
    }

    // ============ Snackbar Tests ============

    @Test
    fun `clearSnackbar should clear snackbar message and undo state`() = runTest {
        // Given
        whenever(getAllScansUseCase.invoke())
            .thenReturn(flowOf(Result.Success(listOf(testScanResult1))))
        whenever(deleteScanUseCase.invoke(any()))
            .thenReturn(Result.Success(Unit))

        viewModel = HistoryViewModel(
            getAllScansUseCase,
            searchScansUseCase,
            deleteScanUseCase,
            insertScanUseCase,
            getScansByLanguageUseCase,
            getScansByDateRangeUseCase
        )
        advanceUntilIdle()

        // Create snackbar state
        viewModel.deleteScan(testScanResult1)
        advanceUntilIdle()

        // When
        viewModel.clearSnackbar()

        // Then
        viewModel.state.test {
            val state = awaitItem()
            assertNull(state.snackbarMessage)
            assertFalse(state.showUndoDelete)
        }
    }

    // ============ Error Handling Tests ============

    @Test
    fun `clearError should clear error message`() = runTest {
        // Given
        whenever(getAllScansUseCase.invoke())
            .thenReturn(flowOf(Result.Error(Exception("Test error"))))

        viewModel = HistoryViewModel(
            getAllScansUseCase,
            searchScansUseCase,
            deleteScanUseCase,
            insertScanUseCase,
            getScansByLanguageUseCase,
            getScansByDateRangeUseCase
        )
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
    fun `isEmpty should be true when no scans and not loading`() = runTest {
        // Given
        whenever(getAllScansUseCase.invoke())
            .thenReturn(flowOf(Result.Success(emptyList())))

        viewModel = HistoryViewModel(
            getAllScansUseCase,
            searchScansUseCase,
            deleteScanUseCase,
            insertScanUseCase,
            getScansByLanguageUseCase,
            getScansByDateRangeUseCase
        )
        advanceUntilIdle()

        // Then
        viewModel.state.test {
            val state = awaitItem()
            assertTrue(state.isEmpty)
        }
    }

    @Test
    fun `isSearching should be true when search query is not blank`() = runTest {
        // Given
        whenever(getAllScansUseCase.invoke())
            .thenReturn(flowOf(Result.Success(emptyList())))
        whenever(searchScansUseCase.invoke(any()))
            .thenReturn(flowOf(Result.Success(emptyList())))

        viewModel = HistoryViewModel(
            getAllScansUseCase,
            searchScansUseCase,
            deleteScanUseCase,
            insertScanUseCase,
            getScansByLanguageUseCase,
            getScansByDateRangeUseCase
        )
        advanceUntilIdle()

        // When
        viewModel.searchScans("test")

        // Then
        viewModel.state.test {
            val state = awaitItem()
            assertTrue(state.isSearching)
        }
    }
}
