# Material Expressive Android OCR App
A modern offline Android application for fast, accurate text extraction from images and documents. Built with Material Design 3 using Jetpack Compose, this app utilizes the Tesseract OCR engine (via tess-two) for robust multi-language OCR. Features include a CameraX-powered scanner, rich Material theming, Room database for local storage, and a clean MVVM architecture with Hilt dependency injection.

Features
- Scan and extract text from photos and documents with Tesseract OCR

- Offline text recognition (no internet needed, works anywhere)

- Material Design 3 UI using Jetpack Compose and dynamic color theming

- Capture images with CameraX and process them for enhanced OCR accuracy

- Save and manage extracted text locally using Room database

- Clean and testable codebase following MVVM and Clean Architecture principles

- Kotlin Coroutines for smooth, efficient background image processing

- Supports multiple languages via downloadable Tesseract trained data

- Easy setup and extensible for custom OCR workflows

## Recommended Stack for Material Expressive Android OCR App with Tesseract

For building an offline Material Design 3 OCR app using Tesseract on Android, here's the comprehensive technology stack you should use:

### UI Framework & Design

**Jetpack Compose with Material 3**[1][2][3]
- Use `androidx.compose.material3` for Material Design 3 components
- Implement Material You with dynamic color theming[1]
- Dependency: `implementation("androidx.compose.material3:material3:1.3.2")`[3]

**Material 3 Components to use:**
- `Scaffold` for layout structure[4][5]
- `TopAppBar` for app navigation[6][5]
- `Button`, `Card`, `TextField` variants (Filled, Elevated, FilledTonal)[3]
- Material color schemes and typography from `MaterialTheme.colorScheme`[7][8]

### OCR Engine

**Tesseract OCR via tess-two library**[9][10][11]
- Add dependency: `implementation 'com.rmtheis:tess-two:9.1.0'`[9]
- Provides Java API wrapper for native Tesseract 3.05 and Leptonica 1.74.1[9]
- Completely offline OCR processing[12][13]
- Supports 100+ languages with trained data files[14][13]

**Implementation approach:**
```kotlin
import com.googlecode.tesseract.android.TessBaseAPI

private fun extractText(bitmap: Bitmap): String {
    val tessBaseApi = TessBaseAPI()
    tessBaseApi.init(DATA_PATH, "eng")
    tessBaseApi.setImage(bitmap)
    val extractedText = tessBaseApi.getUTF8Text()
    tessBaseApi.end()
    return extractedText
}
```


### Architecture Pattern

**MVVM with Clean Architecture**[15][16][17]

**Layer structure:**
- **Presentation Layer**: Composable UI + ViewModel[16][15]
- **Domain Layer**: Use cases for OCR processing logic[17][16]
- **Data Layer**: Repository pattern + Room database + Tesseract wrapper[15][16]

This separation ensures:[16]
- Testable business logic independent of UI
- Decoupled code components
- Easy maintainability and feature additions

### Camera Integration

**CameraX Library**[18][19][20]
- Modern Jetpack camera API with backward compatibility to Android 5.0[21]
- Dependencies:
```kotlin
implementation("androidx.camera:camera-core:1.6.0-alpha01")
implementation("androidx.camera:camera-camera2:1.6.0-alpha01")
implementation("androidx.camera:camera-lifecycle:1.6.0-alpha01")
implementation("androidx.camera:camera-view:1.6.0-alpha01")
```


**Use cases to implement:**
- Preview for viewfinder[19][22]
- ImageCapture for taking photos[18][19]
- ImageAnalysis for real-time OCR (optional)[23]

### Local Data Storage

**Room Database**[24][25][26]
- Jetpack's recommended offline storage solution[27][25]
- Abstraction over SQLite with compile-time SQL verification[25]
- Dependencies:
```kotlin
implementation("androidx.room:room-runtime:2.6.1")
ksp("androidx.room:room-compiler:2.6.1")
implementation("androidx.room:room-ktx:2.6.1")
```


**Store:**
- Scanned document metadata
- Extracted text for offline access
- User preferences and settings

### Dependency Injection

**Dagger Hilt**[28][29][30]
- Jetpack's recommended DI library[29]
- Reduces boilerplate compared to manual DI[28]
- Built on Dagger for compile-time correctness[28]

**Dependencies:**
```kotlin
// Project build.gradle
id("com.google.dagger.hilt.android") version "2.57.1" apply false

// App build.gradle
implementation("com.google.dagger:hilt-android:2.57.1")
ksp("com.google.dagger:hilt-android-compiler:2.57.1")
```


**Annotate:**
- Application class with `@HiltAndroidApp`[64][28]
- Activities/ViewModels with `@AndroidEntryPoint`[58]
- Modules with `@Module` and `@InstallIn`[58]

### Asynchronous Processing

**Kotlin Coroutines**[31][32][33]
- For background OCR processing without blocking UI
- Integration with Room and ViewModel
- Use appropriate dispatchers:
  - `Dispatchers.IO` for image processing and OCR[32]
  - `Dispatchers.Main` for UI updates
  - `Dispatchers.Default` for CPU-intensive transformations

