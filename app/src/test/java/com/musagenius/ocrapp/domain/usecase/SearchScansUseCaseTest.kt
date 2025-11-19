package com.musagenius.ocrapp.domain.usecase

import android.net.Uri
import com.musagenius.ocrapp.domain.model.Result
import com.musagenius.ocrapp.domain.model.ScanResult
import com.musagenius.ocrapp.domain.repository.ScanRepository
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.whenever
import java.util.Date

/**
 * Comprehensive unit tests for [SearchScansUseCase].
 *
 * This test suite validates scan search functionality including:
 * - Search query execution delegated to ScanRepository
 * - Result Flow emission from repository layer
 * - Empty result handling when no matches found
 * - Error propagation from repository failures
 * - Edge case handling (empty queries, special characters)
 * - Query passthrough without modification
 *
 * Tests verify proper Flow emission patterns, error handling,
 * and repository interaction for full-text search operations.
 *
 * @see SearchScansUseCase
 * @see ScanRepository
 */
class SearchScansUseCaseTest {

    /** Mock scan repository for search operations */
    @Mock
    private lateinit var scanRepository: ScanRepository

    /** System under test */
    private lateinit var useCase: SearchScansUseCase

    /** Test scan result with searchable text content */
    private val testScanResult = ScanResult(
        id = 1L,
        timestamp = Date(),
        imageUri = Uri.parse("content://test/image.jpg"),
        extractedText = "Test extracted text for searching",
        language = "eng",
        confidenceScore = 0.95f,
        title = "Test Scan",
        tags = listOf("test"),
        notes = "",
        isFavorite = false,
        modifiedTimestamp = Date()
    )

    /**
     * Sets up the test environment before each test.
     * Initializes mocks and creates the use case instance.
     */
    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        useCase = SearchScansUseCase(scanRepository)
    }

    @Test
    fun `invoke should return search results from repository`() = runTest {
        // Given
        val query = "test"
        val scans = listOf(testScanResult)
        whenever(scanRepository.searchScans(query))
            .thenReturn(flowOf(Result.Success(scans)))

        // When
        val results = useCase.invoke(query).toList()

        // Then
        assertEquals(1, results.size)
        assertTrue(results[0] is Result.Success)
        val data = (results[0] as Result.Success).data
        assertEquals(1, data.size)
        assertEquals("Test extracted text for searching", data[0].extractedText)
        verify(scanRepository).searchScans(query)
    }

    @Test
    fun `invoke should return empty list when no matches`() = runTest {
        // Given
        val query = "nonexistent"
        whenever(scanRepository.searchScans(query))
            .thenReturn(flowOf(Result.Success(emptyList())))

        // When
        val results = useCase.invoke(query).toList()

        // Then
        assertEquals(1, results.size)
        assertTrue(results[0] is Result.Success)
        assertTrue((results[0] as Result.Success).data.isEmpty())
    }

    @Test
    fun `invoke should handle repository errors`() = runTest {
        // Given
        val query = "test"
        whenever(scanRepository.searchScans(query))
            .thenReturn(flowOf(Result.Error("Search failed")))

        // When
        val results = useCase.invoke(query).toList()

        // Then
        assertEquals(1, results.size)
        assertTrue(results[0] is Result.Error)
        assertEquals("Search failed", (results[0] as Result.Error).exception.message)
    }

    @Test
    fun `invoke should handle empty query`() = runTest {
        // Given
        val query = ""
        whenever(scanRepository.searchScans(query))
            .thenReturn(flowOf(Result.Success(emptyList())))

        // When
        val results = useCase.invoke(query).toList()

        // Then
        verify(scanRepository).searchScans(query)
        assertTrue(results[0] is Result.Success)
    }

    @Test
    fun `invoke should handle special characters in query`() = runTest {
        // Given
        val query = "test@#$%"
        whenever(scanRepository.searchScans(query))
            .thenReturn(flowOf(Result.Success(emptyList())))

        // When
        val results = useCase.invoke(query).toList()

        // Then
        verify(scanRepository).searchScans(query)
        assertTrue(results[0] is Result.Success)
    }
}
