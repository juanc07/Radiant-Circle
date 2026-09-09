# Phase 3 MWA AGP 9 Namespace Fix

## Problem

Phase 3 introduced Solana Mobile Wallet Adapter through:

```kotlin
implementation(libs.solana.mwa.client)
```

The original dependency pin used:

```toml
mwaClient = "2.0.3"
```

On Android Gradle Plugin 9, the manifest task fails because the published `2.0.3` KTX artifact and its transitive base client library are both seen with the same Android namespace:

```text
Namespace 'com.solana.mobilewalletadapter.clientlib' is used in multiple modules and/or libraries:
com.solanamobile:mobile-wallet-adapter-clientlib-ktx:2.0.3,
com.solanamobile:mobile-wallet-adapter-clientlib:2.0.3
```

## Fix

Use the newer AGP-9-targeted Mobile Wallet Adapter package version:

```toml
mwaClient = "2.2.0-agp9-beta1"
```

This keeps the app on the MWA Kotlin dependency path while avoiding the old duplicate namespace failure under AGP 9.

## Files changed

```text
gradle/libs.versions.toml
docs/PHASE_3_MWA_AGP9_NAMESPACE_FIX.md
docs/operating-system/CHANGELOG.md
```

## Test command

From the project root:

```bash
./gradlew --stop
./gradlew :app:processDebugMainManifest --stacktrace
./gradlew :app:assembleDebug
```

On Windows PowerShell:

```powershell
.\gradlew.bat --stop
.\gradlew.bat :app:processDebugMainManifest --stacktrace
.\gradlew.bat :app:assembleDebug
```

## Notes

- No wallet behavior changed.
- No Firebase behavior changed.
- No UI behavior changed.
- This is a dependency compatibility fix for the existing Phase 3 wallet connect work.
- If the next build fails, capture the new first real error after this namespace error is gone.
