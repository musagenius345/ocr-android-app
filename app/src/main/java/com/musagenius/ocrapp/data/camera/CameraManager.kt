package com.musagenius.ocrapp.data.camera

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.camera.core.*
import androidx.camera.core.SurfaceOrientedMeteringPointFactory
import androidx.camera.view.CameraController
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.musagenius.ocrapp.presentation.ui.camera.CameraFacing
import com.musagenius.ocrapp.presentation.ui.camera.FlashMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.Executor
import javax.inject.Inject
import dagger.hilt.android.scopes.ActivityRetainedScoped
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

/**
 * Manages CameraX operations using LifecycleCameraController
 * Scoped to activity lifecycle to ensure proper camera resource cleanup
 *
 * Uses LifecycleCameraController for simpler lifecycle management and Compose integration
 */
@ActivityRetainedScoped
class CameraManager @Inject constructor(
    private val context: Context,
    private val lowLightDetector: LowLightDetector
) {
    private var cameraController: LifecycleCameraController? = null
    private val mainExecutor: Executor = ContextCompat.getMainExecutor(context)
    private var currentCameraFacing: CameraFacing = CameraFacing.BACK

    companion object {
        private const val TAG = "CameraManager"
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
    }

    /**
     * Initialize camera with LifecycleCameraController
     * Much simpler than manual use case management - handles lifecycle automatically
     */
    suspend fun startCamera(
        lifecycleOwner: LifecycleOwner,
        previewView: PreviewView,
        flashMode: FlashMode = FlashMode.OFF,
        cameraFacing: CameraFacing = CameraFacing.BACK
    ) = withContext(Dispatchers.Main) {
        try {
            Log.d(TAG, "Initializing camera with ${cameraFacing.getDisplayName()}")

            // Save current camera facing
            currentCameraFacing = cameraFacing

            // Create or reuse controller
            val controller = cameraController ?: LifecycleCameraController(context).also {
                cameraController = it
            }

            // Configure controller
            controller.apply {
                // Set camera selector
                cameraSelector = when (cameraFacing) {
                    CameraFacing.BACK -> CameraSelector.DEFAULT_BACK_CAMERA
                    CameraFacing.FRONT -> CameraSelector.DEFAULT_FRONT_CAMERA
                }

                // Enable image capture
                setEnabledUseCases(
                    CameraController.IMAGE_CAPTURE or CameraController.IMAGE_ANALYSIS
                )

                // Set flash mode
                imageCaptureFlashMode = flashMode.toImageCaptureFlashMode()

                // IMPORTANT: Set PreviewView controller FIRST, then bind lifecycle
                // This ensures the surface is available when binding
                previewView.controller = this

                // Bind controller to lifecycle
                unbind() // Unbind any previous lifecycle
                bindToLifecycle(lifecycleOwner)
            }

            Log.d(TAG, "Camera controller bound successfully")

        } catch (e: Exception) {
            Log.e(TAG, "Failed to start camera", e)
            throw e
        }
    }

    /**
     * Capture image and save to file using controller
     */
    suspend fun captureImage(): Uri = suspendCoroutine { continuation ->
        val controller = cameraController ?: run {
            continuation.resumeWithException(IllegalStateException("Camera controller not initialized"))
            return@suspendCoroutine
        }

        // Create output file
        val photoFile = File(
            context.getExternalFilesDir(null),
            SimpleDateFormat(FILENAME_FORMAT, Locale.US)
                .format(System.currentTimeMillis()) + ".jpg"
        )

        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        controller.takePicture(
            outputOptions,
            mainExecutor,
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    val savedUri = Uri.fromFile(photoFile)
                    Log.d(TAG, "Photo captured successfully: $savedUri")
                    continuation.resume(savedUri)
                }

                override fun onError(exception: ImageCaptureException) {
                    Log.e(TAG, "Photo capture failed", exception)
                    continuation.resumeWithException(exception)
                }
            }
        )
    }

    /**
     * Set flash mode using controller
     */
    fun setFlashMode(flashMode: FlashMode) {
        cameraController?.imageCaptureFlashMode = flashMode.toImageCaptureFlashMode()
    }

    /**
     * Set zoom ratio using controller
     * @param ratio Zoom ratio (1.0 = no zoom)
     */
    fun setZoomRatio(ratio: Float) {
        cameraController?.cameraControl?.setZoomRatio(ratio)
        Log.d(TAG, "Zoom ratio set to: $ratio")
    }

    /**
     * Get zoom state from controller
     */
    fun getZoomState() = cameraController?.cameraInfo?.zoomState

    /**
     * Focus on a point using controller
     * @param x X coordinate (0-1, normalized)
     * @param y Y coordinate (0-1, normalized)
     * @param width Preview width in pixels
     * @param height Preview height in pixels
     */
    fun focusOnPoint(x: Float, y: Float, width: Int, height: Int) {
        val controller = cameraController ?: return

        // Use SurfaceOrientedMeteringPointFactory with display size
        val factory = SurfaceOrientedMeteringPointFactory(width.toFloat(), height.toFloat())

        // Factory expects normalized coordinates, which we already have
        val point = factory.createPoint(x, y)
        val action = FocusMeteringAction.Builder(point)
            .setAutoCancelDuration(3, java.util.concurrent.TimeUnit.SECONDS)
            .build()

        controller.cameraControl?.startFocusAndMetering(action)
        Log.d(TAG, "Focus requested at normalized: ($x, $y), size: ($width, $height)")
    }

    /**
     * Set exposure compensation using controller
     * @param compensation Exposure compensation index
     */
    fun setExposureCompensation(compensation: Int) {
        cameraController?.cameraControl?.setExposureCompensationIndex(compensation)
        Log.d(TAG, "Exposure compensation set to: $compensation")
    }

    /**
     * Get exposure state from controller
     */
    fun getExposureState() = cameraController?.cameraInfo?.exposureState

    /**
     * Flip camera (front/back) using controller
     */
    suspend fun flipCamera() = withContext(Dispatchers.Main) {
        val controller = cameraController ?: return@withContext
        val newFacing = currentCameraFacing.flip()

        // Update camera selector on controller
        controller.cameraSelector = when (newFacing) {
            CameraFacing.BACK -> CameraSelector.DEFAULT_BACK_CAMERA
            CameraFacing.FRONT -> CameraSelector.DEFAULT_FRONT_CAMERA
        }

        currentCameraFacing = newFacing
        Log.d(TAG, "Camera flipped to: ${newFacing.getDisplayName()}")
    }

    /**
     * Get current camera facing
     */
    fun getCurrentCameraFacing(): CameraFacing = currentCameraFacing

    /**
     * Check if camera is available
     */
    suspend fun isCameraAvailable(): Boolean = withContext(Dispatchers.IO) {
        try {
            cameraController?.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA) ?: false
        } catch (e: Exception) {
            Log.e(TAG, "Error checking camera availability", e)
            false
        }
    }

    /**
     * Set callback for lighting condition updates
     * Note: Image analysis can be added via controller.setImageAnalysisAnalyzer() if needed
     */
    fun setLightingConditionCallback(callback: (LowLightDetector.LightingCondition) -> Unit) {
        // TODO: Implement image analysis via controller.setImageAnalysisAnalyzer() if needed
        Log.d(TAG, "Lighting condition callback set (analysis not yet implemented with controller)")
    }

    /**
     * Clear lighting condition callback
     */
    fun clearLightingConditionCallback() {
        cameraController?.clearImageAnalysisAnalyzer()
        Log.d(TAG, "Lighting condition callback cleared")
    }

    /**
     * Release camera resources
     */
    fun release() {
        cameraController?.unbind()
        cameraController = null
        Log.d(TAG, "Camera controller released")
    }
}

/**
 * Convert FlashMode to ImageCapture flash mode
 */
private fun FlashMode.toImageCaptureFlashMode(): Int {
    return when (this) {
        FlashMode.OFF -> ImageCapture.FLASH_MODE_OFF
        FlashMode.ON -> ImageCapture.FLASH_MODE_ON
        FlashMode.AUTO -> ImageCapture.FLASH_MODE_AUTO
    }
}
