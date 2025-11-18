package com.musagenius.ocrapp.domain.usecase

import com.musagenius.ocrapp.domain.repository.LanguageRepository
import javax.inject.Inject

/**
 * Use case for getting total storage used by language files
 */
class GetLanguageStorageUseCase @Inject constructor(
    private val languageRepository: LanguageRepository
) {
    /**
     * Get total storage used by language files in bytes
     */
    suspend operator fun invoke(): Long {
        return languageRepository.getTotalStorageUsed()
    }
}
