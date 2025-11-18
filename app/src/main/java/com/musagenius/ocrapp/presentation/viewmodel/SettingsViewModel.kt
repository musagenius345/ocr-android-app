package com.musagenius.ocrapp.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.musagenius.ocrapp.domain.model.AppTheme
import com.musagenius.ocrapp.domain.model.DefaultCamera
import com.musagenius.ocrapp.domain.model.ImageQuality
import com.musagenius.ocrapp.domain.model.UserPreferences
import com.musagenius.ocrapp.domain.repository.PreferencesRepository
import com.musagenius.ocrapp.domain.usecase.GetUserPreferencesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for Settings screen
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val getUserPreferencesUseCase: GetUserPreferencesUseCase,
    private val preferencesRepository: PreferencesRepository
) : ViewModel() {

    val preferences: StateFlow<UserPreferences> = getUserPreferencesUseCase()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = UserPreferences()
        )

    /**
     * Update theme
     */
    fun updateTheme(theme: AppTheme) {
        viewModelScope.launch {
            preferencesRepository.updateTheme(theme)
        }
    }

    /**
     * Update dynamic color preference
     */
    fun updateDynamicColor(useDynamicColor: Boolean) {
        viewModelScope.launch {
            preferencesRepository.updateDynamicColor(useDynamicColor)
        }
    }

    /**
     * Update default camera
     */
    fun updateDefaultCamera(camera: DefaultCamera) {
        viewModelScope.launch {
            preferencesRepository.updateDefaultCamera(camera)
        }
    }

    /**
     * Update image quality
     */
    fun updateImageQuality(quality: ImageQuality) {
        viewModelScope.launch {
            preferencesRepository.updateImageQuality(quality)
        }
    }

    /**
     * Update auto-focus preference
     */
    fun updateAutoFocus(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.updateAutoFocus(enabled)
        }
    }

    /**
     * Update auto-save to history preference
     */
    fun updateAutoSaveToHistory(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.updateAutoSaveToHistory(enabled)
        }
    }

    /**
     * Update auto-delete old scans preference
     */
    fun updateAutoDeleteOldScans(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.updateAutoDeleteOldScans(enabled)
        }
    }

    /**
     * Update auto-delete days
     */
    fun updateAutoDeleteDays(days: Int) {
        viewModelScope.launch {
            preferencesRepository.updateAutoDeleteDays(days)
        }
    }
}
