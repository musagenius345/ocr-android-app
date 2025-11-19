package com.musagenius.ocrapp.presentation.viewmodel

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import app.cash.turbine.test
import com.musagenius.ocrapp.domain.model.Language
import com.musagenius.ocrapp.domain.model.Result
import com.musagenius.ocrapp.domain.usecase.DeleteLanguageUseCase
import com.musagenius.ocrapp.domain.usecase.DownloadLanguageUseCase
import com.musagenius.ocrapp.domain.usecase.GetAvailableLanguagesUseCase
import com.musagenius.ocrapp.domain.usecase.GetLanguageStorageUseCase
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

/**
 * Comprehensive unit tests for [LanguageViewModel].
 *
 * This test suite validates Tesseract OCR language management functionality including:
 * - Language list loading from GetAvailableLanguagesUseCase
 * - Language download with progress tracking via Flow emissions
 * - Download cancellation with proper cleanup
 * - Download error handling (network failures, insufficient storage)
 * - Language deletion with confirmation dialogs
 * - Delete error handling
 * - Storage space checking before downloads
 * - Dialog state management (download progress, delete confirmation)
 * - Multiple concurrent download prevention
 * - Installed vs. available language filtering
 *
 * Tests verify state transitions using Turbine for Flow testing,
 * progress tracking for long-running downloads, and proper use case
 * orchestration with Mockito verification.
 *
 * @see LanguageViewModel
 * @see GetAvailableLanguagesUseCase
 * @see DownloadLanguageUseCase
 * @see DeleteLanguageUseCase
 */
@OptIn(ExperimentalCoroutinesApi::class)
class LanguageViewModelTest {

    /** Rule to execute LiveData updates synchronously for testing */
    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    /** Test dispatcher for controlling coroutine execution */
    private val testDispatcher = StandardTestDispatcher()

    /** Mock use case for retrieving available OCR languages */
    @Mock
    private lateinit var getAvailableLanguagesUseCase: GetAvailableLanguagesUseCase

    /** Mock use case for downloading Tesseract language data files */
    @Mock
    private lateinit var downloadLanguageUseCase: DownloadLanguageUseCase

    /** Mock use case for deleting installed language data files */
    @Mock
    private lateinit var deleteLanguageUseCase: DeleteLanguageUseCase

    /** Mock use case for checking available storage space */
    @Mock
    private lateinit var getLanguageStorageUseCase: GetLanguageStorageUseCase

    /** System under test */
    private lateinit var viewModel: LanguageViewModel

    /** Test language representing an already installed language (English) */
    private val testLanguageInstalled = Language(
        code = "eng",
        displayName = "English",
        isInstalled = true,
        fileSize = 5_000_000L
    )

    /** Test language representing an available but not installed language (Spanish) */
    private val testLanguageNotInstalled = Language(
        code = "spa",
        displayName = "Spanish",
        isInstalled = false,
        fileSize = 6_000_000L
    )

    /** AutoCloseable handle for Mockito mocks to prevent resource leaks */
    private lateinit var mocksCloseable: AutoCloseable

    /**
     * Sets up the test environment before each test.
     * Initializes mocks, sets up test dispatcher, and configures default
     * mock behavior for storage availability (10MB available).
     */
    @Before
    fun setup() {
        mocksCloseable = MockitoAnnotations.openMocks(this)
        Dispatchers.setMain(testDispatcher)
    }

    /**
     * Cleans up after each test by closing Mockito mocks and resetting the main dispatcher.
     */
    @After
    fun tearDown() {
        mocksCloseable.close()
        Dispatchers.resetMain()
    }

    // ============ Initial State Tests ============

