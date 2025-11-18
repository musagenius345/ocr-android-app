#!/bin/bash
set -e

REPO="musagenius345/ocr-android-app"
MILESTONE="Tesseract 5 Migration"

echo "Creating Phase 0 issues (no labels)..."

gh issue create \
  --repo $REPO \
  --title "[Phase 0] Create Rollback Branch" \
  --body "**Priority:** 🔴 Critical
**Estimated Effort:** 1 hour

## Description
Create a stable rollback point before starting migration.

## Acceptance Criteria
- [ ] Tag current version as \`v1.0-tesseract3-stable\`
- [ ] Create branch \`backup/tesseract3-stable\`
- [ ] Document rollback procedure in ROLLBACK.md
- [ ] Test that rollback works (checkout and build)" \
  --milestone "$MILESTONE"

gh issue create \
  --repo $REPO \
  --title "[Phase 0] Set Up Beta Testing Channel" \
  --body "**Priority:** 🔴 Critical
**Estimated Effort:** 2 hours

## Description
Prepare beta testing infrastructure for gradual rollout.

## Acceptance Criteria
- [ ] Create beta track in Google Play Console
- [ ] Set up internal testing group (10-20 users)
- [ ] Configure crash reporting (Firebase Crashlytics)
- [ ] Create beta feedback form" \
  --milestone "$MILESTONE"

gh issue create \
  --repo $REPO \
  --title "[Phase 0] Create Migration Feature Branch" \
  --body "**Priority:** 🔴 Critical
**Estimated Effort:** 1 hour

## Description
Create dedicated branch for all migration work.

## Acceptance Criteria
- [ ] Create branch: \`feature/tesseract-5-migration\`
- [ ] Set up branch protection rules
- [ ] Configure CI/CD for feature branch
- [ ] Create PR template for migration tasks" \
  --milestone "$MILESTONE"

echo "✅ Created 3 Phase 0 issues"
echo ""
echo "Run this to add them to your project:"
echo "gh issue list --repo $REPO --milestone '$MILESTONE' --json number --jq '.[].number' | xargs -I {} gh project item-add 2 --owner musagenius345 --url https://github.com/$REPO/issues/{}"