**Flow for reactive data:**
```kotlin
fun processOcrFlow() = flow<State<Result>> {
    emit(State.loading())
    val localData = fetchFromLocal().first()
    emit(State.success(localData))
}
```


### Image Processing

**Image preprocessing for better OCR accuracy:**
- Grayscale conversion[14]
- Thresholding (binary conversion)[14]
- Noise removal[14]
- Deskewing (rotation correction)[14]

**Libraries to consider:**
- OpenCV4Android for advanced preprocessing[34]
- Built-in Android Bitmap transformations for basic operations

### Project Structure

```
app/
├── data/
│   ├── local/
│   │   ├── dao/ (Room DAOs)
│   │   └── entity/ (Room entities)
│   └── repository/ (Implementation)
├── domain/
│   ├── model/ (Domain models)
│   ├── repository/ (Repository interfaces)
│   └── usecase/ (Business logic)
├── presentation/
│   ├── ui/
│   │   ├── camera/ (Camera screen)
│   │   ├── results/ (OCR results screen)
│   │   └── theme/ (Material 3 theme)
│   └── viewmodel/
└── di/ (Hilt modules)
```


### Additional Recommendations

**Trained Data Management:**
- Download Tesseract trained data files (tessdata) for required languages[13][9]
- Store in app's private directory: `{DATA_PATH}/tessdata/`[24][7]
- Languages available at: https://github.com/tesseract-ocr/tessdata[13]

**Image Quality Optimization:**
- Implement proper lighting detection
- Add manual focus with CameraX tap-to-focus[35]
- Provide image quality feedback to users[23]

**Performance Considerations:**
- Process OCR on background threads only[32]
- Cache processed results in Room[36][33]
- Implement proper memory management for Bitmap objects
- Use appropriate image compression before OCR processing

### Complete Dependency Setup

```kotlin
// App build.gradle.kts
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp")
    id("com.google.dagger.hilt.android")
}

dependencies {
    // Compose + Material 3
    implementation("androidx.compose.material3:material3:1.3.2")
    implementation("androidx.compose.ui:ui:1.7.6")
    implementation("androidx.activity:activity-compose:1.9.3")
    
    // Tesseract OCR
    implementation("com.rmtheis:tess-two:9.1.0")
    
    // CameraX
    implementation("androidx.camera:camera-core:1.6.0-alpha01")
    implementation("androidx.camera:camera-camera2:1.6.0-alpha01")
    implementation("androidx.camera:camera-lifecycle:1.6.0-alpha01")
    implementation("androidx.camera:camera-view:1.6.0-alpha01")
    
    // Room
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")
    
    // Hilt
    implementation("com.google.dagger:hilt-android:2.57.1")
    ksp("com.google.dagger:hilt-android-compiler:2.57.1")
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")
    
    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
    
    // ViewModel
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
}
```

This stack provides a robust, modern, and completely offline Android OCR application with Material Design 3 aesthetics, clean architecture, and excellent performance.[34][23][3][16][9]