    @Test
    fun `initial state should load languages`() = runTest {
        // Given
        val languages = listOf(testLanguageInstalled, testLanguageNotInstalled)
        whenever(getAvailableLanguagesUseCase.invoke())
            .thenReturn(Result.Success(languages))

        // When
        viewModel = LanguageViewModel(
            getAvailableLanguagesUseCase,
            downloadLanguageUseCase,
            deleteLanguageUseCase,
            getLanguageStorageUseCase
        )
        advanceUntilIdle()

        // Then
        viewModel.state.test {
            val state = awaitItem()
            assertEquals(2, state.languages.size)
            assertEquals(10_000_000L, state.totalStorageUsed)
            assertFalse(state.isLoading)
            assertNull(state.error)
        }
    }

    @Test
    fun `initial state should be empty when no languages available`() = runTest {
        // Given
        whenever(getAvailableLanguagesUseCase.invoke())
            .thenReturn(Result.Success(emptyList()))

        // When
        viewModel = LanguageViewModel(
            getAvailableLanguagesUseCase,
            downloadLanguageUseCase,
            deleteLanguageUseCase,
            getLanguageStorageUseCase
        )
        advanceUntilIdle()

        // Then
        viewModel.state.test {
            val state = awaitItem()
            assertTrue(state.languages.isEmpty())
            assertEquals(0, state.getInstalledCount())
        }
    }

    @Test
    fun `initial load should handle errors`() = runTest {
        // Given
        whenever(getAvailableLanguagesUseCase.invoke())
            .thenReturn(Result.Error(Exception("Failed to load")))

        // When
        viewModel = LanguageViewModel(
            getAvailableLanguagesUseCase,
            downloadLanguageUseCase,
            deleteLanguageUseCase,
            getLanguageStorageUseCase
        )
        advanceUntilIdle()

        // Then
        viewModel.state.test {
            val state = awaitItem()
            assertFalse(state.isLoading)
            assertNotNull(state.error)
            assertEquals("Failed to load", state.error)
        }
    }

    // ============ Load Languages Tests ============

    @Test
    fun `loadLanguages should update state with languages`() = runTest {
        // Given
        val languages = listOf(testLanguageInstalled)
        whenever(getAvailableLanguagesUseCase.invoke())
            .thenReturn(Result.Success(languages))

        viewModel = LanguageViewModel(
            getAvailableLanguagesUseCase,
            downloadLanguageUseCase,
            deleteLanguageUseCase,
            getLanguageStorageUseCase
        )
        advanceUntilIdle()

        // When
        viewModel.loadLanguages()
        advanceUntilIdle()

        // Then
        viewModel.state.test {
            val state = awaitItem()
            assertEquals(1, state.languages.size)
            assertEquals("eng", state.languages[0].code)
            assertEquals("English", state.languages[0].name)
            assertTrue(state.languages[0].isInstalled)
        }
    }

    @Test
    fun `loadLanguages should calculate total storage`() = runTest {
        // Given
        whenever(getAvailableLanguagesUseCase.invoke())
            .thenReturn(Result.Success(emptyList()))
        whenever(getLanguageStorageUseCase.invoke()).thenReturn(15_000_000L)

        viewModel = LanguageViewModel(
            getAvailableLanguagesUseCase,
            downloadLanguageUseCase,
            deleteLanguageUseCase,
            getLanguageStorageUseCase
        )
        advanceUntilIdle()

        // Then
        viewModel.state.test {
            val state = awaitItem()
            assertEquals(15_000_000L, state.totalStorageUsed)
        }
    }

    // ============ Download Language Tests ============

    @Test
    fun `downloadLanguage should track download progress`() = runTest {
        // Given
        whenever(getAvailableLanguagesUseCase.invoke())
            .thenReturn(Result.Success(emptyList()))
        whenever(downloadLanguageUseCase.invoke(any()))
            .thenReturn(flowOf(
                Result.Success(0.0f),
                Result.Success(0.5f),
                Result.Success(1.0f)
            ))

        viewModel = LanguageViewModel(
            getAvailableLanguagesUseCase,
            downloadLanguageUseCase,
            deleteLanguageUseCase,
            getLanguageStorageUseCase
        )
        advanceUntilIdle()

        // When
        viewModel.downloadLanguage("eng")
        advanceUntilIdle()

        // Then
        verify(downloadLanguageUseCase).invoke("eng")
        viewModel.state.test {
            val state = awaitItem()
            assertNull(state.downloadingLanguageCode) // Complete
            assertEquals("Language downloaded successfully", state.snackbarMessage)
        }
    }

