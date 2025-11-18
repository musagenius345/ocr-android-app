package com.musagenius.ocrapp.domain.model

/**
 * Status of a language download
 */
sealed class DownloadStatus {
    data object Idle : DownloadStatus()
    data class Downloading(val progress: Float) : DownloadStatus()
    data object Completed : DownloadStatus()
    data class Failed(val error: String) : DownloadStatus()
}
