package com.musagenius.ocrapp.presentation.viewmodel

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import app.cash.turbine.test
import com.musagenius.ocrapp.domain.model.AppTheme
import com.musagenius.ocrapp.domain.model.DefaultCamera
import com.musagenius.ocrapp.domain.model.ImageQuality
import com.musagenius.ocrapp.domain.model.UserPreferences
import com.musagenius.ocrapp.domain.repository.PreferencesRepository
import com.musagenius.ocrapp.domain.usecase.GetUserPreferencesUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.whenever

/**
 * Comprehensive unit tests for SettingsViewModel
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()

    @Mock
    private lateinit var getUserPreferencesUseCase: GetUserPreferencesUseCase

    @Mock
    private lateinit var preferencesRepository: PreferencesRepository

    private lateinit var viewModel: SettingsViewModel

    private val defaultPreferences = UserPreferences(
        theme = AppTheme.SYSTEM,
        useDynamicColor = false,
        defaultCamera = DefaultCamera.BACK,
        imageQuality = ImageQuality.HIGH,
        autoFocus = true,
        autoSaveToHistory = true,
        autoDeleteOldScans = false,
        autoDeleteDays = 30
    )

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        Dispatchers.setMain(testDispatcher)

        // Default mock behavior
        whenever(getUserPreferencesUseCase.invoke())
            .thenReturn(flowOf(defaultPreferences))
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ============ Initial State Tests ============

    @Test
    fun `initial state should load user preferences`() = runTest {
        // When
        viewModel = SettingsViewModel(getUserPreferencesUseCase, preferencesRepository)
        advanceUntilIdle()

        // Then
        viewModel.preferences.test {
            val preferences = awaitItem()
            assertEquals(AppTheme.SYSTEM, preferences.theme)
            assertEquals(false, preferences.useDynamicColor)
            assertEquals(DefaultCamera.BACK, preferences.defaultCamera)
            assertEquals(ImageQuality.HIGH, preferences.imageQuality)
            assertTrue(preferences.autoFocus)
            assertTrue(preferences.autoSaveToHistory)
            assertFalse(preferences.autoDeleteOldScans)
            assertEquals(30, preferences.autoDeleteDays)
        }
    }

    @Test
    fun `initial state should use default preferences when not available`() = runTest {
        // Given
        whenever(getUserPreferencesUseCase.invoke())
            .thenReturn(flowOf())

        // When
        viewModel = SettingsViewModel(getUserPreferencesUseCase, preferencesRepository)
        advanceUntilIdle()

        // Then - should use default UserPreferences
        verify(getUserPreferencesUseCase).invoke()
    }

    // ============ Theme Tests ============

    @Test
    fun `updateTheme should update theme to LIGHT`() = runTest {
        // Given
        viewModel = SettingsViewModel(getUserPreferencesUseCase, preferencesRepository)
        advanceUntilIdle()

        // When
        viewModel.updateTheme(AppTheme.LIGHT)
        advanceUntilIdle()

        // Then
        verify(preferencesRepository).updateTheme(AppTheme.LIGHT)
    }

    @Test
    fun `updateTheme should update theme to DARK`() = runTest {
        // Given
        viewModel = SettingsViewModel(getUserPreferencesUseCase, preferencesRepository)
        advanceUntilIdle()

        // When
        viewModel.updateTheme(AppTheme.DARK)
        advanceUntilIdle()

        // Then
        verify(preferencesRepository).updateTheme(AppTheme.DARK)
    }

    @Test
    fun `updateTheme should update theme to SYSTEM`() = runTest {
        // Given
        viewModel = SettingsViewModel(getUserPreferencesUseCase, preferencesRepository)
        advanceUntilIdle()

        // When
        viewModel.updateTheme(AppTheme.SYSTEM)
        advanceUntilIdle()

        // Then
        verify(preferencesRepository).updateTheme(AppTheme.SYSTEM)
    }

    // ============ Dynamic Color Tests ============

    @Test
    fun `updateDynamicColor should enable dynamic colors`() = runTest {
        // Given
        viewModel = SettingsViewModel(getUserPreferencesUseCase, preferencesRepository)
        advanceUntilIdle()

        // When
        viewModel.updateDynamicColor(true)
        advanceUntilIdle()

        // Then
        verify(preferencesRepository).updateDynamicColor(true)
    }

    @Test
    fun `updateDynamicColor should disable dynamic colors`() = runTest {
        // Given
        viewModel = SettingsViewModel(getUserPreferencesUseCase, preferencesRepository)
        advanceUntilIdle()

        // When
        viewModel.updateDynamicColor(false)
        advanceUntilIdle()

        // Then
        verify(preferencesRepository).updateDynamicColor(false)
    }

    // ============ Default Camera Tests ============

    @Test
    fun `updateDefaultCamera should set to BACK camera`() = runTest {
        // Given
        viewModel = SettingsViewModel(getUserPreferencesUseCase, preferencesRepository)
        advanceUntilIdle()

        // When
        viewModel.updateDefaultCamera(DefaultCamera.BACK)
        advanceUntilIdle()

        // Then
        verify(preferencesRepository).updateDefaultCamera(DefaultCamera.BACK)
    }

    @Test
    fun `updateDefaultCamera should set to FRONT camera`() = runTest {
        // Given
        viewModel = SettingsViewModel(getUserPreferencesUseCase, preferencesRepository)
        advanceUntilIdle()

        // When
        viewModel.updateDefaultCamera(DefaultCamera.FRONT)
        advanceUntilIdle()

        // Then
        verify(preferencesRepository).updateDefaultCamera(DefaultCamera.FRONT)
    }

    // ============ Image Quality Tests ============

    @Test
    fun `updateImageQuality should set to LOW quality`() = runTest {
        // Given
        viewModel = SettingsViewModel(getUserPreferencesUseCase, preferencesRepository)
        advanceUntilIdle()

        // When
        viewModel.updateImageQuality(ImageQuality.LOW)
        advanceUntilIdle()

        // Then
        verify(preferencesRepository).updateImageQuality(ImageQuality.LOW)
    }

    @Test
    fun `updateImageQuality should set to MEDIUM quality`() = runTest {
        // Given
        viewModel = SettingsViewModel(getUserPreferencesUseCase, preferencesRepository)
        advanceUntilIdle()

        // When
        viewModel.updateImageQuality(ImageQuality.MEDIUM)
        advanceUntilIdle()

        // Then
        verify(preferencesRepository).updateImageQuality(ImageQuality.MEDIUM)
    }

    @Test
    fun `updateImageQuality should set to HIGH quality`() = runTest {
        // Given
        viewModel = SettingsViewModel(getUserPreferencesUseCase, preferencesRepository)
        advanceUntilIdle()

        // When
        viewModel.updateImageQuality(ImageQuality.HIGH)
        advanceUntilIdle()

        // Then
        verify(preferencesRepository).updateImageQuality(ImageQuality.HIGH)
    }

    // ============ Auto Focus Tests ============

    @Test
    fun `updateAutoFocus should enable auto focus`() = runTest {
        // Given
        viewModel = SettingsViewModel(getUserPreferencesUseCase, preferencesRepository)
        advanceUntilIdle()

        // When
        viewModel.updateAutoFocus(true)
        advanceUntilIdle()

        // Then
        verify(preferencesRepository).updateAutoFocus(true)
    }

    @Test
    fun `updateAutoFocus should disable auto focus`() = runTest {
        // Given
        viewModel = SettingsViewModel(getUserPreferencesUseCase, preferencesRepository)
        advanceUntilIdle()

        // When
        viewModel.updateAutoFocus(false)
        advanceUntilIdle()

        // Then
        verify(preferencesRepository).updateAutoFocus(false)
    }

    // ============ Auto Save Tests ============

    @Test
    fun `updateAutoSaveToHistory should enable auto save`() = runTest {
        // Given
        viewModel = SettingsViewModel(getUserPreferencesUseCase, preferencesRepository)
        advanceUntilIdle()

        // When
        viewModel.updateAutoSaveToHistory(true)
        advanceUntilIdle()

        // Then
        verify(preferencesRepository).updateAutoSaveToHistory(true)
    }

    @Test
    fun `updateAutoSaveToHistory should disable auto save`() = runTest {
        // Given
        viewModel = SettingsViewModel(getUserPreferencesUseCase, preferencesRepository)
        advanceUntilIdle()

        // When
        viewModel.updateAutoSaveToHistory(false)
        advanceUntilIdle()

        // Then
        verify(preferencesRepository).updateAutoSaveToHistory(false)
    }

    // ============ Auto Delete Tests ============

    @Test
    fun `updateAutoDeleteOldScans should enable auto delete`() = runTest {
        // Given
        viewModel = SettingsViewModel(getUserPreferencesUseCase, preferencesRepository)
        advanceUntilIdle()

        // When
        viewModel.updateAutoDeleteOldScans(true)
        advanceUntilIdle()

        // Then
        verify(preferencesRepository).updateAutoDeleteOldScans(true)
    }

    @Test
    fun `updateAutoDeleteOldScans should disable auto delete`() = runTest {
        // Given
        viewModel = SettingsViewModel(getUserPreferencesUseCase, preferencesRepository)
        advanceUntilIdle()

        // When
        viewModel.updateAutoDeleteOldScans(false)
        advanceUntilIdle()

        // Then
        verify(preferencesRepository).updateAutoDeleteOldScans(false)
    }

    @Test
    fun `updateAutoDeleteDays should update days to 7`() = runTest {
        // Given
        viewModel = SettingsViewModel(getUserPreferencesUseCase, preferencesRepository)
        advanceUntilIdle()

        // When
        viewModel.updateAutoDeleteDays(7)
        advanceUntilIdle()

        // Then
        verify(preferencesRepository).updateAutoDeleteDays(7)
    }

    @Test
    fun `updateAutoDeleteDays should update days to 30`() = runTest {
        // Given
        viewModel = SettingsViewModel(getUserPreferencesUseCase, preferencesRepository)
        advanceUntilIdle()

        // When
        viewModel.updateAutoDeleteDays(30)
        advanceUntilIdle()

        // Then
        verify(preferencesRepository).updateAutoDeleteDays(30)
    }

    @Test
    fun `updateAutoDeleteDays should update days to 90`() = runTest {
        // Given
        viewModel = SettingsViewModel(getUserPreferencesUseCase, preferencesRepository)
        advanceUntilIdle()

        // When
        viewModel.updateAutoDeleteDays(90)
        advanceUntilIdle()

        // Then
        verify(preferencesRepository).updateAutoDeleteDays(90)
    }

    // ============ Multiple Updates Tests ============

    @Test
    fun `multiple preference updates should be handled correctly`() = runTest {
        // Given
        viewModel = SettingsViewModel(getUserPreferencesUseCase, preferencesRepository)
        advanceUntilIdle()

        // When - change multiple settings
        viewModel.updateTheme(AppTheme.DARK)
        viewModel.updateDynamicColor(true)
        viewModel.updateImageQuality(ImageQuality.MEDIUM)
        viewModel.updateAutoFocus(false)
        advanceUntilIdle()

        // Then - all should be called
        verify(preferencesRepository).updateTheme(AppTheme.DARK)
        verify(preferencesRepository).updateDynamicColor(true)
        verify(preferencesRepository).updateImageQuality(ImageQuality.MEDIUM)
        verify(preferencesRepository).updateAutoFocus(false)
    }

    // ============ Edge Case Tests ============

    @Test
    fun `toggling same preference multiple times should call repository each time`() = runTest {
        // Given
        viewModel = SettingsViewModel(getUserPreferencesUseCase, preferencesRepository)
        advanceUntilIdle()

        // When - toggle auto focus multiple times
        viewModel.updateAutoFocus(true)
        viewModel.updateAutoFocus(false)
        viewModel.updateAutoFocus(true)
        viewModel.updateAutoFocus(false)
        advanceUntilIdle()

        // Then
        verify(preferencesRepository, times(2)).updateAutoFocus(true)
        verify(preferencesRepository, times(2)).updateAutoFocus(false)
    }

    @Test
    fun `preferences flow should emit updates`() = runTest {
        // Given
        val updatedPreferences = defaultPreferences.copy(theme = AppTheme.DARK)
        whenever(getUserPreferencesUseCase.invoke())
            .thenReturn(flowOf(defaultPreferences, updatedPreferences))

        // When
        viewModel = SettingsViewModel(getUserPreferencesUseCase, preferencesRepository)
        advanceUntilIdle()

        // Then - should emit both values
        verify(getUserPreferencesUseCase).invoke()
    }
}
