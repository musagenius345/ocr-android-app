package com.musagenius.ocrapp.presentation.viewmodel

import android.net.Uri
import android.util.Log
import androidx.camera.view.PreviewView
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.musagenius.ocrapp.data.camera.CameraManager
import com.musagenius.ocrapp.presentation.ui.camera.CameraEvent
import com.musagenius.ocrapp.presentation.ui.camera.CameraUiState
import com.musagenius.ocrapp.presentation.ui.camera.FlashMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for camera screen
 */
@HiltViewModel
class CameraViewModel @Inject constructor(
    private val cameraManager: CameraManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(CameraUiState())
    val uiState: StateFlow<CameraUiState> = _uiState.asStateFlow()

    companion object {
        private const val TAG = "CameraViewModel"
    }

    /**
     * Handle camera events
     */
    fun onEvent(event: CameraEvent) {
        when (event) {
            is CameraEvent.CaptureImage -> captureImage()
            is CameraEvent.ToggleFlash -> toggleFlash()
            is CameraEvent.PickFromGallery -> {
                // Will be handled by the UI layer directly
            }
            is CameraEvent.DismissError -> dismissError()
        }
    }

    /**
     * Start camera with preview
     */
    fun startCamera(
        lifecycleOwner: LifecycleOwner,
        previewView: PreviewView
    ) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true, error = null) }

                cameraManager.startCamera(
                    lifecycleOwner = lifecycleOwner,
                    previewView = previewView,
                    flashMode = _uiState.value.flashMode
                )

                _uiState.update { it.copy(isLoading = false) }
                Log.d(TAG, "Camera started successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start camera", e)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "Failed to start camera: ${e.localizedMessage}"
                    )
                }
            }
        }
    }

    /**
     * Capture image
     */
    private fun captureImage() {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isProcessing = true, error = null) }

                val imageUri = cameraManager.captureImage()

                _uiState.update {
                    it.copy(
                        isProcessing = false,
                        capturedImageUri = imageUri
                    )
                }

                Log.d(TAG, "Image captured: $imageUri")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to capture image", e)
                _uiState.update {
                    it.copy(
                        isProcessing = false,
                        error = "Failed to capture image: ${e.localizedMessage}"
                    )
                }
            }
        }
    }

    /**
     * Toggle flash mode
     */
    private fun toggleFlash() {
        val newFlashMode = _uiState.value.flashMode.next()
        _uiState.update { it.copy(flashMode = newFlashMode) }
        cameraManager.setFlashMode(newFlashMode)
        Log.d(TAG, "Flash mode changed to: $newFlashMode")
    }

    /**
     * Clear captured image
     */
    fun clearCapturedImage() {
        _uiState.update { it.copy(capturedImageUri = null) }
    }

    /**
     * Dismiss error
     */
    private fun dismissError() {
        _uiState.update { it.copy(error = null) }
    }

    /**
     * Check if camera is available
     */
    suspend fun isCameraAvailable(): Boolean {
        return cameraManager.isCameraAvailable()
    }

    /**
     * Clean up resources
     */
    override fun onCleared() {
        super.onCleared()
        cameraManager.release()
        Log.d(TAG, "ViewModel cleared, camera released")
    }
}
