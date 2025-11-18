# GitHub Project Board Setup Guide

This guide explains how to create a GitHub Project board for tracking the Tesseract 3 → 4 migration.

## Quick Overview

You have **3 options** to set up the project board:

1. **Option A: Automatic (using script)** - Fastest, requires `gh` CLI locally
2. **Option B: Manual (using web UI)** - Step-by-step, no tools needed
3. **Option C: Hybrid** - Create issues manually, import to project

---

## Option A: Automatic Setup (Recommended if you have gh CLI)

### Prerequisites
- Install GitHub CLI: https://cli.github.com/
- Authenticate: `gh auth login`

### Steps

1. **Clone the repository locally** (if not already):
   ```bash
   git clone https://github.com/musagenius345/ocr-android-app.git
   cd ocr-android-app
   ```

2. **Checkout the migration branch**:
   ```bash
   git checkout claude/investigate-ocr-handling-01VkP7LKFe8YSfn5bE6EiiSx
   git pull origin claude/investigate-ocr-handling-01VkP7LKFe8YSfn5bE6EiiSx
   ```

3. **Run the issue creation script**:
   ```bash
   ./create-project-issues.sh
   ```

   This will:
   - Create a milestone: "Tesseract 4 Migration"
   - Create ~30 key issues with labels and descriptions
   - Link all issues to the milestone

4. **Create the project board**:
   ```bash
   gh project create "Tesseract 4 Migration" \
     --owner musagenius345 \
     --template "Board"
   ```

5. **Add issues to project**:
   ```bash
   # Get project number (from previous command or project URL)
   PROJECT_NUMBER=1  # Replace with actual number

   # Add all milestone issues to project
   gh issue list --milestone "Tesseract 4 Migration" --json number --jq '.[].number' | \
     xargs -I {} gh project item-add $PROJECT_NUMBER --owner musagenius345 --url https://github.com/musagenius345/ocr-android-app/issues/{}
   ```

6. **Done!** View your project at:
   - https://github.com/users/musagenius345/projects/[PROJECT_NUMBER]

---

## Option B: Manual Setup via Web UI (No CLI needed)

### Step 1: Create the Project Board

1. Go to: https://github.com/musagenius345/ocr-android-app
2. Click **Projects** tab
3. Click **New project**
4. Choose **Board** template
5. Name it: `Tesseract 4 Migration`
6. Click **Create project**

### Step 2: Create Columns

Click **+ Add column** to create these columns (in order):

1. **📋 Backlog** (for all new tasks)
2. **🏗️ Phase 0: Pre-Migration**
3. **🏗️ Phase 1: Foundation**
4. **🎯 Phase 2: Domain**
5. **💾 Phase 3: Data Layer**
6. **🔌 Phase 4: DI & ViewModel**
7. **🎨 Phase 5: Presentation**
8. **🔄 Phase 6: Migration**
9. **✅ Phase 7: Testing**
10. **🚀 Phase 8: Beta Release**
11. **✔️ Done**

### Step 3: Create a Milestone

1. Go to: https://github.com/musagenius345/ocr-android-app/milestones
2. Click **New milestone**
3. Title: `Tesseract 4 Migration`
4. Description: `Migration from Tesseract 3 to Tesseract 4 for improved accuracy and features`
5. Click **Create milestone**

### Step 4: Create Issues from Task List

Open `PROJECT-BOARD-TASKS.md` in this repository. For each task:

1. Go to: https://github.com/musagenius345/ocr-android-app/issues/new
2. Copy task title → GitHub issue title
3. Copy task description → GitHub issue body
4. Add labels (e.g., `phase-1`, `critical`, `dependencies`)
5. Assign to milestone: `Tesseract 4 Migration`
6. Click **Submit new issue**
7. Go to your project board and click **Add item** → select the issue

**Pro tip:** Start with critical tasks first (marked 🔴 in the task list)

### Key Issues to Create First (Critical Path)

**Phase 0:**
- [ ] Create Rollback Branch
- [ ] Set Up Beta Testing Channel
- [ ] Create Migration Feature Branch

**Phase 1:**
- [ ] Remove tess-two Dependency
- [ ] Add tesseract4android to Version Catalog
- [ ] Create Feature Module Structure

**Phase 2:**
- [ ] Create ImageTextReader Interface
- [ ] Create RecognitionType Enum
- [ ] Create TessParams Data Class

