package com.musagenius.ocrapp.di

import com.musagenius.ocrapp.data.repository.PreferencesRepositoryImpl
import com.musagenius.ocrapp.data.repository.ScanRepositoryImpl
import com.musagenius.ocrapp.domain.repository.PreferencesRepository
import com.musagenius.ocrapp.domain.repository.ScanRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Repository module
 * Binds repository interfaces to their implementations
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindScanRepository(
        scanRepositoryImpl: ScanRepositoryImpl
    ): ScanRepository

    @Binds
    @Singleton
    abstract fun bindPreferencesRepository(
        preferencesRepositoryImpl: PreferencesRepositoryImpl
    ): PreferencesRepository
}
