# Phase 10 Android smoke-test Home anchor fix

## Problem

`connectedDebugAndroidTest` entered the app shell successfully, but the smoke test then asserted that `radiant_run_play` existed immediately. The Radiant Run launcher lives inside the Home `LazyColumn`. Compose lazy containers only compose visible/near-visible items, so a button below the fold may legitimately have no semantics node until the list is scrolled.

## Fix

- Added stable `UiTestTags.HOME_SCREEN = "screen_home"`.
- Tagged the Home `LazyColumn` root with `HOME_SCREEN`.
- The shell smoke test now asserts that the Home screen itself is displayed plus the Home/Demo navigation destinations exist.
- Removed the invalid requirement that an off-screen lazy child already exist.

This does **not** weaken Phase 10 gameplay validation. Radiant Run still requires the manual device QA defined in `TESTING_AND_RELEASE.md`; the instrumentation test remains a deterministic app-shell/navigation smoke test.

## Test

```powershell
./gradlew --stop
./gradlew :app:connectedDebugAndroidTest
./gradlew :app:testDebugUnitTest
./gradlew :app:assembleDebug
```

## Documentation decision

- `CHANGELOG.md`: updated because the Android test harness and semantic test anchor changed.
- `TESTING_AND_RELEASE.md`: updated to document why lazy children are not shell-smoke anchors.
- `ARCHITECTURE.md`: no change; application architecture is unchanged.
- `AGENTS.md`: no change; contributor policy is unchanged.
- `SOLANA_SECURITY_AND_DATA_RULES.md`: no change; no Solana/data behavior changed.
- `MOBILE_UI_UX_STANDARDS.md`: no change; `testTag` does not alter user-facing layout.
- Version: no bump; production behavior is unchanged.