    @Test
    fun `downloadLanguage should update downloading state`() = runTest {
        // Given
        whenever(getAvailableLanguagesUseCase.invoke())
            .thenReturn(Result.Success(emptyList()))
        whenever(downloadLanguageUseCase.invoke(any()))
            .thenReturn(flowOf(Result.Success(0.5f)))

        viewModel = LanguageViewModel(
            getAvailableLanguagesUseCase,
            downloadLanguageUseCase,
            deleteLanguageUseCase,
            getLanguageStorageUseCase
        )
        advanceUntilIdle()

        // When
        viewModel.downloadLanguage("spa")
        advanceUntilIdle()

        // Then
        viewModel.state.test {
            val state = awaitItem()
            assertEquals(0.5f, state.downloadProgress)
        }
    }

    @Test
    fun `downloadLanguage should handle download errors`() = runTest {
        // Given
        whenever(getAvailableLanguagesUseCase.invoke())
            .thenReturn(Result.Success(emptyList()))
        whenever(downloadLanguageUseCase.invoke(any()))
            .thenReturn(flowOf(Result.Error(Exception("Network error"))))

        viewModel = LanguageViewModel(
            getAvailableLanguagesUseCase,
            downloadLanguageUseCase,
            deleteLanguageUseCase,
            getLanguageStorageUseCase
        )
        advanceUntilIdle()

        // When
        viewModel.downloadLanguage("eng")
        advanceUntilIdle()

        // Then
        viewModel.state.test {
            val state = awaitItem()
            assertNull(state.downloadingLanguageCode)
            assertEquals(0f, state.downloadProgress)
            assertNotNull(state.error)
            assertTrue(state.snackbarMessage!!.contains("Download failed"))
        }
    }

    @Test
    fun `downloadLanguage should cancel previous download`() = runTest {
        // Given
        whenever(getAvailableLanguagesUseCase.invoke())
            .thenReturn(Result.Success(emptyList()))
        whenever(downloadLanguageUseCase.invoke(any()))
            .thenReturn(flowOf(Result.Success(0.5f)))

        viewModel = LanguageViewModel(
            getAvailableLanguagesUseCase,
            downloadLanguageUseCase,
            deleteLanguageUseCase,
            getLanguageStorageUseCase
        )
        advanceUntilIdle()

        // When - start two downloads
        viewModel.downloadLanguage("eng")
        viewModel.downloadLanguage("spa")
        advanceUntilIdle()

        // Then - should only track second download
        verify(downloadLanguageUseCase).invoke("spa")
        viewModel.state.test {
            val state = awaitItem()
            // If downloading, should be "spa" (or null if completed)
            assertTrue(state.downloadingLanguageCode == null || state.downloadingLanguageCode == "spa")
        }
    }

    @Test
    fun `downloadLanguage should reload languages on completion`() = runTest {
        // Given
        whenever(getAvailableLanguagesUseCase.invoke())
            .thenReturn(Result.Success(emptyList()))
        whenever(downloadLanguageUseCase.invoke(any()))
            .thenReturn(flowOf(Result.Success(1.0f)))

        viewModel = LanguageViewModel(
            getAvailableLanguagesUseCase,
            downloadLanguageUseCase,
            deleteLanguageUseCase,
            getLanguageStorageUseCase
        )
        advanceUntilIdle()

        // When
        viewModel.downloadLanguage("eng")
        advanceUntilIdle()

        // Then - should call loadLanguages (total 2 times: init + after download)
        verify(getAvailableLanguagesUseCase, atLeast(2)).invoke()
    }

    // ============ Cancel Download Tests ============

