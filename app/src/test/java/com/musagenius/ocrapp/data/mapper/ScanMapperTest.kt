package com.musagenius.ocrapp.data.mapper

import android.net.Uri
import com.musagenius.ocrapp.data.local.entity.ScanEntity
import com.musagenius.ocrapp.domain.model.ScanResult
import org.junit.Assert.*
import org.junit.Test
import java.util.Date

/**
 * Unit tests for ScanMapper
 */
class ScanMapperTest {

    private val testTimestamp = Date()
    private val testUri = Uri.parse("content://test/image.jpg")

    @Test
    fun `entity to domain conversion should map all fields correctly`() {
        // Given
        val entity = ScanEntity(
            id = 1L,
            timestamp = testTimestamp,
            imageUri = testUri,
            extractedText = "Test text",
            language = "eng",
            confidenceScore = 0.95f,
            title = "Test Title",
            tags = "tag1,tag2,tag3",
            notes = "Test notes",
            isFavorite = true,
            modifiedTimestamp = testTimestamp
        )

        // When
        val domain = entity.toDomain()

        // Then
        assertEquals(1L, domain.id)
        assertEquals(testTimestamp, domain.timestamp)
        assertEquals(testUri, domain.imageUri)
        assertEquals("Test text", domain.extractedText)
        assertEquals("eng", domain.language)
        assertEquals(0.95f, domain.confidenceScore, 0.001f)
        assertEquals("Test Title", domain.title)
        assertEquals(listOf("tag1", "tag2", "tag3"), domain.tags)
        assertEquals("Test notes", domain.notes)
        assertTrue(domain.isFavorite)
        assertEquals(testTimestamp, domain.modifiedTimestamp)
    }

    @Test
    fun `domain to entity conversion should map all fields correctly`() {
        // Given
        val domain = ScanResult(
            id = 1L,
            timestamp = testTimestamp,
            imageUri = testUri,
            extractedText = "Test text",
            language = "eng",
            confidenceScore = 0.95f,
            title = "Test Title",
            tags = listOf("tag1", "tag2", "tag3"),
            notes = "Test notes",
            isFavorite = true,
            modifiedTimestamp = testTimestamp
        )

        // When
        val entity = domain.toEntity()

        // Then
        assertEquals(1L, entity.id)
        assertEquals(testTimestamp, entity.timestamp)
        assertEquals(testUri, entity.imageUri)
        assertEquals("Test text", entity.extractedText)
        assertEquals("eng", entity.language)
        assertEquals(0.95f, entity.confidenceScore, 0.001f)
        assertEquals("Test Title", entity.title)
        assertEquals("tag1,tag2,tag3", entity.tags)
        assertEquals("Test notes", entity.notes)
        assertTrue(entity.isFavorite)
        assertEquals(testTimestamp, entity.modifiedTimestamp)
    }

    @Test
    fun `entity with empty tags should convert to empty list`() {
        // Given
        val entity = ScanEntity(
            id = 1L,
            timestamp = testTimestamp,
            imageUri = testUri,
            extractedText = "Test",
            language = "eng",
            confidenceScore = 0.9f,
            tags = ""
        )

        // When
        val domain = entity.toDomain()

        // Then
        assertTrue(domain.tags.isEmpty())
    }

    @Test
    fun `domain with empty tags list should convert to empty string`() {
        // Given
        val domain = ScanResult(
            timestamp = testTimestamp,
            imageUri = testUri,
            extractedText = "Test",
            language = "eng",
            confidenceScore = 0.9f,
            tags = emptyList()
        )

        // When
        val entity = domain.toEntity()

        // Then
        assertEquals("", entity.tags)
    }

    @Test
    fun `list of entities should convert to list of domain models`() {
        // Given
        val entities = listOf(
            ScanEntity(
                id = 1L,
                timestamp = testTimestamp,
                imageUri = testUri,
                extractedText = "Test 1",
                language = "eng",
                confidenceScore = 0.9f
            ),
            ScanEntity(
                id = 2L,
                timestamp = testTimestamp,
                imageUri = testUri,
                extractedText = "Test 2",
                language = "fra",
                confidenceScore = 0.85f
            )
        )

        // When
        val domainList = entities.toDomain()

        // Then
        assertEquals(2, domainList.size)
        assertEquals(1L, domainList[0].id)
        assertEquals(2L, domainList[1].id)
        assertEquals("Test 1", domainList[0].extractedText)
        assertEquals("Test 2", domainList[1].extractedText)
    }

    @Test
    fun `bidirectional conversion should maintain data integrity`() {
        // Given
        val originalDomain = ScanResult(
            id = 1L,
            timestamp = testTimestamp,
            imageUri = testUri,
            extractedText = "Test text",
            language = "eng",
            confidenceScore = 0.95f,
            title = "Test Title",
            tags = listOf("tag1", "tag2"),
            notes = "Notes",
            isFavorite = true,
            modifiedTimestamp = testTimestamp
        )

        // When
        val entity = originalDomain.toEntity()
        val convertedDomain = entity.toDomain()

        // Then
        assertEquals(originalDomain.id, convertedDomain.id)
        assertEquals(originalDomain.extractedText, convertedDomain.extractedText)
        assertEquals(originalDomain.language, convertedDomain.language)
        assertEquals(originalDomain.confidenceScore, convertedDomain.confidenceScore, 0.001f)
        assertEquals(originalDomain.tags, convertedDomain.tags)
    }
}
