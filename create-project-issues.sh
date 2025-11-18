#!/bin/bash

# Script to create GitHub issues for Tesseract 4 Migration
# Requires: gh CLI (GitHub CLI) installed and authenticated
# Usage: ./create-project-issues.sh

set -e

REPO="musagenius345/ocr-android-app"
PROJECT_NAME="Tesseract 5 Migration"
MILESTONE="Tesseract 5 Migration"

echo "🚀 Creating GitHub issues for Tesseract 5 Migration"
echo "Repository: $REPO"
echo ""

# Check if gh CLI is installed
if ! command -v gh &> /dev/null; then
    echo "❌ Error: GitHub CLI (gh) is not installed"
    echo "Install it from: https://cli.github.com/"
    exit 1
fi

# Check if authenticated
if ! gh auth status &> /dev/null; then
    echo "❌ Error: Not authenticated with GitHub"
    echo "Run: gh auth login"
    exit 1
fi

echo "✅ GitHub CLI is installed and authenticated"
echo ""

# Create milestone if it doesn't exist
echo "📌 Creating milestone: $MILESTONE"
gh api repos/$REPO/milestones -f title="$MILESTONE" -f description="Migration from Tesseract 3 to Tesseract 4" 2>/dev/null || echo "Milestone may already exist"
echo ""

# Function to create issue
create_issue() {
    local title="$1"
    local body="$2"
    local labels="$3"
    local phase="$4"

    echo "Creating: $title"
    gh issue create \
        --repo "$REPO" \
        --title "$title" \
        --body "$body" \
        --label "$labels" \
        --milestone "$MILESTONE" 2>/dev/null || echo "  ⚠️  Issue may already exist"
}

echo "📝 Creating Phase 0 issues..."
echo ""

create_issue \
"[Phase 0] Create Rollback Branch" \
"**Priority:** 🔴 Critical
**Phase:** 0 - Pre-Migration Setup
**Estimated Effort:** 1 hour

## Description
Create a stable rollback point before starting migration.

