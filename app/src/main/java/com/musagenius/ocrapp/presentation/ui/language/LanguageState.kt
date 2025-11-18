package com.musagenius.ocrapp.presentation.ui.language

import com.musagenius.ocrapp.domain.model.TesseractLanguage

/**
 * UI state for Language Management screen
 */
data class LanguageState(
    val languages: List<TesseractLanguage> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val downloadingLanguageCode: String? = null,
    val downloadProgress: Float = 0f,
    val totalStorageUsed: Long = 0L,
    val snackbarMessage: String? = null,
    val showDeleteConfirmation: String? = null // Language code to delete
) {
    /**
     * Get formatted storage size
     */
    fun getFormattedStorageSize(): String {
        val mb = totalStorageUsed / (1024.0 * 1024.0)
        return if (mb >= 1.0) {
            String.format("%.1f MB", mb)
        } else {
            val kb = totalStorageUsed / 1024.0
            String.format("%.1f KB", kb)
        }
    }

    /**
     * Get installed languages count
     */
    fun getInstalledCount(): Int {
        return languages.count { it.isInstalled }
    }
}
