package com.musagenius.ocrapp.domain.usecase

import com.musagenius.ocrapp.domain.model.Result
import com.musagenius.ocrapp.domain.model.ScanResult
import com.musagenius.ocrapp.domain.repository.ScanRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Use case for retrieving scans filtered by language
 */
class GetScansByLanguageUseCase @Inject constructor(
    private val scanRepository: ScanRepository
) {
    /**
     * Get scans by language
     * @param language The language code to filter by
     * @return Flow of Result containing list of scans
     */
    operator fun invoke(language: String): Flow<Result<List<ScanResult>>> {
        return scanRepository.getScansByLanguage(language)
    }
}
