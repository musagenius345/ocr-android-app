package com.musagenius.ocrapp.domain.usecase

import com.musagenius.ocrapp.domain.model.DateRange
import com.musagenius.ocrapp.domain.model.Result
import com.musagenius.ocrapp.domain.model.ScanResult
import com.musagenius.ocrapp.domain.repository.ScanRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Use case for retrieving scans within a date range
 */
class GetScansByDateRangeUseCase @Inject constructor(
    private val scanRepository: ScanRepository
) {
    /**
     * Get scans within a date range
     * @param dateRange The date range to filter by
     * @return Flow of Result containing list of scans
     */
    operator fun invoke(dateRange: DateRange): Flow<Result<List<ScanResult>>> {
        return scanRepository.getScansByDateRange(dateRange)
    }
}
