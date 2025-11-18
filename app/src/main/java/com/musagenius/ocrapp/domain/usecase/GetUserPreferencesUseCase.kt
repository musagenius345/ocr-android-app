package com.musagenius.ocrapp.domain.usecase

import com.musagenius.ocrapp.domain.model.UserPreferences
import com.musagenius.ocrapp.domain.repository.PreferencesRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Use case for retrieving user preferences
 */
class GetUserPreferencesUseCase @Inject constructor(
    private val preferencesRepository: PreferencesRepository
) {
    /**
     * Get user preferences as a Flow
     */
    operator fun invoke(): Flow<UserPreferences> {
        return preferencesRepository.getUserPreferences()
    }
}
