package com.musagenius.ocrapp.domain.usecase

import android.net.Uri
import com.musagenius.ocrapp.domain.model.OCRConfig
import com.musagenius.ocrapp.domain.model.Result
import com.musagenius.ocrapp.domain.model.ScanResult
import com.musagenius.ocrapp.domain.repository.ScanRepository
import com.musagenius.ocrapp.domain.service.OCRService
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.argThat
import org.mockito.kotlin.whenever
import java.util.Date

/**
 * Unit tests for scan management use cases.
 *
 * This test suite validates the following use cases:
 * - [GetAllScansUseCase]: Retrieves all scan results with Flow emissions
 * - [DeleteScanUseCase]: Deletes scans by ID with error handling
 * - [InitializeOCRUseCase]: Initializes OCR engine with configuration
 *
 * Tests cover:
 * - Successful operations with correct data flow
 * - Error propagation from repository/service layers
 * - Loading states and Flow emissions
 * - Language availability checks
 * - Multiple emissions and concurrent operations
 * - Edge cases (non-existent IDs, empty results, etc.)
 *
 * Uses Mockito for mocking repository and service dependencies,
 * and coroutine test utilities for async operation testing.
 *
 * @see GetAllScansUseCase
 * @see DeleteScanUseCase
 * @see InitializeOCRUseCase
 */
class ScanManagementUseCasesTest {

    /** Mock repository for scan data operations */
    @Mock
    private lateinit var scanRepository: ScanRepository

    /** Mock OCR service for initialization testing */
    @Mock
    private lateinit var ocrService: OCRService

    /** Use case under test for retrieving all scans */
    private lateinit var getAllScansUseCase: GetAllScansUseCase

    /** Use case under test for deleting scans */
    private lateinit var deleteScanUseCase: DeleteScanUseCase

    /** Use case under test for OCR initialization */
    private lateinit var initializeOCRUseCase: InitializeOCRUseCase

