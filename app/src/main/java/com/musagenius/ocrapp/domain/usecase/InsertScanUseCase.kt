package com.musagenius.ocrapp.domain.usecase

import com.musagenius.ocrapp.domain.model.Result
import com.musagenius.ocrapp.domain.model.ScanResult
import com.musagenius.ocrapp.domain.repository.ScanRepository
import javax.inject.Inject

/**
 * Use case for inserting a scan into the repository
 */
class InsertScanUseCase @Inject constructor(
    private val scanRepository: ScanRepository
) {
    /**
     * Insert a scan
     * @param scan The scan to insert
     * @return Result containing the ID of the inserted scan
     */
    suspend operator fun invoke(scan: ScanResult): Result<Long> {
        return scanRepository.insertScan(scan)
    }
}
