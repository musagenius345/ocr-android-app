package com.musagenius.ocrapp.data.repository

import com.musagenius.ocrapp.data.local.dao.ScanDao
import com.musagenius.ocrapp.data.mapper.toDomain
import com.musagenius.ocrapp.data.mapper.toEntity
import com.musagenius.ocrapp.domain.model.DateRange
import com.musagenius.ocrapp.domain.model.Result
import com.musagenius.ocrapp.domain.model.ScanResult
import com.musagenius.ocrapp.domain.repository.ScanRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.util.Calendar
import java.util.Date
import javax.inject.Inject

/**
 * Implementation of ScanRepository
 * Handles data operations and error handling
 */
class ScanRepositoryImpl @Inject constructor(
    private val scanDao: ScanDao
) : ScanRepository {

    override suspend fun insertScan(scan: ScanResult): Result<Long> {
        return try {
            val id = scanDao.insert(scan.toEntity())
            Result.success(id)
        } catch (e: Exception) {
            Result.error(e, "Failed to insert scan")
        }
    }

    override suspend fun updateScan(scan: ScanResult): Result<Unit> {
        return try {
            scanDao.update(scan.toEntity())
            Result.success(Unit)
        } catch (e: Exception) {
            Result.error(e, "Failed to update scan")
        }
    }

    override suspend fun deleteScan(scanId: Long): Result<Unit> {
        return try {
            scanDao.deleteById(scanId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.error(e, "Failed to delete scan")
        }
    }

    override suspend fun deleteScans(scanIds: List<Long>): Result<Unit> {
        return try {
            scanDao.deleteByIds(scanIds)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.error(e, "Failed to delete scans")
        }
    }

    override suspend fun deleteAllScans(): Result<Unit> {
        return try {
            scanDao.deleteAll()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.error(e, "Failed to delete all scans")
        }
    }

    override suspend fun getScanById(scanId: Long): Result<ScanResult> {
        return try {
            val scan = scanDao.getById(scanId)
            if (scan != null) {
                Result.success(scan.toDomain())
            } else {
                Result.error(Exception("Scan not found"), "Scan with ID $scanId not found")
            }
        } catch (e: Exception) {
            Result.error(e, "Failed to get scan")
        }
    }

    override fun getScanByIdFlow(scanId: Long): Flow<Result<ScanResult>> {
        return scanDao.getByIdFlow(scanId)
            .map { entity ->
                if (entity != null) {
                    Result.success(entity.toDomain())
                } else {
                    Result.error(Exception("Scan not found"), "Scan with ID $scanId not found")
                }
            }
            .catch { e ->
                emit(Result.error(Exception(e), "Failed to get scan"))
            }
    }

    override fun getAllScans(): Flow<Result<List<ScanResult>>> {
        return scanDao.getAllScans()
            .map { entities ->
                Result.success(entities.toDomain())
            }
            .catch { e ->
                emit(Result.error(Exception(e), "Failed to get all scans"))
            }
    }

    override fun searchScans(query: String): Flow<Result<List<ScanResult>>> {
        return scanDao.searchScans(query)
            .map { entities ->
                Result.success(entities.toDomain())
            }
            .catch { e ->
                emit(Result.error(Exception(e), "Failed to search scans"))
            }
    }

    override fun getScansByLanguage(language: String): Flow<Result<List<ScanResult>>> {
        return scanDao.getScansByLanguage(language)
            .map { entities ->
                Result.success(entities.toDomain())
            }
            .catch { e ->
                emit(Result.error(Exception(e), "Failed to get scans by language"))
            }
    }

    override fun getFavoriteScans(): Flow<Result<List<ScanResult>>> {
        return scanDao.getFavoriteScans()
            .map { entities ->
                Result.success(entities.toDomain())
            }
            .catch { e ->
                emit(Result.error(Exception(e), "Failed to get favorite scans"))
            }
    }

    override fun getScansByDateRange(dateRange: DateRange): Flow<Result<List<ScanResult>>> {
        return scanDao.getScansByDateRange(dateRange.startDate, dateRange.endDate)
            .map { entities ->
                Result.success(entities.toDomain())
            }
            .catch { e ->
                emit(Result.error(Exception(e), "Failed to get scans by date range"))
            }
    }

    override fun getScansByTag(tag: String): Flow<Result<List<ScanResult>>> {
        return scanDao.getScansByTag(tag)
            .map { entities ->
                Result.success(entities.toDomain())
            }
            .catch { e ->
                emit(Result.error(Exception(e), "Failed to get scans by tag"))
            }
    }

    override suspend fun getScansCount(): Result<Int> {
        return try {
            val count = scanDao.getScansCount()
            Result.success(count)
        } catch (e: Exception) {
            Result.error(e, "Failed to get scans count")
        }
    }

    override fun getScansCountFlow(): Flow<Result<Int>> {
        return scanDao.getScansCountFlow()
            .map { count ->
                Result.success(count)
            }
            .catch { e ->
                emit(Result.error(Exception(e), "Failed to get scans count"))
            }
    }

    override suspend fun updateFavoriteStatus(scanId: Long, isFavorite: Boolean): Result<Unit> {
        return try {
            scanDao.updateFavoriteStatus(scanId, isFavorite)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.error(e, "Failed to update favorite status")
        }
    }

    override suspend fun updateExtractedText(scanId: Long, text: String): Result<Unit> {
        return try {
            val modifiedTime = Date().time
            scanDao.updateExtractedText(scanId, text, modifiedTime)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.error(e, "Failed to update extracted text")
        }
    }

    override suspend fun updateTitleAndNotes(scanId: Long, title: String, notes: String): Result<Unit> {
        return try {
            val modifiedTime = Date().time
            scanDao.updateTitleAndNotes(scanId, title, notes, modifiedTime)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.error(e, "Failed to update title and notes")
        }
    }

    override suspend fun deleteOldScans(daysOld: Int): Result<Int> {
        return try {
            val calendar = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, -daysOld)
            }
            val cutoffDate = calendar.timeInMillis
            val deletedCount = scanDao.deleteOldScans(cutoffDate)
            Result.success(deletedCount)
        } catch (e: Exception) {
            Result.error(e, "Failed to delete old scans")
        }
    }

    override suspend fun getMostRecentScan(): Result<ScanResult> {
        return try {
            val scan = scanDao.getMostRecentScan()
            if (scan != null) {
                Result.success(scan.toDomain())
            } else {
                Result.error(Exception("No scans found"), "No scans available")
            }
        } catch (e: Exception) {
            Result.error(e, "Failed to get most recent scan")
        }
    }

    override fun getHighConfidenceScans(threshold: Float): Flow<Result<List<ScanResult>>> {
        return scanDao.getHighConfidenceScans(threshold)
            .map { entities ->
                Result.success(entities.toDomain())
            }
            .catch { e ->
                emit(Result.error(Exception(e), "Failed to get high confidence scans"))
            }
    }

    override suspend fun getScansWithPagination(limit: Int, offset: Int): Result<List<ScanResult>> {
        return try {
            val scans = scanDao.getScansWithPagination(limit, offset)
            Result.success(scans.toDomain())
        } catch (e: Exception) {
            Result.error(e, "Failed to get scans with pagination")
        }
    }
}
