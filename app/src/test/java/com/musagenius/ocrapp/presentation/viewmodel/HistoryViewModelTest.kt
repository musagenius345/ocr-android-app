package com.musagenius.ocrapp.presentation.viewmodel

import android.net.Uri
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.musagenius.ocrapp.domain.model.FilterOptions
import com.musagenius.ocrapp.domain.model.Result
import com.musagenius.ocrapp.domain.model.ScanResult
import com.musagenius.ocrapp.domain.model.SortBy
import com.musagenius.ocrapp.domain.usecase.DeleteScanUseCase
import com.musagenius.ocrapp.domain.usecase.GetAllScansUseCase
import com.musagenius.ocrapp.domain.usecase.GetScansByDateRangeUseCase
import com.musagenius.ocrapp.domain.usecase.GetScansByLanguageUseCase
import com.musagenius.ocrapp.domain.usecase.InsertScanUseCase
import com.musagenius.ocrapp.domain.usecase.SearchScansUseCase
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
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations
import java.util.Date

/**
 * Unit tests for HistoryViewModel
 */
@OptIn(ExperimentalCoroutinesApi::class)
class HistoryViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    @Mock
    private lateinit var getAllScansUseCase: GetAllScansUseCase

    @Mock
    private lateinit var searchScansUseCase: SearchScansUseCase

    @Mock
    private lateinit var deleteScanUseCase: DeleteScanUseCase

    @Mock
    private lateinit var insertScanUseCase: InsertScanUseCase

    @Mock
    private lateinit var getScansByLanguageUseCase: GetScansByLanguageUseCase

    @Mock
    private lateinit var getScansByDateRangeUseCase: GetScansByDateRangeUseCase

    private lateinit var viewModel: HistoryViewModel

    private val testDispatcher = StandardTestDispatcher()

    private val testScan1 = ScanResult(
        id = 1L,
        timestamp = Date(),
        imageUri = Uri.parse("content://test/1.jpg"),
        extractedText = "Test scan 1",
        language = "eng",
        confidenceScore = 0.95f,
        title = "Document 1",
        tags = emptyList(),
        notes = "",
        isFavorite = false,
        modifiedTimestamp = Date()
    )

    private val testScan2 = ScanResult(
        id = 2L,
        timestamp = Date(),
        imageUri = Uri.parse("content://test/2.jpg"),
        extractedText = "Test scan 2",
        language = "spa",
        confidenceScore = 0.85f,
        title = "Document 2",
        tags = emptyList(),
        notes = "",
        isFavorite = true,
        modifiedTimestamp = Date()
    )

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        Dispatchers.setMain(testDispatcher)

        // Setup default behavior
        `when`(getAllScansUseCase.invoke())
            .thenReturn(flowOf(Result.Success(listOf(testScan1, testScan2))))
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(): HistoryViewModel {
        return HistoryViewModel(
            getAllScansUseCase,
            searchScansUseCase,
            deleteScanUseCase,
            insertScanUseCase,
            getScansByLanguageUseCase,
            getScansByDateRangeUseCase
        )
    }

    @Test
    fun `init loads scans from use case`() = runTest {
        // When
        viewModel = createViewModel()
        advanceUntilIdle()

        // Then
        val state = viewModel.state.value
        assertEquals(2, state.scans.size)
        assertFalse(state.isLoading)
        assertNull(state.error)
        verify(getAllScansUseCase).invoke()
    }

    @Test
    fun `loadScans updates state with success`() = runTest {
        // Given
        viewModel = createViewModel()
        advanceUntilIdle()

        // When
        viewModel.loadScans()
        advanceUntilIdle()

        // Then
        val state = viewModel.state.value
        assertEquals(2, state.scans.size)
        assertFalse(state.isLoading)
        assertNull(state.error)
    }

    @Test
    fun `loadScans updates available languages`() = runTest {
        // Given
        viewModel = createViewModel()
        advanceUntilIdle()

        // When
        val state = viewModel.state.value

        // Then
        assertEquals(2, state.availableLanguages.size)
        assertTrue(state.availableLanguages.contains("eng"))
        assertTrue(state.availableLanguages.contains("spa"))
    }

    @Test
    fun `loadScans sets error when use case fails`() = runTest {
        // Given
        val errorMessage = "Database error"
        `when`(getAllScansUseCase.invoke())
            .thenReturn(flowOf(Result.Error(Exception(errorMessage), errorMessage)))

        // When
        viewModel = createViewModel()
        advanceUntilIdle()

        // Then
        val state = viewModel.state.value
        assertFalse(state.isLoading)
        assertEquals(errorMessage, state.error)
    }

    @Test
    fun `searchScans updates search query in state`() = runTest {
        // Given
        viewModel = createViewModel()
        advanceUntilIdle()
        val query = "test"
        `when`(searchScansUseCase.invoke(query))
            .thenReturn(flowOf(Result.Success(listOf(testScan1))))

        // When
        viewModel.searchScans(query)
        advanceTimeBy(350) // Wait for debounce
        advanceUntilIdle()

        // Then
        assertEquals(query, viewModel.state.value.searchQuery)
    }

    @Test
    fun `searchScans with empty query loads all scans`() = runTest {
        // Given
        viewModel = createViewModel()
        advanceUntilIdle()

        // When
        viewModel.searchScans("")
        advanceUntilIdle()

        // Then
        verify(getAllScansUseCase, atLeast(2)).invoke() // Init + searchScans
    }

    @Test
    fun `searchScans debounces search requests`() = runTest {
        // Given
        viewModel = createViewModel()
        advanceUntilIdle()
        val query = "test"
        `when`(searchScansUseCase.invoke(query))
            .thenReturn(flowOf(Result.Success(emptyList())))

        // When
        viewModel.searchScans(query)
        advanceTimeBy(100) // Less than debounce time
        viewModel.searchScans(query + "2")
        advanceTimeBy(100)
        viewModel.searchScans(query + "3")
        advanceTimeBy(350) // Wait for debounce
        advanceUntilIdle()

        // Then - Only the last search should execute
        verify(searchScansUseCase, never()).invoke(query)
        verify(searchScansUseCase, never()).invoke(query + "2")
        verify(searchScansUseCase).invoke(query + "3")
    }

    @Test
    fun `deleteScan removes scan and shows snackbar`() = runTest {
        // Given
        viewModel = createViewModel()
        advanceUntilIdle()
        `when`(deleteScanUseCase.invoke(testScan1.id))
            .thenReturn(Result.Success(Unit))

        // When
        viewModel.deleteScan(testScan1)
        advanceUntilIdle()

        // Then
        val state = viewModel.state.value
        assertEquals("Scan deleted", state.snackbarMessage)
        assertTrue(state.showUndoDelete)
        verify(deleteScanUseCase).invoke(testScan1.id)
    }

    @Test
    fun `deleteScan shows error when delete fails`() = runTest {
        // Given
        viewModel = createViewModel()
        advanceUntilIdle()
        val errorMessage = "Delete failed"
        `when`(deleteScanUseCase.invoke(testScan1.id))
            .thenReturn(Result.Error(Exception(errorMessage), errorMessage))

        // When
        viewModel.deleteScan(testScan1)
        advanceUntilIdle()

        // Then
        val state = viewModel.state.value
        assertEquals("Failed to delete scan", state.snackbarMessage)
        assertNotNull(state.error)
    }

    @Test
    fun `undoDelete restores deleted scan`() = runTest {
        // Given
        viewModel = createViewModel()
        advanceUntilIdle()
        `when`(deleteScanUseCase.invoke(testScan1.id))
            .thenReturn(Result.Success(Unit))
        `when`(insertScanUseCase.invoke(testScan1))
            .thenReturn(Result.Success(testScan1.id))

        // When
        viewModel.deleteScan(testScan1)
        advanceUntilIdle()
        viewModel.undoDelete()
        advanceUntilIdle()

        // Then
        val state = viewModel.state.value
        assertEquals("Scan restored", state.snackbarMessage)
        assertFalse(state.showUndoDelete)
        verify(insertScanUseCase).invoke(testScan1)
    }

    @Test
    fun `undoDelete does nothing when no scan to restore`() = runTest {
        // Given
        viewModel = createViewModel()
        advanceUntilIdle()

        // When
        viewModel.undoDelete()
        advanceUntilIdle()

        // Then
        verify(insertScanUseCase, never()).invoke(any(ScanResult::class.java))
    }

    @Test
    fun `clearSnackbar clears message and undo flag`() = runTest {
        // Given
        viewModel = createViewModel()
        advanceUntilIdle()
        `when`(deleteScanUseCase.invoke(testScan1.id))
            .thenReturn(Result.Success(Unit))
        viewModel.deleteScan(testScan1)
        advanceUntilIdle()

        // When
        viewModel.clearSnackbar()

        // Then
        val state = viewModel.state.value
        assertNull(state.snackbarMessage)
        assertFalse(state.showUndoDelete)
    }

    @Test
    fun `clearError clears error message`() = runTest {
        // Given
        `when`(getAllScansUseCase.invoke())
            .thenReturn(flowOf(Result.Error(Exception("Error"), "Error")))
        viewModel = createViewModel()
        advanceUntilIdle()

        // When
        viewModel.clearError()

        // Then
        assertNull(viewModel.state.value.error)
    }

    @Test
    fun `changeSortOrder updates sort and closes dialog`() = runTest {
        // Given
        viewModel = createViewModel()
        advanceUntilIdle()

        // When
        viewModel.changeSortOrder(SortBy.TITLE_ASC)

        // Then
        val state = viewModel.state.value
        assertEquals(SortBy.TITLE_ASC, state.sortBy)
        assertFalse(state.showSortDialog)
    }

    @Test
    fun `toggleFilterSheet changes visibility`() = runTest {
        // Given
        viewModel = createViewModel()
        advanceUntilIdle()

        // When
        viewModel.toggleFilterSheet()

        // Then
        assertTrue(viewModel.state.value.showFilterSheet)

        // When
        viewModel.toggleFilterSheet()

        // Then
        assertFalse(viewModel.state.value.showFilterSheet)
    }

    @Test
    fun `toggleSortDialog changes visibility`() = runTest {
        // Given
        viewModel = createViewModel()
        advanceUntilIdle()

        // When
        viewModel.toggleSortDialog()

        // Then
        assertTrue(viewModel.state.value.showSortDialog)

        // When
        viewModel.toggleSortDialog()

        // Then
        assertFalse(viewModel.state.value.showSortDialog)
    }

    @Test
    fun `applyFilter updates filter options and closes sheet`() = runTest {
        // Given
        viewModel = createViewModel()
        advanceUntilIdle()
        val filterOptions = FilterOptions(language = "eng")

        // When
        viewModel.applyFilter(filterOptions)
        advanceUntilIdle()

        // Then
        val state = viewModel.state.value
        assertEquals("eng", state.filterOptions.language)
        assertFalse(state.showFilterSheet)
    }

    @Test
    fun `clearFilters resets filter options`() = runTest {
        // Given
        viewModel = createViewModel()
        advanceUntilIdle()
        viewModel.applyFilter(FilterOptions(language = "eng"))
        advanceUntilIdle()

        // When
        viewModel.clearFilters()
        advanceUntilIdle()

        // Then
        val state = viewModel.state.value
        assertNull(state.filterOptions.language)
        assertFalse(state.filterOptions.isActive())
    }

    // Helper function to handle any() for Kotlin
    private fun <T> any(type: Class<T>): T = org.mockito.Mockito.any(type)
}
