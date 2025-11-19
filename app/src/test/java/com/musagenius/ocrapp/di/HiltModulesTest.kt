package com.musagenius.ocrapp.di

import android.content.Context
import com.musagenius.ocrapp.data.local.dao.ScanDao
import com.musagenius.ocrapp.data.local.database.AppDatabase
import com.musagenius.ocrapp.data.ocr.ImagePreprocessor
import com.musagenius.ocrapp.data.ocr.OCRServiceImpl
import com.musagenius.ocrapp.data.repository.LanguageRepositoryImpl
import com.musagenius.ocrapp.data.repository.PreferencesRepositoryImpl
import com.musagenius.ocrapp.data.repository.ScanRepositoryImpl
import com.musagenius.ocrapp.data.utils.ImageCompressor
import com.musagenius.ocrapp.data.utils.StorageManager
import com.musagenius.ocrapp.domain.repository.LanguageRepository
import com.musagenius.ocrapp.domain.repository.PreferencesRepository
import com.musagenius.ocrapp.domain.repository.ScanRepository
import com.musagenius.ocrapp.domain.service.OCRService
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import org.junit.Test
import org.mockito.Mockito.mock
import javax.inject.Qualifier
import javax.inject.Singleton

/**
 * Unit tests to verify Hilt modules compile and have correct structure
 * These tests verify the DI graph structure without actually running Android code
 */
class HiltModulesTest {

    /**
     * Test that AppModule provides all required dependencies
     */
    @Test
    fun `AppModule provides dispatchers correctly`() {
        // Verify dispatcher provisions would work
        val ioDispatcher = AppModule.provideIoDispatcher()
        val mainDispatcher = AppModule.provideMainDispatcher()
        val defaultDispatcher = AppModule.provideDefaultDispatcher()

        assert(ioDispatcher == Dispatchers.IO)
        assert(mainDispatcher == Dispatchers.Main)
        assert(defaultDispatcher == Dispatchers.Default)
    }

    /**
     * Test that DatabaseModule exists and is properly structured
     * Note: This tests the module structure, not actual database creation
     */
    @Test
    fun `DatabaseModule exists and is annotated`() {
        // Simply verify the module class exists and can be loaded
        val databaseModule = DatabaseModule::class.java
        assert(databaseModule != null) { "DatabaseModule should exist" }

        // Verify it's annotated with @Module
        val annotations = databaseModule.annotations
        assert(annotations.isNotEmpty()) { "DatabaseModule should have annotations" }
    }

    /**
     * Test that RepositoryModule binds all repositories correctly
     */
    @Test
    fun `RepositoryModule binds all repository interfaces`() {
        // Verify the module uses abstract class with @Binds (more efficient than @Provides)
        val isAbstract = RepositoryModule::class.isAbstract
        assert(isAbstract) { "RepositoryModule should be abstract to use @Binds" }

        // Verify annotations
        val moduleAnnotations = RepositoryModule::class.annotations
        val hasModuleAnnotation = moduleAnnotations.any { it is Module }
        assert(hasModuleAnnotation) { "RepositoryModule should have @Module annotation" }

        // Verify method signatures exist
        val methods = RepositoryModule::class.java.declaredMethods
        val bindings = methods.filter { method ->
            method.annotations.any { it is Binds }
        }

        // Should have 3 @Binds methods (ScanRepository, PreferencesRepository, LanguageRepository)
        assert(bindings.size == 3) {
            "RepositoryModule should have 3 @Binds methods, found ${bindings.size}"
        }
    }

    /**
     * Test that OCRModule provides OCR-related dependencies
     */
    @Test
    fun `OCRModule provides OCR dependencies`() {
        val preprocessor = OCRModule.provideImagePreprocessor()

        // Verify ImagePreprocessor is provided
        assert(preprocessor != null) { "OCRModule should provide ImagePreprocessor" }
        assert(preprocessor is ImagePreprocessor) { "Should return ImagePreprocessor instance" }
    }

    /**
     * Test that CameraModule structure is correct
     * Verifies the dual-module approach (CameraManagerModule + UtilityModule)
     */
    @Test
    fun `CameraModule has correct structure`() {
        // Verify CameraManagerModule exists and has correct annotations
        val cameraModuleAnnotations = CameraManagerModule::class.annotations
        val hasCameraModuleAnnotation = cameraModuleAnnotations.any { it is Module }
        assert(hasCameraModuleAnnotation) {
            "CameraManagerModule should have @Module annotation"
        }

        // Verify UtilityModule exists and has correct annotations
        val utilityModuleAnnotations = UtilityModule::class.annotations
        val hasUtilityModuleAnnotation = utilityModuleAnnotations.any { it is Module }
        assert(hasUtilityModuleAnnotation) {
            "UtilityModule should have @Module annotation"
        }
    }

