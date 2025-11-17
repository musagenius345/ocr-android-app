package com.musagenius.ocrapp.di

import android.content.Context
import com.musagenius.ocrapp.data.ocr.ImagePreprocessor
import com.musagenius.ocrapp.data.ocr.OCRService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for OCR-related dependencies
 */
@Module
@InstallIn(SingletonComponent::class)
object OCRModule {

    @Provides
    @Singleton
    fun provideImagePreprocessor(): ImagePreprocessor {
        return ImagePreprocessor()
    }

    @Provides
    @Singleton
    fun provideOCRService(
        @ApplicationContext context: Context,
        imagePreprocessor: ImagePreprocessor
    ): OCRService {
        return OCRService(context, imagePreprocessor)
    }
}
