package com.musagenius.ocrapp.domain.usecase

import com.musagenius.ocrapp.domain.model.Result
import com.musagenius.ocrapp.domain.repository.LanguageRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Use case for downloading a Tesseract language file
 */
class DownloadLanguageUseCase @Inject constructor(
    private val languageRepository: LanguageRepository
) {
    /**
     * Download a language file with progress tracking
     * @param languageCode The language code to download
     * @return Flow of download progress (0.0 to 1.0)
     */
    operator fun invoke(languageCode: String): Flow<Result<Float>> {
        return languageRepository.downloadLanguage(languageCode)
    }
}
