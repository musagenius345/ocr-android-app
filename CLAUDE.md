# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Material Expressive Android OCR App - A modern offline Android application for text extraction from images using Tesseract OCR. Built with Jetpack Compose, Material Design 3, and Clean Architecture.

**Key Technologies:**
- Jetpack Compose with Material 3 for UI
- Tesseract OCR (via tess-two library) for offline text recognition
- CameraX for image capture
- Room Database for local storage
- Hilt for dependency injection
- Kotlin Coroutines for async operations
- MVVM + Clean Architecture pattern

## Development Commands

### Building the App
```bash
./gradlew build                    # Build and test the project
./gradlew assembleDebug            # Build debug APK
./gradlew assembleRelease          # Build release APK (minified)
./gradlew clean                    # Clean build directory
```

### Running Tests
```bash
./gradlew test                     # Run all unit tests
./gradlew testDebugUnitTest        # Run debug unit tests only
./gradlew testReleaseUnitTest      # Run release unit tests only
./gradlew connectedAndroidTest     # Run instrumentation tests on connected device
./gradlew connectedDebugAndroidTest # Run debug instrumentation tests
```

### Code Quality
```bash
./gradlew lint                     # Run lint checks
./gradlew lintDebug                # Run lint on debug variant
./gradlew lintFix                  # Run lint and apply safe fixes
./gradlew check                    # Run all checks (lint + tests)
```

### Installation
```bash
./gradlew installDebug             # Install debug APK on connected device
./gradlew uninstallDebug           # Uninstall debug APK
```

## Architecture Overview

This project follows **Clean Architecture with MVVM pattern**, organized in three layers:

### Domain Layer (`domain/`)
The innermost layer containing business logic, independent of Android framework.

- **`model/`**: Core domain models (Language, OCRConfig, OCRResult, ScanResult, UserPreferences, etc.)
- **`repository/`**: Repository interfaces defining data contracts (ScanRepository, LanguageRepository, PreferencesRepository)
- **`service/`**: Service interfaces (OCRService) defining core operations
- **`usecase/`**: Single-responsibility business logic units (ProcessImageUseCase, InitializeOCRUseCase, GetAllScansUseCase, etc.)

Each use case encapsulates one specific business operation and depends only on repository/service interfaces.

### Data Layer (`data/`)
Implementation of domain contracts, handling data sources and external libraries.

- **`camera/`**: CameraX integration (CameraManager, DocumentEdgeDetector, LowLightDetector)
- **`local/`**: Room database components
  - `dao/`: Data Access Objects (ScanDao)
  - `entity/`: Room entities (ScanEntity)
  - `database/`: Database definition (AppDatabase)
- **`ocr/`**: Tesseract OCR implementation
  - `OCRServiceImpl`: Implements OCRService using TessBaseAPI with mutex for thread-safety
  - `ImagePreprocessor`: Image enhancement for better OCR accuracy (grayscale, thresholding, denoising)
- **`repository/`**: Repository implementations (ScanRepositoryImpl, LanguageRepositoryImpl, PreferencesRepositoryImpl)
- **`mapper/`**: Entity-to-domain model converters (ScanMapper)
- **`utils/`**: Utility classes (ImageCompressor, ImageEditor, StorageManager)

### Presentation Layer (`presentation/`)
UI layer built with Jetpack Compose and Material 3.

- **`ui/`**: Composable screens and components
  - `camera/`: Camera capture screen with live preview (CameraScreen, CameraPreview, DocumentOverlay)
  - `editor/`: Image editing functionality (ImageEditorScreen)
  - `ocr/`: OCR results display (OCRResultScreen)
  - `history/`: Scan history list (HistoryScreen with filtering/sorting)
  - `detail/`: Scan detail view (ScanDetailScreen)
  - `language/`: Language management (LanguageManagementScreen)
  - `settings/`: App settings (SettingsScreen)
  - `components/`: Reusable UI components (GridOverlay, HapticFeedback, CameraAnimations)
  - `theme/`: Material 3 theming (Color, Theme, Type)
- **`viewmodel/`**: ViewModels managing UI state
  - CameraViewModel, OCRViewModel, ImageEditorViewModel
  - HistoryViewModel, ScanDetailViewModel
  - LanguageViewModel, SettingsViewModel
- **`navigation/`**: Navigation routes (Screen sealed class)