    /** Test scan result fixture for use in tests */
    private val testScanResult = ScanResult(
        id = 1L,
        timestamp = Date(),
        imageUri = Uri.parse("content://test/image.jpg"),
        extractedText = "Test text",
        language = "eng",
        confidenceScore = 0.95f,
        title = "Test",
        tags = emptyList(),
        notes = "",
        isFavorite = false,
        modifiedTimestamp = Date()
    )

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        getAllScansUseCase = GetAllScansUseCase(scanRepository)
        deleteScanUseCase = DeleteScanUseCase(scanRepository)
        initializeOCRUseCase = InitializeOCRUseCase(ocrService)
    }

    // ============ GetAllScansUseCase Tests ============

    @Test
    fun `getAllScansUseCase should return all scans from repository`() = runTest {
        // Given
        val scans = listOf(testScanResult)
        whenever(scanRepository.getAllScans())
            .thenReturn(flowOf(Result.Success(scans)))

        // When
        val results = getAllScansUseCase().toList()

        // Then
        assertEquals(1, results.size)
        assertTrue(results[0] is Result.Success)
        val data = (results[0] as Result.Success).data
        assertEquals(1, data.size)
        assertEquals("Test text", data[0].extractedText)
        verify(scanRepository).getAllScans()
    }

    @Test
    fun `getAllScansUseCase should return empty list when no scans`() = runTest {
        // Given
        whenever(scanRepository.getAllScans())
            .thenReturn(flowOf(Result.Success(emptyList())))

        // When
        val results = getAllScansUseCase().toList()

        // Then
        assertEquals(1, results.size)
        assertTrue(results[0] is Result.Success)
        assertTrue((results[0] as Result.Success).data.isEmpty())
    }

    @Test
    fun `getAllScansUseCase should propagate repository errors`() = runTest {
        // Given
        whenever(scanRepository.getAllScans())
            .thenReturn(flowOf(Result.Error("Database error")))

        // When
        val results = getAllScansUseCase().toList()

        // Then
        assertEquals(1, results.size)
        assertTrue(results[0] is Result.Error)
        assertEquals("Database error", (results[0] as Result.Error).exception.message)
    }

    @Test
    fun `getAllScansUseCase should handle loading state`() = runTest {
        // Given
        whenever(scanRepository.getAllScans())
            .thenReturn(flowOf(
                Result.Loading(),
                Result.Success(listOf(testScanResult))
            ))

        // When
        val results = getAllScansUseCase().toList()

        // Then
        assertEquals(2, results.size)
        assertTrue(results[0] is Result.Loading)
        assertTrue(results[1] is Result.Success)
    }

    // ============ DeleteScanUseCase Tests ============

    @Test
    fun `deleteScanUseCase should delete scan successfully`() = runTest {
        // Given
        whenever(scanRepository.deleteScan(1L))
            .thenReturn(Result.Success(Unit))

        // When
        val result = deleteScanUseCase(1L)

        // Then
        assertTrue(result is Result.Success)
        verify(scanRepository).deleteScan(1L)
    }

    @Test
    fun `deleteScanUseCase should handle deletion errors`() = runTest {
        // Given
        whenever(scanRepository.deleteScan(1L))
            .thenReturn(Result.Error("Delete failed"))

        // When
        val result = deleteScanUseCase(1L)

        // Then
        assertTrue(result is Result.Error)
        assertEquals("Delete failed", (result as Result.Error).exception.message)
    }

    @Test
    fun `deleteScanUseCase should handle non-existent scan ID`() = runTest {
        // Given
        whenever(scanRepository.deleteScan(999L))
            .thenReturn(Result.Error("Scan not found"))

        // When
        val result = deleteScanUseCase(999L)

        // Then
        assertTrue(result is Result.Error)
        assertTrue((result as Result.Error).exception.message!!.contains("not found"))
    }

    @Test
    fun `deleteScanUseCase should call repository with correct ID`() = runTest {
        // Given
        val scanId = 42L
        whenever(scanRepository.deleteScan(scanId))
            .thenReturn(Result.Success(Unit))

        // When
        deleteScanUseCase(scanId)

        // Then
        verify(scanRepository).deleteScan(scanId)
        verify(scanRepository, never()).deleteScan(argThat { this != scanId })
    }

    // ============ InitializeOCRUseCase Tests ============

    @Test
    fun `initializeOCRUseCase should initialize OCR service successfully`() = runTest {
        // Given
        val config = OCRConfig(language = "eng")
        whenever(ocrService.initialize(config))
            .thenReturn(Result.Success(Unit))

        // When
        val result = initializeOCRUseCase(config)

        // Then
        assertTrue(result is Result.Success)
        verify(ocrService).initialize(config)
    }

    @Test
    fun `initializeOCRUseCase should use default config when not provided`() = runTest {
        // Given
        whenever(ocrService.initialize(any()))
            .thenReturn(Result.Success(Unit))

        // When
        initializeOCRUseCase()

        // Then
        verify(ocrService).initialize(argThat { config ->
            config.language == "eng" &&
            config.preprocessImage &&
            config.maxImageDimension == 2000
        })
    }

    @Test
    fun `initializeOCRUseCase should handle initialization errors`() = runTest {
        // Given
        val config = OCRConfig()
        whenever(ocrService.initialize(config))
            .thenReturn(Result.Error("Init failed"))

        // When
        val result = initializeOCRUseCase(config)

        // Then
        assertTrue(result is Result.Error)
        assertEquals("Init failed", (result as Result.Error).exception.message)
    }

    @Test
    fun `initializeOCRUseCase should initialize with custom language`() = runTest {
        // Given
        val config = OCRConfig(language = "spa")
        whenever(ocrService.initialize(config))
            .thenReturn(Result.Success(Unit))

        // When
        initializeOCRUseCase(config)

        // Then
        verify(ocrService).initialize(argThat { it.language == "spa" })
    }

    @Test
    fun `isLanguageAvailable should check language availability`() = runTest {
        // Given
        whenever(ocrService.isLanguageAvailable("eng"))
            .thenReturn(true)

        // When
        val result = initializeOCRUseCase.isLanguageAvailable("eng")

        // Then
        assertTrue(result)
        verify(ocrService).isLanguageAvailable("eng")
    }

    @Test
    fun `isLanguageAvailable should return false for unavailable language`() = runTest {
        // Given
        whenever(ocrService.isLanguageAvailable("xyz"))
            .thenReturn(false)

        // When
        val result = initializeOCRUseCase.isLanguageAvailable("xyz")

        // Then
        assertFalse(result)
    }

    @Test
    fun `getAvailableLanguages should return list of languages`() = runTest {
        // Given
        val languages = listOf("eng", "spa", "fra")
        whenever(ocrService.getAvailableLanguages())
            .thenReturn(languages)

        // When
        val result = initializeOCRUseCase.getAvailableLanguages()

        // Then
        assertEquals(3, result.size)
        assertTrue(result.contains("eng"))
        assertTrue(result.contains("spa"))
        assertTrue(result.contains("fra"))
        verify(ocrService).getAvailableLanguages()
    }

    @Test
    fun `getAvailableLanguages should return empty list when no languages installed`() = runTest {
        // Given
        whenever(ocrService.getAvailableLanguages())
            .thenReturn(emptyList())

        // When
        val result = initializeOCRUseCase.getAvailableLanguages()

        // Then
        assertTrue(result.isEmpty())
    }

    // ============ Edge Case Tests ============

    @Test
    fun `getAllScansUseCase should handle multiple emissions`() = runTest {
        // Given
        val scan1 = testScanResult.copy(id = 1L)
        val scan2 = testScanResult.copy(id = 2L)
        whenever(scanRepository.getAllScans())
            .thenReturn(flowOf(
                Result.Success(listOf(scan1)),
                Result.Success(listOf(scan1, scan2))
            ))

        // When
        val results = getAllScansUseCase().toList()

        // Then
        assertEquals(2, results.size)
        assertEquals(1, (results[0] as Result.Success).data.size)
        assertEquals(2, (results[1] as Result.Success).data.size)
    }

    @Test
    fun `deleteScanUseCase should handle concurrent deletions`() = runTest {
        // Given
        whenever(scanRepository.deleteScan(any()))
            .thenReturn(Result.Success(Unit))

        // When - delete multiple scans
        deleteScanUseCase(1L)
        deleteScanUseCase(2L)
        deleteScanUseCase(3L)

        // Then
        verify(scanRepository).deleteScan(1L)
        verify(scanRepository).deleteScan(2L)
        verify(scanRepository).deleteScan(3L)
    }

    @Test
    fun `initializeOCRUseCase should handle different preprocessing settings`() = runTest {
        // Given
        val configWithPreprocessing = OCRConfig(preprocessImage = true)
        val configWithoutPreprocessing = OCRConfig(preprocessImage = false)
        whenever(ocrService.initialize(any()))
            .thenReturn(Result.Success(Unit))

        // When
        initializeOCRUseCase(configWithPreprocessing)
        initializeOCRUseCase(configWithoutPreprocessing)

        // Then
        verify(ocrService).initialize(argThat { it.preprocessImage })
        verify(ocrService).initialize(argThat { !it.preprocessImage })
    }
}
