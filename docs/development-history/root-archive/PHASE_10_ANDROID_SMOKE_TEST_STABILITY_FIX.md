# Phase 10 Android smoke-test stability fix

## Problem

`connectedDebugAndroidTest` failed on a physical Samsung device even though Phase 10 unit tests and the debug APK build passed. The failing assertion searched for the marketing headline `Connect. Quest. Prove.` and required that text node itself to be displayed.

On compact devices, large font scales, or layouts where the scroll viewport changes, copy can exist in the semantics tree without satisfying `assertIsDisplayed()`. That made the smoke test depend on presentation copy and viewport placement instead of the behavior it was intended to verify.

## Fix

The welcome smoke test now anchors on `UiTestTags.WELCOME_OPEN_RUSH`, scrolls the CTA into view, verifies that the CTA is displayed, and clicks it. The post-navigation assertions remain unchanged: Home, Radiant Run, and Demo navigation must exist.

The judge-demo smoke test uses the same stable welcome CTA tag before verifying the Demo screen.

## Scope

This is Android test-source only. No production UI, Firebase, wallet, SKR, ticket, game, capsule, collectible, leaderboard, or persistence logic is changed. No app version bump is required.

## Verification

Run on a connected Android device:

```bash
./gradlew :app:connectedDebugAndroidTest
```

For the full Phase 10 gate also run:

```bash
./gradlew :app:testDebugUnitTest
./gradlew :app:assembleDebug
./gradlew :app:connectedDebugAndroidTest
```