**Phase 3:**
- [ ] Create ThreadSafeTessAPI Wrapper ⚠️ CRITICAL
- [ ] Create AndroidImageTextReader Implementation
- [ ] Create LanguageDownloadManager

...and so on (see full list in `PROJECT-BOARD-TASKS.md`)

### Step 5: Organize Issues on Board

1. Drag all issues to **📋 Backlog** initially
2. Move Phase 0 tasks to **🏗️ Phase 0: Pre-Migration** column
3. When you start a task, move it to **In Progress** (or keep in phase column)
4. When complete, move to **✔️ Done**

---

## Option C: Hybrid Approach

### For Teams Who Want More Control

1. **Manually create** the most critical issues (use web UI)
2. **Use the script** for bulk creation of remaining tasks
3. **Customize** issue descriptions as needed
4. **Add to project board** manually or via script

This gives you flexibility to adjust descriptions, priorities, and assignments.

---

## Project Board Customization

### Add Custom Fields

GitHub Projects supports custom fields. Consider adding:

1. **Effort** (Number field): 1-10 or hours
   - Helps track velocity and sprint planning

2. **Priority** (Single select): Critical, High, Medium, Low
   - Quick visual filtering

3. **Blocked By** (Text field): Issue numbers or description
   - Track dependencies

4. **Status** (Single select): Not Started, In Progress, In Review, Done
   - More granular than column position

### Add Views

Create filtered views for different team members:

1. **My Issues**: `assignee:@me`
2. **Critical Only**: `label:critical`
3. **Phase 3 Focus**: `label:phase-3`
4. **Testing Tasks**: `label:phase-7,testing`

### Set Up Automation

GitHub Projects (Beta) supports automation:

1. **Auto-add to project**: When issue has label `tesseract-4-migration`
2. **Auto-move to Done**: When issue is closed
3. **Auto-assign**: Based on labels or file paths

---

## Labels to Create

If they don't exist, create these labels in your repository:

### By Phase
- `phase-0`, `phase-1`, `phase-2`, `phase-3`, `phase-4`, `phase-5`, `phase-6`, `phase-7`, `phase-8`

### By Priority
- `critical` (red)
- `high` (orange)
- `medium` (yellow)
- `low` (green)

### By Type
- `dependencies` (blue)
- `architecture` (purple)
- `domain` (pink)
- `data` (cyan)
- `ui` (light blue)
- `testing` (green)
- `documentation` (gray)
- `migration` (brown)

### Special
- `thread-safety` (dark red) - Critical concurrency issues
- `breaking-change` (red) - Breaking changes
- `blocked` (black) - Waiting on something

**To create labels:**
1. Go to: https://github.com/musagenius345/ocr-android-app/labels
2. Click **New label**
3. Enter name, description, and color
4. Click **Create label**

---

## Project Tracking Best Practices

### Daily Workflow

1. **Morning standup**:
   - Check **In Progress** column
   - Update issue comments with progress
   - Move blockers to **Blocked** (if you create that column)

2. **During work**:
   - Reference issue number in commits: `git commit -m "Implement ThreadSafeTessAPI (#42)"`
   - Check off acceptance criteria as you complete them
   - Comment with questions or blockers

3. **End of day**:
   - Update issue status
   - Move completed tasks to **Done**
   - Add next day's tasks to **In Progress**

### Weekly Review

1. **Velocity tracking**:
   - Count completed issues per week
   - Estimate remaining time
   - Adjust timeline if needed

2. **Blocker review**:
   - Identify stuck issues
   - Reassign or get help
   - Update documentation

3. **Phase progress**:
   - Calculate % complete per phase
   - Forecast completion dates
   - Report to stakeholders

### Milestones

Set milestone due dates:
- Phase 1-2: Week 1
- Phase 3-4: Week 2
- Phase 5-6: Week 3
- Phase 7-8: Week 4

Track progress at: https://github.com/musagenius345/ocr-android-app/milestones

---

## Integration with Pull Requests

### Link PRs to Issues

In your PR description, use keywords:
```markdown
Closes #42
Fixes #43
Resolves #44
```

This will:
- Automatically close the issue when PR is merged
- Link the PR to the issue for traceability
- Update project board automatically (if automation enabled)

### PR Checklist Template

Create `.github/pull_request_template.md`:

