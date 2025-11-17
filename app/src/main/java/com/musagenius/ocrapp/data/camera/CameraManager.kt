package com.musagenius.ocrapp.data.camera

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
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
 * Manages CameraX operations
 * Scoped to activity lifecycle to ensure proper camera resource cleanup
 */
@ActivityRetainedScoped
class CameraManager @Inject constructor(
    private val context: Context
) {
    private var cameraProvider: ProcessCameraProvider? = null
    private var camera: Camera? = null
    private var preview: Preview? = null
    private var imageCapture: ImageCapture? = null
    private val executor: Executor = ContextCompat.getMainExecutor(context)
    private var currentCameraFacing: CameraFacing = CameraFacing.BACK
    private var lifecycleOwner: LifecycleOwner? = null
    private var previewView: PreviewView? = null

    companion object {
        private const val TAG = "CameraManager"
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
    }

    /**
     * Initialize camera with preview
     */
    suspend fun startCamera(
        lifecycleOwner: LifecycleOwner,
        previewView: PreviewView,
        flashMode: FlashMode = FlashMode.OFF,
        cameraFacing: CameraFacing = CameraFacing.BACK
    ) = withContext(Dispatchers.Main) {
        try {
            // Save references for camera operations
            this@CameraManager.lifecycleOwner = lifecycleOwner
            this@CameraManager.previewView = previewView
            this@CameraManager.currentCameraFacing = cameraFacing

            cameraProvider = getCameraProvider()

            // Unbind all use cases before rebinding
            cameraProvider?.unbindAll()

            // Set up Preview use case
            preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }

            // Set up ImageCapture use case
            imageCapture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                .setFlashMode(flashMode.toImageCaptureFlashMode())
                .build()

            // Select camera based on facing
            val cameraSelector = when (cameraFacing) {
                CameraFacing.BACK -> CameraSelector.DEFAULT_BACK_CAMERA
                CameraFacing.FRONT -> CameraSelector.DEFAULT_FRONT_CAMERA
            }

            // Bind use cases to camera
            camera = cameraProvider?.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                preview,
                imageCapture
            )

            Log.d(TAG, "Camera started successfully with ${cameraFacing.getDisplayName()}")
        } catch (e: Exception) {
            Log.e(TAG, "Error starting camera", e)
            throw e
        }
    }

    /**
     * Capture image and save to file
     */
    suspend fun captureImage(): Uri = suspendCoroutine { continuation ->
        val imageCapture = imageCapture ?: run {
            continuation.resumeWithException(IllegalStateException("ImageCapture not initialized"))
            return@suspendCoroutine
        }

        // Create output file
        val photoFile = File(
            context.getExternalFilesDir(null),
            SimpleDateFormat(FILENAME_FORMAT, Locale.US)
                .format(System.currentTimeMillis()) + ".jpg"
        )

        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        imageCapture.takePicture(
            outputOptions,
            executor,
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
     * Set flash mode
     */
    fun setFlashMode(flashMode: FlashMode) {
        imageCapture?.flashMode = flashMode.toImageCaptureFlashMode()
    }

    /**
     * Set zoom ratio
     * @param ratio Zoom ratio (1.0 = no zoom)
     */
    fun setZoomRatio(ratio: Float) {
        camera?.cameraControl?.setZoomRatio(ratio)
        Log.d(TAG, "Zoom ratio set to: $ratio")
    }

    /**
     * Get zoom state
     */
    fun getZoomState() = camera?.cameraInfo?.zoomState

    /**
     * Focus on a point
     * @param x X coordinate (0-1)
     * @param y Y coordinate (0-1)
     * @param width Preview width
     * @param height Preview height
     */
    fun focusOnPoint(x: Float, y: Float, width: Int, height: Int) {
        val factory = previewView?.meteringPointFactory ?: return
        val point = factory.createPoint(x, y)
        val action = FocusMeteringAction.Builder(point)
            .setAutoCancelDuration(3, java.util.concurrent.TimeUnit.SECONDS)
            .build()

        camera?.cameraControl?.startFocusAndMetering(action)
        Log.d(TAG, "Focus requested at: ($x, $y)")
    }

    /**
     * Set exposure compensation
     * @param compensation Exposure compensation index
     */
    fun setExposureCompensation(compensation: Int) {
        camera?.cameraControl?.setExposureCompensationIndex(compensation)
        Log.d(TAG, "Exposure compensation set to: $compensation")
    }

    /**
     * Get exposure state
     */
    fun getExposureState() = camera?.cameraInfo?.exposureState

    /**
     * Flip camera (front/back)
     */
    suspend fun flipCamera() = withContext(Dispatchers.Main) {
        val newFacing = currentCameraFacing.flip()
        val owner = lifecycleOwner ?: return@withContext
        val view = previewView ?: return@withContext

        startCamera(
            lifecycleOwner = owner,
            previewView = view,
            cameraFacing = newFacing
        )

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
            val provider = getCameraProvider()
            provider.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA)
        } catch (e: Exception) {
            Log.e(TAG, "Error checking camera availability", e)
            false
        }
    }

    /**
     * Release camera resources
     */
    fun release() {
        cameraProvider?.unbindAll()
        camera = null
        preview = null
        imageCapture = null
        Log.d(TAG, "Camera released")
    }

    /**
     * Get CameraProvider instance
     */
    private suspend fun getCameraProvider(): ProcessCameraProvider = suspendCoroutine { continuation ->
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            try {
                continuation.resume(cameraProviderFuture.get())
            } catch (e: Exception) {
                continuation.resumeWithException(e)
            }
        }, executor)
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
