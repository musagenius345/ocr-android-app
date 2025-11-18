package com.musagenius.ocrapp.domain.repository

import com.musagenius.ocrapp.domain.model.AppTheme
import com.musagenius.ocrapp.domain.model.DefaultCamera
import com.musagenius.ocrapp.domain.model.ImageQuality
import com.musagenius.ocrapp.domain.model.UserPreferences
import kotlinx.coroutines.flow.Flow

/**
 * Repository for managing user preferences
 */
interface PreferencesRepository {
    /**
     * Get user preferences as a Flow
     */
    fun getUserPreferences(): Flow<UserPreferences>

    /**
     * Update theme
     */
    suspend fun updateTheme(theme: AppTheme)

    /**
     * Update dynamic color preference
     */
    suspend fun updateDynamicColor(useDynamicColor: Boolean)

    /**
     * Update default camera
     */
    suspend fun updateDefaultCamera(camera: DefaultCamera)

    /**
     * Update image quality
     */
    suspend fun updateImageQuality(quality: ImageQuality)

    /**
     * Update auto-focus preference
     */
    suspend fun updateAutoFocus(enabled: Boolean)

    /**
     * Update auto-save to history preference
     */
    suspend fun updateAutoSaveToHistory(enabled: Boolean)

    /**
     * Update auto-delete old scans preference
     */
    suspend fun updateAutoDeleteOldScans(enabled: Boolean)

    /**
     * Update auto-delete days
     */
    suspend fun updateAutoDeleteDays(days: Int)
}