[1](https://daily.dev/blog/material-design-3-for-android-new-features)
[2](https://developer.android.com/develop/ui/compose/designsystems/material3)
[3](https://proandroiddev.com/getting-started-with-material-design-3-in-jetpack-compose-e2e6db2b9fce)
[4](https://composables.com/docs/androidx.compose.material3/material3/components/Scaffold)
[5](https://developer.android.com/develop/ui/compose/components/app-bars)
[6](https://www.youtube.com/watch?v=EqCvUETekjk)
[7](https://developer.android.com/develop/ui/compose/tutorial)
[8](https://developer.android.com/codelabs/jetpack-compose-theming)
[9](https://github.com/rmtheis/tess-two)
[10](https://gaut.am/making-an-ocr-android-app-using-tesseract/)
[11](https://priyankvex.com/2015/09/02/making-an-ocr-app-for-android-using-tesseract/)
[12](https://stackoverflow.com/questions/36453199/offline-image-to-text-recognition-ocr-in-android)
[13](https://github.com/tesseract-ocr/tesseract)
[14](https://www.tenorshare.com/image-translator/tesseract-ocr.html)
[15](https://stackoverflow.com/questions/58484680/what-is-difference-between-mvvm-with-clean-architecture-and-mvvm-without-clean-a)
[16](https://www.toptal.com/android/android-apps-mvvm-with-clean-architecture)
[17](https://tomasgis.com/clean-architecture-on-mvvm-architecture-pattern-on-android-project-d09c5011453e)
[18](https://www.geeksforgeeks.org/android/how-to-create-custom-camera-using-camerax-in-android/)
[19](https://proandroiddev.com/lets-build-an-android-camera-app-camerax-compose-9ea47356aa80)
[20](https://developer.android.com/jetpack/androidx/releases/camera)
[21](https://developer.android.com/media/camera/camerax)
[22](https://developer.android.com/codelabs/camerax-getting-started)
[23](https://blog.filestack.com/ocr-sdk-for-android-mobile-app-development/)
[24](https://www.youtube.com/watch?v=ln85bqPqDsY)
[25](https://developer.android.com/training/data-storage/room)
[26](https://www.linkedin.com/pulse/building-offline-first-apps-jetpack-compose-room-mircea-ioan-soit-ajicf)
[27](https://stackoverflow.com/questions/58854907/how-to-keep-a-local-database-to-store-data-in-an-android-app-when-the-app-is-off)
[28](https://developer.android.com/training/dependency-injection/hilt-android)
[29](https://developer.android.com/training/dependency-injection)
[30](https://www.geeksforgeeks.org/android/dagger-hilt-in-android-with-example/)
[31](https://www.tothenew.com/blog/coil-an-effective-android-image-loading-library/)
[32](https://www.kodeco.com/books/kotlin-coroutines-by-tutorials/v2.0/chapters/17-coroutines-on-android-part-1)
[33](https://stackoverflow.com/questions/63072321/kotlin-flow-first-offline-approach)
[34](https://transloadit.com/devtips/ocr-android-sdk/)
[35](https://github.com/Coding-Meet/Camera-Using-CameraX)
[36](https://developer.android.com/topic/architecture/data-layer/offline-first)
[37](https://stackoverflow.com/questions/74891336/i-dont-know-how-to-implement-material-design-3-in-android-studio-kotlin)
[38](https://stackoverflow.com/questions/7710123/how-can-i-use-tesseract-in-android)
[39](https://github.com/Sanster/DeepAndroidOcr)
[40](https://m3.material.io/develop)
[41](https://play.google.com/store/apps/details?id=com.offline.imagetotext.ocr&hl=en)
[42](https://www.designveloper.com/blog/mobile-ocr-libraries/)
[43](https://m3.material.io/develop/android/mdc-android)
[44](https://developers.google.com/ml-kit/vision/text-recognition/v2/android)
[45](https://www.sciencedirect.com/science/article/pii/S1877050919311640)
[46](https://m3.material.io/develop/android/jetpack-compose)
[47](https://itnext.io/offline-ocr-with-tesseractjs-and-ionic-5054fc7eef86)
[48](https://www.codeproject.com/articles/Android-OCR-Application-Based-on-Tesseract)
[49](https://m3.material.io)
[50](https://www.youtube.com/watch?v=h7K4n9C2jkI)
[51](https://ironsoftware.com/csharp/ocr/blog/ocr-tools/android-ocr-library-list/)
[52](https://coderwall.com/p/eurvaq/tesseract-with-andoird-and-gradle)
[53](https://www.octalsoftware.com/blog/how-to-develop-ocr-scanner-mobile-app-complete-guide)
[54](https://github.com/deekoder/tess_two)
[55](https://zapier.com/blog/best-mobile-scanning-ocr-apps/)
[56](https://stackoverflow.com/questions/29973831/android-using-the-tess-two-library)
[57](https://www.securescan.com/articles/records-management/the-best-mobile-scanning-apps-rated/)
[58](https://codelabs.developers.google.com/jetpack-compose-theming)
[59](https://groups.google.com/g/tesseract-ocr/c/3iIvHgiuLo4)
[60](https://www.blog.finotes.com/post/using-tensorflow-lite-for-image-processing-in-kotlin-android-apps)
[61](https://proandroiddev.com/mvvm-with-clean-architecture-c2c021e05c89)
[62](https://pub.dev/packages/camera_android_camerax)
[63](https://developer.android.com/topic/architecture)
[64](https://github.com/DavidAmunga/kotlin-coroutines-image-processing-example)
[65](https://www.reddit.com/r/androiddev/comments/8mz642/does_anybody_prefer_clean_architecture_over_mvvm/)
[66](https://github.com/samadtalukder/Android-Clean-Architecture-MVVM-Kotlin)
[67](https://proandroiddev.com/the-hidden-dangers-of-room-database-performance-and-how-to-fix-them-ac93830885bd)
[68](https://proandroiddev.com/mastering-assisted-injection-in-hilt-a-complete-guide-d95037dd38b1)
[69](https://m3.material.io/components/app-bars/overview)
[70](https://dev.to/abdul_rehman_2050/how-to-properly-setup-hilt-in-android-jetpack-compose-project-in-2025-o56)
[71](https://stackoverflow.com/questions/76075850/jetpack-compose-why-does-my-material3-topappbar-have-a-huge-padding-at-the-top)
[72](https://blog.stackademic.com/mastering-dependency-injection-with-hilt-in-android-81b3d221da9a)
[73](https://proandroiddev.com/creating-a-collapsing-topappbar-with-jetpack-compose-d25ad19d6113)
[74](https://www.oneclickitsolution.com/centerofexcellence/android/local-database-management-android)
[75](https://www.youtube.com/watch?v=N24BWz52EMA)
