package com.musagenius.ocrapp.domain.usecase

import com.musagenius.ocrapp.domain.model.Result
import com.musagenius.ocrapp.domain.repository.ScanRepository
import javax.inject.Inject

/**
 * Use case for updating scan title and notes
 */
class UpdateTitleAndNotesUseCase @Inject constructor(
    private val scanRepository: ScanRepository
) {
    /**
     * Update title and notes
     * @param scanId The ID of the scan
     * @param title The new title
     * @param notes The new notes
     * @return Result indicating success or failure
     */
    suspend operator fun invoke(scanId: Long, title: String, notes: String): Result<Unit> {
        return scanRepository.updateTitleAndNotes(scanId, title, notes)
    }
}
