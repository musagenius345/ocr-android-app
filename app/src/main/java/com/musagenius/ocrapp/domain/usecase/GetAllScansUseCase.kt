package com.musagenius.ocrapp.domain.usecase

import com.musagenius.ocrapp.domain.model.Result
import com.musagenius.ocrapp.domain.model.ScanResult
import com.musagenius.ocrapp.domain.repository.ScanRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Use case for retrieving all scans from the repository
 */
class GetAllScansUseCase @Inject constructor(
    private val scanRepository: ScanRepository
) {
    /**
     * Get all scans as a Flow
     * @return Flow of Result containing list of scans
     */
    operator fun invoke(): Flow<Result<List<ScanResult>>> {
        return scanRepository.getAllScans()
    }
}
