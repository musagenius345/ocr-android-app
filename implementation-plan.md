# OCR Engine Implementation Plan

## Overview

This document describes the implementation of the OCR (Optical Character Recognition) engine in the Android app. The app uses **Tesseract OCR** via the `tess-two` library to provide offline text recognition from images captured by the camera or selected from the gallery.

## Architecture

### Clean Architecture Layers

The OCR implementation follows Clean Architecture principles with clear separation of concerns:

```
┌─────────────────────────────────────────────────────────┐
│                  Presentation Layer                      │
│  - OCRViewModel                                          │
│  - OCRResultScreen                                       │
│  - CameraScreen                                          │
└───────────────────────┬─────────────────────────────────┘
                        │
┌───────────────────────▼─────────────────────────────────┐
│                    Domain Layer                          │
│  - OCRService (Interface)                               │
│  - Use Cases:                                            │
│    • InitializeOCRUseCase                               │
│    • ProcessImageUseCase                                │
│    • ValidateImageQualityUseCase                        │
│  - Models:                                               │
│    • OCRConfig                                           │
│    • OCRResult                                           │
│    • OCRProgress                                         │
│    • Language                                            │
└───────────────────────┬─────────────────────────────────┘
                        │
┌───────────────────────▼─────────────────────────────────┐
│                     Data Layer                           │
│  - OCRServiceImpl (Tesseract Implementation)            │
│  - ImagePreprocessor                                     │
│  - ImageCompressor                                       │
└─────────────────────────────────────────────────────────┘
```

## Technology Stack

### Core Library
- **tess-two** (v9.1.0): Java wrapper for Tesseract OCR
  - Based on Tesseract 3.05
  - Based on Leptonica 1.74.1
  - Provides complete offline OCR capabilities
  - Location: `app/build.gradle.kts:85`

### Supporting Technologies
- **Kotlin Coroutines**: Asynchronous processing
- **Hilt**: Dependency injection
- **CameraX**: Image capture
- **Jetpack Compose**: UI rendering
- **Room**: OCR history storage

## Component Details

### 1. OCRService Interface

**Location:** `app/src/main/java/com/musagenius/ocrapp/domain/service/OCRService.kt`

**Purpose:** Defines the contract for OCR operations

**Key Methods:**
```kotlin
interface OCRService {
    suspend fun initialize(language: String): Result<Unit>
    suspend fun recognizeText(
        bitmap: Bitmap,
        config: OCRConfig,
        onProgress: (OCRProgress) -> Unit
    ): Result<OCRResult>
    fun cancel()
    fun cleanup()
}
```

### 2. OCRServiceImpl

**Location:** `app/src/main/java/com/musagenius/ocrapp/data/ocr/OCRServiceImpl.kt`

**Purpose:** Concrete implementation using Tesseract OCR

**Key Features:**
- Thread-safe singleton with Mutex
- Manages TessBaseAPI lifecycle
- Handles tessdata file management
- Progress tracking and cancellation support

**Critical Implementation Details:**

#### Initialization Flow
1. Checks for tessdata directory in external storage
2. Verifies language file exists (e.g., `eng.traineddata`)
3. Copies from assets if missing
4. Initializes TessBaseAPI with:
   - Data path
   - Language code
   - Engine mode
   - Page segmentation mode

#### Thread Safety
```kotlin
private val tessMutex = Mutex()

suspend fun recognizeText(...) = tessMutex.withLock {
    // All Tesseract API calls serialized
}
```

### 3. ImagePreprocessor

**Location:** `app/src/main/java/com/musagenius/ocrapp/data/ocr/ImagePreprocessor.kt`

**Purpose:** Enhance image quality for better OCR accuracy

**Processing Pipeline:**

#### Step 1: Image Scaling
- Checks if image exceeds max dimension (default: 2000px)
- Scales down while maintaining aspect ratio
- Prevents memory issues with large images

#### Step 2: Grayscale Conversion
- Converts RGB to grayscale using luminance formula:
  ```
  gray = 0.299 * R + 0.587 * G + 0.114 * B
  ```
- Reduces processing complexity
- Improves text detection

