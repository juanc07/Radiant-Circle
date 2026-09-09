# Radiant Rush Phase 8 Submission Package Patch

Apply this patch on branch:

```bash
phase-8-submission-package
```

This patch adds final hackathon submission materials only. It does not touch wallet logic, Firebase logic, SKR scan logic, Gradle dependencies, or app version.

## Added files

```text
docs/PHASE_8_SUBMISSION_PACKAGE.md
docs/submission/JUDGE_README.md
docs/submission/DEMO_VIDEO_SCRIPT.md
docs/submission/PITCH_TALKING_POINTS.md
docs/submission/SCREENSHOT_CHECKLIST.md
docs/submission/FINAL_RELEASE_CHECKLIST.md
docs/submission/SUBMISSION_FORM_COPY.md
docs/submission/KNOWN_LIMITATIONS.md
scripts/copy_debug_apk_to_submission.sh
scripts/copy_debug_apk_to_submission.ps1
```

## Local APK note

Do not commit the APK by default. Keep it local under:

```text
release/submission/RadiantRush-debug.apk
```

Upload the APK directly to the hackathon form or GitHub Releases if needed.

## After applying

```bash
bash scripts/copy_debug_apk_to_submission.sh
git add docs/PHASE_8_SUBMISSION_PACKAGE.md docs/submission scripts/copy_debug_apk_to_submission.sh scripts/copy_debug_apk_to_submission.ps1
git status
git commit -m "Add Phase 8 submission package"
git push -u origin phase-8-submission-package
```

Make sure `app/google-services.json` and `release/submission/*.apk` are not staged.
