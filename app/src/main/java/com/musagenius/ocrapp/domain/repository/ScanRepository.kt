package com.musagenius.ocrapp.domain.repository

import com.musagenius.ocrapp.domain.model.DateRange
import com.musagenius.ocrapp.domain.model.Result
import com.musagenius.ocrapp.domain.model.ScanResult
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for scan operations
 * This is part of the domain layer and is independent of implementation details
 */
interface ScanRepository {

    /**
     * Insert a new scan into the repository
     * @return Result containing the ID of the inserted scan
     */
    suspend fun insertScan(scan: ScanResult): Result<Long>

    /**
     * Update an existing scan
     */
    suspend fun updateScan(scan: ScanResult): Result<Unit>

    /**
     * Delete a scan by its ID
     */
    suspend fun deleteScan(scanId: Long): Result<Unit>

    /**
     * Delete multiple scans by their IDs
     */
    suspend fun deleteScans(scanIds: List<Long>): Result<Unit>

    /**
     * Delete all scans
     */
    suspend fun deleteAllScans(): Result<Unit>

    /**
     * Get a scan by its ID
     */
    suspend fun getScanById(scanId: Long): Result<ScanResult>

    /**
     * Get a scan by its ID as Flow (reactive)
     */
    fun getScanByIdFlow(scanId: Long): Flow<Result<ScanResult>>

    /**
     * Get all scans
     */
    fun getAllScans(): Flow<Result<List<ScanResult>>>

    /**
     * Search scans by text content
     */
    fun searchScans(query: String): Flow<Result<List<ScanResult>>>

    /**
     * Get scans filtered by language
     */
    fun getScansByLanguage(language: String): Flow<Result<List<ScanResult>>>

    /**
     * Get favorite scans
     */
    fun getFavoriteScans(): Flow<Result<List<ScanResult>>>

    /**
     * Get scans within a date range
     */
    fun getScansByDateRange(dateRange: DateRange): Flow<Result<List<ScanResult>>>

    /**
     * Get scans containing a specific tag
     */
    fun getScansByTag(tag: String): Flow<Result<List<ScanResult>>>

    /**
     * Get the total count of scans
     */
    suspend fun getScansCount(): Result<Int>

    /**
     * Get the count of scans as Flow (reactive)
     */
    fun getScansCountFlow(): Flow<Result<Int>>

    /**
     * Update favorite status
     */
    suspend fun updateFavoriteStatus(scanId: Long, isFavorite: Boolean): Result<Unit>

    /**
     * Update only the extracted text
     */
    suspend fun updateExtractedText(scanId: Long, text: String): Result<Unit>

    /**
     * Update title and notes
     */
    suspend fun updateTitleAndNotes(scanId: Long, title: String, notes: String): Result<Unit>

    /**
     * Delete old scans (older than specified days)
     * @return Number of deleted scans
     */
    suspend fun deleteOldScans(daysOld: Int): Result<Int>

    /**
     * Get the most recent scan
     */
    suspend fun getMostRecentScan(): Result<ScanResult>

    /**
     * Get scans with confidence score above threshold
     */
    fun getHighConfidenceScans(threshold: Float = 0.8f): Flow<Result<List<ScanResult>>>

    /**
     * Get scans with pagination
     */
    suspend fun getScansWithPagination(limit: Int, offset: Int): Result<List<ScanResult>>
}
