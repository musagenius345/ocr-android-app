package com.musagenius.ocrapp.data.local

import android.net.Uri
import androidx.room.TypeConverter
import java.util.Date

/**
 * Type converters for Room database
 * Converts complex types to primitive types that Room can store
 */
class Converters {

    /**
     * Convert timestamp (Long) to Date object
     */
    @TypeConverter
    fun fromTimestamp(value: Long?): Date? {
        return value?.let { Date(it) }
    }

    /**
     * Convert Date object to timestamp (Long)
     */
    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? {
        return date?.time
    }

    /**
     * Convert String to Uri
     */
    @TypeConverter
    fun fromString(value: String?): Uri? {
        return value?.let { Uri.parse(it) }
    }

    /**
     * Convert Uri to String
     */
    @TypeConverter
    fun uriToString(uri: Uri?): String? {
        return uri?.toString()
    }
}
