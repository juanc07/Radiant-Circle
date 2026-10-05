# Gradle AAR Metadata Fix — Compose/Core Dependency Compatibility

## Problem

Android Studio reported AAR metadata errors because the project used Android Gradle Plugin 9.0.1 with `compileSdk = 36`, while some dependencies resolved to AndroidX/Compose versions requiring API 37 and a newer Android Gradle Plugin.

The failing dependency set included Compose `1.12.0` artifacts and AndroidX Core `1.19.0`.

## Cause

The project used Compose BOM `2026.08.00` and AndroidX Core KTX `1.19.0`. Those versions are too new for the current Phase 1 build baseline:

```text
Android Gradle Plugin: 9.0.1
compileSdk: 36
targetSdk: 36
```

## Fix applied

Keep AGP 9.0.1 and SDK 36 for Phase 1, but pin dependencies to versions compatible with that baseline:

```toml
composeBom = "2026.04.01"
activityCompose = "1.12.4"
coreKtx = "1.17.0"
```

This avoids pulling Compose 1.12.x and AndroidX Core 1.19.x while preserving the native Jetpack Compose Phase 1 UI shell.

## Why not update to API 37 now?

Phase 1 should stay conservative and easy to sync on the current Android Studio/SDK setup. Updating to API 37 and AGP 9.1+ can be done later, but should be a deliberate build-baseline upgrade with its own test pass.

## Files changed

```text
gradle/libs.versions.toml
README.md
docs/GRADLE_AAR_METADATA_FIX_COMPOSE_202608.md
docs/operating-system/DOCUMENTATION_AND_VERSIONING_RULES.md
docs/operating-system/README.md
docs/operating-system/AGENTS.md
docs/operating-system/ARCHITECTURE.md
docs/operating-system/TESTING_AND_RELEASE.md
docs/operating-system/CHANGELOG.md
```

## Required local check

Run:

```bash
./gradlew :app:assembleDebug
```

Or in Android Studio:

```text
File -> Sync Project with Gradle Files
Build -> Make Project
```

If Android Studio keeps old dependency errors, run Gradle sync again or delete the project `.gradle` cache folder and reopen the project.