#### Step 3: Contrast Enhancement
- Applies histogram stretching
- Finds min/max pixel values
- Stretches to full 0-255 range
- Improves text clarity

#### Image Quality Assessment
```kotlin
fun assessImageQuality(bitmap: Bitmap): ImageQuality
```
- **Blur Detection:** Laplacian variance method
- **Brightness Analysis:** Average luminance (0.0-1.0)
- **Resolution Check:** Warns if < 500K pixels
- Returns user-friendly warnings

### 4. OCRViewModel

**Location:** `app/src/main/java/com/musagenius/ocrapp/presentation/viewmodel/OCRViewModel.kt`

**Purpose:** Manages OCR state and coordinates use cases

**State Management:**
```kotlin
data class OCRState(
    val isProcessing: Boolean = false,
    val result: OCRResult? = null,
    val error: String? = null,
    val progress: OCRProgress? = null
)
```

**Key Responsibilities:**
- Loads image from URI
- Executes OCR processing
- Tracks progress
- Handles errors
- Manages cancellation

### 5. Use Cases

#### InitializeOCRUseCase
**Location:** `app/src/main/java/com/musagenius/ocrapp/domain/usecase/InitializeOCRUseCase.kt`

**Purpose:** Initialize OCR engine with specific language

**Flow:**
1. Validates language parameter
2. Calls OCRService.initialize()
3. Returns Result<Unit>

#### ProcessImageUseCase
**Location:** `app/src/main/java/com/musagenius/ocrapp/domain/usecase/ProcessImageUseCase.kt`

**Purpose:** Process image and extract text

**Overloads:**
- `invoke(uri: Uri, config: OCRConfig)`: Load from URI
- `invoke(bitmap: Bitmap, config: OCRConfig)`: Direct bitmap

**Flow:**
1. Load bitmap from URI if needed
2. Validate image quality (optional)
3. Call OCRService.recognizeText()
4. Emit progress updates
5. Return OCRResult

#### ValidateImageQualityUseCase
**Location:** `app/src/main/java/com/musagenius/ocrapp/domain/usecase/ValidateImageQualityUseCase.kt`

**Purpose:** Assess image quality before OCR

**Returns:**
- Blur warnings
- Brightness warnings
- Resolution warnings

## Data Models

### OCRConfig

**Location:** `app/src/main/java/com/musagenius/ocrapp/domain/model/OCRConfig.kt`

**Configuration Options:**
```kotlin
data class OCRConfig(
    val language: String = "eng",
    val pageSegmentationMode: PageSegMode = PageSegMode.AUTO,
    val engineMode: EngineMode = EngineMode.DEFAULT,
    val preprocessImage: Boolean = true,
    val maxImageDimension: Int = 2000
)
```

**Page Segmentation Modes:**
- `OSD_ONLY`: Orientation and script detection only
- `AUTO_OSD`: Automatic with orientation
- `AUTO`: Fully automatic (default)
- `SINGLE_COLUMN`: Single column text
- `SINGLE_BLOCK`: Single uniform block
- `SINGLE_LINE`: Single text line
- `SINGLE_WORD`: Single word
- `CIRCLE_WORD`: Circle word
- `SINGLE_CHAR`: Single character
- `SPARSE_TEXT`: Find as much text as possible
- `SPARSE_TEXT_OSD`: Sparse text with orientation
- `RAW_LINE`: Raw line (bypass layout analysis)

**Engine Modes:**
- `DEFAULT`: Based on availability
- `LSTM_ONLY`: Neural nets LSTM engine
- `LEGACY_ONLY`: Legacy Tesseract engine
- `LSTM_AND_LEGACY`: Both engines

### OCRResult

**Location:** `app/src/main/java/com/musagenius/ocrapp/domain/model/OCRResult.kt`

**Result Data:**
```kotlin
data class OCRResult(
    val text: String,
    val confidence: Float,        // 0.0 to 1.0
    val processingTime: Long,     // milliseconds
    val language: String,
    val wordCount: Int,
    val characterCount: Int
)
```

### OCRProgress

**Location:** `app/src/main/java/com/musagenius/ocrapp/domain/model/OCRProgress.kt`