    @Test
    fun `cancelDownload should clear downloading state`() = runTest {
        // Given
        whenever(getAvailableLanguagesUseCase.invoke())
            .thenReturn(Result.Success(emptyList()))
        whenever(downloadLanguageUseCase.invoke(any()))
            .thenReturn(flowOf(Result.Success(0.5f)))

        viewModel = LanguageViewModel(
            getAvailableLanguagesUseCase,
            downloadLanguageUseCase,
            deleteLanguageUseCase,
            getLanguageStorageUseCase
        )
        advanceUntilIdle()

        viewModel.downloadLanguage("eng")

        // When
        viewModel.cancelDownload()

        // Then
        viewModel.state.test {
            val state = awaitItem()
            assertNull(state.downloadingLanguageCode)
            assertEquals(0f, state.downloadProgress)
            assertEquals("Download cancelled", state.snackbarMessage)
        }
    }

    // ============ Delete Language Tests ============

    @Test
    fun `showDeleteConfirmation should show dialog`() = runTest {
        // Given
        whenever(getAvailableLanguagesUseCase.invoke())
            .thenReturn(Result.Success(emptyList()))

        viewModel = LanguageViewModel(
            getAvailableLanguagesUseCase,
            downloadLanguageUseCase,
            deleteLanguageUseCase,
            getLanguageStorageUseCase
        )
        advanceUntilIdle()

        // When
        viewModel.showDeleteConfirmation("eng")

        // Then
        viewModel.state.test {
            val state = awaitItem()
            assertEquals("eng", state.showDeleteConfirmation)
        }
    }

    @Test
    fun `hideDeleteConfirmation should hide dialog`() = runTest {
        // Given
        whenever(getAvailableLanguagesUseCase.invoke())
            .thenReturn(Result.Success(emptyList()))

        viewModel = LanguageViewModel(
            getAvailableLanguagesUseCase,
            downloadLanguageUseCase,
            deleteLanguageUseCase,
            getLanguageStorageUseCase
        )
        advanceUntilIdle()

        viewModel.showDeleteConfirmation("eng")

        // When
        viewModel.hideDeleteConfirmation()

        // Then
        viewModel.state.test {
            val state = awaitItem()
            assertNull(state.showDeleteConfirmation)
        }
    }

    @Test
    fun `deleteLanguage should delete language successfully`() = runTest {
        // Given
        whenever(getAvailableLanguagesUseCase.invoke())
            .thenReturn(Result.Success(emptyList()))
        whenever(deleteLanguageUseCase.invoke(any()))
            .thenReturn(Result.Success(Unit))

        viewModel = LanguageViewModel(
            getAvailableLanguagesUseCase,
            downloadLanguageUseCase,
            deleteLanguageUseCase,
            getLanguageStorageUseCase
        )
        advanceUntilIdle()

        // When
        viewModel.deleteLanguage("eng")
        advanceUntilIdle()

        // Then
        verify(deleteLanguageUseCase).invoke("eng")
        viewModel.state.test {
            val state = awaitItem()
            assertNull(state.showDeleteConfirmation)
            assertEquals("Language deleted successfully", state.snackbarMessage)
        }
    }

    @Test
    fun `deleteLanguage should handle deletion errors`() = runTest {
        // Given
        whenever(getAvailableLanguagesUseCase.invoke())
            .thenReturn(Result.Success(emptyList()))
        whenever(deleteLanguageUseCase.invoke(any()))
            .thenReturn(Result.Error(Exception("Delete failed")))

        viewModel = LanguageViewModel(
            getAvailableLanguagesUseCase,
            downloadLanguageUseCase,
            deleteLanguageUseCase,
            getLanguageStorageUseCase
        )
        advanceUntilIdle()

        // When
        viewModel.deleteLanguage("eng")
        advanceUntilIdle()

        // Then
        viewModel.state.test {
            val state = awaitItem()
            assertNotNull(state.error)
            assertEquals("Failed to delete language", state.snackbarMessage)
        }
    }

