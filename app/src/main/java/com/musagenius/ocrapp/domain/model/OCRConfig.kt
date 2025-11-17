package com.musagenius.ocrapp.domain.model

/**
 * Configuration for OCR processing
 */
data class OCRConfig(
    val language: String = "eng",
    val pageSegmentationMode: PageSegMode = PageSegMode.AUTO,
    val engineMode: EngineMode = EngineMode.DEFAULT,
    val preprocessImage: Boolean = true,
    val maxImageDimension: Int = 2000, // Max width/height in pixels
    val enableAutoRotation: Boolean = false
) {
    /**
     * Get the tessdata path for the given language
     */
    fun getTessdataFileName(): String = "$language.traineddata"
}

/**
 * Page segmentation modes for Tesseract
 * @see <a href="https://tesseract-ocr.github.io/tessdoc/ImproveQuality.html#page-segmentation-method">Tesseract PSM</a>
 */
enum class PageSegMode(val value: Int) {
    OSD_ONLY(0),           // Orientation and script detection only
    AUTO_OSD(1),           // Automatic page segmentation with OSD
    AUTO_ONLY(2),          // Automatic page segmentation, but no OSD
    AUTO(3),               // Fully automatic page segmentation (default)
    SINGLE_COLUMN(4),      // Assume a single column of text of variable sizes
    SINGLE_BLOCK_VERT(5),  // Assume a single uniform block of vertically aligned text
    SINGLE_BLOCK(6),       // Assume a single uniform block of text
    SINGLE_LINE(7),        // Treat the image as a single text line
    SINGLE_WORD(8),        // Treat the image as a single word
    CIRCLE_WORD(9),        // Treat the image as a single word in a circle
    SINGLE_CHAR(10),       // Treat the image as a single character
    SPARSE_TEXT(11),       // Sparse text. Find as much text as possible in no particular order
    SPARSE_TEXT_OSD(12),   // Sparse text with OSD
    RAW_LINE(13)           // Raw line. Treat the image as a single text line, bypassing hacks that are Tesseract-specific
}

/**
 * OCR engine modes
 */
enum class EngineMode(val value: Int) {
    TESSERACT_ONLY(0),     // Legacy engine only
    LSTM_ONLY(1),          // Neural nets LSTM engine only
    TESSERACT_LSTM(2),     // Legacy + LSTM engines
    DEFAULT(3)             // Default, based on what is available
}
