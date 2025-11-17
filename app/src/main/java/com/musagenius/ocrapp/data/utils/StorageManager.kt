package com.musagenius.ocrapp.data.utils

import android.content.Context
import android.os.Environment
import android.os.StatFs
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manager for storage operations and cleanup
 */
@Singleton
class StorageManager @Inject constructor(
    private val context: Context
) {

    companion object {
        private const val TAG = "StorageManager"
        private const val MIN_FREE_SPACE_MB = 100L // Minimum free space required
        private const val TEMP_FILE_MAX_AGE_DAYS = 7 // Clean up temp files older than this
    }

    /**
     * Check if sufficient storage is available
     * @param requiredMB Required space in megabytes
     * @return true if sufficient space available
     */
    fun hasAvailableStorage(requiredMB: Long = MIN_FREE_SPACE_MB): Boolean {
        return try {
            val availableMB = getAvailableStorageMB()
            availableMB >= requiredMB
        } catch (e: Exception) {
            Log.e(TAG, "Error checking storage", e)
            false
        }
    }

    /**
     * Get available storage in megabytes
     */
    fun getAvailableStorageMB(): Long {
        return try {
            val externalStorageDir = context.getExternalFilesDir(null)
            val stat = StatFs(externalStorageDir?.path ?: return 0L)
            val bytesAvailable = stat.availableBlocksLong * stat.blockSizeLong
            bytesAvailable / (1024 * 1024)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting available storage", e)
            0L
        }
    }

    /**
     * Get total storage in megabytes
     */
    fun getTotalStorageMB(): Long {
        return try {
            val externalStorageDir = context.getExternalFilesDir(null)
            val stat = StatFs(externalStorageDir?.path ?: return 0L)
            val bytesTotal = stat.blockCountLong * stat.blockSizeLong
            bytesTotal / (1024 * 1024)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting total storage", e)
            0L
        }
    }

    /**
     * Get used storage in megabytes
     */
    fun getUsedStorageMB(): Long {
        return try {
            getTotalStorageMB() - getAvailableStorageMB()
        } catch (e: Exception) {
            0L
        }
    }

    /**
     * Calculate storage used by app files
     */
    suspend fun calculateAppStorageUsage(): StorageInfo = withContext(Dispatchers.IO) {
        try {
            val externalDir = context.getExternalFilesDir(null)
            val cacheDir = context.cacheDir

            val externalSize = calculateDirectorySize(externalDir)
            val cacheSize = calculateDirectorySize(cacheDir)

            StorageInfo(
                totalSizeMB = (externalSize + cacheSize) / (1024 * 1024),
                imagesSizeMB = externalSize / (1024 * 1024),
                cacheSizeMB = cacheSize / (1024 * 1024)
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error calculating storage usage", e)
            StorageInfo()
        }
    }

    /**
     * Calculate total size of a directory
     */
    private fun calculateDirectorySize(directory: File?): Long {
        if (directory == null || !directory.exists()) return 0L

        var size = 0L
        directory.listFiles()?.forEach { file ->
            size += if (file.isDirectory) {
                calculateDirectorySize(file)
            } else {
                file.length()
            }
        }
        return size
    }

    /**
     * Clean up old temporary files
     */
    suspend fun cleanupOldFiles(maxAgeDays: Int = TEMP_FILE_MAX_AGE_DAYS): Int = withContext(Dispatchers.IO) {
        try {
            val externalDir = context.getExternalFilesDir(null) ?: return@withContext 0
            val cutoffTime = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(maxAgeDays.toLong())

            var deletedCount = 0
            externalDir.listFiles()?.forEach { file ->
                if (file.isFile && file.lastModified() < cutoffTime && file.name.startsWith("compressed_")) {
                    if (file.delete()) {
                        deletedCount++
                        Log.d(TAG, "Deleted old file: ${file.name}")
                    }
                }
            }

            Log.d(TAG, "Cleanup complete: deleted $deletedCount files")
            deletedCount
        } catch (e: Exception) {
            Log.e(TAG, "Error during cleanup", e)
            0
        }
    }

    /**
     * Clear all cache files
     */
    suspend fun clearCache(): Boolean = withContext(Dispatchers.IO) {
        try {
            val cacheDir = context.cacheDir
            deleteRecursively(cacheDir)
            Log.d(TAG, "Cache cleared")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing cache", e)
            false
        }
    }

    /**
     * Delete directory and all contents
     */
    private fun deleteRecursively(file: File): Boolean {
        if (file.isDirectory) {
            file.listFiles()?.forEach { child ->
                deleteRecursively(child)
            }
        }
        return file.delete()
    }

    /**
     * Delete specific file
     */
    fun deleteFile(filePath: String): Boolean {
        return try {
            val file = File(filePath)
            file.delete()
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting file: $filePath", e)
            false
        }
    }

    /**
     * Check if external storage is writable
     */
    fun isExternalStorageWritable(): Boolean {
        val state = Environment.getExternalStorageState()
        return Environment.MEDIA_MOUNTED == state
    }
}

/**
 * Storage information data class
 */
data class StorageInfo(
    val totalSizeMB: Long = 0,
    val imagesSizeMB: Long = 0,
    val cacheSizeMB: Long = 0
) {
    fun getTotalSizeFormatted(): String = "${totalSizeMB}MB"
    fun getImagesSizeFormatted(): String = "${imagesSizeMB}MB"
    fun getCacheSizeFormatted(): String = "${cacheSizeMB}MB"
}
