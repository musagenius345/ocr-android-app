package com.musagenius.ocrapp.domain.model

/**
 * Represents a Tesseract OCR language
 */
data class TesseractLanguage(
    val code: String,
    val name: String,
    val downloadUrl: String,
    val fileSizeBytes: Long,
    val isInstalled: Boolean = false,
    val isDownloading: Boolean = false,
    val downloadProgress: Float = 0f
) {
    /**
     * Get human-readable file size
     */
    fun getFileSizeFormatted(): String {
        val kb = fileSizeBytes / 1024.0
        val mb = kb / 1024.0

        return when {
            mb >= 1.0 -> String.format("%.1f MB", mb)
            kb >= 1.0 -> String.format("%.1f KB", kb)
            else -> "$fileSizeBytes B"
        }
    }

    companion object {
        /**
         * Tesseract download base URL
         */
        private const val BASE_URL = "https://github.com/tesseract-ocr/tessdata/raw/main/"

        /**
         * List of supported languages with metadata
         */
        fun getSupportedLanguages(): List<TesseractLanguage> {
            return listOf(
                TesseractLanguage("eng", "English", "${BASE_URL}eng.traineddata", 25_355_243),
                TesseractLanguage("ara", "Arabic", "${BASE_URL}ara.traineddata", 22_589_446),
                TesseractLanguage("chi_sim", "Chinese (Simplified)", "${BASE_URL}chi_sim.traineddata", 51_623_653),
                TesseractLanguage("chi_tra", "Chinese (Traditional)", "${BASE_URL}chi_tra.traineddata", 51_602_573),
                TesseractLanguage("fra", "French", "${BASE_URL}fra.traineddata", 23_871_774),
                TesseractLanguage("deu", "German", "${BASE_URL}deu.traineddata", 26_885_738),
                TesseractLanguage("hin", "Hindi", "${BASE_URL}hin.traineddata", 25_437_996),
                TesseractLanguage("ita", "Italian", "${BASE_URL}ita.traineddata", 23_789_054),
                TesseractLanguage("jpn", "Japanese", "${BASE_URL}jpn.traineddata", 31_748_590),
                TesseractLanguage("kor", "Korean", "${BASE_URL}kor.traineddata", 20_846_347),
                TesseractLanguage("por", "Portuguese", "${BASE_URL}por.traineddata", 22_891_662),
                TesseractLanguage("rus", "Russian", "${BASE_URL}rus.traineddata", 29_996_689),
                TesseractLanguage("spa", "Spanish", "${BASE_URL}spa.traineddata", 24_890_508),
                TesseractLanguage("tha", "Thai", "${BASE_URL}tha.traineddata", 8_936_639),
                TesseractLanguage("tur", "Turkish", "${BASE_URL}tur.traineddata", 23_958_695),
                TesseractLanguage("vie", "Vietnamese", "${BASE_URL}vie.traineddata", 8_267_178)
            )
        }
    }
}