### Dependency Injection (`di/`)
Hilt modules providing dependencies:
- **AppModule**: Application-level dependencies
- **DatabaseModule**: Room database and DAOs
- **RepositoryModule**: Repository implementations
- **CameraModule**: CameraX dependencies
- **OCRModule**: OCR service and image preprocessor

## Key Implementation Details

### OCR Service Thread Safety
`OCRServiceImpl` uses a `Mutex` to serialize all Tesseract operations, preventing race conditions when multiple coroutines attempt OCR simultaneously. The TessBaseAPI instance is shared and reused, but access is controlled through `tessMutex.withLock { }`.

### Tesseract Language Files
- Trained data files (.traineddata) must be in `{externalFilesDir}/tessdata/` or `{filesDir}/tessdata/`
- Files can be bundled in `app/src/main/assets/tessdata/` and are copied on first use
- Language availability is checked before initialization
- Languages can be downloaded at runtime via LanguageManagementScreen

### Image Processing Pipeline
1. **Capture/Select**: CameraX capture or gallery selection
2. **Optional Editing**: Crop, rotate, adjust contrast (ImageEditorScreen)
3. **Preprocessing**: Grayscale conversion, thresholding, noise removal (ImagePreprocessor)
4. **OCR Recognition**: Tesseract processing with progress tracking
5. **Storage**: Save to Room database with extracted text and metadata

### Navigation Flow
```
CameraScreen (start)
  ├─> ImageEditorScreen (gallery images) ─> OCRResultScreen
  ├─> OCRResultScreen (camera captures)
  ├─> HistoryScreen ─> ScanDetailScreen
  └─> SettingsScreen ─> LanguageManagementScreen
```

### State Management
- ViewModels expose `StateFlow` for UI state
- Use cases return `Result<T>` wrapper for success/error handling
- OCR operations can emit progress via `Flow<Result<OCRProgress>>`
- Room queries return `Flow` for reactive updates

## Testing Approach

Unit tests are located in `app/src/test/`. Current test coverage includes:
- Domain model tests (OCRConfigTest, OCRResultTest, ImageQualityTest)
- Mapper tests (ScanMapperTest)
- Repository tests (ScanRepositoryImplTest)

When writing new tests:
- Mock dependencies using test doubles
- Test use cases with fake repositories
- Test ViewModels with fake use cases
- Use JUnit 4 and kotlinx-coroutines-test for coroutine testing

## Important Conventions

### Commit Messages
Do **NOT** credit Claude in commit messages. Use standard conventional commit format without AI attribution or generated message footers.

### Coroutine Dispatchers
- Use `Dispatchers.IO` for OCR processing, file operations, and database access
- Use `Dispatchers.Default` for CPU-intensive image processing
- Use `Dispatchers.Main` for UI updates (automatically handled by ViewModels)

### Error Handling
- Domain layer uses `Result<T>` sealed class (Success, Error, Loading)
- Always handle `Result.Error` cases in ViewModels
- Provide user-friendly error messages in UI state

### Resource Management
- Call `OCRService.cleanup()` when done to release Tesseract resources
- Properly recycle Bitmaps after processing to prevent memory leaks
- Use `use { }` for file streams to ensure proper closure

### Hilt Annotations
- Application class: `@HiltAndroidApp`
- Activities: `@AndroidEntryPoint`
- ViewModels: `@HiltViewModel` with `@Inject constructor`
- Modules: `@Module @InstallIn(SingletonComponent::class)` or appropriate component

## Build Configuration

- **Min SDK**: 24 (Android 7.0)
- **Target SDK**: 35 (Android 15)
- **Compile SDK**: 35
- **Java Version**: 17
- **Kotlin Version**: 2.1.20
- **Compose Compiler**: Kotlin 2.1.20 plugin
- **ProGuard**: Enabled in release builds

## Dependencies to Know

- **androidx.compose.material3**: Material 3 components, prefer over material2
- **com.rmtheis:tess-two**: Tesseract OCR wrapper, uses native libraries
- **androidx.camera:camera-***: CameraX for image capture
- **androidx.room:room-***: Local database, use KSP for annotation processing
- **com.google.dagger:hilt-android**: DI framework, use KSP compiler
- **io.coil-kt:coil-compose**: Image loading library for Compose
- **com.google.accompanist:accompanist-permissions**: Runtime permission handling