**Progress Tracking:**
```kotlin
data class OCRProgress(
    val stage: ProcessingStage,
    val progress: Float,          // 0.0 to 1.0
    val message: String
)

enum class ProcessingStage {
    INITIALIZING,    // 0-20%
    PREPROCESSING,   // 20-40%
    RECOGNIZING,     // 40-95%
    COMPLETING       // 95-100%
}
```

### Language

**Location:** `app/src/main/java/com/musagenius/ocrapp/domain/model/Language.kt`

**Language Support:**
- 100+ languages supported
- Each language requires `.traineddata` file
- Stored in `{externalFilesDir}/tessdata/`

## Complete OCR Flow

### Entry Points

#### 1. Camera Capture Flow
```
User opens CameraScreen
    ↓
User taps capture button
    ↓
CameraViewModel captures image via CameraX
    ↓
Image saved to external storage
    ↓
ImageCompressor.compressImage() (optional)
    ↓
Navigate to OCRResultScreen with image URI
    ↓
OCR processing begins automatically
```

#### 2. Gallery Selection Flow
```
User opens CameraScreen
    ↓
User taps gallery button
    ↓
Android photo picker shown
    ↓
User selects image
    ↓
Navigate to ImageEditorScreen
    ↓
User crops/rotates image (optional)
    ↓
Navigate to OCRResultScreen with edited image URI
    ↓
OCR processing begins automatically
```

### OCR Processing Pipeline

#### Stage 1: Initialization (0-20% Progress)

**Actions:**
1. Check tessdata directory exists
2. Verify language file present
3. Copy from assets if missing
4. Create TessBaseAPI instance
5. Initialize with language and config

**Error Handling:**
- Missing language file → Copy from assets or show error
- Initialization failure → Return error with message

#### Stage 2: Image Loading (20% Progress)

**Actions:**
1. Load bitmap from URI using ImageCompressor
2. Validate bitmap not null
3. Optional: Assess image quality

**Error Handling:**
- Failed to load → Return error
- Low quality → Show warnings (continue processing)

#### Stage 3: Preprocessing (20-40% Progress)

**If `preprocessImage = true`:**

1. **Scale Image (25%)**
   ```kotlin
   if (width > maxDim || height > maxDim) {
       val scale = maxDim.toFloat() / max(width, height)
       scaledBitmap = Bitmap.createScaledBitmap(...)
   }
   ```

2. **Convert to Grayscale (30%)**
   ```kotlin
   for (each pixel) {
       gray = 0.299 * red + 0.587 * green + 0.114 * blue
   }
   ```

3. **Enhance Contrast (35%)**
   ```kotlin
   Find min and max pixel values
   Stretch histogram: new = (old - min) * 255 / (max - min)
   ```

**Output:** Enhanced bitmap ready for OCR

#### Stage 4: OCR Recognition (40-95% Progress)

**Actions:**
1. Set image in Tesseract (40%)
   ```kotlin
   tessBaseAPI.setImage(preprocessedBitmap)
   ```

2. Configure page segmentation mode (45%)
   ```kotlin
   tessBaseAPI.setPageSegMode(config.pageSegmentationMode.value)
   ```

