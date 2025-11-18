package com.musagenius.ocrapp.domain.usecase

import com.musagenius.ocrapp.domain.model.Result
import com.musagenius.ocrapp.domain.repository.ScanRepository
import javax.inject.Inject

/**
 * Use case for updating the favorite status of a scan
 */
class UpdateFavoriteStatusUseCase @Inject constructor(
    private val scanRepository: ScanRepository
) {
    /**
     * Update favorite status
     * @param scanId The ID of the scan
     * @param isFavorite The new favorite status
     * @return Result indicating success or failure
     */
    suspend operator fun invoke(scanId: Long, isFavorite: Boolean): Result<Unit> {
        return scanRepository.updateFavoriteStatus(scanId, isFavorite)
    }
}
