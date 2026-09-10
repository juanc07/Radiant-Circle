# Phase 10 Android Test v2 launch fix

## Symptom

On a physical Samsung SM-A556E, `:app:connectedDebugAndroidTest` became intermittent after the Phase 10 smoke-test assertions were updated. One run still reported the earlier off-screen `radiant_run_play` assertion, while the next run failed both tests with:

`IllegalStateException: No compose hierarchies found in the app.`

Production Kotlin compilation, unit tests, and `assembleDebug` were already passing.

## Root cause addressed

The smoke test was still using the deprecated pre-v2 `createAndroidComposeRule`. Current Compose UI Test recommends the v2 JUnit4 rule, which uses the newer test scheduling / ActivityScenario integration. The smoke test also assumed that the Welcome destination was immediately available, which is unnecessarily brittle on a real device and does not tolerate Android restoring the already-entered shell.

## Fix

- Migrate the smoke test to `androidx.compose.ui.test.junit4.v2.createAndroidComposeRule`.
- Wait for either the Welcome CTA or Home root semantics before interacting.
- Treat an already-visible Home screen as a valid startup state.
- Wait for the Home root after entering the shell.
- Keep the smoke test focused on stable screen-level semantics, not an off-screen `LazyColumn` child.

No production wallet, Firebase, Solana, SKR, leaderboard, ticket, run-score, capsule, collectible, or reward logic changes are included.

## Verification

Run on a connected Android device:

```powershell
./gradlew --stop
./gradlew :app:connectedDebugAndroidTest
```

Then run the complete Phase 10 gate:

```powershell
./gradlew :app:testDebugUnitTest
./gradlew :app:assembleDebug
./gradlew :app:connectedDebugAndroidTest
```

If `No compose hierarchies found` remains after this migration, capture `adb logcat` around the test launch because that would indicate the Activity itself is crashing before `setContent`, rather than an assertion/readiness problem.