3. Extract text (45-90%)
   ```kotlin
   val text = tessBaseAPI.getUTF8Text()
   ```
   - Progress simulated (Tesseract doesn't provide real-time updates)
   - Emit progress updates at intervals

4. Get confidence score (90%)
   ```kotlin
   val confidence = tessBaseAPI.meanConfidence() / 100f
   ```

5. Calculate statistics (95%)
   ```kotlin
   wordCount = text.trim().split("\\s+".toRegex()).size
   characterCount = text.length
   ```

**Output:** Extracted text with metadata

#### Stage 5: Completion (95-100% Progress)

**Actions:**
1. Create OCRResult object
2. Cleanup temporary bitmaps
3. Return Result.success(ocrResult)

**Error Handling:**
- Empty text → Still valid (might be blank image)
- Low confidence → Still valid (user can retry)
- Exception → Cleanup and return error

### Post-Processing

**In OCRResultScreen:**

1. **Display Results:**
   - Show extracted text in selectable card
   - Display confidence percentage
   - Show processing time
   - Display word count

2. **User Actions:**
   - **Copy:** Copy text to clipboard
   - **Share:** Open Android share sheet
   - **Save:** Store to Room database history
   - **Retry:** Process again with different settings

## Language Management

### Language File Storage

**Directory Structure:**
```
{externalFilesDir}/tessdata/
    ├── eng.traineddata      (English)
    ├── spa.traineddata      (Spanish)
    ├── fra.traineddata      (French)
    └── ...
```

### Language Use Cases

#### GetAvailableLanguagesUseCase
**Location:** `app/src/main/java/com/musagenius/ocrapp/domain/usecase/GetAvailableLanguagesUseCase.kt`

**Purpose:** List all installed languages

**Flow:**
1. Scan tessdata directory
2. Find all `.traineddata` files
3. Map to Language objects
4. Return list

#### CheckLanguageAvailabilityUseCase
**Location:** `app/src/main/java/com/musagenius/ocrapp/domain/usecase/CheckLanguageAvailabilityUseCase.kt`

**Purpose:** Check if specific language is available

**Flow:**
1. Check if file exists at expected path
2. Return boolean result

#### DeleteLanguageUseCase
**Location:** `app/src/main/java/com/musagenius/ocrapp/domain/usecase/DeleteLanguageUseCase.kt`

**Purpose:** Remove language data to free space

**Flow:**
1. Validate not deleting last language
2. Delete `.traineddata` file
3. Return Result<Unit>

## Dependency Injection

### OCRModule

**Location:** `app/src/main/java/com/musagenius/ocrapp/di/OCRModule.kt`

**Provides:**

```kotlin
@Module
@InstallIn(SingletonComponent::class)
object OCRModule {

    @Provides
    @Singleton
    fun provideOCRService(
        @ApplicationContext context: Context
    ): OCRService = OCRServiceImpl(context)

    @Provides
    @Singleton
    fun provideImagePreprocessor(): ImagePreprocessor =
        ImagePreprocessor()
}
```

**Singleton Scope:**
- OCRService: Expensive to create, reused across app
- ImagePreprocessor: Stateless, can be shared

## Threading Model

### Dispatcher Usage

**Dispatchers.IO:**
- File operations (loading images, reading tessdata)
- Copying language files from assets
- Database operations (saving history)

**Dispatchers.Default:**
- Image preprocessing (CPU-intensive)
- OCR processing (CPU-intensive)
- Bitmap operations

**Dispatchers.Main:**
- UI updates
- Progress callbacks
- Navigation

### Concurrency Control

**Mutex for Tesseract:**
```kotlin
private val tessMutex = Mutex()

// Ensures only one OCR operation at a time
suspend fun recognizeText(...) = tessMutex.withLock {
    withContext(Dispatchers.Default) {
        // Tesseract operations here
    }
}
```

**Why Mutex?**
- Tesseract is not thread-safe
- TessBaseAPI cannot be used concurrently
- Mutex provides cooperative locking in coroutines

### Cancellation Support

**In OCRViewModel:**
```kotlin
viewModelScope.launch {
    processImageUseCase(imageUri, config).collect { result ->
        // Update UI
    }
}
```

**In OCRServiceImpl:**
```kotlin
fun cancel() {
    tessBaseAPI?.stop()
}
```

**When cancelled:**
- Tesseract stops processing immediately
- Resources cleaned up
- Flow completes with cancellation

## Performance Optimizations

### 1. Image Compression
- Reduces file size before OCR
- Uses configurable quality (default: 80%)
- Fallback to original if compression fails

### 2. Image Scaling
- Max dimension limit (default: 2000px)
- Prevents memory issues
- Balances quality vs. speed

### 3. Bitmap Recycling
```kotlin
try {
    // Use bitmap
} finally {
    bitmap.recycle()
}
```

### 4. Single Tesseract Instance
- Singleton pattern with @Singleton scope
- Avoid expensive re-initialization
- Reuse across multiple OCR operations

### 5. Progress Simulation
- Provides user feedback during long operations
- Prevents "frozen" UI appearance
- Updates at key processing stages

### 6. Lazy Initialization
- Tesseract initialized only when needed
- Language files loaded on demand
- Reduces app startup time

## Error Handling

### Result Wrapper Pattern

All OCR operations return `Result<T>`:
```kotlin
sealed class Result<out T> {
    data class Success<T>(val data: T) : Result<T>()
    data class Error(val exception: Exception) : Result<Nothing>()
}
```

### Error Categories

#### 1. Initialization Errors
- **Missing tessdata:** Copy from assets or download
- **Invalid language:** Show available languages
- **Tesseract init failed:** Check logs, retry

#### 2. Image Loading Errors
- **File not found:** URI invalid or file deleted
- **Out of memory:** Image too large
- **Corrupt image:** Cannot decode

#### 3. Processing Errors
- **OCR failed:** Tesseract exception
- **Cancelled:** User or system cancelled
- **Timeout:** Processing too long

#### 4. Quality Warnings (Not Errors)
- **Blurry image:** Suggest retake
- **Dark/bright image:** Suggest adjust lighting
- **Low resolution:** Suggest higher quality

### User-Facing Error Messages

**In OCRViewModel:**
```kotlin
when (result) {
    is Result.Success -> updateState(result.data)
    is Result.Error -> updateState(error =
        result.exception.message ?: "Unknown error")
}
```

**In UI:**
- Show error in dialog or snackbar
- Provide "Retry" button
- Suggest corrective actions

## Testing Considerations

### Unit Tests

**OCRServiceImpl:**
- Mock TessBaseAPI
- Test initialization flow
- Test error handling
- Test cleanup

**ImagePreprocessor:**
- Test scaling logic
- Test grayscale conversion
- Test contrast enhancement
- Test quality assessment

**Use Cases:**
- Mock dependencies
- Test business logic
- Test error propagation

### Integration Tests

**OCR Flow:**
- Test with real images
- Verify accuracy on known text
- Test different languages
- Test quality warnings

**Performance:**
- Measure processing time
- Test memory usage
- Test cancellation

### UI Tests

**OCRResultScreen:**
- Test loading state
- Test success state
- Test error state
- Test copy/share actions

## Future Enhancements

### Potential Improvements

1. **Real-Time Progress:**
   - Hook into Tesseract internals for actual progress
   - More accurate progress bar

2. **Advanced Preprocessing:**
   - Deskewing (straighten rotated text)
   - Denoising (remove artifacts)
   - Binarization (adaptive thresholding)

3. **Multiple Languages:**
   - Detect language automatically
   - Support multiple languages in single image

4. **Text Layout Analysis:**
   - Preserve formatting
   - Detect columns, tables
   - Export structured data

5. **Tesseract 4/5:**
   - Upgrade to latest version
   - Use improved LSTM models
   - Better accuracy

6. **Cloud OCR Fallback:**
   - Use cloud OCR for difficult images
   - Hybrid approach (local first, cloud backup)

7. **Batch Processing:**
   - Process multiple images
   - PDF support (multi-page)

8. **Custom Training:**
   - Train custom models for specific fonts
   - Fine-tune for handwriting

## Performance Metrics

### Typical Processing Times

**On Mid-Range Device (2023):**
- Small image (< 1MP): 1-2 seconds
- Medium image (1-4MP): 2-5 seconds
- Large image (4-8MP): 5-10 seconds

**Breakdown:**
- Initialization: 200-500ms (first time only)
- Preprocessing: 200-800ms
- OCR: 1-8 seconds (depends on text amount)
- Cleanup: < 100ms

### Memory Usage

**Peak Memory:**
- Original bitmap: width × height × 4 bytes (ARGB)
- Preprocessed bitmap: width × height × 1 byte (grayscale)
- Tesseract internal: ~50-100MB

**Example:**
- 3000×4000 image: ~48MB original + 12MB grayscale
- Total peak: ~100-150MB

## Conclusion

The OCR implementation provides a robust, offline text recognition system with:
- Clean architecture for maintainability
- Comprehensive error handling
- User-friendly progress tracking
- Image quality assessment
- Multi-language support
- Thread-safe operation
- Performance optimizations

The modular design allows easy testing, maintenance, and future enhancements while providing a smooth user experience.
