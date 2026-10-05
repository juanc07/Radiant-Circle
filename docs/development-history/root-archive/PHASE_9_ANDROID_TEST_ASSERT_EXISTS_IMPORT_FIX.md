# Phase 9 Android Test `assertExists` Import Fix

## Problem

`./gradlew :app:connectedDebugAndroidTest` failed during `compileDebugAndroidTestKotlin` because `RadiantRushSmokeTest.kt` explicitly imported:

```kotlin
import androidx.compose.ui.test.assertExists
```

With the Compose UI test API used by this project, `assertExists()` is available on `SemanticsNodeInteraction`; the explicit top-level import is not required and fails to resolve.

## Fix

Removed only the obsolete/unresolved `assertExists` import. The existing calls remain unchanged:

```kotlin
composeRule.onNodeWithTag(UiTestTags.NAV_HOME).assertExists()
composeRule.onNodeWithTag(UiTestTags.NAV_DEMO).assertExists()
```

No production app code, wallet logic, Firebase logic, Solana logic, SKR logic, or reward-loop behavior is changed.

## Verification

Run with an Android phone or emulator connected:

```bash
./gradlew --stop
./gradlew :app:connectedDebugAndroidTest
```

Then rerun the regular local checks:

```bash
./gradlew :app:testDebugUnitTest
./gradlew :app:assembleDebug
```
