package com.musagenius.ocrapp.presentation.viewmodel

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
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
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations

/**
 * Unit tests for SettingsViewModel
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    @Mock
    private lateinit var getUserPreferencesUseCase: GetUserPreferencesUseCase

    @Mock
    private lateinit var preferencesRepository: PreferencesRepository

    private lateinit var viewModel: SettingsViewModel

    private val testDispatcher = StandardTestDispatcher()

    private val defaultPreferences = UserPreferences(
        theme = AppTheme.SYSTEM,
        useDynamicColor = true,
        defaultCamera = DefaultCamera.BACK,
        imageQuality = ImageQuality.HIGH,
        enableAutoFocus = true,
        autoSaveToHistory = true,
        autoDeleteOldScans = false,
        autoDeleteDays = 30
    )

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        Dispatchers.setMain(testDispatcher)

        // Setup default behavior
        `when`(getUserPreferencesUseCase.invoke()).thenReturn(flowOf(defaultPreferences))

        viewModel = SettingsViewModel(getUserPreferencesUseCase, preferencesRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `preferences flow emits initial value from use case`() = runTest {
        // When
        advanceUntilIdle()
        val preferences = viewModel.preferences.value

        // Then
        assertEquals(defaultPreferences.theme, preferences.theme)
        assertEquals(defaultPreferences.useDynamicColor, preferences.useDynamicColor)
        verify(getUserPreferencesUseCase).invoke()
    }

    @Test
    fun `updateTheme calls repository with correct theme`() = runTest {
        // Given
        val newTheme = AppTheme.DARK

        // When
        viewModel.updateTheme(newTheme)
        advanceUntilIdle()

        // Then
        verify(preferencesRepository).updateTheme(newTheme)
    }

    @Test
    fun `updateTheme with light theme calls repository`() = runTest {
        // Given
        val newTheme = AppTheme.LIGHT

        // When
        viewModel.updateTheme(newTheme)
        advanceUntilIdle()

        // Then
        verify(preferencesRepository).updateTheme(AppTheme.LIGHT)
    }

    @Test
    fun `updateDynamicColor calls repository with correct value`() = runTest {
        // Given
        val useDynamicColor = false

        // When
        viewModel.updateDynamicColor(useDynamicColor)
        advanceUntilIdle()

        // Then
        verify(preferencesRepository).updateDynamicColor(false)
    }

    @Test
    fun `updateDefaultCamera calls repository with front camera`() = runTest {
        // Given
        val camera = DefaultCamera.FRONT

        // When
        viewModel.updateDefaultCamera(camera)
        advanceUntilIdle()

        // Then
        verify(preferencesRepository).updateDefaultCamera(DefaultCamera.FRONT)
    }

    @Test
    fun `updateDefaultCamera calls repository with back camera`() = runTest {
        // Given
        val camera = DefaultCamera.BACK

        // When
        viewModel.updateDefaultCamera(camera)
        advanceUntilIdle()

        // Then
        verify(preferencesRepository).updateDefaultCamera(DefaultCamera.BACK)
    }

    @Test
    fun `updateImageQuality calls repository with high quality`() = runTest {
        // Given
        val quality = ImageQuality.HIGH

        // When
        viewModel.updateImageQuality(quality)
        advanceUntilIdle()

        // Then
        verify(preferencesRepository).updateImageQuality(ImageQuality.HIGH)
    }

    @Test
    fun `updateImageQuality calls repository with medium quality`() = runTest {
        // Given
        val quality = ImageQuality.MEDIUM

        // When
        viewModel.updateImageQuality(quality)
        advanceUntilIdle()

        // Then
        verify(preferencesRepository).updateImageQuality(ImageQuality.MEDIUM)
    }

    @Test
    fun `updateImageQuality calls repository with low quality`() = runTest {
        // Given
        val quality = ImageQuality.LOW

        // When
        viewModel.updateImageQuality(quality)
        advanceUntilIdle()

        // Then
        verify(preferencesRepository).updateImageQuality(ImageQuality.LOW)
    }

    @Test
    fun `updateAutoFocus calls repository with enabled`() = runTest {
        // Given
        val enabled = true

        // When
        viewModel.updateAutoFocus(enabled)
        advanceUntilIdle()

        // Then
        verify(preferencesRepository).updateAutoFocus(true)
    }

    @Test
    fun `updateAutoFocus calls repository with disabled`() = runTest {
        // Given
        val enabled = false

        // When
        viewModel.updateAutoFocus(enabled)
        advanceUntilIdle()

        // Then
        verify(preferencesRepository).updateAutoFocus(false)
    }

    @Test
    fun `updateAutoSaveToHistory calls repository with enabled`() = runTest {
        // Given
        val enabled = true

        // When
        viewModel.updateAutoSaveToHistory(enabled)
        advanceUntilIdle()

        // Then
        verify(preferencesRepository).updateAutoSaveToHistory(true)
    }

    @Test
    fun `updateAutoSaveToHistory calls repository with disabled`() = runTest {
        // Given
        val enabled = false

        // When
        viewModel.updateAutoSaveToHistory(enabled)
        advanceUntilIdle()

        // Then
        verify(preferencesRepository).updateAutoSaveToHistory(false)
    }

    @Test
    fun `updateAutoDeleteOldScans calls repository with enabled`() = runTest {
        // Given
        val enabled = true

        // When
        viewModel.updateAutoDeleteOldScans(enabled)
        advanceUntilIdle()

        // Then
        verify(preferencesRepository).updateAutoDeleteOldScans(true)
    }

    @Test
    fun `updateAutoDeleteOldScans calls repository with disabled`() = runTest {
        // Given
        val enabled = false

        // When
        viewModel.updateAutoDeleteOldScans(enabled)
        advanceUntilIdle()

        // Then
        verify(preferencesRepository).updateAutoDeleteOldScans(false)
    }

    @Test
    fun `updateAutoDeleteDays calls repository with correct days`() = runTest {
        // Given
        val days = 60

        // When
        viewModel.updateAutoDeleteDays(days)
        advanceUntilIdle()

        // Then
        verify(preferencesRepository).updateAutoDeleteDays(60)
    }

    @Test
    fun `updateAutoDeleteDays with minimum days calls repository`() = runTest {
        // Given
        val days = 7

        // When
        viewModel.updateAutoDeleteDays(days)
        advanceUntilIdle()

        // Then
        verify(preferencesRepository).updateAutoDeleteDays(7)
    }

    @Test
    fun `updateAutoDeleteDays with maximum days calls repository`() = runTest {
        // Given
        val days = 365

        // When
        viewModel.updateAutoDeleteDays(days)
        advanceUntilIdle()

        // Then
        verify(preferencesRepository).updateAutoDeleteDays(365)
    }

    @Test
    fun `multiple sequential updates are all processed`() = runTest {
        // When
        viewModel.updateTheme(AppTheme.DARK)
        viewModel.updateDynamicColor(false)
        viewModel.updateDefaultCamera(DefaultCamera.FRONT)
        advanceUntilIdle()

        // Then
        verify(preferencesRepository).updateTheme(AppTheme.DARK)
        verify(preferencesRepository).updateDynamicColor(false)
        verify(preferencesRepository).updateDefaultCamera(DefaultCamera.FRONT)
    }
}
