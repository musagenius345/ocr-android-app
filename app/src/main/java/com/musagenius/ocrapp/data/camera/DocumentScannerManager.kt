package com.musagenius.ocrapp.data.camera

import android.app.Activity
import android.content.Context
import android.content.IntentSender
import android.net.Uri
import android.util.Log
import androidx.activity.result.IntentSenderRequest
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.mlkit.vision.documentscanner.GmsDocumentScanner
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions.RESULT_FORMAT_JPEG
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions.SCANNER_MODE_FULL
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult
import com.musagenius.ocrapp.domain.model.Result
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

/**
 * Manager for ML Kit Document Scanner
 * Provides document scanning with automatic edge detection, perspective correction,
 * and image enhancement
 */
@Singleton
class DocumentScannerManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "DocumentScannerManager"
        private const val MAX_PAGES = 5
    }

    private val scanner: GmsDocumentScanner by lazy {
        val options = GmsDocumentScannerOptions.Builder()
            .setGalleryImportAllowed(true)
            .setPageLimit(MAX_PAGES)
            .setResultFormats(RESULT_FORMAT_JPEG)
            .setScannerMode(SCANNER_MODE_FULL)
            .build()

        GmsDocumentScanning.getClient(options)
    }

    /**
     * Check if Google Play Services is available for ML Kit
     */
    fun isAvailable(): Boolean {
        val googleApiAvailability = GoogleApiAvailability.getInstance()
        val resultCode = googleApiAvailability.isGooglePlayServicesAvailable(context)
        return resultCode == ConnectionResult.SUCCESS
    }

    /**
     * Get IntentSenderRequest for launching the document scanner
     * This should be used with ActivityResultContracts
     */
    suspend fun getScannerIntent(): Result<IntentSenderRequest> {
        return try {
            if (!isAvailable()) {
                return Result.error(
                    Exception("Google Play Services not available"),
                    "Document scanner requires Google Play Services. Please install or update Google Play Services."
                )
            }

            suspendCancellableCoroutine { continuation ->
                scanner.getStartScanIntent(context as Activity)
                    .addOnSuccessListener { intentSender ->
                        val request = IntentSenderRequest.Builder(intentSender).build()
                        continuation.resume(Result.success(request))
                    }
                    .addOnFailureListener { exception ->
                        Log.e(TAG, "Failed to get scanner intent", exception)
                        continuation.resume(
                            Result.error(
                                exception,
                                "Failed to start document scanner: ${exception.message}"
                            )
                        )
                    }
                    .addOnCanceledListener {
                        Log.d(TAG, "Scanner intent cancelled")
                        continuation.resume(
                            Result.error(
                                Exception("Scanner cancelled"),
                                "Document scanner was cancelled"
                            )
                        )
                    }

                continuation.invokeOnCancellation {
                    Log.d(TAG, "Coroutine cancelled while getting scanner intent")
                }
            }
        } catch (e: ClassCastException) {
            Log.e(TAG, "Context is not an Activity", e)
            Result.error(
                e,
                "Document scanner can only be launched from an Activity"
            )
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error getting scanner intent", e)
            Result.error(
                e,
                "Unexpected error: ${e.message}"
            )
        }
    }

    /**
     * Process scanner result and extract scanned page URIs
     */
    fun processResult(result: GmsDocumentScanningResult?): Result<List<Uri>> {
        return try {
            if (result == null) {
                return Result.error(
                    Exception("No result"),
                    "Document scanner returned no result"
                )
            }

            val pages = result.pages
            if (pages.isNullOrEmpty()) {
                return Result.error(
                    Exception("No pages scanned"),
                    "No pages were scanned. Please try again."
                )
            }

            val uris = pages.mapNotNull { page -> page.imageUri }
            if (uris.isEmpty()) {
                return Result.error(
                    Exception("No images in result"),
                    "Scanned pages contain no images"
                )
            }

            Log.d(TAG, "Successfully scanned ${uris.size} page(s)")
            Result.success(uris)
        } catch (e: Exception) {
            Log.e(TAG, "Error processing scanner result", e)
            Result.error(
                e,
                "Failed to process scanned images: ${e.message}"
            )
        }
    }

    /**
     * Get PDF if user chose to save as PDF
     * Note: This requires RESULT_FORMAT_PDF to be enabled in options
     */
    fun getPdf(result: GmsDocumentScanningResult?): Result<Uri?> {
        return try {
            val pdfUri = result?.pdf?.uri
            if (pdfUri != null) {
                Log.d(TAG, "PDF saved at: $pdfUri")
                Result.success(pdfUri)
            } else {
                Result.success(null)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting PDF", e)
            Result.error(e, "Failed to get PDF: ${e.message}")
        }
    }

    /**
     * Get user-friendly error message for common issues
     */
    fun getErrorMessage(exception: Exception): String {
        return when {
            exception is IntentSender.SendIntentException -> {
                "Failed to launch document scanner. Please try again."
            }
            exception.message?.contains("RESULT_CANCELED") == true -> {
                "Scanning cancelled"
            }
            exception.message?.contains("network") == true -> {
                "Network error. Please check your connection and try again."
            }
            !isAvailable() -> {
                "Document scanner requires Google Play Services"
            }
            else -> {
                exception.message ?: "Unknown error occurred"
            }
        }
    }

    /**
     * Clean up resources
     */
    fun cleanup() {
        // ML Kit scanner doesn't require explicit cleanup
        Log.d(TAG, "Document scanner manager cleaned up")
    }
}
