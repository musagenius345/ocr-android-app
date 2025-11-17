package com.musagenius.ocrapp.data.local.dao

import androidx.room.*
import com.musagenius.ocrapp.data.local.entity.ScanEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for scan operations
 * Provides methods to interact with the scans table
 */
@Dao
interface ScanDao {

    /**
     * Insert a new scan into the database
     * @return The ID of the inserted scan
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(scan: ScanEntity): Long

    /**
     * Insert multiple scans
     * @return List of inserted IDs
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(scans: List<ScanEntity>): List<Long>

    /**
     * Update an existing scan
     */
    @Update
    suspend fun update(scan: ScanEntity)

    /**
     * Delete a scan from the database
     */
    @Delete
    suspend fun delete(scan: ScanEntity)

    /**
     * Delete scan by ID
     */
    @Query("DELETE FROM scans WHERE id = :scanId")
    suspend fun deleteById(scanId: Long)

    /**
     * Delete multiple scans by their IDs
     */
    @Query("DELETE FROM scans WHERE id IN (:scanIds)")
    suspend fun deleteByIds(scanIds: List<Long>)

    /**
     * Delete all scans from the database
     */
    @Query("DELETE FROM scans")
    suspend fun deleteAll()

    /**
     * Get a scan by its ID
     */
    @Query("SELECT * FROM scans WHERE id = :scanId")
    suspend fun getById(scanId: Long): ScanEntity?

    /**
     * Get a scan by its ID as Flow (reactive)
     */
    @Query("SELECT * FROM scans WHERE id = :scanId")
    fun getByIdFlow(scanId: Long): Flow<ScanEntity?>

    /**
     * Get all scans ordered by timestamp (newest first)
     */
    @Query("SELECT * FROM scans ORDER BY timestamp DESC")
    fun getAllScans(): Flow<List<ScanEntity>>

    /**
     * Get all scans with pagination
     */
    @Query("SELECT * FROM scans ORDER BY timestamp DESC LIMIT :limit OFFSET :offset")
    suspend fun getScansWithPagination(limit: Int, offset: Int): List<ScanEntity>

    /**
     * Search scans by text content
     * Searches in extracted_text, title, tags, and notes
     */
    @Query("""
        SELECT * FROM scans
        WHERE extracted_text LIKE '%' || :query || '%'
        OR title LIKE '%' || :query || '%'
        OR tags LIKE '%' || :query || '%'
        OR notes LIKE '%' || :query || '%'
        ORDER BY timestamp DESC
    """)
    fun searchScans(query: String): Flow<List<ScanEntity>>

    /**
     * Get scans filtered by language
     */
    @Query("SELECT * FROM scans WHERE language = :language ORDER BY timestamp DESC")
    fun getScansByLanguage(language: String): Flow<List<ScanEntity>>

    /**
     * Get favorite scans
     */
    @Query("SELECT * FROM scans WHERE is_favorite = 1 ORDER BY timestamp DESC")
    fun getFavoriteScans(): Flow<List<ScanEntity>>

    /**
     * Get scans within a date range
     */
    @Query("""
        SELECT * FROM scans
        WHERE timestamp BETWEEN :startDate AND :endDate
        ORDER BY timestamp DESC
    """)
    fun getScansByDateRange(startDate: Long, endDate: Long): Flow<List<ScanEntity>>

    /**
     * Get scans containing a specific tag
     */
    @Query("SELECT * FROM scans WHERE tags LIKE '%' || :tag || '%' ORDER BY timestamp DESC")
    fun getScansByTag(tag: String): Flow<List<ScanEntity>>

    /**
     * Get the total count of scans
     */
    @Query("SELECT COUNT(*) FROM scans")
    suspend fun getScansCount(): Int

    /**
     * Get the count of scans as Flow (reactive)
     */
    @Query("SELECT COUNT(*) FROM scans")
    fun getScansCountFlow(): Flow<Int>

    /**
     * Update favorite status
     */
    @Query("UPDATE scans SET is_favorite = :isFavorite WHERE id = :scanId")
    suspend fun updateFavoriteStatus(scanId: Long, isFavorite: Boolean)

    /**
     * Update only the extracted text (useful for editing)
     */
    @Query("UPDATE scans SET extracted_text = :text, modified_timestamp = :modifiedTime WHERE id = :scanId")
    suspend fun updateExtractedText(scanId: Long, text: String, modifiedTime: Long)

    /**
     * Update title and notes
     */
    @Query("UPDATE scans SET title = :title, notes = :notes, modified_timestamp = :modifiedTime WHERE id = :scanId")
    suspend fun updateTitleAndNotes(scanId: Long, title: String, notes: String, modifiedTime: Long)

    /**
     * Delete old scans (older than specified days)
     */
    @Query("DELETE FROM scans WHERE timestamp < :cutoffDate")
    suspend fun deleteOldScans(cutoffDate: Long): Int

    /**
     * Get the most recent scan
     */
    @Query("SELECT * FROM scans ORDER BY timestamp DESC LIMIT 1")
    suspend fun getMostRecentScan(): ScanEntity?

    /**
     * Get scans with confidence score above threshold
     */
    @Query("SELECT * FROM scans WHERE confidence_score >= :threshold ORDER BY timestamp DESC")
    fun getHighConfidenceScans(threshold: Float): Flow<List<ScanEntity>>
}
