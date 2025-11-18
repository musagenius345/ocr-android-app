package com.musagenius.ocrapp.domain.usecase

import com.musagenius.ocrapp.domain.model.Result
import com.musagenius.ocrapp.domain.model.ScanResult
import com.musagenius.ocrapp.domain.repository.ScanRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Use case for searching scans by text content
 */
class SearchScansUseCase @Inject constructor(
    private val scanRepository: ScanRepository
) {
    /**
     * Search scans by query text
     * @param query The search query
     * @return Flow of Result containing list of matching scans
     */
    operator fun invoke(query: String): Flow<Result<List<ScanResult>>> {
        return scanRepository.searchScans(query)
    }
}
