package com.musagenius.ocrapp.domain.model

/**
 * User preferences for app settings
 */
data class UserPreferences(
    val theme: AppTheme = AppTheme.SYSTEM,
    val useDynamicColor: Boolean = true,
    val defaultCamera: DefaultCamera = DefaultCamera.BACK,
    val imageQuality: ImageQuality = ImageQuality.HIGH,
    val autoFocus: Boolean = true,
    val autoSaveToHistory: Boolean = true,
    val autoDeleteOldScans: Boolean = false,
    val autoDeleteDays: Int = 90
)
