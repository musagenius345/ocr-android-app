package com.musagenius.ocrapp.domain.usecase

import com.musagenius.ocrapp.domain.model.Result
import com.musagenius.ocrapp.domain.repository.ScanRepository
import javax.inject.Inject

/**
 * Use case for updating the extracted text of a scan
 */
class UpdateExtractedTextUseCase @Inject constructor(
    private val scanRepository: ScanRepository
) {
    /**
     * Update extracted text
     * @param scanId The ID of the scan
     * @param text The new extracted text
     * @return Result indicating success or failure
     */
    suspend operator fun invoke(scanId: Long, text: String): Result<Unit> {
        return scanRepository.updateExtractedText(scanId, text)
    }
}