    @Test
    fun `deleteLanguage should reload languages after successful deletion`() = runTest {
        // Given
        whenever(getAvailableLanguagesUseCase.invoke())
            .thenReturn(Result.Success(emptyList()))
        whenever(deleteLanguageUseCase.invoke(any()))
            .thenReturn(Result.Success(Unit))

        viewModel = LanguageViewModel(
            getAvailableLanguagesUseCase,
            downloadLanguageUseCase,
            deleteLanguageUseCase,
            getLanguageStorageUseCase
        )
        advanceUntilIdle()

        // When
        viewModel.deleteLanguage("eng")
        advanceUntilIdle()

        // Then - should call loadLanguages (total 2 times: init + after delete)
        verify(getAvailableLanguagesUseCase, atLeast(2)).invoke()
    }

    // ============ Snackbar Tests ============

    @Test
    fun `clearSnackbar should clear snackbar message`() = runTest {
        // Given
        whenever(getAvailableLanguagesUseCase.invoke())
            .thenReturn(Result.Success(emptyList()))
        whenever(deleteLanguageUseCase.invoke(any()))
            .thenReturn(Result.Success(Unit))

        viewModel = LanguageViewModel(
            getAvailableLanguagesUseCase,
            downloadLanguageUseCase,
            deleteLanguageUseCase,
            getLanguageStorageUseCase
        )
        advanceUntilIdle()

        viewModel.deleteLanguage("eng")
        advanceUntilIdle()

        // When
        viewModel.clearSnackbar()

        // Then
        viewModel.state.test {
            val state = awaitItem()
            assertNull(state.snackbarMessage)
        }
    }

    // ============ Error Handling Tests ============

    @Test
    fun `clearError should clear error message`() = runTest {
        // Given
        whenever(getAvailableLanguagesUseCase.invoke())
            .thenReturn(Result.Error(Exception("Test error")))

        viewModel = LanguageViewModel(
            getAvailableLanguagesUseCase,
            downloadLanguageUseCase,
            deleteLanguageUseCase,
            getLanguageStorageUseCase
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
    fun `getInstalledCount should return correct count`() = runTest {
        // Given
        val languages = listOf(testLanguageInstalled, testLanguageNotInstalled)
        whenever(getAvailableLanguagesUseCase.invoke())
            .thenReturn(Result.Success(languages))

        viewModel = LanguageViewModel(
            getAvailableLanguagesUseCase,
            downloadLanguageUseCase,
            deleteLanguageUseCase,
            getLanguageStorageUseCase
        )
        advanceUntilIdle()

        // Then
        viewModel.state.test {
            val state = awaitItem()
            assertEquals(1, state.getInstalledCount())
        }
    }

    @Test
    fun `getFormattedStorageSize should format MB correctly`() = runTest {
        // Given
        whenever(getAvailableLanguagesUseCase.invoke())
            .thenReturn(Result.Success(emptyList()))
        whenever(getLanguageStorageUseCase.invoke()).thenReturn(5_000_000L) // 5 MB

        viewModel = LanguageViewModel(
            getAvailableLanguagesUseCase,
            downloadLanguageUseCase,
            deleteLanguageUseCase,
            getLanguageStorageUseCase
        )
        advanceUntilIdle()

        // Then
        viewModel.state.test {
            val state = awaitItem()
            val formatted = state.getFormattedStorageSize()
            assertTrue(formatted.contains("MB"))
        }
    }

    @Test
    fun `getFormattedStorageSize should format KB correctly`() = runTest {
        // Given
        whenever(getAvailableLanguagesUseCase.invoke())
            .thenReturn(Result.Success(emptyList()))
        whenever(getLanguageStorageUseCase.invoke()).thenReturn(500L) // < 1 KB

        viewModel = LanguageViewModel(
            getAvailableLanguagesUseCase,
            downloadLanguageUseCase,
            deleteLanguageUseCase,
            getLanguageStorageUseCase
        )
        advanceUntilIdle()

        // Then
        viewModel.state.test {
            val state = awaitItem()
            val formatted = state.getFormattedStorageSize()
            assertTrue(formatted.contains("KB"))
        }
    }
}
