package com.musagenius.ocrapp.presentation.ui.camera

import android.net.Uri
import com.musagenius.ocrapp.data.camera.DocumentEdgeDetector

/**
 * UI state for the camera screen
 */
data class CameraUiState(
    val isLoading: Boolean = false,
    val capturedImageUri: Uri? = null,
    val error: String? = null,
    val flashMode: FlashMode = FlashMode.OFF,
    val isProcessing: Boolean = false,
    val processingProgress: Float = 0f,
    val zoomRatio: Float = 1f,
    val minZoomRatio: Float = 1f,
    val maxZoomRatio: Float = 1f,
    val cameraFacing: CameraFacing = CameraFacing.BACK,
    val showGridOverlay: Boolean = false,
    val showDocumentOverlay: Boolean = true,
    val documentCorners: DocumentEdgeDetector.DocumentCorners? = null,
    val previewWidth: Float = 0f,
    val previewHeight: Float = 0f,
    val exposureCompensation: Int = 0,
    val minExposure: Int = 0,
    val maxExposure: Int = 0
)

/**
 * Camera facing direction
 */
enum class CameraFacing {
    BACK,
    FRONT;

    fun flip(): CameraFacing {
        return when (this) {
            BACK -> FRONT
            FRONT -> BACK
        }
    }

    fun getDisplayName(): String {
        return when (this) {
            BACK -> "Back Camera"
            FRONT -> "Front Camera"
        }
    }
}

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
    data class SetZoom(val ratio: Float) : CameraEvent()
    data class TapToFocus(val x: Float, val y: Float) : CameraEvent()
    data class SetExposure(val compensation: Int) : CameraEvent()
    data object FlipCamera : CameraEvent()
    data object ToggleGridOverlay : CameraEvent()
    data object ToggleDocumentOverlay : CameraEvent()
    data class UpdateDocumentCorners(val corners: DocumentEdgeDetector.DocumentCorners?) : CameraEvent()
    data class UpdatePreviewSize(val width: Float, val height: Float) : CameraEvent()
}
