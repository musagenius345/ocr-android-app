package com.musagenius.ocrapp.data.local.entity

import android.net.Uri
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.Date

/**
 * Room entity representing a scanned document in the database
 *
 * Indexes are added on timestamp and language for efficient filtering and sorting
 */
@Entity(
    tableName = "scans",
    indices = [
        Index(value = ["timestamp"]),
        Index(value = ["language"])
    ]
)
data class ScanEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,

    @ColumnInfo(name = "timestamp")
    val timestamp: Date,

    @ColumnInfo(name = "image_uri")
    val imageUri: Uri,

    @ColumnInfo(name = "extracted_text")
    val extractedText: String,

    @ColumnInfo(name = "language")
    val language: String,

    @ColumnInfo(name = "confidence_score")
    val confidenceScore: Float,

    @ColumnInfo(name = "title", defaultValue = "")
    val title: String = "",

    @ColumnInfo(name = "tags", defaultValue = "")
    val tags: String = "", // Comma-separated tags

    @ColumnInfo(name = "notes", defaultValue = "")
    val notes: String = "",

    @ColumnInfo(name = "is_favorite", defaultValue = "0")
    val isFavorite: Boolean = false,

    @ColumnInfo(name = "modified_timestamp")
    val modifiedTimestamp: Date = timestamp
)
