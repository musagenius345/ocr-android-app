package com.musagenius.ocrapp.domain.usecase

import com.musagenius.ocrapp.domain.model.Result
import com.musagenius.ocrapp.domain.model.ScanResult
import com.musagenius.ocrapp.domain.repository.ScanRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Use case for retrieving a specific scan by ID
 */
class GetScanByIdUseCase @Inject constructor(
    private val scanRepository: ScanRepository
) {
    /**
     * Get a scan by its ID as a Flow (reactive)
     * @param scanId The ID of the scan to retrieve
     * @return Flow of Result containing the scan
     */
    operator fun invoke(scanId: Long): Flow<Result<ScanResult>> {
        return scanRepository.getScanByIdFlow(scanId)
    }
}
