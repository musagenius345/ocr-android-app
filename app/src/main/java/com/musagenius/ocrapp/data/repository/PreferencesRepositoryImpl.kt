package com.musagenius.ocrapp.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.musagenius.ocrapp.domain.model.AppTheme
import com.musagenius.ocrapp.domain.model.DefaultCamera
import com.musagenius.ocrapp.domain.model.ImageQuality
import com.musagenius.ocrapp.domain.model.UserPreferences
import com.musagenius.ocrapp.domain.repository.PreferencesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

/**
 * Implementation of PreferencesRepository using DataStore
 */
@Singleton
class PreferencesRepositoryImpl @Inject constructor(
    private val context: Context
) : PreferencesRepository {

    private object PreferencesKeys {
        val THEME = stringPreferencesKey("theme")
        val USE_DYNAMIC_COLOR = booleanPreferencesKey("use_dynamic_color")
        val DEFAULT_CAMERA = stringPreferencesKey("default_camera")
        val IMAGE_QUALITY = stringPreferencesKey("image_quality")
        val AUTO_FOCUS = booleanPreferencesKey("auto_focus")
        val AUTO_SAVE_TO_HISTORY = booleanPreferencesKey("auto_save_to_history")
        val AUTO_DELETE_OLD_SCANS = booleanPreferencesKey("auto_delete_old_scans")
        val AUTO_DELETE_DAYS = intPreferencesKey("auto_delete_days")
    }

    override fun getUserPreferences(): Flow<UserPreferences> {
        return context.dataStore.data.map { preferences ->
            UserPreferences(
                theme = preferences[PreferencesKeys.THEME]?.let {
                    AppTheme.valueOf(it)
                } ?: AppTheme.SYSTEM,
                useDynamicColor = preferences[PreferencesKeys.USE_DYNAMIC_COLOR] ?: true,
                defaultCamera = preferences[PreferencesKeys.DEFAULT_CAMERA]?.let {
                    DefaultCamera.valueOf(it)
                } ?: DefaultCamera.BACK,
                imageQuality = preferences[PreferencesKeys.IMAGE_QUALITY]?.let {
                    ImageQuality.valueOf(it)
                } ?: ImageQuality.HIGH,
                autoFocus = preferences[PreferencesKeys.AUTO_FOCUS] ?: true,
                autoSaveToHistory = preferences[PreferencesKeys.AUTO_SAVE_TO_HISTORY] ?: true,
                autoDeleteOldScans = preferences[PreferencesKeys.AUTO_DELETE_OLD_SCANS] ?: false,
                autoDeleteDays = preferences[PreferencesKeys.AUTO_DELETE_DAYS] ?: 90
            )
        }
    }

    override suspend fun updateTheme(theme: AppTheme) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.THEME] = theme.name
        }
    }

    override suspend fun updateDynamicColor(useDynamicColor: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.USE_DYNAMIC_COLOR] = useDynamicColor
        }
    }

    override suspend fun updateDefaultCamera(camera: DefaultCamera) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.DEFAULT_CAMERA] = camera.name
        }
    }

    override suspend fun updateImageQuality(quality: ImageQuality) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.IMAGE_QUALITY] = quality.name
        }
    }

    override suspend fun updateAutoFocus(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.AUTO_FOCUS] = enabled
        }
    }

    override suspend fun updateAutoSaveToHistory(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.AUTO_SAVE_TO_HISTORY] = enabled
        }
    }

    override suspend fun updateAutoDeleteOldScans(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.AUTO_DELETE_OLD_SCANS] = enabled
        }
    }

    override suspend fun updateAutoDeleteDays(days: Int) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.AUTO_DELETE_DAYS] = days
        }
    }
}
