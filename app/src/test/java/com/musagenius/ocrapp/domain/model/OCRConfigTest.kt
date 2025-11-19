package com.musagenius.ocrapp.domain.model

import org.junit.Assert.*
import org.junit.Test

/**
 * Comprehensive unit tests for [OCRConfig] domain model.
 *
 * This test suite validates OCR configuration settings including:
 * - Tessdata filename generation from language codes
 * - Default configuration values (language, page segmentation mode, engine mode)
 * - Custom configuration overrides
 * - PageSegMode enum values mapping to Tesseract constants
 * - EngineMode enum values mapping to Tesseract constants
 * - Image preprocessing settings
 * - Maximum image dimension constraints
 * - Auto-rotation configuration
 *
 * Tests ensure proper configuration object creation, default values,
 * and correct mapping of enum values to Tesseract API constants.
 *
 * @see OCRConfig
 * @see PageSegMode
 * @see EngineMode
 */
class OCRConfigTest {

    @Test
    fun `getTessdataFileName returns correct format`() {
        val config = OCRConfig(language = "eng")
        assertEquals("eng.traineddata", config.getTessdataFileName())

        val frenchConfig = OCRConfig(language = "fra")
        assertEquals("fra.traineddata", frenchConfig.getTessdataFileName())
    }

    @Test
    fun `default config has correct values`() {
        val config = OCRConfig()

        assertEquals("eng", config.language)
        assertEquals(PageSegMode.AUTO, config.pageSegmentationMode)
        assertEquals(EngineMode.DEFAULT, config.engineMode)
        assertTrue(config.preprocessImage)
        assertEquals(2000, config.maxImageDimension)
        assertFalse(config.enableAutoRotation)
    }

    @Test
    fun `custom config overrides defaults`() {
        val config = OCRConfig(
            language = "fra",
            pageSegmentationMode = PageSegMode.SINGLE_LINE,
            engineMode = EngineMode.LSTM_ONLY,
            preprocessImage = false,
            maxImageDimension = 3000,
            enableAutoRotation = true
        )

        assertEquals("fra", config.language)
        assertEquals(PageSegMode.SINGLE_LINE, config.pageSegmentationMode)
        assertEquals(EngineMode.LSTM_ONLY, config.engineMode)
        assertFalse(config.preprocessImage)
        assertEquals(3000, config.maxImageDimension)
        assertTrue(config.enableAutoRotation)
    }

    @Test
    fun `PageSegMode values are correct`() {
        assertEquals(0, PageSegMode.OSD_ONLY.value)
        assertEquals(3, PageSegMode.AUTO.value)
        assertEquals(7, PageSegMode.SINGLE_LINE.value)
        assertEquals(11, PageSegMode.SPARSE_TEXT.value)
    }

    @Test
    fun `EngineMode values are correct`() {
        assertEquals(0, EngineMode.TESSERACT_ONLY.value)
        assertEquals(1, EngineMode.LSTM_ONLY.value)
        assertEquals(2, EngineMode.TESSERACT_LSTM.value)
        assertEquals(3, EngineMode.DEFAULT.value)
    }
}
