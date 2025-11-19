package com.musagenius.ocrapp.domain.model

import org.junit.Assert.*
import org.junit.Test

/**
 * Comprehensive unit tests for [OCRResult] domain model.
 *
 * This test suite validates OCR result data and utility methods including:
 * - Confidence threshold validation (hasAcceptableConfidence)
 * - Text presence checking (hasText)
 * - Confidence percentage formatting (0.856 → "85%")
 * - Processing time conversion (milliseconds to seconds)
 * - Word count calculation from extracted text
 * - Character count calculation
 * - Empty result factory method
 * - Language code preservation
 *
 * Tests ensure proper result object creation, data validation,
 * formatting utilities, and statistical calculations from OCR output.
 *
 * @see OCRResult
 */
class OCRResultTest {

    @Test
    fun `hasAcceptableConfidence returns true for confidence above threshold`() {
        val result = OCRResult(
            text = "Test",
            confidence = 0.8f,
            processingTimeMs = 1000,
            language = "eng"
        )

        assertTrue(result.hasAcceptableConfidence(0.5f))
        assertTrue(result.hasAcceptableConfidence(0.8f))
        assertFalse(result.hasAcceptableConfidence(0.9f))
    }

    @Test
    fun `hasText returns true for non-empty text`() {
        val resultWithText = OCRResult(
            text = "Hello World",
            confidence = 0.9f,
            processingTimeMs = 1000,
            language = "eng"
        )

        val resultWithoutText = OCRResult(
            text = "",
            confidence = 0.9f,
            processingTimeMs = 1000,
            language = "eng"
        )

        assertTrue(resultWithText.hasText())
        assertFalse(resultWithoutText.hasText())
    }

    @Test
    fun `getConfidencePercentage returns correct format`() {
        val result = OCRResult(
            text = "Test",
            confidence = 0.856f,
            processingTimeMs = 1000,
            language = "eng"
        )

        assertEquals("85%", result.getConfidencePercentage())
    }

    @Test
    fun `getProcessingTimeSeconds converts correctly`() {
        val result = OCRResult(
            text = "Test",
            confidence = 0.9f,
            processingTimeMs = 2500,
            language = "eng"
        )

        assertEquals(2.5, result.getProcessingTimeSeconds(), 0.001)
    }

    @Test
    fun `word count is calculated correctly`() {
        val result = OCRResult(
            text = "Hello world this is a test",
            confidence = 0.9f,
            processingTimeMs = 1000,
            language = "eng"
        )

        assertEquals(6, result.wordCount)
    }

    @Test
    fun `character count is correct`() {
        val result = OCRResult(
            text = "Hello",
            confidence = 0.9f,
            processingTimeMs = 1000,
            language = "eng"
        )

        assertEquals(5, result.characterCount)
    }

    @Test
    fun `empty factory method creates empty result`() {
        val empty = OCRResult.empty("fra")

        assertEquals("", empty.text)
        assertEquals(0f, empty.confidence, 0.001f)
        assertEquals(0L, empty.processingTimeMs)
        assertEquals("fra", empty.language)
        assertEquals(0, empty.wordCount)
    }
}