```markdown
## Related Issue
Closes #[issue number]

## Changes
- [ ] Implementation complete
- [ ] Tests written
- [ ] Documentation updated
- [ ] Acceptance criteria met

## Phase
- [ ] Phase 0
- [ ] Phase 1
- [ ] Phase 2
- [ ] Phase 3
- [ ] Phase 4
- [ ] Phase 5
- [ ] Phase 6
- [ ] Phase 7
- [ ] Phase 8

## Checklist
- [ ] Code compiles
- [ ] All tests pass
- [ ] No new warnings
- [ ] Thread safety verified (if applicable)
```

---

## Monitoring Progress

### GitHub Insights

Use built-in insights to track:
- Burndown chart (issues closed over time)
- Velocity (issues per week)
- Open vs. closed ratio
- Time to close

### Custom Dashboard

Create a dashboard document (e.g., `MIGRATION-DASHBOARD.md`):

```markdown
# Tesseract 4 Migration Dashboard

Last Updated: 2025-11-20

## Overall Progress
- [ ] Phase 0: Pre-Migration (0/3 - 0%)
- [ ] Phase 1: Foundation (0/8 - 0%)
- [ ] Phase 2: Domain (0/9 - 0%)
- [ ] Phase 3: Data Layer (0/10 - 0%)
- [ ] Phase 4: DI & ViewModel (0/7 - 0%)
- [ ] Phase 5: Presentation (0/8 - 0%)
- [ ] Phase 6: Migration (0/6 - 0%)
- [ ] Phase 7: Testing (0/13 - 0%)
- [ ] Phase 8: Beta Release (0/3 - 0%)

**Total: 0/82 tasks (0%)**

## This Week's Focus
- Phase 0: All tasks
- Phase 1: Dependency updates

## Blockers
None currently

## Risks
- Thread safety implementation (Phase 3) is critical
- Download manager complexity may extend timeline

## Timeline
- Start: 2025-11-XX
- Target completion: 2025-12-XX
- Estimated: 4 weeks (1 developer) or 2.5 weeks (2 developers)
```

Update this weekly and commit to repository.

---

## Tips for Success

### 1. Start Small
- Don't create all 82 issues at once
- Create Phase 0 and Phase 1 first
- Add more as you progress

### 2. Be Granular
- Break large tasks into smaller sub-tasks
- Use checklists in issue descriptions
- Aim for tasks completable in 1-4 hours

### 3. Document Decisions
- Use issue comments to record technical decisions
- Link to relevant documentation
- Capture alternatives considered

### 4. Review Regularly
- Weekly review of project board
- Update estimates based on actual time
- Adjust priorities as needed

### 5. Celebrate Wins
- Mark phases complete with a summary comment
- Share progress with team
- Reflect on lessons learned

---

## Troubleshooting

### "I don't have permission to create projects"
- Ask the repository owner to give you **Write** or **Admin** access
- Or ask the owner to create the project and invite you as a collaborator

### "The gh CLI script fails"
- Ensure you're authenticated: `gh auth status`
- Check you have internet connection
- Verify repository name is correct
- Try running with `--debug` flag for more info

### "Too many issues to create manually"
- Focus on critical path issues first (marked 🔴)
- Use the script for bulk creation (Option A)
- Or recruit team members to help create issues

### "Project board is overwhelming"
- Start with just 3 columns: Backlog, In Progress, Done
- Add phase columns later as needed
- Use labels and filters to focus on current phase

---

## Next Steps

After setting up the project board:

1. **Review with team**: Ensure everyone understands the scope
2. **Assign initial tasks**: Get Phase 0 tasks assigned
3. **Set up CI/CD**: Ensure tests run on PRs
4. **Create feature branch**: `feature/tesseract-4-migration`
5. **Start coding**: Begin with Task 0.1 (Create Rollback Branch)

---

## Resources

- **Task List**: `PROJECT-BOARD-TASKS.md` (all 82 tasks with details)
- **Review Document**: `ISSUE-5-REVIEW.md` (technical analysis)
- **Implementation Plan**: `implementation-plan.md` (current system docs)
- **Script**: `create-project-issues.sh` (automated issue creation)

- **GitHub Projects Docs**: https://docs.github.com/en/issues/planning-and-tracking-with-projects
- **GitHub CLI Docs**: https://cli.github.com/manual/
- **Issue Templates**: https://docs.github.com/en/communities/using-templates-to-encourage-useful-issues-and-pull-requests

---

**Good luck with the migration! 🚀**

If you have questions, refer to:
- `ISSUE-5-REVIEW.md` for technical details
- `PROJECT-BOARD-TASKS.md` for task specifications
- GitHub documentation for platform-specific help
