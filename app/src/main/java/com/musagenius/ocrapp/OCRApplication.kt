package com.musagenius.ocrapp

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Main application class for OCR App
 * Annotated with @HiltAndroidApp to enable Hilt dependency injection
 */
@HiltAndroidApp
class OCRApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        // Initialize app-wide configurations here if needed
    }
}
