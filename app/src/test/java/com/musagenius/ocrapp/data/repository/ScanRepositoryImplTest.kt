package com.musagenius.ocrapp.data.repository

import android.net.Uri
import com.musagenius.ocrapp.data.local.dao.ScanDao
import com.musagenius.ocrapp.data.local.entity.ScanEntity
import com.musagenius.ocrapp.domain.model.DateRange
import com.musagenius.ocrapp.domain.model.Result
import com.musagenius.ocrapp.domain.model.ScanResult
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations
import java.util.Date

/**
 * Unit tests for ScanRepositoryImpl
 */
class ScanRepositoryImplTest {

    @Mock
    private lateinit var scanDao: ScanDao

    private lateinit var repository: ScanRepositoryImpl

    private val testTimestamp = Date()
    private val testUri = Uri.parse("content://test/image.jpg")

    private val testScanEntity = ScanEntity(
        id = 1L,
        timestamp = testTimestamp,
        imageUri = testUri,
        extractedText = "Test text",
        language = "eng",
        confidenceScore = 0.95f,
        title = "Test",
        tags = "tag1,tag2",
        notes = "Notes",
        isFavorite = false,
        modifiedTimestamp = testTimestamp
    )

    private val testScanResult = ScanResult(
        id = 1L,
        timestamp = testTimestamp,
        imageUri = testUri,
        extractedText = "Test text",
        language = "eng",
        confidenceScore = 0.95f,
        title = "Test",
        tags = listOf("tag1", "tag2"),
        notes = "Notes",
        isFavorite = false,
        modifiedTimestamp = testTimestamp
    )

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        repository = ScanRepositoryImpl(scanDao)
    }

    @Test
    fun `insertScan should return success with scan id`() = runTest {
        // Given
        val expectedId = 1L
        `when`(scanDao.insert(any(ScanEntity::class.java))).thenReturn(expectedId)

        // When
        val result = repository.insertScan(testScanResult)

        // Then
        assertTrue(result is Result.Success)
        assertEquals(expectedId, (result as Result.Success).data)
        verify(scanDao).insert(any(ScanEntity::class.java))
    }

    @Test
    fun `insertScan should return error when exception occurs`() = runTest {
        // Given
        `when`(scanDao.insert(any(ScanEntity::class.java)))
            .thenThrow(RuntimeException("Database error"))

        // When
        val result = repository.insertScan(testScanResult)

        // Then
        assertTrue(result is Result.Error)
        assertNotNull((result as Result.Error).message)
    }

    @Test
    fun `getScanById should return success with scan when found`() = runTest {
        // Given
        `when`(scanDao.getById(1L)).thenReturn(testScanEntity)

        // When
        val result = repository.getScanById(1L)

        // Then
        assertTrue(result is Result.Success)
        val scan = (result as Result.Success).data
        assertEquals(1L, scan.id)
        assertEquals("Test text", scan.extractedText)
        verify(scanDao).getById(1L)
    }

    @Test
    fun `getScanById should return error when scan not found`() = runTest {
        // Given
        `when`(scanDao.getById(1L)).thenReturn(null)

        // When
        val result = repository.getScanById(1L)

        // Then
        assertTrue(result is Result.Error)
        assertTrue((result as Result.Error).message?.contains("not found") == true)
    }

    @Test
    fun `getAllScans should return success with list of scans`() = runTest {
        // Given
        val entities = listOf(testScanEntity)
        `when`(scanDao.getAllScans()).thenReturn(flowOf(entities))

        // When
        val result = repository.getAllScans().first()

        // Then
        assertTrue(result is Result.Success)
        val scans = (result as Result.Success).data
        assertEquals(1, scans.size)
        assertEquals("Test text", scans[0].extractedText)
    }

    @Test
    fun `deleteScan should return success when deletion succeeds`() = runTest {
        // Given
        `when`(scanDao.deleteById(1L)).thenReturn(Unit)

        // When
        val result = repository.deleteScan(1L)

        // Then
        assertTrue(result is Result.Success)
        verify(scanDao).deleteById(1L)
    }

    @Test
    fun `searchScans should return filtered results`() = runTest {
        // Given
        val query = "test"
        val entities = listOf(testScanEntity)
        `when`(scanDao.searchScans(query)).thenReturn(flowOf(entities))

        // When
        val result = repository.searchScans(query).first()

        // Then
        assertTrue(result is Result.Success)
        val scans = (result as Result.Success).data
        assertEquals(1, scans.size)
        verify(scanDao).searchScans(query)
    }

    @Test
    fun `updateFavoriteStatus should call dao with correct parameters`() = runTest {
        // Given
        val scanId = 1L
        val isFavorite = true
        `when`(scanDao.updateFavoriteStatus(scanId, isFavorite)).thenReturn(Unit)

        // When
        val result = repository.updateFavoriteStatus(scanId, isFavorite)

        // Then
        assertTrue(result is Result.Success)
        verify(scanDao).updateFavoriteStatus(scanId, isFavorite)
    }

    @Test
    fun `getFavoriteScans should return only favorite scans`() = runTest {
        // Given
        val favoriteEntity = testScanEntity.copy(isFavorite = true)
        `when`(scanDao.getFavoriteScans()).thenReturn(flowOf(listOf(favoriteEntity)))

        // When
        val result = repository.getFavoriteScans().first()

        // Then
        assertTrue(result is Result.Success)
        val scans = (result as Result.Success).data
        assertEquals(1, scans.size)
        assertTrue(scans[0].isFavorite)
    }

    @Test
    fun `getScansByLanguage should filter by language`() = runTest {
        // Given
        val language = "eng"
        `when`(scanDao.getScansByLanguage(language)).thenReturn(flowOf(listOf(testScanEntity)))

        // When
        val result = repository.getScansByLanguage(language).first()

        // Then
        assertTrue(result is Result.Success)
        val scans = (result as Result.Success).data
        assertEquals(1, scans.size)
        assertEquals(language, scans[0].language)
    }

    @Test
    fun `getScansCount should return total count`() = runTest {
        // Given
        val expectedCount = 5
        `when`(scanDao.getScansCount()).thenReturn(expectedCount)

        // When
        val result = repository.getScansCount()

        // Then
        assertTrue(result is Result.Success)
        assertEquals(expectedCount, (result as Result.Success).data)
    }

    @Test
    fun `deleteOldScans should delete scans older than specified days`() = runTest {
        // Given
        val daysOld = 30
        val deletedCount = 3
        `when`(scanDao.deleteOldScans(anyLong())).thenReturn(deletedCount)

        // When
        val result = repository.deleteOldScans(daysOld)

        // Then
        assertTrue(result is Result.Success)
        assertEquals(deletedCount, (result as Result.Success).data)
        verify(scanDao).deleteOldScans(anyLong())
    }

    @Test
    fun `getHighConfidenceScans should filter by threshold`() = runTest {
        // Given
        val threshold = 0.8f
        val highConfidenceEntity = testScanEntity.copy(confidenceScore = 0.95f)
        `when`(scanDao.getHighConfidenceScans(threshold))
            .thenReturn(flowOf(listOf(highConfidenceEntity)))

        // When
        val result = repository.getHighConfidenceScans(threshold).first()

        // Then
        assertTrue(result is Result.Success)
        val scans = (result as Result.Success).data
        assertEquals(1, scans.size)
        assertTrue(scans[0].confidenceScore >= threshold)
    }

    // Helper function to handle any() for Kotlin
    private fun <T> any(type: Class<T>): T = Mockito.any(type)
}