## Acceptance Criteria
- [ ] Tag current version as \`v1.0-tesseract3-stable\`
- [ ] Create branch \`backup/tesseract3-stable\`
- [ ] Document rollback procedure in \`ROLLBACK.md\`
- [ ] Test that rollback works (checkout and build)

## Related Files
- Git tags and branches" \
"phase-0,setup,safety,critical" \
"Phase 0"

create_issue \
"[Phase 0] Set Up Beta Testing Channel" \
"**Priority:** 🔴 Critical
**Phase:** 0 - Pre-Migration Setup
**Estimated Effort:** 2 hours

## Description
Prepare beta testing infrastructure for gradual rollout.

## Acceptance Criteria
- [ ] Create beta track in Google Play Console
- [ ] Set up internal testing group (10-20 users)
- [ ] Configure crash reporting (Firebase Crashlytics)
- [ ] Create beta feedback form

## Related Files
- Google Play Console configuration" \
"phase-0,setup,testing,critical" \
"Phase 0"

create_issue \
"[Phase 0] Create Migration Feature Branch" \
"**Priority:** 🔴 Critical
**Phase:** 0 - Pre-Migration Setup
**Estimated Effort:** 1 hour

## Description
Create dedicated branch for all migration work.

## Acceptance Criteria
- [ ] Create branch: \`feature/tesseract-4-migration\`
- [ ] Set up branch protection rules
- [ ] Configure CI/CD for feature branch
- [ ] Create PR template for migration tasks

## Related Files
- \`.github/workflows/\`" \
"phase-0,setup,git,critical" \
"Phase 0"

echo ""
echo "📝 Creating Phase 1 issues..."
echo ""

create_issue \
"[Phase 1] Remove tess-two Dependency" \
"**Priority:** 🟡 High
**Phase:** 1 - Foundation & Dependencies
**Estimated Effort:** 0.5 hours

## Description
Remove the old Tesseract 3 library from build configuration.

## Acceptance Criteria
- [ ] Remove line 85: \`implementation(\"com.rmtheis:tess-two:9.1.0\")\`
- [ ] Verify project still compiles (with errors expected)
- [ ] Document removed dependency in CHANGELOG

## Related Files
- \`app/build.gradle.kts\`" \
"phase-1,dependencies,high" \
"Phase 1"

create_issue \
"[Phase 1] Add tesseract4android to Version Catalog" \
"**Priority:** 🟡 High
**Phase:** 1 - Foundation & Dependencies
**Estimated Effort:** 0.5 hours

## Description
Add the new Tesseract 4 library to Gradle version catalog.

## Acceptance Criteria
- [ ] Add version: \`tesseract4Android = \"4.9.0\"\`
- [ ] Add library to libs.versions.toml
- [ ] Verify TOML syntax is valid

## Related Files
- \`gradle/libs.versions.toml\`" \
"phase-1,dependencies,high" \
"Phase 1"

create_issue \
"[Phase 1] Add tesseract4android Implementation" \
"**Priority:** 🟡 High
**Phase:** 1 - Foundation & Dependencies
**Estimated Effort:** 0.5 hours

## Description
Add the new library to app dependencies.

## Acceptance Criteria
- [ ] Add: \`implementation(libs.tesseract4android.openmp)\`
- [ ] Sync Gradle successfully
- [ ] Verify library is in dependencies tree
- [ ] Check for version conflicts

## Related Files
- \`app/build.gradle.kts\`" \
"phase-1,dependencies,high" \
"Phase 1"

create_issue \
"[Phase 1] Create Feature Module Structure" \
"**Priority:** 🟡 High
**Phase:** 1 - Foundation & Dependencies
**Estimated Effort:** 1 hour

## Description
Create the new \`feature/recognize-text/\` module directory structure.

## Acceptance Criteria
- [ ] Create directory: \`app/src/main/java/com/musagenius/ocrapp/feature/recognizetext/\`
- [ ] Create subdirectories: \`domain/\`, \`data/\`, \`presentation/\`, \`di/\`
- [ ] Create subdirectories: \`domain/models/\`, \`domain/usecases/\`
- [ ] Create subdirectories: \`data/preprocessing/\`, \`data/download/\`
- [ ] Create subdirectories: \`presentation/ui/\`, \`presentation/viewmodel/\`
- [ ] Document structure in README

## Directory Structure
\`\`\`
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
\`\`\`" \
"phase-1,architecture,high" \
"Phase 1"

echo ""
echo "📝 Creating Phase 2 issues..."
echo ""

create_issue \
"[Phase 2] Create ImageTextReader Interface" \
"**Priority:** 🔴 Critical
**Phase:** 2 - Domain Layer Implementation
**Estimated Effort:** 2 hours

## Description
Create the main service interface replacing OCRService.

## Acceptance Criteria
- [ ] Define \`getTextFromImage(bitmap: Bitmap, params: TessParams): Result<TextRecognitionResult>\`
- [ ] Define \`downloadTrainingData(language: OCRLanguage, quality: RecognitionType): Flow<DownloadProgress>\`
- [ ] Define \`getLanguages(): List<OCRLanguage>\`
- [ ] Define \`deleteLanguage(language: OCRLanguage, quality: RecognitionType): Result<Unit>\`
- [ ] Add KDoc documentation
- [ ] Follow naming conventions

## Related Files
- \`feature/recognizetext/domain/ImageTextReader.kt\`" \
"phase-2,domain,interface,critical" \
"Phase 2"

create_issue \
"[Phase 2] Create RecognitionType Enum" \
"**Priority:** 🔴 Critical
**Phase:** 2 - Domain Layer Implementation
**Estimated Effort:** 2 hours

## Description
Define quality tiers for OCR models (Fast/Standard/Best).

## Acceptance Criteria
- [ ] Define enum: \`FAST\`, \`STANDARD\`, \`BEST\`
- [ ] Map to GitHub repo URLs
- [ ] Map to file system paths
- [ ] Add model size estimates
- [ ] Add performance characteristics

## Related Files
- \`feature/recognizetext/domain/models/RecognitionType.kt\`" \
"phase-2,domain,model,critical" \
"Phase 2"

create_issue \
"[Phase 2] Create TessParams Data Class" \
"**Priority:** 🔴 Critical
**Phase:** 2 - Domain Layer Implementation
**Estimated Effort:** 2 hours

## Description
Define comprehensive OCR configuration (replaces OCRConfig).

## Acceptance Criteria
- [ ] Define all configuration parameters
- [ ] Provide sensible defaults
- [ ] Support all Tesseract 4 options
- [ ] Add builder pattern (optional)

## Related Files
- \`feature/recognizetext/domain/models/TessParams.kt\`
- \`feature/recognizetext/domain/models/TessConstants.kt\`" \
"phase-2,domain,model,critical" \
"Phase 2"

echo ""
echo "📝 Creating Phase 3 issues (Critical Thread Safety)..."
echo ""

create_issue \
"[Phase 3] Create ThreadSafeTessAPI Wrapper" \
"**Priority:** 🔴 CRITICAL - THREAD SAFETY
**Phase:** 3 - Data Layer Refactoring
**Estimated Effort:** 4 hours

## Description
Create wrapper class to enforce mutex-based thread safety. **This is critical to prevent crashes and race conditions.**

## Acceptance Criteria
- [ ] Inject Mutex for serialization
- [ ] Wrap all TessBaseAPI operations
- [ ] Prevent direct TessBaseAPI access
- [ ] Write thread-safety tests
- [ ] Document why mutex is needed

## ⚠️ CRITICAL
TessBaseAPI is NOT thread-safe. All access must be serialized via Mutex.withLock.

## Related Files
- \`feature/recognizetext/data/ThreadSafeTessAPI.kt\`
- Reference: Current implementation at \`data/ocr/OCRServiceImpl.kt:19-37\`" \
"phase-3,data,thread-safety,critical" \
"Phase 3"

create_issue \
"[Phase 3] Create AndroidImageTextReader Implementation" \
"**Priority:** 🔴 Critical
**Phase:** 3 - Data Layer Refactoring
**Estimated Effort:** 8 hours

## Description
Implement ImageTextReader interface using tesseract4android.

## Acceptance Criteria
- [ ] Implement all interface methods
- [ ] Use ThreadSafeTessAPI for all TessBaseAPI calls
- [ ] Handle initialization with new file paths
- [ ] Map TessParams to Tesseract API
- [ ] Implement proper error handling
- [ ] Add logging

## Dependencies
- Blocked by: ThreadSafeTessAPI wrapper

## Related Files
- \`feature/recognizetext/data/AndroidImageTextReader.kt\`" \
"phase-3,data,core,critical" \
"Phase 3"

create_issue \
"[Phase 3] Create LanguageDownloadManager" \
"**Priority:** 🔴 Critical
**Phase:** 3 - Data Layer Refactoring
**Estimated Effort:** 8 hours

## Description
Implement language file download manager.

## Acceptance Criteria
- [ ] Download from GitHub URLs
- [ ] Support all RecognitionType qualities
- [ ] Emit download progress via Flow
- [ ] Save to correct internal storage path
- [ ] Handle network errors with retry
- [ ] Verify file integrity (checksum)

## Download Sources
- Fast: \`https://github.com/tesseract-ocr/tessdata_fast/\`
- Standard: \`https://github.com/tesseract-ocr/tessdata/\`
- Best: \`https://github.com/tesseract-ocr/tessdata_best/\`

## Related Files
- \`feature/recognizetext/data/download/LanguageDownloadManager.kt\`" \
"phase-3,data,download,critical" \
"Phase 3"

echo ""
echo "📝 Creating Phase 4 issues..."
echo ""

create_issue \
"[Phase 4] Create RecognizeTextModule" \
"**Priority:** 🔴 Critical
**Phase:** 4 - DI & ViewModel Migration
**Estimated Effort:** 2 hours

## Description
Create Hilt module for new components.

## Acceptance Criteria
- [ ] Provide ImageTextReader binding
- [ ] Provide ThreadSafeTessAPI singleton
- [ ] Provide LanguageDownloadManager
- [ ] Provide StorageManager
- [ ] Provide FilterChain with default filters

## Related Files
- \`feature/recognizetext/di/RecognizeTextModule.kt\`" \
"phase-4,di,hilt,critical" \
"Phase 4"

create_issue \
"[Phase 4] Refactor OCRViewModel to use ImageTextReader" \
"**Priority:** 🔴 Critical
**Phase:** 4 - DI & ViewModel Migration
**Estimated Effort:** 5 hours

## Description
Replace OCRService injection with ImageTextReader.

## Acceptance Criteria
- [ ] Update state to hold TessParams instead of OCRConfig
- [ ] Add state for available languages
- [ ] Add state for download progress
- [ ] Change constructor parameter from OCRService to ImageTextReader
- [ ] Update all method calls to use new interface
- [ ] Verify ViewModel still compiles

## Related Files
- \`presentation/viewmodel/OCRViewModel.kt\`" \
"phase-4,viewmodel,refactoring,critical" \
"Phase 4"

echo ""
echo "📝 Creating Phase 5 issues..."
echo ""

create_issue \
"[Phase 5] Create LanguageSelector Component" \
"**Priority:** 🔴 Critical
**Phase:** 5 - Presentation Layer
**Estimated Effort:** 6 hours

## Description
Create UI for selecting and downloading languages.

## Acceptance Criteria
- [ ] Display list of all available languages
- [ ] Show download status per quality tier
- [ ] Show download button for uninstalled languages
- [ ] Show progress bar during download
- [ ] Show delete button for installed languages
- [ ] Filter/search functionality
- [ ] Follow Material 3 design

## Related Files
- \`feature/recognizetext/presentation/ui/LanguageSelector.kt\`" \
"phase-5,ui,compose,critical" \
"Phase 5"

create_issue \
"[Phase 5] Create RecognitionTypeSelector Component" \
"**Priority:** 🟡 High
**Phase:** 5 - Presentation Layer
**Estimated Effort:** 3 hours

## Description
UI component to select quality tier (Fast/Standard/Best).

## Acceptance Criteria
- [ ] Display 3 options: Fast, Standard, Best
- [ ] Show characteristics of each (speed, accuracy, size)
- [ ] Highlight selected option
- [ ] Update ViewModel state on selection
- [ ] Show which languages are downloaded for selected quality

## Related Files
- \`feature/recognizetext/presentation/ui/RecognitionTypeSelector.kt\`" \
"phase-5,ui,compose,high" \
"Phase 5"

echo ""
echo "📝 Creating Phase 6 issues (Migration)..."
echo ""

create_issue \
"[Phase 6] Create TesseractMigrationManager" \
"**Priority:** 🔴 Critical
**Phase:** 6 - Data & Storage Migration
**Estimated Effort:** 4 hours

## Description
Implement migration logic from old to new storage structure.

## Acceptance Criteria
- [ ] Check if migration already completed (DataStore flag)
- [ ] Detect old tessdata directory
- [ ] Scan for installed languages
- [ ] Return list of previously installed languages
- [ ] Mark migration as completed
- [ ] Handle edge cases (no old data, already migrated)

## Old Storage
- \`{externalFilesDir}/tessdata/*.traineddata\`

## New Storage
- \`{filesDir}/fast/tessdata/\`
- \`{filesDir}/standard/tessdata/\`
- \`{filesDir}/best/tessdata/\`

## Related Files
- \`feature/recognizetext/data/migration/TesseractMigrationManager.kt\`" \
"phase-6,migration,data,critical" \
"Phase 6"

create_issue \
"[Phase 6] Create Migration Dialog UI" \
"**Priority:** 🔴 Critical
**Phase:** 6 - Data & Storage Migration
**Estimated Effort:** 3 hours

## Description
Design and implement user-facing migration dialog.

## Acceptance Criteria
- [ ] Show migration notice
- [ ] List previously installed languages
- [ ] Explain why re-download is needed
- [ ] Mention quality tier options
- [ ] \"Go to Language Manager\" button
- [ ] \"Dismiss\" button
- [ ] Don't show again after dismissed

## Related Files
- \`feature/recognizetext/presentation/ui/MigrationDialog.kt\`" \
"phase-6,ui,migration,critical" \
"Phase 6"

create_issue \
"[Phase 6] Remove Old Traineddata from Assets" \
"**Priority:** 🟡 High
**Phase:** 6 - Data & Storage Migration
**Estimated Effort:** 1 hour

## Description
Delete bundled .traineddata files from assets folder.

## Acceptance Criteria
- [ ] Delete tessdata directory from assets
- [ ] Verify APK size reduction
- [ ] Update documentation
- [ ] Remove references to bundled assets

## Expected Impact
-5 to -50 MB APK size reduction

## Files to Delete
- All files in \`app/src/main/assets/tessdata/\`" \
"phase-6,cleanup,assets,high" \
"Phase 6"

echo ""
echo "📝 Creating Phase 7 issues (Testing)..."
echo ""

create_issue \
"[Phase 7] Write Thread Safety Tests" \
"**Priority:** 🔴 Critical
**Phase:** 7 - Testing & Finalization
**Estimated Effort:** 6 hours

## Description
Verify mutex prevents concurrent TessBaseAPI access.

## Test Cases
- [ ] Test concurrent OCR requests (10+ simultaneous)
- [ ] Test queued requests complete successfully
- [ ] Test cancellation doesn't affect other requests
- [ ] Stress test with 100 consecutive operations
- [ ] Verify no deadlocks

## Related Files
- \`app/src/test/java/.../ThreadSafetyTest.kt\`" \
"phase-7,testing,concurrency,critical" \
"Phase 7"

create_issue \
"[Phase 7] Write Download Manager Tests" \
"**Priority:** 🔴 Critical
**Phase:** 7 - Testing & Finalization
**Estimated Effort:** 6 hours

## Description
Test download functionality and error handling.

## Test Cases
- [ ] Test successful download
- [ ] Test download progress emissions
- [ ] Test network error triggers retry
- [ ] Test exponential backoff timing
- [ ] Test rate limiting handling
- [ ] Test corrupt file detection
- [ ] Test concurrent downloads queued
- [ ] Test download cancellation

## Related Files
- \`app/src/test/java/.../LanguageDownloadManagerTest.kt\`" \
"phase-7,testing,download,critical" \
"Phase 7"

create_issue \
"[Phase 7] Manual Testing - App Update Migration" \
"**Priority:** 🔴 Critical
**Phase:** 7 - Testing & Finalization
**Estimated Effort:** 3 hours

## Description
Test upgrade scenario from Tesseract 3 to 4.

## Test Checklist
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

## Devices to Test
- [ ] Android 8.0 (min SDK 24)
- [ ] Android 14 (current)
- [ ] Low-end device
- [ ] High-end device" \
"phase-7,testing,manual,critical" \
"Phase 7"

echo ""
echo "📝 Creating Phase 8 issues (Beta Release)..."
echo ""

create_issue \
"[Phase 8] Beta Release to Internal Track" \
"**Priority:** 🔴 Critical
**Phase:** 8 - Beta Release & Monitoring
**Estimated Effort:** 4 hours

## Description
Release to internal testing track.

## Checklist
- [ ] Create release build
- [ ] Upload to Google Play Internal Testing
- [ ] Invite 10-20 internal testers
- [ ] Monitor crash reports (first 48 hours)
- [ ] Collect initial feedback
- [ ] Fix critical bugs if any

## Success Criteria
- Crash rate < 1%
- No critical bugs" \
"phase-8,release,beta,critical" \
"Phase 8"

create_issue \
"[Phase 8] Beta Release to Closed Track" \
"**Priority:** 🔴 Critical
**Phase:** 8 - Beta Release & Monitoring
**Estimated Effort:** 8 hours

## Description
Release to closed beta testing.

## Checklist
- [ ] Upload to Closed Testing track
- [ ] Invite 100+ beta testers
- [ ] Monitor for 1-2 weeks
- [ ] Track metrics: crash rate, download success, retention
- [ ] Collect user feedback
- [ ] Iterate on issues

## Success Criteria
- [ ] Crash rate < 0.5%
- [ ] Download success rate > 95%
- [ ] No critical bugs reported" \
"phase-8,release,beta,critical" \
"Phase 8"

create_issue \
"[Phase 8] Production Release" \
"**Priority:** 🔴 Critical
**Phase:** 8 - Beta Release & Monitoring
**Estimated Effort:** 4 hours + ongoing monitoring

## Description
Final release to production.

## Checklist
- [ ] Verify all 82 tasks completed
- [ ] Beta testing successful
- [ ] Upload to Production track
- [ ] Stage rollout: 10% → 50% → 100%
- [ ] Monitor crash reports
- [ ] Respond to user reviews
- [ ] Be ready for hotfix if needed" \
"phase-8,release,production,critical" \
"Phase 8"

echo ""
echo "✅ Done! Created issues for Tesseract 4 Migration"
echo ""
echo "📊 Summary:"
echo "   - Created ~30 key issues (sample - full list has 82 tasks)"
echo "   - All issues assigned to milestone: $MILESTONE"
echo "   - Labels applied for filtering"
echo ""
echo "🔗 View issues: https://github.com/$REPO/issues"
echo "🔗 View milestone: https://github.com/$REPO/milestones"
echo ""
echo "💡 Next steps:"
echo "   1. Review issues on GitHub"
echo "   2. Create project board and add issues"
echo "   3. Assign issues to team members"
echo "   4. Start with Phase 0 tasks"
echo ""
