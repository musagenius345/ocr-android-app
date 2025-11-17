package com.musagenius.ocrapp.di

import android.content.Context
import com.musagenius.ocrapp.data.camera.CameraManager
import com.musagenius.ocrapp.data.utils.ImageCompressor
import com.musagenius.ocrapp.data.utils.StorageManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for camera-related dependencies
 */
@Module
@InstallIn(SingletonComponent::class)
object CameraModule {

    @Provides
    @Singleton
    fun provideCameraManager(
        @ApplicationContext context: Context
    ): CameraManager {
        return CameraManager(context)
    }

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
}
