# Phase 3 Manifest / Dependency Merge Fix

Date: 2026-09-09
Patch type: changed-files-only
Phase: 3 - Mobile Wallet Adapter wallet connect

## Problem

After applying the Phase 3 Mobile Wallet Adapter patch, Android Studio reported:

```text
Manifest merger failed with multiple errors, see logs
```

The editor also showed many red underlines around Gradle version-catalog aliases. Those editor underlines can happen when Android Studio has not fully resolved the Gradle/Kotlin script model after a failed sync. The build blocker to fix first is the manifest/dependency merge task.

## Fix

This patch updates:

```text
app/build.gradle.kts
app/src/main/AndroidManifest.xml
docs/PHASE_3_MANIFEST_MERGE_FIX.md
docs/operating-system/CHANGELOG.md
```

### `app/build.gradle.kts`

- Keeps AGP `9.0.1` and `compileSdk 36`.
- Keeps Phase 3 version `0.3.0-phase3`.
- Keeps Mobile Wallet Adapter integration.
- Excludes runtime test/mock dependencies that are not needed by the production app.
- Adds safe packaging resource excludes for common metadata collisions.

### `AndroidManifest.xml`

- Adds `xmlns:tools`.
- Adds `tools:targetApi="31"` on `<application>`.
- Adds explicit launcher activity label/theme.
- Keeps portrait orientation.
- Keeps Internet permission.

## Apply

1. Extract the patch ZIP.
2. Copy the patch contents into the project root.
3. Overwrite existing files.
4. In Android Studio, run **Sync Project with Gradle Files**.
5. Build again.

## Build commands

Git Bash:

```bash
./gradlew --stop
./gradlew :app:assembleDebug
```

PowerShell:

```powershell
.\gradlew.bat --stop
.\gradlew.bat :app:assembleDebug
```

## If it still fails

Run this and capture the first real manifest error above the final failure message:

```bash
./gradlew :app:processDebugMainManifest --stacktrace --info
```

PowerShell:

```powershell
.\gradlew.bat :app:processDebugMainManifest --stacktrace --info
```

Also check this report if Android Studio generated it:

```text
app/build/intermediates/manifest_merge_blame_file/debug/processDebugMainManifest/manifest-merger-blame-debug-report.txt
```

If that path does not exist, search under:

```text
app/build/intermediates/
```

for:

```text
manifest-merger-blame-debug-report.txt
```

## Security note

This patch does not add private keys, seed phrases, signing authorities, mint authorities, reward authorities, or API secrets.
