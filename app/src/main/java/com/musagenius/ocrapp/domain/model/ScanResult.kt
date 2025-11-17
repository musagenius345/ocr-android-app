package com.musagenius.ocrapp.domain.model

import android.net.Uri
import java.util.Date

/**
 * Domain model representing a scanned document
 * This is independent of the data layer implementation
 */
data class ScanResult(
    val id: Long = 0,
    val timestamp: Date,
    val imageUri: Uri,
    val extractedText: String,
    val language: String,
    val confidenceScore: Float,
    val title: String = "",
    val tags: List<String> = emptyList(),
    val notes: String = "",
    val isFavorite: Boolean = false,
    val modifiedTimestamp: Date = timestamp
) {
    /**
     * Get a preview of the extracted text (first 100 characters)
     */
    fun getTextPreview(maxLength: Int = 100): String {
        return if (extractedText.length <= maxLength) {
            extractedText
        } else {
            extractedText.take(maxLength) + "..."
        }
    }

    /**
     * Check if the scan has high confidence
     */
    fun hasHighConfidence(threshold: Float = 0.8f): Boolean {
        return confidenceScore >= threshold
    }

    /**
     * Get formatted confidence percentage
     */
    fun getConfidencePercentage(): String {
        return "${(confidenceScore * 100).toInt()}%"
    }

    /**
     * Check if scan has been modified after creation
     */
    fun isModified(): Boolean {
        return modifiedTimestamp.after(timestamp)
    }

    /**
     * Get word count of extracted text
     */
    fun getWordCount(): Int {
        return extractedText.trim().split("\\s+".toRegex()).size
    }

    /**
     * Check if scan has a specific tag
     */
    fun hasTag(tag: String): Boolean {
        return tags.any { it.equals(tag, ignoreCase = true) }
    }
}
