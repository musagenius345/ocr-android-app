package com.musagenius.ocrapp.di

import android.content.Context
import com.musagenius.ocrapp.data.camera.CameraManager
import com.musagenius.ocrapp.data.camera.DocumentScannerManager
import com.musagenius.ocrapp.data.camera.LowLightDetector
import com.musagenius.ocrapp.data.utils.ImageCompressor
import com.musagenius.ocrapp.data.utils.StorageManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityRetainedComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.scopes.ActivityRetainedScoped
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for camera manager (activity-retained scope)
 * CameraManager is scoped to the activity lifecycle to ensure proper
 * resource cleanup when the activity is destroyed
 */
@Module
@InstallIn(ActivityRetainedComponent::class)
object CameraManagerModule {

    @Provides
    @ActivityRetainedScoped
    fun provideLowLightDetector(): LowLightDetector {
        return LowLightDetector()
    }

    @Provides
    @ActivityRetainedScoped
    fun provideCameraManager(
        @ApplicationContext context: Context,
        lowLightDetector: LowLightDetector
    ): CameraManager {
        return CameraManager(context, lowLightDetector)
    }
}

/**
 * Hilt module for utility dependencies (singleton scope)
 * ImageCompressor and StorageManager are stateless utilities that can be
 * safely shared across the entire application lifecycle
 */
@Module
@InstallIn(SingletonComponent::class)
object UtilityModule {

    @Provides
    @Singleton
    fun provideImageCompressor(
        @ApplicationContext context: Context
    ): ImageCompressor {
        return ImageCompressor(context)
    }

    @Provides
    @Singleton
    fun provideStorageManager(
        @ApplicationContext context: Context
    ): StorageManager {
        return StorageManager(context)
    }

    @Provides
    @Singleton
    fun provideDocumentScannerManager(
        @ApplicationContext context: Context
    ): DocumentScannerManager {
        return DocumentScannerManager(context)
    }
}
