package com.musagenius.ocrapp.presentation.ui.editor

import android.net.Uri

/**
 * UI state for image editor screen
 */
data class ImageEditorUiState(
    val sourceImageUri: Uri? = null,
    val rotationDegrees: Float = 0f,
    val isProcessing: Boolean = false,
    val error: String? = null
)

/**
 * Image editor events from UI
 */
sealed class ImageEditorEvent {
    data object RotateClockwise : ImageEditorEvent()
    data object RotateCounterClockwise : ImageEditorEvent()
    data object ResetRotation : ImageEditorEvent()
    data object SaveImage : ImageEditorEvent()
    data object Cancel : ImageEditorEvent()
    data object DismissError : ImageEditorEvent()
}
