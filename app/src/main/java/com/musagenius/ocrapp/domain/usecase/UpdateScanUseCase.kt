package com.musagenius.ocrapp.domain.usecase

import com.musagenius.ocrapp.domain.model.Result
import com.musagenius.ocrapp.domain.model.ScanResult
import com.musagenius.ocrapp.domain.repository.ScanRepository
import javax.inject.Inject

/**
 * Use case for updating a scan
 */
class UpdateScanUseCase @Inject constructor(
    private val scanRepository: ScanRepository
) {
    /**
     * Update a scan
     * @param scan The scan to update
     * @return Result indicating success or failure
     */
    suspend operator fun invoke(scan: ScanResult): Result<Unit> {
        return scanRepository.updateScan(scan)
    }
}
