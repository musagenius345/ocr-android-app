package com.musagenius.ocrapp.presentation.viewmodel

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.musagenius.ocrapp.data.utils.ImageEditor
import com.musagenius.ocrapp.presentation.ui.editor.ImageEditorEvent
import com.musagenius.ocrapp.presentation.ui.editor.ImageEditorUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for image editor screen
 */
@HiltViewModel
class ImageEditorViewModel @Inject constructor(
    private val imageEditor: ImageEditor
) : ViewModel() {

    private val _uiState = MutableStateFlow(ImageEditorUiState())
    val uiState: StateFlow<ImageEditorUiState> = _uiState.asStateFlow()

    // Callback when image is saved
    private var onImageSaved: ((Uri) -> Unit)? = null

    // Callback when editing is cancelled
    private var onEditCancelled: (() -> Unit)? = null

    companion object {
        private const val TAG = "ImageEditorViewModel"
    }

    /**
     * Initialize with source image URI
     */
    fun initializeImage(
        uri: Uri,
        onSaved: (Uri) -> Unit,
        onCancelled: () -> Unit
    ) {
        _uiState.update {
            it.copy(
                sourceImageUri = uri,
                rotationDegrees = 0f,
                isProcessing = false,
                error = null
            )
        }
        onImageSaved = onSaved
        onEditCancelled = onCancelled
        Log.d(TAG, "Image editor initialized with URI: $uri")
    }

    /**
     * Handle image editor events
     */
    fun onEvent(event: ImageEditorEvent) {
        when (event) {
            is ImageEditorEvent.RotateClockwise -> rotateClockwise()
            is ImageEditorEvent.RotateCounterClockwise -> rotateCounterClockwise()
            is ImageEditorEvent.ResetRotation -> resetRotation()
            is ImageEditorEvent.SaveImage -> saveImage()
            is ImageEditorEvent.Cancel -> cancel()
            is ImageEditorEvent.DismissError -> dismissError()
        }
    }

    /**
     * Rotate image 90 degrees clockwise
     */
    private fun rotateClockwise() {
        _uiState.update {
            val newRotation = (it.rotationDegrees + 90f) % 360f
            it.copy(rotationDegrees = newRotation)
        }
        Log.d(TAG, "Rotated clockwise to ${_uiState.value.rotationDegrees} degrees")
    }

    /**
     * Rotate image 90 degrees counter-clockwise
     */
    private fun rotateCounterClockwise() {
        _uiState.update {
            val newRotation = (it.rotationDegrees - 90f + 360f) % 360f
            it.copy(rotationDegrees = newRotation)
        }
        Log.d(TAG, "Rotated counter-clockwise to ${_uiState.value.rotationDegrees} degrees")
    }

    /**
     * Reset rotation to 0 degrees
     */
    private fun resetRotation() {
        _uiState.update { it.copy(rotationDegrees = 0f) }
        Log.d(TAG, "Reset rotation to 0 degrees")
    }

    /**
     * Save edited image
     */
    private fun saveImage() {
        viewModelScope.launch {
            try {
                val sourceUri = _uiState.value.sourceImageUri
                if (sourceUri == null) {
                    _uiState.update { it.copy(error = "No image to save") }
                    return@launch
                }

                _uiState.update { it.copy(isProcessing = true, error = null) }
                Log.d(TAG, "Saving image with rotation: ${_uiState.value.rotationDegrees} degrees")

                val result = imageEditor.editImage(
                    sourceUri = sourceUri,
                    rotationDegrees = _uiState.value.rotationDegrees,
                    cropRect = null // Cropping not implemented yet, can be added later
                )

                result.fold(
                    onSuccess = { editedUri ->
                        Log.d(TAG, "Image saved successfully: $editedUri")
                        _uiState.update { it.copy(isProcessing = false) }
                        onImageSaved?.invoke(editedUri)
                    },
                    onFailure = { exception ->
                        Log.e(TAG, "Failed to save image", exception)
                        _uiState.update {
                            it.copy(
                                isProcessing = false,
                                error = "Failed to save image: ${exception.localizedMessage}"
                            )
                        }
                    }
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error saving image", e)
                _uiState.update {
                    it.copy(
                        isProcessing = false,
                        error = "Error saving image: ${e.localizedMessage}"
                    )
                }
            }
        }
    }

    /**
     * Cancel editing
     */
    private fun cancel() {
        Log.d(TAG, "Image editing cancelled")
        onEditCancelled?.invoke()
    }

    /**
     * Dismiss error message
     */
    private fun dismissError() {
        _uiState.update { it.copy(error = null) }
        Log.d(TAG, "Error dismissed")
    }

    override fun onCleared() {
        super.onCleared()
        onImageSaved = null
        onEditCancelled = null
        Log.d(TAG, "ViewModel cleared")
    }
}
