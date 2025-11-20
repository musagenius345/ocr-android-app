package com.musagenius.ocrapp.presentation.viewmodel

import android.net.Uri
import android.util.Log
import androidx.activity.result.IntentSenderRequest
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult
import com.musagenius.ocrapp.data.camera.CameraManager
import com.musagenius.ocrapp.data.camera.DocumentScannerManager
import com.musagenius.ocrapp.data.camera.LowLightDetector
import com.musagenius.ocrapp.data.utils.ImageCompressor
import com.musagenius.ocrapp.data.utils.StorageManager
import com.musagenius.ocrapp.domain.model.Result
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
    private val documentScannerManager: DocumentScannerManager,
    private val imageCompressor: ImageCompressor,
    private val storageManager: StorageManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(CameraUiState())
    val uiState: StateFlow<CameraUiState> = _uiState.asStateFlow()

    // Document scanner intent request for launching the scanner
    private val _scannerIntentRequest = MutableStateFlow<IntentSenderRequest?>(null)
    val scannerIntentRequest: StateFlow<IntentSenderRequest?> = _scannerIntentRequest.asStateFlow()

    // Track if camera has been bound to prevent duplicate bindings
    private var isCameraBound = false

    companion object {
        private const val TAG = "CameraViewModel"
    }

    /**
     * Handle camera events
     */
    fun onEvent(event: CameraEvent) {
        when (event) {
            is CameraEvent.CaptureImage -> captureImage()
            is CameraEvent.ScanDocument -> launchDocumentScanner()
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
     * Get the LifecycleCameraController for binding to PreviewView
     * Controller is managed by CameraManager and reused across configuration changes
     */
    fun getCameraController() = cameraManager.getCameraController()

    /**
     * Bind camera controller to lifecycle
     * Should be called once when the camera screen is composed
     * Uses flag to prevent duplicate bindings from multiple recompositions
     */
    fun bindCameraToLifecycle(lifecycleOwner: LifecycleOwner) {
        // Early return if already bound
        if (isCameraBound) {
            Log.d(TAG, "Camera already bound, skipping duplicate bind request")
            return
        }

        // Set flag immediately to block concurrent calls
        isCameraBound = true

        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true, error = null) }

                cameraManager.bindToLifecycle(
                    lifecycleOwner = lifecycleOwner,
                    flashMode = _uiState.value.flashMode,
                    cameraFacing = _uiState.value.cameraFacing
                )

                // Set up lighting condition callback
                cameraManager.setLightingConditionCallback { condition ->
                    onEvent(CameraEvent.UpdateLightingCondition(condition))
                }

                // Update camera capabilities after starting
                updateCameraCapabilities()

                _uiState.update { it.copy(isLoading = false) }
                Log.d(TAG, "Camera bound to lifecycle successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to bind camera", e)
                isCameraBound = false // Reset on error to allow retry
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "Failed to start camera: ${e.localizedMessage}. Pull down to retry."
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
     * Launch ML Kit Document Scanner
     */
    private fun launchDocumentScanner() {
        viewModelScope.launch {
            try {
                // Check if scanner is available
                if (!documentScannerManager.isAvailable()) {
                    _uiState.update {
                        it.copy(
                            error = "Document scanner requires Google Play Services. Please install or update Google Play Services."
                        )
                    }
                    Log.w(TAG, "Google Play Services not available for document scanner")
                    return@launch
                }

                _uiState.update { it.copy(isLoading = true, error = null) }

                // Get scanner intent
                val result = documentScannerManager.getScannerIntent()

                when (result) {
                    is Result.Success -> {
                        // Emit the intent request for the UI to launch
                        _scannerIntentRequest.value = result.data
                        _uiState.update { it.copy(isLoading = false) }
                        Log.d(TAG, "Document scanner intent ready")
                    }
                    is Result.Error -> {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                error = result.message ?: "Failed to launch document scanner"
                            )
                        }
                        Log.e(TAG, "Failed to get scanner intent", result.exception)
                    }
                    is Result.Loading -> {
                        // Should not happen with getScannerIntent
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Unexpected error launching document scanner", e)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "Failed to launch document scanner: ${e.localizedMessage}"
                    )
                }
            }
        }
    }

    /**
     * Handle document scanner result
     * Called by the UI after the scanner activity returns
     */
    fun handleScannerResult(scanResult: GmsDocumentScanningResult?) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isProcessing = true, error = null) }

                // Clear the intent request
                _scannerIntentRequest.value = null

                val result = documentScannerManager.processResult(scanResult)

                when (result) {
                    is Result.Success -> {
                        val scannedUris = result.data
                        Log.d(TAG, "Document scanner returned ${scannedUris.size} page(s)")

                        // For now, just use the first page
                        // TODO: In the future, handle multiple pages
                        if (scannedUris.isNotEmpty()) {
                            val firstPageUri = scannedUris.first()

                            // Compress the scanned image
                            val compressResult = imageCompressor.compressImage(
                                sourceUri = firstPageUri,
                                quality = 85,
                                maxSize = 2048
                            )

                            compressResult.fold(
                                onSuccess = { compressedUri ->
                                    val fileSizeKB = imageCompressor.getFileSizeKB(compressedUri)
                                    Log.d(TAG, "Scanned image compressed: $compressedUri, size: ${fileSizeKB}KB")

                                    _uiState.update {
                                        it.copy(
                                            isProcessing = false,
                                            capturedImageUri = compressedUri
                                        )
                                    }
                                },
                                onFailure = { error ->
                                    Log.e(TAG, "Failed to compress scanned image", error)
                                    // Fall back to raw scanned image if compression fails
                                    _uiState.update {
                                        it.copy(
                                            isProcessing = false,
                                            capturedImageUri = firstPageUri,
                                            error = "Image compression failed, using original"
                                        )
                                    }
                                }
                            )
                        } else {
                            _uiState.update {
                                it.copy(
                                    isProcessing = false,
                                    error = "No pages were scanned"
                                )
                            }
                        }
                    }
                    is Result.Error -> {
                        _uiState.update {
                            it.copy(
                                isProcessing = false,
                                error = result.message ?: "Failed to process scanned document"
                            )
                        }
                        Log.e(TAG, "Failed to process scanner result", result.exception)
                    }
                    is Result.Loading -> {
                        // Should not happen with processResult
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Unexpected error processing scanner result", e)
                _uiState.update {
                    it.copy(
                        isProcessing = false,
                        error = "Failed to process scanned document: ${e.localizedMessage}"
                    )
                }
            }
        }
    }

    /**
     * Clear scanner intent request (e.g., if user cancels)
     */
    fun clearScannerIntentRequest() {
        _scannerIntentRequest.value = null
        _uiState.update { it.copy(isLoading = false) }
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
     * Set camera resolution
     * Note: LifecycleCameraController doesn't support runtime resolution changes easily
     * Resolution change would require recreating the entire controller and rebinding
     * For now, just update the UI state
     */
    private fun setResolution(resolution: CameraResolution) {
        _uiState.update {
            it.copy(
                resolution = resolution,
                showResolutionDialog = false,
                error = "Resolution change requires app restart with LifecycleCameraController"
            )
        }
        Log.d(TAG, "Resolution setting saved (restart required): ${resolution.getDisplayName()}")
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

        Log.d(TAG, "ViewModel cleared, camera released")
    }
}
