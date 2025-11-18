# GitHub Project Board: Tesseract 3 → 4 Migration

This file contains all tasks for creating a GitHub Project board to track the Tesseract migration (Issue #5).

## Quick Setup Instructions

### Option 1: Using GitHub Web UI (Recommended)

1. Go to your repository: `https://github.com/musagenius345/ocr-android-app`
2. Click **Projects** tab → **New project**
3. Choose **Board** template
4. Name it: `Tesseract 4 Migration`
5. Create the following columns:
   - 📋 **Backlog** (for all tasks initially)
   - 🏗️ **Phase 1: Foundation**
   - 🎯 **Phase 2: Domain**
   - 💾 **Phase 3: Data Layer**
   - 🔌 **Phase 4: DI & ViewModel**
   - 🎨 **Phase 5: Presentation**
   - 🔄 **Phase 6: Migration**
   - ✅ **Phase 7: Testing**
   - ✔️ **Done**
6. Create issues from the tasks below and add them to the project

### Option 2: Using GitHub CLI (If Available)

If you have `gh` CLI installed locally, run:

```bash
cd /path/to/ocr-android-app

# Create the project
gh project create "Tesseract 4 Migration" --owner musagenius345

# Create issues from this file (see script below)
```

---

## All Tasks Organized by Phase

### Phase 0: Pre-Migration Setup (Added Recommendations)

#### Task 0.1: Create Rollback Branch
**Priority:** 🔴 Critical
**Labels:** `setup`, `safety`
**Assignee:** Developer
**Description:**
Create a stable rollback point before starting migration.

**Acceptance Criteria:**
- [ ] Tag current version as `v1.0-tesseract3-stable`
- [ ] Create branch `backup/tesseract3-stable`
- [ ] Document rollback procedure in `ROLLBACK.md`
- [ ] Test that rollback works (checkout and build)

**Estimated Effort:** 1 hour

---

#### Task 0.2: Set Up Beta Testing Channel
**Priority:** 🔴 Critical
**Labels:** `setup`, `testing`
**Description:**
Prepare beta testing infrastructure for gradual rollout.

**Acceptance Criteria:**
- [ ] Create beta track in Google Play Console
- [ ] Set up internal testing group (10-20 users)
- [ ] Configure crash reporting (Firebase Crashlytics)
- [ ] Create beta feedback form

**Estimated Effort:** 2 hours

---

#### Task 0.3: Create Migration Feature Branch
**Priority:** 🔴 Critical
**Labels:** `setup`, `git`
**Description:**
Create dedicated branch for all migration work.

**Acceptance Criteria:**
- [ ] Create branch: `feature/tesseract-4-migration`
- [ ] Set up branch protection rules
- [ ] Configure CI/CD for feature branch
- [ ] Create PR template for migration tasks

**Estimated Effort:** 1 hour

---

### Phase 1: Foundation & Dependencies (8 hours)

#### Task 1.1: Remove tess-two Dependency
**Priority:** 🟡 High
**Labels:** `phase-1`, `dependencies`
**Description:**
Remove the old Tesseract 3 library from build configuration.

**Files to Modify:**
- `app/build.gradle.kts`

**Acceptance Criteria:**
- [ ] Remove line 85: `implementation("com.rmtheis:tess-two:9.1.0")`
- [ ] Verify project still compiles (with errors expected)
- [ ] Document removed dependency in CHANGELOG

**Estimated Effort:** 0.5 hours

**Related Tasks:** Task 1.2

---

#### Task 1.2: Add tesseract4android to Version Catalog
**Priority:** 🟡 High
**Labels:** `phase-1`, `dependencies`
**Description:**
Add the new Tesseract 4 library to Gradle version catalog.

**Files to Modify:**
- `gradle/libs.versions.toml`

**Acceptance Criteria:**
- [ ] Add version: `tesseract4Android = "4.9.0"`
- [ ] Add library: `tesseract4android-openmp = { group = "io.github.adaptech-cz", name = "tesseract4android-openmp", version.ref = "tesseract4Android" }`
- [ ] Verify TOML syntax is valid

**Acceptance Criteria Code:**
```toml
[versions]
tesseract4Android = "4.9.0"

[libraries]
tesseract4android-openmp = { group = "io.github.adaptech-cz", name = "tesseract4android-openmp", version.ref = "tesseract4Android" }
```

**Estimated Effort:** 0.5 hours

**Related Tasks:** Task 1.3

---

#### Task 1.3: Add tesseract4android Implementation
**Priority:** 🟡 High
**Labels:** `phase-1`, `dependencies`
**Description:**
Add the new library to app dependencies.

**Files to Modify:**
- `app/build.gradle.kts`

**Acceptance Criteria:**
- [ ] Add: `implementation(libs.tesseract4android.openmp)`
- [ ] Sync Gradle successfully
- [ ] Verify library is in dependencies tree
- [ ] Check for version conflicts

**Estimated Effort:** 0.5 hours

**Related Tasks:** Task 1.2

---

#### Task 1.4: Create Feature Module Structure
**Priority:** 🟡 High
**Labels:** `phase-1`, `architecture`
**Description:**
Create the new `feature/recognize-text/` module directory structure.

**Acceptance Criteria:**
- [ ] Create directory: `app/src/main/java/com/musagenius/ocrapp/feature/recognizetext/`
- [ ] Create subdirectories: `domain/`, `data/`, `presentation/`, `di/`
- [ ] Create subdirectories: `domain/models/`, `domain/usecases/`
- [ ] Create subdirectories: `data/preprocessing/`, `data/download/`
- [ ] Create subdirectories: `presentation/ui/`, `presentation/viewmodel/`
- [ ] Document structure in README

**Directory Structure:**
```
feature/recognizetext/
├── domain/
│   ├── models/
│   └── usecases/
├── data/
│   ├── preprocessing/
│   └── download/
├── presentation/
│   ├── ui/
│   └── viewmodel/
└── di/
```

**Estimated Effort:** 1 hour

---

#### Task 1.5: Move Existing OCR UI Screens
**Priority:** 🟢 Medium
**Labels:** `phase-1`, `refactoring`
**Description:**
Move existing UI screens to the new module structure.

**Files to Move:**
- `presentation/ui/ocr/OCRResultScreen.kt` → `feature/recognizetext/presentation/ui/`
- `presentation/ui/camera/CameraScreen.kt` → Keep in place (camera is separate feature)

**Acceptance Criteria:**
- [ ] Move `OCRResultScreen.kt` to new location
- [ ] Update package declarations
- [ ] Update all import statements in other files
- [ ] Verify app compiles and runs
- [ ] No broken navigation

**Estimated Effort:** 2 hours

---

#### Task 1.6: Update ProGuard Rules
**Priority:** 🟢 Medium
**Labels:** `phase-1`, `configuration`
**Description:**
Add ProGuard rules for tesseract4android to prevent issues in release builds.

**Files to Modify:**
- `app/proguard-rules.pro`

**Acceptance Criteria:**
- [ ] Add rules for tesseract4android classes
- [ ] Test release build
- [ ] Verify OCR works in release mode
- [ ] Document rules

**ProGuard Rules:**
```proguard
# Tesseract4Android
-keep class com.googlecode.tesseract.android.** { *; }
-keep class org.opencv.** { *; }
-dontwarn com.googlecode.tesseract.android.**
```

**Estimated Effort:** 1 hour

---

#### Task 1.7: Update NDK ABI Filters
**Priority:** 🟢 Medium
**Labels:** `phase-1`, `configuration`
**Description:**
Verify NDK ABI filters are compatible with tesseract4android.

**Files to Modify:**
- `app/build.gradle.kts`

**Acceptance Criteria:**
- [ ] Review current ABI filters (line 27)
- [ ] Verify tesseract4android supports all ABIs
- [ ] Test on different architectures
- [ ] Document supported ABIs

**Current ABIs:**
```kotlin
abiFilters += listOf("armeabi-v7a", "arm64-v8a", "x86", "x86_64")
```

**Estimated Effort:** 1 hour

---

#### Task 1.8: Create Migration Tracking Document
**Priority:** 🟢 Medium
**Labels:** `phase-1`, `documentation`
**Description:**
Create document to track migration progress and decisions.

**Acceptance Criteria:**
- [ ] Create `MIGRATION-LOG.md`
- [ ] Document initial state (Tesseract 3 version, dependencies)
- [ ] Create decision log section
- [ ] Create blockers section
- [ ] Link to this project board

**Estimated Effort:** 1 hour

---

### Phase 2: Domain Layer Implementation (16 hours)

#### Task 2.1: Create ImageTextReader Interface
**Priority:** 🔴 Critical
**Labels:** `phase-2`, `domain`, `interface`
**Description:**
Create the main service interface replacing OCRService.

**Files to Create:**
- `feature/recognizetext/domain/ImageTextReader.kt`

**Acceptance Criteria:**
- [ ] Define `getTextFromImage(bitmap: Bitmap, params: TessParams): Result<TextRecognitionResult>`
- [ ] Define `downloadTrainingData(language: OCRLanguage, quality: RecognitionType): Flow<DownloadProgress>`
- [ ] Define `getLanguages(): List<OCRLanguage>`
- [ ] Define `deleteLanguage(language: OCRLanguage, quality: RecognitionType): Result<Unit>`
- [ ] Add KDoc documentation
- [ ] Follow naming conventions

**Interface Skeleton:**
```kotlin
interface ImageTextReader {
    suspend fun getTextFromImage(
        bitmap: Bitmap,
        params: TessParams
    ): Result<TextRecognitionResult>

    fun downloadTrainingData(
        language: OCRLanguage,
        quality: RecognitionType
    ): Flow<DownloadProgress>

    suspend fun getLanguages(): List<OCRLanguage>

    suspend fun deleteLanguage(
        language: OCRLanguage,
        quality: RecognitionType
    ): Result<Unit>
}
```

**Estimated Effort:** 2 hours

---

#### Task 2.2: Create OcrEngineMode Enum
**Priority:** 🟡 High
**Labels:** `phase-2`, `domain`, `model`
**Description:**
Define OCR engine mode options for Tesseract 4.

**Files to Create:**
- `feature/recognizetext/domain/models/OcrEngineMode.kt`

**Acceptance Criteria:**
- [ ] Define enum values: `TESSERACT_ONLY`, `LSTM_ONLY`, `TESSERACT_LSTM_COMBINED`, `DEFAULT`
- [ ] Map to Tesseract API values
- [ ] Add descriptions
- [ ] Write unit tests

**Enum Definition:**
```kotlin
enum class OcrEngineMode(val value: Int) {
    TESSERACT_ONLY(0),           // Legacy engine only
    LSTM_ONLY(1),                 // Neural nets LSTM engine only
    TESSERACT_LSTM_COMBINED(2),  // Both engines
    DEFAULT(3)                    // Based on what is available
}
```

**Estimated Effort:** 1 hour

---

#### Task 2.3: Create RecognitionType Enum
**Priority:** 🔴 Critical
**Labels:** `phase-2`, `domain`, `model`
**Description:**
Define quality tiers for OCR models (Fast/Standard/Best).

**Files to Create:**
- `feature/recognizetext/domain/models/RecognitionType.kt`

**Acceptance Criteria:**
- [ ] Define enum: `FAST`, `STANDARD`, `BEST`
- [ ] Map to GitHub repo URLs
- [ ] Map to file system paths
- [ ] Add model size estimates
- [ ] Add performance characteristics

**Enum Definition:**
```kotlin
enum class RecognitionType(
    val displayName: String,
    val repoName: String,
    val relativePath: String,
    val estimatedSizeMB: Int
) {
    FAST(
        displayName = "Fast",
        repoName = "tessdata_fast",
        relativePath = "fast/tessdata",
        estimatedSizeMB = 2
    ),
    STANDARD(
        displayName = "Standard",
        repoName = "tessdata",
        relativePath = "standard/tessdata",
        estimatedSizeMB = 10
    ),
    BEST(
        displayName = "Best",
        repoName = "tessdata_best",
        relativePath = "best/tessdata",
        estimatedSizeMB = 20
    )
}
```

**Estimated Effort:** 2 hours

---

#### Task 2.4: Create SegmentationMode Enum
**Priority:** 🟡 High
**Labels:** `phase-2`, `domain`, `model`
**Description:**
Define page segmentation modes (same as current PageSegMode).

**Files to Create:**
- `feature/recognizetext/domain/models/SegmentationMode.kt`

**Acceptance Criteria:**
- [ ] Copy all modes from current `PageSegMode` enum
- [ ] Update to match Tesseract 4 API
- [ ] Add usage recommendations
- [ ] Write migration guide from old enum

**Estimated Effort:** 1 hour

**Related Tasks:** Task 2.8 (Deprecate OCRConfig)

---

#### Task 2.5: Create OCRLanguage Data Class
**Priority:** 🟡 High
**Labels:** `phase-2`, `domain`, `model`
**Description:**
Define language model with metadata.

**Files to Create:**
- `feature/recognizetext/domain/models/OCRLanguage.kt`

**Acceptance Criteria:**
- [ ] Define properties: `code`, `name`, `isDownloaded`, `downloadedQualities`
- [ ] Create companion object with predefined languages
- [ ] Add function to get download URL
- [ ] Support 100+ languages

**Data Class:**
```kotlin
data class OCRLanguage(
    val code: String,           // e.g., "eng"
    val name: String,           // e.g., "English"
    val isDownloaded: Boolean = false,
    val downloadedQualities: Set<RecognitionType> = emptySet()
) {
    companion object {
        val ENGLISH = OCRLanguage("eng", "English")
        val SPANISH = OCRLanguage("spa", "Spanish")
        // ... 100+ languages

        val ALL_LANGUAGES = listOf(ENGLISH, SPANISH, /* ... */)
    }

    fun getDownloadUrl(quality: RecognitionType): String {
        return "https://github.com/tesseract-ocr/${quality.repoName}/raw/main/${code}.traineddata"
    }
}
```

**Estimated Effort:** 3 hours

---

#### Task 2.6: Create TextRecognitionResult Data Class
**Priority:** 🔴 Critical
**Labels:** `phase-2`, `domain`, `model`
**Description:**
Define OCR result model (replaces OCRResult).

**Files to Create:**
- `feature/recognizetext/domain/models/TextRecognitionResult.kt`

**Acceptance Criteria:**
- [ ] Define properties matching OCRResult
- [ ] Add new properties for Tesseract 4 features
- [ ] Include confidence per word (optional)
- [ ] Write migration guide from OCRResult

**Data Class:**
```kotlin
data class TextRecognitionResult(
    val text: String,
    val confidence: Float,                      // 0.0 to 1.0
    val processingTimeMs: Long,
    val language: String,
    val recognitionType: RecognitionType,
    val wordCount: Int = text.split("\\s+").size,
    val characterCount: Int = text.length,
    val wordConfidences: Map<String, Float>? = null  // New in v4
)
```

**Estimated Effort:** 2 hours

---

#### Task 2.7: Create TessParams Data Class
**Priority:** 🔴 Critical
**Labels:** `phase-2`, `domain`, `model`
**Description:**
Define comprehensive OCR configuration (replaces OCRConfig).

**Files to Create:**
- `feature/recognizetext/domain/models/TessParams.kt`
- `feature/recognizetext/domain/models/TessConstants.kt`

**Acceptance Criteria:**
- [ ] Define all configuration parameters
- [ ] Provide sensible defaults
- [ ] Support all Tesseract 4 options
- [ ] Add builder pattern (optional)

**Data Class:**
```kotlin
data class TessParams(
    val language: String = "eng",
    val recognitionType: RecognitionType = RecognitionType.FAST,
    val engineMode: OcrEngineMode = OcrEngineMode.LSTM_ONLY,
    val segmentationMode: SegmentationMode = SegmentationMode.AUTO,
    val preprocessImage: Boolean = true,
    val maxImageDimension: Int = 2000,
    val variables: Map<String, String> = emptyMap()  // Tesseract variables
)
```

**Estimated Effort:** 2 hours

---

#### Task 2.8: Deprecate Old Domain Models
**Priority:** 🟡 High
**Labels:** `phase-2`, `deprecation`
**Description:**
Mark old OCR models as deprecated with migration instructions.

**Files to Modify:**
- `domain/model/OCRConfig.kt`
- `domain/model/OCRResult.kt`
- `domain/model/OCRProgress.kt`

**Acceptance Criteria:**
- [ ] Add `@Deprecated` annotation to each class
- [ ] Add migration message pointing to new classes
- [ ] Add `ReplaceWith` suggestions
- [ ] Update existing usages to suppress warnings

**Example:**
```kotlin
@Deprecated(
    message = "Use TessParams instead",
    replaceWith = ReplaceWith(
        expression = "TessParams(language, segmentationMode, engineMode, preprocessImage, maxImageDimension)",
        imports = ["com.musagenius.ocrapp.feature.recognizetext.domain.models.TessParams"]
    ),
    level = DeprecationLevel.WARNING
)
data class OCRConfig(...)
```

**Estimated Effort:** 2 hours

---

#### Task 2.9: Create DownloadProgress Data Class
**Priority:** 🟡 High
**Labels:** `phase-2`, `domain`, `model`
**Description:**
Define progress tracking for language downloads.

**Files to Create:**
- `feature/recognizetext/domain/models/DownloadProgress.kt`

**Acceptance Criteria:**
- [ ] Track bytes downloaded and total
- [ ] Calculate percentage
- [ ] Include download status enum
- [ ] Support error states

**Data Class:**
```kotlin
data class DownloadProgress(
    val bytesDownloaded: Long,
    val totalBytes: Long,
    val percentage: Float = (bytesDownloaded.toFloat() / totalBytes * 100).coerceIn(0f, 100f),
    val status: DownloadStatus,
    val error: String? = null
)

enum class DownloadStatus {
    PENDING,
    DOWNLOADING,
    VERIFYING,
    COMPLETED,
    FAILED,
    CANCELLED
}
```

**Estimated Effort:** 1 hour

---

### Phase 3: Data Layer Refactoring (40 hours - HIGHEST EFFORT)

#### Task 3.1: Create ThreadSafeTessAPI Wrapper
**Priority:** 🔴 Critical
**Labels:** `phase-3`, `data`, `thread-safety`
**Description:**
Create wrapper class to enforce mutex-based thread safety.

**Files to Create:**
- `feature/recognizetext/data/ThreadSafeTessAPI.kt`

**Acceptance Criteria:**
- [ ] Inject Mutex for serialization
- [ ] Wrap all TessBaseAPI operations
- [ ] Prevent direct TessBaseAPI access
- [ ] Write thread-safety tests
- [ ] Document why mutex is needed

**Implementation:**
```kotlin
@Singleton
class ThreadSafeTessAPI @Inject constructor() {
    private val mutex = Mutex()
    private var api: TessBaseAPI? = null

    suspend fun <T> withApi(block: suspend (TessBaseAPI) -> T): T =
        mutex.withLock {
            val apiInstance = api ?: error("TessBaseAPI not initialized")
            block(apiInstance)
        }

    suspend fun initialize(dataPath: String, language: String): Result<Unit> =
        mutex.withLock {
            // Initialize TessBaseAPI
        }

    suspend fun cleanup() = mutex.withLock {
        api?.end()
        api = null
    }
}
```

**Estimated Effort:** 4 hours

---

#### Task 3.2: Create AndroidImageTextReader Implementation
**Priority:** 🔴 Critical
**Labels:** `phase-3`, `data`, `core`
**Description:**
Implement ImageTextReader interface using tesseract4android.

**Files to Create:**
- `feature/recognizetext/data/AndroidImageTextReader.kt`

**Acceptance Criteria:**
- [ ] Implement all interface methods
- [ ] Use ThreadSafeTessAPI for all TessBaseAPI calls
- [ ] Handle initialization with new file paths
- [ ] Map TessParams to Tesseract API
- [ ] Implement proper error handling
- [ ] Add logging

**Key Methods:**
```kotlin
@Singleton
class AndroidImageTextReader @Inject constructor(
    private val context: Context,
    private val tessAPI: ThreadSafeTessAPI,
    private val imagePreprocessor: ImagePreprocessor
) : ImageTextReader {

    override suspend fun getTextFromImage(
        bitmap: Bitmap,
        params: TessParams
    ): Result<TextRecognitionResult> = withContext(Dispatchers.Default) {
        // Implementation using tessAPI.withApi { }
    }

    // ... other methods
}
```

**Estimated Effort:** 8 hours

---

#### Task 3.3: Implement getTextFromImage Method
**Priority:** 🔴 Critical
**Labels:** `phase-3`, `data`, `core`
**Description:**
Core OCR recognition method.

**Files to Modify:**
- `feature/recognizetext/data/AndroidImageTextReader.kt`

**Acceptance Criteria:**
- [ ] Initialize TessBaseAPI with correct data path
- [ ] Set page segmentation mode
- [ ] Set engine mode
- [ ] Preprocess image if enabled
- [ ] Perform OCR and extract text
- [ ] Get confidence score
- [ ] Get word-level confidences (if available)
- [ ] Return TextRecognitionResult

**Estimated Effort:** 6 hours

---

#### Task 3.4: Create LanguageDownloadManager
**Priority:** 🔴 Critical
**Labels:** `phase-3`, `data`, `download`
**Description:**
Implement language file download manager.

**Files to Create:**
- `feature/recognizetext/data/download/LanguageDownloadManager.kt`

**Acceptance Criteria:**
- [ ] Download from GitHub URLs
- [ ] Support all RecognitionType qualities
- [ ] Emit download progress via Flow
- [ ] Save to correct internal storage path
- [ ] Handle network errors with retry
- [ ] Verify file integrity (checksum)

**Interface:**
```kotlin
interface LanguageDownloadManager {
    fun downloadLanguage(
        language: OCRLanguage,
        quality: RecognitionType
    ): Flow<DownloadProgress>

    suspend fun cancelDownload(language: OCRLanguage, quality: RecognitionType)
}
```

**Estimated Effort:** 8 hours

---

#### Task 3.5: Implement Download with Retry Logic
**Priority:** 🔴 Critical
**Labels:** `phase-3`, `data`, `download`
**Description:**
Add robust network error handling and retry mechanism.

**Files to Modify:**
- `feature/recognizetext/data/download/LanguageDownloadManager.kt`

**Acceptance Criteria:**
- [ ] Implement exponential backoff (2s, 4s, 8s, 16s)
- [ ] Max 4 retry attempts
- [ ] Distinguish retryable vs non-retryable errors
- [ ] Handle rate limiting (HTTP 429)
- [ ] Handle network unavailable
- [ ] Emit error states to UI

**Estimated Effort:** 4 hours

---

#### Task 3.6: Implement File Storage Management
**Priority:** 🟡 High
**Labels:** `phase-3`, `data`, `storage`
**Description:**
Manage multi-tier storage structure for language files.

**Files to Create:**
- `feature/recognizetext/data/storage/StorageManager.kt`

**Acceptance Criteria:**
- [ ] Create directory structure: `{filesDir}/{quality}/tessdata/`
- [ ] Check if language is downloaded for specific quality
- [ ] Get file path for language + quality combination
- [ ] Delete language files
- [ ] Calculate storage usage
- [ ] Check available storage before download

**Directory Structure:**
```
{app.filesDir}/
├── fast/tessdata/
│   ├── eng.traineddata
│   └── spa.traineddata
├── standard/tessdata/
│   └── eng.traineddata
└── best/tessdata/
    └── eng.traineddata
```

**Estimated Effort:** 3 hours

---

#### Task 3.7: Implement getLanguages Method
**Priority:** 🟡 High
**Labels:** `phase-3`, `data`, `core`
**Description:**
List installed languages across all quality tiers.

**Files to Modify:**
- `feature/recognizetext/data/AndroidImageTextReader.kt`

**Acceptance Criteria:**
- [ ] Scan all quality directories
- [ ] Build OCRLanguage objects with download status
- [ ] Mark which qualities are downloaded per language
- [ ] Return comprehensive list
- [ ] Cache results (invalidate on download/delete)

**Estimated Effort:** 3 hours

---

#### Task 3.8: Create Filter-Based Preprocessing Chain
**Priority:** 🟢 Medium
**Labels:** `phase-3`, `data`, `preprocessing`
**Description:**
Implement new filter chain architecture for image preprocessing.

**Files to Create:**
- `feature/recognizetext/data/preprocessing/ImageFilter.kt`
- `feature/recognizetext/data/preprocessing/ContrastFilter.kt`
- `feature/recognizetext/data/preprocessing/SharpenFilter.kt`
- `feature/recognizetext/data/preprocessing/ThresholdFilter.kt`
- `feature/recognizetext/data/preprocessing/FilterChain.kt`

**Acceptance Criteria:**
- [ ] Define ImageFilter interface
- [ ] Implement Contrast filter
- [ ] Implement Sharpen filter
- [ ] Implement Threshold filter
- [ ] Create FilterChain to compose filters
- [ ] Provide preset chains

**Interface:**
```kotlin
interface ImageFilter {
    fun apply(bitmap: Bitmap): Bitmap
}

class FilterChain(private val filters: List<ImageFilter>) {
    fun process(bitmap: Bitmap): Bitmap =
        filters.fold(bitmap) { acc, filter -> filter.apply(acc) }
}
```

**Estimated Effort:** 4 hours

---

#### Task 3.9: Port ImagePreprocessor to Filter Chain
**Priority:** 🟢 Medium
**Labels:** `phase-3`, `data`, `preprocessing`
**Description:**
Convert existing ImagePreprocessor logic to new filter system.

**Files to Modify:**
- `data/ocr/ImagePreprocessor.kt` (keep for backward compatibility)
- Create new filters based on existing logic

**Acceptance Criteria:**
- [ ] Create GrayscaleFilter from existing code
- [ ] Create ContrastEnhancementFilter from existing code
- [ ] Create ScaleFilter from existing code
- [ ] Provide default filter chain matching old behavior
- [ ] Verify results are identical

**Estimated Effort:** 3 hours

---

#### Task 3.10: Implement SHA256 Checksum Verification
**Priority:** 🟡 High
**Labels:** `phase-3`, `data`, `download`, `security`
**Description:**
Verify downloaded files are not corrupt.

**Files to Modify:**
- `feature/recognizetext/data/download/LanguageDownloadManager.kt`

**Acceptance Criteria:**
- [ ] Calculate SHA256 hash of downloaded file
- [ ] Compare with known good hash (if available)
- [ ] Delete and re-download if mismatch
- [ ] Log verification results
- [ ] Handle missing checksum gracefully

**Note:** GitHub doesn't provide checksums by default, may need to maintain our own checksum file.

**Estimated Effort:** 3 hours

---

### Phase 4: DI & ViewModel Migration (16 hours)

#### Task 4.1: Create RecognizeTextModule
**Priority:** 🔴 Critical
**Labels:** `phase-4`, `di`, `hilt`
**Description:**
Create Hilt module for new components.

**Files to Create:**
- `feature/recognizetext/di/RecognizeTextModule.kt`

**Acceptance Criteria:**
- [ ] Provide ImageTextReader binding
- [ ] Provide ThreadSafeTessAPI singleton
- [ ] Provide LanguageDownloadManager
- [ ] Provide StorageManager
- [ ] Provide FilterChain with default filters

**Module:**
```kotlin
@Module
@InstallIn(SingletonComponent::class)
abstract class RecognizeTextModule {

    @Binds
    @Singleton
    abstract fun bindImageTextReader(
        impl: AndroidImageTextReader
    ): ImageTextReader

    // ... other bindings
}
```

**Estimated Effort:** 2 hours

---

#### Task 4.2: Remove Old OCRModule Binding
**Priority:** 🟡 High
**Labels:** `phase-4`, `di`, `cleanup`
**Description:**
Remove or deprecate old OCRService binding.

**Files to Modify:**
- `di/OCRModule.kt`

**Acceptance Criteria:**
- [ ] Comment out OCRService binding (or remove if no usages)
- [ ] Add deprecation notice
- [ ] Update documentation
- [ ] Verify DI graph is valid

**Estimated Effort:** 1 hour

---

#### Task 4.3: Refactor OCRViewModel - Part 1 (State)
**Priority:** 🔴 Critical
**Labels:** `phase-4`, `viewmodel`, `state`
**Description:**
Update ViewModel state to use new models.

**Files to Modify:**
- `presentation/viewmodel/OCRViewModel.kt`

**Acceptance Criteria:**
- [ ] Update state to hold TessParams instead of OCRConfig
- [ ] Add state for available languages
- [ ] Add state for download progress
- [ ] Add state for selected quality tier
- [ ] Keep backward compatible state (temporarily)

**New State:**
```kotlin
data class OCRState(
    val isProcessing: Boolean = false,
    val result: TextRecognitionResult? = null,
    val error: String? = null,
    val params: TessParams = TessParams(),
    val availableLanguages: List<OCRLanguage> = emptyList(),
    val downloadProgress: Map<String, DownloadProgress> = emptyMap(),
    val selectedQuality: RecognitionType = RecognitionType.FAST
)
```

**Estimated Effort:** 3 hours

---

#### Task 4.4: Refactor OCRViewModel - Part 2 (Inject ImageTextReader)
**Priority:** 🔴 Critical
**Labels:** `phase-4`, `viewmodel`, `di`
**Description:**
Replace OCRService injection with ImageTextReader.

**Files to Modify:**
- `presentation/viewmodel/OCRViewModel.kt`

**Acceptance Criteria:**
- [ ] Change constructor parameter from OCRService to ImageTextReader
- [ ] Remove old service usages
- [ ] Update all method calls to use new interface
- [ ] Verify ViewModel still compiles

**Estimated Effort:** 2 hours

---

#### Task 4.5: Implement Language Download in ViewModel
**Priority:** 🟡 High
**Labels:** `phase-4`, `viewmodel`, `feature`
**Description:**
Add language download management to ViewModel.

**Files to Modify:**
- `presentation/viewmodel/OCRViewModel.kt`

**Acceptance Criteria:**
- [ ] Add `downloadLanguage(language, quality)` method
- [ ] Collect download progress Flow
- [ ] Update state with progress
- [ ] Handle download errors
- [ ] Notify UI on completion

**Method:**
```kotlin
fun downloadLanguage(language: OCRLanguage, quality: RecognitionType) {
    viewModelScope.launch {
        imageTextReader.downloadTrainingData(language, quality)
            .collect { progress ->
                updateDownloadProgress(language.code, progress)
            }
    }
}
```

**Estimated Effort:** 3 hours

---

#### Task 4.6: Update processImage Method
**Priority:** 🔴 Critical
**Labels:** `phase-4`, `viewmodel`, `core`
**Description:**
Update OCR processing method to use new API.

**Files to Modify:**
- `presentation/viewmodel/OCRViewModel.kt`

**Acceptance Criteria:**
- [ ] Call imageTextReader.getTextFromImage() instead of ocrService.recognizeText()
- [ ] Pass TessParams instead of OCRConfig
- [ ] Handle TextRecognitionResult instead of OCRResult
- [ ] Update error handling
- [ ] Maintain progress tracking

**Estimated Effort:** 3 hours

---

#### Task 4.7: Add Language Management Methods
**Priority:** 🟡 High
**Labels:** `phase-4`, `viewmodel`, `feature`
**Description:**
Add methods to manage installed languages.

**Files to Modify:**
- `presentation/viewmodel/OCRViewModel.kt`

**Acceptance Criteria:**
- [ ] Add `loadAvailableLanguages()` method
- [ ] Add `deleteLanguage(language, quality)` method
- [ ] Add `isLanguageDownloaded(language, quality)` method
- [ ] Update state when languages change
- [ ] Handle errors

**Estimated Effort:** 2 hours

---

### Phase 5: Presentation Layer (24 hours)

#### Task 5.1: Create LanguageSelector Component
**Priority:** 🔴 Critical
**Labels:** `phase-5`, `ui`, `compose`
**Description:**
Create UI for selecting and downloading languages.

**Files to Create:**
- `feature/recognizetext/presentation/ui/LanguageSelector.kt`

**Acceptance Criteria:**
- [ ] Display list of all available languages
- [ ] Show download status per quality tier
- [ ] Show download button for uninstalled languages
- [ ] Show progress bar during download
- [ ] Show delete button for installed languages
- [ ] Filter/search functionality
- [ ] Follow Material 3 design

**UI Components:**
- LazyColumn with language items
- Each item shows: name, size, download/delete button
- Progress indicator during download

**Estimated Effort:** 6 hours

---

#### Task 5.2: Create RecognitionTypeSelector Component
**Priority:** 🟡 High
**Labels:** `phase-5`, `ui`, `compose`
**Description:**
UI component to select quality tier (Fast/Standard/Best).

**Files to Create:**
- `feature/recognizetext/presentation/ui/RecognitionTypeSelector.kt`

**Acceptance Criteria:**
- [ ] Display 3 options: Fast, Standard, Best
- [ ] Show characteristics of each (speed, accuracy, size)
- [ ] Highlight selected option
- [ ] Update ViewModel state on selection
- [ ] Show which languages are downloaded for selected quality

**UI Design:**
```
┌─────────────────────────────────────┐
│ Recognition Quality                 │
├─────────────────────────────────────┤
│ ○ Fast        1-3 MB   ⚡ Fastest   │
│ ● Standard   5-15 MB   ⚖️ Balanced  │
│ ○ Best      10-30 MB   🎯 Accurate  │
└─────────────────────────────────────┘
```

**Estimated Effort:** 3 hours

---

#### Task 5.3: Create OcrEngineModeSelector Component
**Priority:** 🟢 Medium
**Labels:** `phase-5`, `ui`, `compose`
**Description:**
UI to select OCR engine mode (advanced setting).

**Files to Create:**
- `feature/recognizetext/presentation/ui/OcrEngineModeSelector.kt`

**Acceptance Criteria:**
- [ ] Display all engine modes
- [ ] Show description for each mode
- [ ] Default to LSTM_ONLY (recommended)
- [ ] Update params in ViewModel
- [ ] Add tooltips explaining modes

**Estimated Effort:** 2 hours

---

#### Task 5.4: Create SegmentationModeSelector Component
**Priority:** 🟢 Medium
**Labels:** `phase-5`, `ui`, `compose`
**Description:**
UI to select page segmentation mode (advanced setting).

**Files to Create:**
- `feature/recognizetext/presentation/ui/SegmentationModeSelector.kt`

**Acceptance Criteria:**
- [ ] Display common segmentation modes (AUTO, SINGLE_BLOCK, etc.)
- [ ] Show use case for each mode
- [ ] Organize in dropdown or expandable list
- [ ] Update params in ViewModel

**Estimated Effort:** 2 hours

---

#### Task 5.5: Create TessParamsSelector Component
**Priority:** 🟢 Low
**Labels:** `phase-5`, `ui`, `compose`
**Description:**
Advanced settings panel for all OCR parameters.

**Files to Create:**
- `feature/recognizetext/presentation/ui/TessParamsSelector.kt`

**Acceptance Criteria:**
- [ ] Combine all parameter selectors
- [ ] Add preprocessing toggle
- [ ] Add max image dimension slider
- [ ] Add custom variables input (advanced)
- [ ] Save preferences to DataStore

**Estimated Effort:** 4 hours

---

#### Task 5.6: Integrate Selectors into Settings Screen
**Priority:** 🟡 High
**Labels:** `phase-5`, `ui`, `integration`
**Description:**
Add new components to existing settings or create settings screen.

**Files to Modify:**
- Create `presentation/ui/settings/SettingsScreen.kt` (if doesn't exist)
- Update navigation

**Acceptance Criteria:**
- [ ] Create "OCR Settings" section
- [ ] Add LanguageSelector
- [ ] Add RecognitionTypeSelector
- [ ] Add advanced settings expandable
- [ ] Wire to ViewModel
- [ ] Add navigation from main screen

**Estimated Effort:** 3 hours

---

#### Task 5.7: Update OCRResultScreen
**Priority:** 🟡 High
**Labels:** `phase-5`, `ui`, `refactoring`
**Description:**
Update result screen to use new models and show quality info.

**Files to Modify:**
- `feature/recognizetext/presentation/ui/OCRResultScreen.kt`

**Acceptance Criteria:**
- [ ] Update to receive TextRecognitionResult instead of OCRResult
- [ ] Show recognition quality used
- [ ] Show word-level confidences (if available)
- [ ] Update stats display
- [ ] Keep existing copy/share functionality

**Estimated Effort:** 2 hours

---

#### Task 5.8: Create First-Run Language Setup Flow
**Priority:** 🟡 High
**Labels:** `phase-5`, `ui`, `onboarding`
**Description:**
Guide new users through downloading their first language.

**Files to Create:**
- `feature/recognizetext/presentation/ui/FirstRunSetup.kt`

**Acceptance Criteria:**
- [ ] Detect first run (no languages downloaded)
- [ ] Show welcome dialog
- [ ] Suggest downloading English (Fast)
- [ ] Show download progress
- [ ] Navigate to main screen on completion
- [ ] Don't show again after first language downloaded

**Estimated Effort:** 2 hours

---

### Phase 6: Data & Storage Migration (16 hours)

#### Task 6.1: Create TesseractMigrationManager
**Priority:** 🔴 Critical
**Labels:** `phase-6`, `migration`, `data`
**Description:**
Implement migration logic from old to new storage structure.

**Files to Create:**
- `feature/recognizetext/data/migration/TesseractMigrationManager.kt`

**Acceptance Criteria:**
- [ ] Check if migration already completed (DataStore flag)
- [ ] Detect old tessdata directory
- [ ] Scan for installed languages
- [ ] Return list of previously installed languages
- [ ] Mark migration as completed
- [ ] Handle edge cases (no old data, already migrated)

**Estimated Effort:** 4 hours

---

#### Task 6.2: Implement Old Directory Cleanup
**Priority:** 🔴 Critical
**Labels:** `phase-6`, `migration`, `cleanup`
**Description:**
Delete old tessdata directory after user confirmation.

**Files to Modify:**
- `feature/recognizetext/data/migration/TesseractMigrationManager.kt`

**Acceptance Criteria:**
- [ ] Delete `{externalFilesDir}/tessdata/` recursively
- [ ] Verify deletion succeeded
- [ ] Log space freed
- [ ] Handle permission errors
- [ ] Don't delete if migration not confirmed

**Estimated Effort:** 2 hours

---

#### Task 6.3: Create Migration Dialog UI
**Priority:** 🔴 Critical
**Labels:** `phase-6`, `ui`, `migration`
**Description:**
Design and implement user-facing migration dialog.

**Files to Create:**
- `feature/recognizetext/presentation/ui/MigrationDialog.kt`

**Acceptance Criteria:**
- [ ] Show migration notice
- [ ] List previously installed languages
- [ ] Explain why re-download is needed
- [ ] Mention quality tier options
- [ ] "Go to Language Manager" button
- [ ] "Dismiss" button
- [ ] Don't show again after dismissed

**Dialog Design:**
```
┌─────────────────────────────────────────┐
│   🎉 OCR Engine Upgraded to v4!         │
├─────────────────────────────────────────┤
│                                          │
│  We've upgraded to Tesseract 4 for      │
│  better accuracy and performance.       │
│                                          │
│  Previously installed languages:        │
│  • English                               │
│  • Spanish                               │
│                                          │
│  Please re-download these languages      │
│  in the new Language Manager.            │
│                                          │
│  Note: You can now choose between        │
│  Fast, Standard, and Best quality!      │
│                                          │
│  [Go to Language Manager]  [Dismiss]    │
└─────────────────────────────────────────┘
```

**Estimated Effort:** 3 hours

---

#### Task 6.4: Integrate Migration Check in App Startup
**Priority:** 🔴 Critical
**Labels:** `phase-6`, `app`, `migration`
**Description:**
Add migration check to Application.onCreate or main activity.

**Files to Modify:**
- `OCRApplication.kt` or create new `MigrationInitializer`

**Acceptance Criteria:**
- [ ] Check migration status on app start
- [ ] If not migrated and old data exists, show dialog
- [ ] Don't block app startup (async check)
- [ ] Handle migration in background
- [ ] Show dialog on next screen navigation

**Estimated Effort:** 3 hours

---

#### Task 6.5: Remove Old Traineddata from Assets
**Priority:** 🟡 High
**Labels:** `phase-6`, `cleanup`, `assets`
**Description:**
Delete bundled .traineddata files from assets folder.

**Files to Delete:**
- All files in `app/src/main/assets/tessdata/`

**Acceptance Criteria:**
- [ ] Delete tessdata directory from assets
- [ ] Verify APK size reduction
- [ ] Update documentation
- [ ] Remove references to bundled assets

**Expected Impact:** -5 to -50 MB APK size reduction

**Estimated Effort:** 1 hour

---

#### Task 6.6: Create Migration Documentation
**Priority:** 🟢 Medium
**Labels:** `phase-6`, `documentation`
**Description:**
Document migration process for users and developers.

**Files to Create:**
- `MIGRATION_GUIDE.md` (user-facing)
- Update `README.md`

**Acceptance Criteria:**
- [ ] Explain why migration is needed
- [ ] Step-by-step user guide
- [ ] FAQ section
- [ ] Troubleshooting tips
- [ ] Developer notes on migration logic

**Estimated Effort:** 3 hours

---

### Phase 7: Testing & Finalization (40 hours)

#### Task 7.1: Write AndroidImageTextReader Unit Tests
**Priority:** 🔴 Critical
**Labels:** `phase-7`, `testing`, `unit-test`
**Description:**
Comprehensive unit tests for core OCR implementation.

**Files to Create:**
- `app/src/test/java/...AndroidImageTextReaderTest.kt`

**Test Cases:**
- [ ] Test initialization with different languages
- [ ] Test getTextFromImage with mock bitmap
- [ ] Test parameter mapping (TessParams → Tesseract API)
- [ ] Test error handling (initialization failure, etc.)
- [ ] Test cleanup

**Estimated Effort:** 6 hours

---

#### Task 7.2: Write Thread Safety Tests
**Priority:** 🔴 Critical
**Labels:** `phase-7`, `testing`, `concurrency`
**Description:**
Verify mutex prevents concurrent TessBaseAPI access.

**Files to Create:**
- `app/src/test/java/...ThreadSafetyTest.kt`

**Test Cases:**
- [ ] Test concurrent OCR requests (10+ simultaneous)
- [ ] Test queued requests complete successfully
- [ ] Test cancellation doesn't affect other requests
- [ ] Stress test with 100 consecutive operations
- [ ] Verify no deadlocks

**Estimated Effort:** 6 hours

---

#### Task 7.3: Write Download Manager Tests
**Priority:** 🔴 Critical
**Labels:** `phase-7`, `testing`, `unit-test`
**Description:**
Test download functionality and error handling.

**Files to Create:**
- `app/src/test/java/...LanguageDownloadManagerTest.kt`

**Test Cases:**
- [ ] Test successful download
- [ ] Test download progress emissions
- [ ] Test network error triggers retry
- [ ] Test exponential backoff timing
- [ ] Test rate limiting handling
- [ ] Test corrupt file detection
- [ ] Test concurrent downloads queued
- [ ] Test download cancellation

**Estimated Effort:** 6 hours

---

#### Task 7.4: Write Migration Tests
**Priority:** 🟡 High
**Labels:** `phase-7`, `testing`, `unit-test`
**Description:**
Test data migration logic.

**Files to Create:**
- `app/src/test/java/...TesseractMigrationManagerTest.kt`

**Test Cases:**
- [ ] Test old directory detection
- [ ] Test old files deletion
- [ ] Test migration runs only once
- [ ] Test handles missing old directory
- [ ] Test migration status persistence

**Estimated Effort:** 4 hours

---

#### Task 7.5: Write Configuration Mapping Tests
**Priority:** 🟡 High
**Labels:** `phase-7`, `testing`, `unit-test`
**Description:**
Verify all configurations map correctly.

**Files to Create:**
- `app/src/test/java/...ConfigMappingTest.kt`

**Test Cases:**
- [ ] Test all SegmentationModes map correctly
- [ ] Test all OcrEngineModes map correctly
- [ ] Test RecognitionType paths are correct
- [ ] Test TessParams → API parameter conversion

**Estimated Effort:** 3 hours

---

#### Task 7.6: Write Integration Tests
**Priority:** 🔴 Critical
**Labels:** `phase-7`, `testing`, `integration-test`
**Description:**
End-to-end flow testing.

**Files to Create:**
- `app/src/androidTest/java/...OCRIntegrationTest.kt`

**Test Cases:**
- [ ] Test full flow: download → OCR → result
- [ ] Test with all quality tiers
- [ ] Test with multiple languages
- [ ] Test language switching
- [ ] Test preprocessing on/off

**Estimated Effort:** 6 hours

---

#### Task 7.7: Manual Testing - Clean Install
**Priority:** 🔴 Critical
**Labels:** `phase-7`, `testing`, `manual`
**Description:**
Test clean installation scenario.

**Test Checklist:**
- [ ] Install on device with no previous version
- [ ] Verify first-run setup shows
- [ ] Download English (Fast)
- [ ] Monitor download progress
- [ ] Verify file saved to correct location
- [ ] Capture image with text
- [ ] Verify OCR works correctly
- [ ] Check storage usage

**Devices:**
- [ ] Test on Android 8.0 (min SDK 24)
- [ ] Test on Android 14 (current)
- [ ] Test on low-end device
- [ ] Test on high-end device

**Estimated Effort:** 3 hours

---

#### Task 7.8: Manual Testing - App Update (Migration)
**Priority:** 🔴 Critical
**Labels:** `phase-7`, `testing`, `manual`
**Description:**
Test upgrade scenario from Tesseract 3 to 4.

**Test Checklist:**
- [ ] Install old version (Tesseract 3)
- [ ] Download eng + spa languages
- [ ] Perform OCR to verify working
- [ ] Update to new version (Tesseract 4)
- [ ] Verify migration dialog shows
- [ ] Verify old languages listed correctly
- [ ] Verify old tessdata deleted
- [ ] Re-download languages
- [ ] Verify OCR works with new version
- [ ] Check no data corruption

**Estimated Effort:** 3 hours

---

#### Task 7.9: Manual Testing - Multi-Quality
**Priority:** 🟡 High
**Labels:** `phase-7`, `testing`, `manual`
**Description:**
Test all quality tiers and compare results.

**Test Checklist:**
- [ ] Download same language in all 3 qualities
- [ ] Prepare test image with known text
- [ ] Run OCR with Fast model → record time & accuracy
- [ ] Run OCR with Standard model → record time & accuracy
- [ ] Run OCR with Best model → record time & accuracy
- [ ] Compare results
- [ ] Verify expected performance differences
- [ ] Test switching between qualities

**Estimated Effort:** 2 hours

---

#### Task 7.10: Manual Testing - Network Resilience
**Priority:** 🟡 High
**Labels:** `phase-7`, `testing`, `manual`
**Description:**
Test download behavior under poor network conditions.

**Test Checklist:**
- [ ] Start download on WiFi
- [ ] Switch to airplane mode mid-download
- [ ] Verify error message shown
- [ ] Re-enable network
- [ ] Verify retry works or can resume
- [ ] Test on cellular data
- [ ] Test with slow connection (throttled)
- [ ] Test rate limiting scenario

**Estimated Effort:** 2 hours

---

#### Task 7.11: Performance Benchmarking
**Priority:** 🟢 Medium
**Labels:** `phase-7`, `testing`, `performance`
**Description:**
Measure and compare performance vs. Tesseract 3.

**Test Checklist:**
- [ ] Benchmark small images (< 1MP)
- [ ] Benchmark medium images (1-4MP)
- [ ] Benchmark large images (4-8MP)
- [ ] Compare processing times across qualities
- [ ] Measure memory usage
- [ ] Measure battery consumption
- [ ] Compare accuracy on standard test set
- [ ] Document results

**Estimated Effort:** 4 hours

---

#### Task 7.12: Update Documentation
**Priority:** 🟡 High
**Labels:** `phase-7`, `documentation`
**Description:**
Update all project documentation for Tesseract 4.

**Files to Update:**
- [ ] `README.md` - Update OCR section
- [ ] `implementation-plan.md` - Mark as updated to Tesseract 4
- [ ] `CHANGELOG.md` - Add migration entry
- [ ] Code comments - Update references
- [ ] Create release notes

**Estimated Effort:** 3 hours

---

#### Task 7.13: Prepare Play Store Release Notes
**Priority:** 🟡 High
**Labels:** `phase-7`, `documentation`, `release`
**Description:**
Write user-facing release notes.

**Content:**
- [ ] Highlight Tesseract 4 upgrade
- [ ] Mention accuracy improvements
- [ ] Explain quality tier options
- [ ] Note APK size reduction
- [ ] Mention re-download requirement
- [ ] Keep under 500 words

**Estimated Effort:** 1 hour

---

### Phase 8: Beta Release & Monitoring (Added)

#### Task 8.1: Beta Release to Internal Track
**Priority:** 🔴 Critical
**Labels:** `phase-8`, `release`, `beta`
**Description:**
Release to internal testing track.

**Checklist:**
- [ ] Create release build
- [ ] Upload to Google Play Internal Testing
- [ ] Invite 10-20 internal testers
- [ ] Monitor crash reports (first 48 hours)
- [ ] Collect initial feedback
- [ ] Fix critical bugs if any

**Estimated Effort:** 4 hours

---

#### Task 8.2: Beta Release to Closed Track
**Priority:** 🔴 Critical
**Labels:** `phase-8`, `release`, `beta`
**Description:**
Release to closed beta testing.

**Checklist:**
- [ ] Upload to Closed Testing track
- [ ] Invite 100+ beta testers
- [ ] Monitor for 1-2 weeks
- [ ] Track metrics: crash rate, download success, retention
- [ ] Collect user feedback
- [ ] Iterate on issues

**Success Criteria:**
- [ ] Crash rate < 0.5%
- [ ] Download success rate > 95%
- [ ] No critical bugs reported

**Estimated Effort:** 8 hours (spread over 2 weeks)

---

#### Task 8.3: Production Release
**Priority:** 🔴 Critical
**Labels:** `phase-8`, `release`, `production`
**Description:**
Final release to production.

**Checklist:**
- [ ] Verify all 50+ tasks completed
- [ ] Beta testing successful
- [ ] Upload to Production track
- [ ] Stage rollout: 10% → 50% → 100%
- [ ] Monitor crash reports
- [ ] Respond to user reviews
- [ ] Be ready for hotfix if needed

**Estimated Effort:** 4 hours + ongoing monitoring

---

## Summary Statistics

**Total Tasks:** 82 tasks (50+ from original plan + 32 enhanced/added)

**Total Estimated Effort:** ~200 hours (5 weeks)

**Breakdown by Phase:**
- Phase 0 (Pre-Migration): 3 tasks, 4 hours
- Phase 1 (Foundation): 8 tasks, 8 hours
- Phase 2 (Domain): 9 tasks, 16 hours
- Phase 3 (Data Layer): 10 tasks, 40 hours ⚠️ Highest effort
- Phase 4 (DI & ViewModel): 7 tasks, 16 hours
- Phase 5 (Presentation): 8 tasks, 24 hours
- Phase 6 (Migration): 6 tasks, 16 hours
- Phase 7 (Testing): 13 tasks, 40 hours
- Phase 8 (Beta Release): 3 tasks, 16 hours

**Critical Path Tasks:** 24 tasks marked 🔴 Critical
**High Priority Tasks:** 18 tasks marked 🟡 High
**Medium/Low Priority:** 40 tasks marked 🟢 Medium/Low

---

## Task Labels

Use these labels when creating GitHub issues:

### By Phase
- `phase-0`, `phase-1`, `phase-2`, `phase-3`, `phase-4`, `phase-5`, `phase-6`, `phase-7`, `phase-8`

### By Type
- `dependencies`, `architecture`, `domain`, `data`, `ui`, `testing`, `documentation`
- `migration`, `cleanup`, `setup`, `refactoring`

### By Component
- `core`, `download`, `storage`, `preprocessing`, `thread-safety`
- `di`, `viewmodel`, `compose`, `hilt`

### By Priority
- `critical`, `high`, `medium`, `low`

---

## Next Steps

1. **Review this task breakdown** with your team
2. **Create GitHub Project board** using instructions above
3. **Create GitHub issues** from each task (copy description, acceptance criteria, etc.)
4. **Assign tasks** to developers
5. **Set milestones** for each phase
6. **Begin Phase 0** (pre-migration setup)

---

**Document Version:** 1.0
**Last Updated:** 2025-11-18
**Total Tasks:** 82
**Total Effort:** ~200 hours