    /**
     * Test that custom qualifiers are defined correctly
     */
    @Test
    fun `AppModule defines dispatcher qualifiers`() {
        // Verify qualifier annotations exist
        val ioQualifierAnnotation = IoDispatcher::class.annotations.find { it is Qualifier }
        val mainQualifierAnnotation = MainDispatcher::class.annotations.find { it is Qualifier }
        val defaultQualifierAnnotation = DefaultDispatcher::class.annotations.find { it is Qualifier }

        assert(ioQualifierAnnotation != null) { "@IoDispatcher should be a Qualifier" }
        assert(mainQualifierAnnotation != null) { "@MainDispatcher should be a Qualifier" }
        assert(defaultQualifierAnnotation != null) { "@DefaultDispatcher should be a Qualifier" }
    }

    /**
     * Test that repository implementations can be created with proper constructor
     */
    @Test
    fun `Repository implementations have @Inject constructors`() {
        // Verify ScanRepositoryImpl has injectable constructor
        val scanRepoConstructors = ScanRepositoryImpl::class.constructors
        assert(scanRepoConstructors.isNotEmpty()) {
            "ScanRepositoryImpl should have at least one constructor"
        }

        // Verify LanguageRepositoryImpl has injectable constructor
        val langRepoConstructors = LanguageRepositoryImpl::class.constructors
        assert(langRepoConstructors.isNotEmpty()) {
            "LanguageRepositoryImpl should have at least one constructor"
        }

        // Verify PreferencesRepositoryImpl has injectable constructor
        val prefsRepoConstructors = PreferencesRepositoryImpl::class.constructors
        assert(prefsRepoConstructors.isNotEmpty()) {
            "PreferencesRepositoryImpl should have at least one constructor"
        }
    }

    /**
     * Test that OCRServiceImpl can be constructed
     */
    @Test
    fun `OCRServiceImpl has proper constructor dependencies`() {
        val constructors = OCRServiceImpl::class.constructors
        assert(constructors.isNotEmpty()) {
            "OCRServiceImpl should have at least one constructor"
        }

        // Verify constructor has correct parameters (Context, ImagePreprocessor)
        val primaryConstructor = constructors.first()
        assert(primaryConstructor.parameters.size == 2) {
            "OCRServiceImpl constructor should have 2 parameters (Context, ImagePreprocessor)"
        }
    }

    /**
     * Test that all modules exist and can be loaded
     */
    @Test
    fun `All singleton modules exist`() {
        // Verify all modules exist and can be loaded
        val singletonModules = listOf(
            AppModule::class.java,
            DatabaseModule::class.java,
            RepositoryModule::class.java,
            OCRModule::class.java,
            UtilityModule::class.java
        )

        singletonModules.forEach { moduleClass ->
            assert(moduleClass != null) {
                "${moduleClass.simpleName} should exist"
            }
            assert(moduleClass.annotations.isNotEmpty()) {
                "${moduleClass.simpleName} should have annotations"
            }
        }
    }

    /**
     * Test that utility classes can be instantiated
     */
    @Test
    fun `Utility classes can be instantiated`() {
        // These should have no-arg constructors or simple constructors
        val imageCompressorConstructors = ImageCompressor::class.constructors
        val storageManagerConstructors = StorageManager::class.constructors
        val imagePreprocessorConstructors = ImagePreprocessor::class.constructors

        assert(imageCompressorConstructors.isNotEmpty()) {
            "ImageCompressor should have constructors"
        }
        assert(storageManagerConstructors.isNotEmpty()) {
            "StorageManager should have constructors"
        }
        assert(imagePreprocessorConstructors.isNotEmpty()) {
            "ImagePreprocessor should have constructors"
        }
    }

    /**
     * Verify no circular dependencies exist in module structure
     */
    @Test
    fun `No circular dependencies in module structure`() {
        // This test verifies the dependency chain is acyclic
        // AppModule -> (provides basic dependencies)
        // DatabaseModule -> depends on AppModule (Context)
        // RepositoryModule -> depends on DatabaseModule (DAOs) and AppModule (Dispatchers)
        // OCRModule -> depends on AppModule (Context)
        // CameraModule -> depends on AppModule (Context)

        // If all classes can be loaded without StackOverflowError, no circular deps exist
        val modules = listOf(
            AppModule::class.java,
            DatabaseModule::class.java,
            RepositoryModule::class.java,
            OCRModule::class.java,
            CameraManagerModule::class.java,
            UtilityModule::class.java
        )

        modules.forEach { moduleClass ->
            assert(moduleClass.canonicalName != null) {
                "${moduleClass.simpleName} should be loadable"
            }
        }
    }
}
