package com.musagenius.ocrapp.domain.model

/**
 * Represents a language available for OCR
 */
data class Language(
    /** Language code (e.g., "eng", "fra", "deu") */
    val code: String,

    /** Human-readable display name (e.g., "English", "French", "German") */
    val displayName: String,

    /** Whether the language data is currently installed/available */
    val isInstalled: Boolean = false,

    /** Size of the language data file in bytes (if installed) */
    val fileSize: Long = 0L
) {
    companion object {
        /**
         * Map of language codes to display names for common languages
         */
        val LANGUAGE_NAMES = mapOf(
            "afr" to "Afrikaans",
            "amh" to "Amharic",
            "ara" to "Arabic",
            "asm" to "Assamese",
            "aze" to "Azerbaijani",
            "aze_cyrl" to "Azerbaijani (Cyrillic)",
            "bel" to "Belarusian",
            "ben" to "Bengali",
            "bod" to "Tibetan",
            "bos" to "Bosnian",
            "bul" to "Bulgarian",
            "cat" to "Catalan",
            "ceb" to "Cebuano",
            "ces" to "Czech",
            "chi_sim" to "Chinese (Simplified)",
            "chi_tra" to "Chinese (Traditional)",
            "chr" to "Cherokee",
            "cym" to "Welsh",
            "dan" to "Danish",
            "deu" to "German",
            "dzo" to "Dzongkha",
            "ell" to "Greek",
            "eng" to "English",
            "enm" to "Middle English",
            "epo" to "Esperanto",
            "est" to "Estonian",
            "eus" to "Basque",
            "fas" to "Persian",
            "fin" to "Finnish",
            "fra" to "French",
            "frk" to "Frankish",
            "frm" to "Middle French",
            "gle" to "Irish",
            "glg" to "Galician",
            "grc" to "Ancient Greek",
            "guj" to "Gujarati",
            "hat" to "Haitian",
            "heb" to "Hebrew",
            "hin" to "Hindi",
            "hrv" to "Croatian",
            "hun" to "Hungarian",
            "iku" to "Inuktitut",
            "ind" to "Indonesian",
            "isl" to "Icelandic",
            "ita" to "Italian",
            "ita_old" to "Italian (Old)",
            "jav" to "Javanese",
            "jpn" to "Japanese",
            "kan" to "Kannada",
            "kat" to "Georgian",
            "kat_old" to "Georgian (Old)",
            "kaz" to "Kazakh",
            "khm" to "Khmer",
            "kir" to "Kyrgyz",
            "kor" to "Korean",
            "kur" to "Kurdish",
            "lao" to "Lao",
            "lat" to "Latin",
            "lav" to "Latvian",
            "lit" to "Lithuanian",
            "mal" to "Malayalam",
            "mar" to "Marathi",
            "mkd" to "Macedonian",
            "mlt" to "Maltese",
            "msa" to "Malay",
            "mya" to "Burmese",
            "nep" to "Nepali",
            "nld" to "Dutch",
            "nor" to "Norwegian",
            "ori" to "Oriya",
            "pan" to "Punjabi",
            "pol" to "Polish",
            "por" to "Portuguese",
            "pus" to "Pashto",
            "ron" to "Romanian",
            "rus" to "Russian",
            "san" to "Sanskrit",
            "sin" to "Sinhala",
            "slk" to "Slovak",
            "slv" to "Slovenian",
            "spa" to "Spanish",
            "spa_old" to "Spanish (Old)",
            "sqi" to "Albanian",
            "srp" to "Serbian",
            "srp_latn" to "Serbian (Latin)",
            "swa" to "Swahili",
            "swe" to "Swedish",
            "syr" to "Syriac",
            "tam" to "Tamil",
            "tel" to "Telugu",
            "tgk" to "Tajik",
            "tgl" to "Tagalog",
            "tha" to "Thai",
            "tir" to "Tigrinya",
            "tur" to "Turkish",
            "uig" to "Uighur",
            "ukr" to "Ukrainian",
            "urd" to "Urdu",
            "uzb" to "Uzbek",
            "uzb_cyrl" to "Uzbek (Cyrillic)",
            "vie" to "Vietnamese",
            "yid" to "Yiddish"
        )

        /**
         * Get display name for a language code
         */
        fun getDisplayName(code: String): String {
            return LANGUAGE_NAMES[code] ?: code.uppercase()
        }
    }
}
