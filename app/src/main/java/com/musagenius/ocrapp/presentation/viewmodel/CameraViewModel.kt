package com.musagenius.ocrapp.presentation.viewmodel

import android.net.Uri
import android.util.Log
import androidx.camera.view.PreviewView
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.musagenius.ocrapp.data.camera.CameraManager
import com.musagenius.ocrapp.data.camera.LowLightDetector
import com.musagenius.ocrapp.data.utils.ImageCompressor
import com.musagenius.ocrapp.data.utils.StorageManager
import com.musagenius.ocrapp.presentation.ui.camera.CameraEvent
import com.musagenius.ocrapp.presentation.ui.camera.CameraFacing
import com.musagenius.ocrapp.presentation.ui.camera.CameraResolution
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
    private val cameraManager: CameraManager,
    private val imageCompressor: ImageCompressor,
    private val storageManager: StorageManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(CameraUiState())
    val uiState: StateFlow<CameraUiState> = _uiState.asStateFlow()

    // Store references for camera restart operations
    private var lifecycleOwner: LifecycleOwner? = null
    private var previewView: PreviewView? = null

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
            is CameraEvent.SetZoom -> setZoom(event.ratio)
            is CameraEvent.TapToFocus -> handleTapToFocus(event.x, event.y)
            is CameraEvent.SetExposure -> setExposure(event.compensation)
            is CameraEvent.FlipCamera -> flipCamera()
            is CameraEvent.ToggleGridOverlay -> toggleGridOverlay()
            is CameraEvent.UpdateLightingCondition -> updateLightingCondition(event.condition)
            is CameraEvent.DismissLowLightWarning -> dismissLowLightWarning()
            is CameraEvent.ShowResolutionDialog -> showResolutionDialog()
            is CameraEvent.DismissResolutionDialog -> dismissResolutionDialog()
            is CameraEvent.SetResolution -> setResolution(event.resolution)
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
                // Store references for camera restart operations (e.g., resolution change)
                this@CameraViewModel.lifecycleOwner = lifecycleOwner
                this@CameraViewModel.previewView = previewView

                _uiState.update { it.copy(isLoading = true, error = null) }

                cameraManager.startCamera(
                    lifecycleOwner = lifecycleOwner,
                    previewView = previewView,
                    flashMode = _uiState.value.flashMode,
                    cameraFacing = _uiState.value.cameraFacing,
                    resolution = _uiState.value.resolution
                )

                // Set up lighting condition callback
                cameraManager.setLightingConditionCallback { condition ->
                    onEvent(CameraEvent.UpdateLightingCondition(condition))
                }

                // Update camera capabilities after starting
                updateCameraCapabilities()

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

                // Check storage availability before capture
                if (!storageManager.hasAvailableStorage(requiredMB = 50L)) {
                    _uiState.update {
                        it.copy(
                            isProcessing = false,
                            error = "Insufficient storage space. Please free up space and try again."
                        )
                    }
                    Log.w(TAG, "Insufficient storage space")
                    return@launch
                }

                // Capture image
                val rawImageUri = cameraManager.captureImage()
                Log.d(TAG, "Image captured: $rawImageUri")

                // Compress image
                val compressResult = imageCompressor.compressImage(
                    sourceUri = rawImageUri,
                    quality = 85,
                    maxSize = 2048
                )

                compressResult.fold(
                    onSuccess = { compressedUri ->
                        val fileSizeKB = imageCompressor.getFileSizeKB(compressedUri)
                        Log.d(TAG, "Image compressed: $compressedUri, size: ${fileSizeKB}KB")

                        _uiState.update {
                            it.copy(
                                isProcessing = false,
                                capturedImageUri = compressedUri
                            )
                        }
                    },
                    onFailure = { error ->
                        Log.e(TAG, "Failed to compress image", error)
                        // Fall back to raw image if compression fails
                        _uiState.update {
                            it.copy(
                                isProcessing = false,
                                capturedImageUri = rawImageUri,
                                error = "Image compression failed, using original"
                            )
                        }
                    }
                )
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
     * Set zoom ratio
     */
    private fun setZoom(ratio: Float) {
        val clampedRatio = ratio.coerceIn(_uiState.value.minZoomRatio, _uiState.value.maxZoomRatio)
        _uiState.update { it.copy(zoomRatio = clampedRatio) }
        cameraManager.setZoomRatio(clampedRatio)
    }

    /**
     * Handle tap-to-focus
     */
    private fun handleTapToFocus(x: Float, y: Float) {
        // The CameraManager will handle the actual focus operation
        // x and y are normalized coordinates (0-1)
        viewModelScope.launch {
            try {
                cameraManager.focusOnPoint(x, y, 1, 1)
                Log.d(TAG, "Focus triggered at: ($x, $y)")
            } catch (e: Exception) {
                Log.e(TAG, "Focus failed", e)
            }
        }
    }

    /**
     * Set exposure compensation
     */
    private fun setExposure(compensation: Int) {
        val clampedCompensation = compensation.coerceIn(_uiState.value.minExposure, _uiState.value.maxExposure)
        _uiState.update { it.copy(exposureCompensation = clampedCompensation) }
        cameraManager.setExposureCompensation(clampedCompensation)
    }

    /**
     * Flip camera (front/back)
     */
    private fun flipCamera() {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true) }
                cameraManager.flipCamera()
                val newFacing = cameraManager.getCurrentCameraFacing()
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        cameraFacing = newFacing
                    )
                }

                // Update zoom and exposure ranges after camera flip
                updateCameraCapabilities()

                Log.d(TAG, "Camera flipped to: ${newFacing.getDisplayName()}")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to flip camera", e)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "Failed to flip camera: ${e.localizedMessage}"
                    )
                }
            }
        }
    }

    /**
     * Toggle grid overlay
     */
    private fun toggleGridOverlay() {
        _uiState.update { it.copy(showGridOverlay = !it.showGridOverlay) }
        Log.d(TAG, "Grid overlay: ${_uiState.value.showGridOverlay}")
    }

    /**
     * Update lighting condition from camera analysis
     */
    private fun updateLightingCondition(condition: LowLightDetector.LightingCondition) {
        _uiState.update { it.copy(lightingCondition = condition) }
        Log.d(TAG, "Lighting condition updated: $condition")
    }

    /**
     * Dismiss low light warning
     */
    private fun dismissLowLightWarning() {
        _uiState.update { it.copy(showLowLightWarning = false) }
        Log.d(TAG, "Low light warning dismissed")
    }

    /**
     * Show resolution selection dialog
     */
    private fun showResolutionDialog() {
        _uiState.update { it.copy(showResolutionDialog = true) }
        Log.d(TAG, "Resolution dialog shown")
    }

    /**
     * Dismiss resolution selection dialog
     */
    private fun dismissResolutionDialog() {
        _uiState.update { it.copy(showResolutionDialog = false) }
        Log.d(TAG, "Resolution dialog dismissed")
    }

    /**
     * Set camera resolution and restart camera
     */
    private fun setResolution(resolution: CameraResolution) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(resolution = resolution, showResolutionDialog = false, isLoading = true) }
                Log.d(TAG, "Resolution changed to: ${resolution.getDisplayName()}")

                // Restart camera with new resolution
                val lifecycleOwner = lifecycleOwner ?: return@launch
                val previewView = previewView ?: return@launch

                cameraManager.startCamera(
                    lifecycleOwner = lifecycleOwner,
                    previewView = previewView,
                    flashMode = _uiState.value.flashMode,
                    cameraFacing = _uiState.value.cameraFacing,
                    resolution = resolution
                )

                _uiState.update { it.copy(isLoading = false) }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to change resolution", e)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "Failed to change resolution: ${e.localizedMessage}"
                    )
                }
            }
        }
    }

    /**
     * Update camera capabilities (zoom range, exposure range)
     */
    private fun updateCameraCapabilities() {
        cameraManager.getZoomState()?.value?.let { zoomState ->
            _uiState.update {
                it.copy(
                    minZoomRatio = zoomState.minZoomRatio,
                    maxZoomRatio = zoomState.maxZoomRatio,
                    zoomRatio = zoomState.zoomRatio
                )
            }
        }

        cameraManager.getExposureState()?.let { exposureState ->
            _uiState.update {
                it.copy(
                    minExposure = exposureState.exposureCompensationRange.lower,
                    maxExposure = exposureState.exposureCompensationRange.upper,
                    exposureCompensation = exposureState.exposureCompensationIndex
                )
            }
        }
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
        cameraManager.clearLightingConditionCallback()
        cameraManager.release()

        // Clear references to avoid memory leaks
        lifecycleOwner = null
        previewView = null

        Log.d(TAG, "ViewModel cleared, camera released")
    }
}
