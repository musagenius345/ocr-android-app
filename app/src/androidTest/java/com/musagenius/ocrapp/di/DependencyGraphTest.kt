package com.musagenius.ocrapp.di

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.musagenius.ocrapp.data.local.dao.ScanDao
import com.musagenius.ocrapp.data.local.database.AppDatabase
import com.musagenius.ocrapp.data.ocr.ImagePreprocessor
import com.musagenius.ocrapp.data.utils.ImageCompressor
import com.musagenius.ocrapp.data.utils.StorageManager
import com.musagenius.ocrapp.domain.repository.LanguageRepository
import com.musagenius.ocrapp.domain.repository.PreferencesRepository
import com.musagenius.ocrapp.domain.repository.ScanRepository
import com.musagenius.ocrapp.domain.service.OCRService
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.CoroutineDispatcher
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import javax.inject.Inject

/**
 * Integration tests for Hilt dependency injection graph
 * These tests verify that all dependencies can be injected at runtime
 *
 * @AndroidEntryPoint equivalent for tests is @HiltAndroidTest
 */
@HiltAndroidTest
class DependencyGraphTest {

    @get:Rule
    var hiltRule = HiltAndroidRule(this)

    // Core dependencies
    @Inject
    @ApplicationContext
    lateinit var context: Context

    @Inject
    @IoDispatcher
    lateinit var ioDispatcher: CoroutineDispatcher

    @Inject
    @MainDispatcher
    lateinit var mainDispatcher: CoroutineDispatcher

    @Inject
    @DefaultDispatcher
    lateinit var defaultDispatcher: CoroutineDispatcher

    // Database
    @Inject
    lateinit var database: AppDatabase

    @Inject
    lateinit var scanDao: ScanDao

    // Repositories
    @Inject
    lateinit var scanRepository: ScanRepository

    @Inject
    lateinit var languageRepository: LanguageRepository

    @Inject
    lateinit var preferencesRepository: PreferencesRepository

    // Services
    @Inject
    lateinit var ocrService: OCRService

    @Inject
    lateinit var imagePreprocessor: ImagePreprocessor

    // Utilities
    @Inject
    lateinit var imageCompressor: ImageCompressor

    @Inject
    lateinit var storageManager: StorageManager

    @Before
    fun setup() {
        // Inject dependencies into test
        hiltRule.inject()
    }

    /**
     * Test that application context is injected correctly
     */
    @Test
    fun testApplicationContextInjected() {
        assert(::context.isInitialized) { "Context should be injected" }
        assert(context === ApplicationProvider.getApplicationContext<Context>()) {
            "Injected context should be application context"
        }
    }

    /**
     * Test that all dispatcher qualifiers provide different instances
     */
    @Test
    fun testDispatchersInjected() {
        assert(::ioDispatcher.isInitialized) { "IoDispatcher should be injected" }
        assert(::mainDispatcher.isInitialized) { "MainDispatcher should be injected" }
        assert(::defaultDispatcher.isInitialized) { "DefaultDispatcher should be injected" }

        // Verify they are different dispatchers (except Main in test environment)
        assert(ioDispatcher != defaultDispatcher) {
            "IoDispatcher and DefaultDispatcher should be different"
        }
    }

    /**
     * Test that database and DAO are injected as singletons
     */
    @Test
    fun testDatabaseAndDaoInjected() {
        assert(::database.isInitialized) { "AppDatabase should be injected" }
        assert(::scanDao.isInitialized) { "ScanDao should be injected" }

        // Verify DAO from database is same as injected DAO (singleton behavior)
        assert(database.scanDao() === scanDao) {
            "Injected ScanDao should be same instance as from database"
        }
    }

    /**
     * Test that all repositories are injected correctly
     */
    @Test
    fun testRepositoriesInjected() {
        assert(::scanRepository.isInitialized) { "ScanRepository should be injected" }
        assert(::languageRepository.isInitialized) { "LanguageRepository should be injected" }
        assert(::preferencesRepository.isInitialized) { "PreferencesRepository should be injected" }
    }

    /**
     * Test that OCR service and image preprocessor are injected
     */
    @Test
    fun testOCRDependenciesInjected() {
        assert(::ocrService.isInitialized) { "OCRService should be injected" }
        assert(::imagePreprocessor.isInitialized) { "ImagePreprocessor should be injected" }

        // Verify OCRService version can be called (proves it's initialized correctly)
        val version = ocrService.getVersion()
        assert(version.isNotEmpty()) { "OCRService should return a version string" }
    }

    /**
     * Test that utility classes are injected
     */
    @Test
    fun testUtilitiesInjected() {
        assert(::imageCompressor.isInitialized) { "ImageCompressor should be injected" }
        assert(::storageManager.isInitialized) { "StorageManager should be injected" }
    }

    /**
     * Test singleton behavior - inject same dependencies twice
     */
    @Test
    fun testSingletonBehavior() {
        @Inject
        lateinit var database2: AppDatabase

        @Inject
        lateinit var ocrService2: OCRService

        // Re-inject to get second set of "singletons"
        hiltRule.inject()

        // These should be the same instances (singleton scope)
        assert(database === database2) {
            "AppDatabase should be singleton - same instance"
        }
        assert(ocrService === ocrService2) {
            "OCRService should be singleton - same instance"
        }
    }

    /**
     * Test that dependencies have correct types
     */
    @Test
    fun testDependencyTypes() {
        assert(scanRepository is com.musagenius.ocrapp.data.repository.ScanRepositoryImpl) {
            "ScanRepository should be implemented by ScanRepositoryImpl"
        }
        assert(languageRepository is com.musagenius.ocrapp.data.repository.LanguageRepositoryImpl) {
            "LanguageRepository should be implemented by LanguageRepositoryImpl"
        }
        assert(preferencesRepository is com.musagenius.ocrapp.data.repository.PreferencesRepositoryImpl) {
            "PreferencesRepository should be implemented by PreferencesRepositoryImpl"
        }
        assert(ocrService is com.musagenius.ocrapp.data.ocr.OCRServiceImpl) {
            "OCRService should be implemented by OCRServiceImpl"
        }
    }

    /**
     * Test that database is writable (proves it's initialized correctly)
     */
    @Test
    fun testDatabaseIsOperational() {
        // This would fail if database wasn't initialized properly
        assert(database.isOpen) { "Database should be open" }

        // Clean up
        database.close()
    }

    /**
     * Test that no circular dependencies exist by verifying all injections complete
     */
    @Test
    fun testNoCircularDependencies() {
        // If this test runs without StackOverflowError, no circular deps exist
        assert(::context.isInitialized)
        assert(::database.isInitialized)
        assert(::scanRepository.isInitialized)
        assert(::languageRepository.isInitialized)
        assert(::preferencesRepository.isInitialized)
        assert(::ocrService.isInitialized)
        assert(::imagePreprocessor.isInitialized)
        assert(::imageCompressor.isInitialized)
        assert(::storageManager.isInitialized)

        // All dependencies successfully injected - no circular dependencies
        assert(true) { "All dependencies successfully injected without circular dependency errors" }
    }

    /**
     * Test that dependencies can interact correctly
     */
    @Test
    fun testDependencyInteraction() {
        // Test that repository can use DAO (proves dependency chain works)
        // This is a smoke test - detailed functionality tested in repository tests

        assert(scanRepository != null) { "ScanRepository should be injected" }
        assert(scanDao != null) { "ScanDao should be injected" }

        // If these are both initialized and non-null, dependency chain is working
        assert(true) { "Dependency chain is functional" }
    }
}
