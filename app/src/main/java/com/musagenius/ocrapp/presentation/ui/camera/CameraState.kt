package com.musagenius.ocrapp.presentation.ui.camera

import android.net.Uri

/**
 * UI state for the camera screen
 */
data class CameraUiState(
    val isLoading: Boolean = false,
    val capturedImageUri: Uri? = null,
    val error: String? = null,
    val flashMode: FlashMode = FlashMode.OFF,
    val isProcessing: Boolean = false,
    val processingProgress: Float = 0f
)

/**
 * Camera flash modes
 */
enum class FlashMode {
    OFF,
    ON,
    AUTO;

    fun next(): FlashMode {
        return when (this) {
            OFF -> ON
            ON -> AUTO
            AUTO -> OFF
        }
    }

    fun getDisplayName(): String {
        return when (this) {
            OFF -> "Flash Off"
            ON -> "Flash On"
            AUTO -> "Flash Auto"
        }
    }

    fun getContentDescription(): String {
        return when (this) {
            OFF -> "Flash is off. Tap to turn on."
            ON -> "Flash is on. Tap for auto mode."
            AUTO -> "Flash is in auto mode. Tap to turn off."
        }
    }
}

/**
 * Camera events from UI
 */
sealed class CameraEvent {
    data object CaptureImage : CameraEvent()
    data object ToggleFlash : CameraEvent()
    data object PickFromGallery : CameraEvent()
    data object DismissError : CameraEvent()
}
