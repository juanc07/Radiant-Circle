# Radiant Rush Phase 3 MWA Lifecycle Crash Fix Patch

This is a changed-files-only patch.

## What it fixes

The app built but crashed immediately on the phone because `MobileWalletRepository` was created inside Compose. That repository creates `ActivityResultSender`, which registers an Activity Result launcher. Android requires that registration before the Activity reaches `STARTED`.

The fix moves `MobileWalletRepository` creation into `MainActivity.onCreate()` before `setContent`.

## Apply

Copy this patch folder into your project root and overwrite existing files.

## Build

```bash
./gradlew :app:assembleDebug
```

PowerShell:

```powershell
.\gradlew.bat :app:assembleDebug
```

## Changed files

```text
app/src/main/java/com/thinkblox/radiantrush/MainActivity.kt
app/src/main/java/com/thinkblox/radiantrush/ui/RadiantRushApp.kt
docs/PHASE_3_MWA_LIFECYCLE_CRASH_FIX.md
docs/operating-system/ARCHITECTURE.md
docs/operating-system/CHANGELOG.md
```

## Packaging check

```text
No .git metadata
No google-services.json
Changed-files-only patch
```
