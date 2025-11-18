package com.musagenius.ocrapp.domain.usecase

import com.musagenius.ocrapp.domain.model.Result
import com.musagenius.ocrapp.domain.repository.ScanRepository
import javax.inject.Inject

/**
 * Use case for deleting a scan from the repository
 */
class DeleteScanUseCase @Inject constructor(
    private val scanRepository: ScanRepository
) {
    /**
     * Delete a scan by its ID
     * @param scanId The ID of the scan to delete
     * @return Result indicating success or failure
     */
    suspend operator fun invoke(scanId: Long): Result<Unit> {
        return scanRepository.deleteScan(scanId)
    }
}
