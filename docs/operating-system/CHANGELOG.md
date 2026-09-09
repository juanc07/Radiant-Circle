# RadiantSolanaHackatonAndroid — Changelog

All meaningful changes should be recorded here. Keep exactly one `[Unreleased]` section at the top.

## [Unreleased]

### Added

- Added Phase 2 Firebase foundation: Firebase dependencies, safe no-config fallback, anonymous Auth bootstrap, Firestore profile/progress save, leaderboard rows, Firestore rules, and setup documentation.
- Added `PHASE_2_FIREBASE_FOUNDATION.md`, `firebase/firestore.rules`, and `app/google-services.json.example`.

- Added Git metadata delivery rules forbidding `.git/` folders and internal Git files in patch/full-source ZIPs.
- Initial Android/Solana development documentation generated from the Blox Monster Adventure source audit.
- Source authority, mobile UI, Solana security, architecture, testing, and roadmap rules.
- `PHASED_DELIVERY_PLAN.md` covering Radiant Rush phases from repository foundation through final APK, demo video, GitHub repo, and pitch deck.
- `DOCUMENTATION_AND_VERSIONING_RULES.md` covering when to update changelog, architecture, agent rules, testing rules, UI standards, Solana security rules, and Android app versions.
- `GRADLE_AAR_METADATA_FIX_COMPOSE_202608.md` documenting the Compose/Core dependency compatibility fix.

### Changed

- Bumped Android app version to `versionCode = 2`, `versionName = "0.2.0-phase2"` for the Firebase foundation milestone.
- Updated the app shell copy and dynamic UI state so screens display Firebase readiness/progress instead of Phase 1 static preview only.

- Updated `AGENTS.md` and `DOCUMENTATION_AND_VERSIONING_RULES.md` to require Git-metadata-free packaging and no-Git checks before delivery.
- Updated `README.md` to include the Radiant Rush product direction and new phased delivery plan.
- Updated `BACKLOG_AND_ROADMAP.md` to prioritize the wallet connect -> quest proof -> Firebase save -> XP/streak MVP flow.
- Updated `AGENTS.md`, `ARCHITECTURE.md`, `TESTING_AND_RELEASE.md`, and docs `README.md` to enforce documentation/versioning decisions for future patches.
- Downgraded Phase 1 AndroidX/Compose dependency pins to avoid API 37 / AGP 9.1+ AAR metadata requirements while staying on AGP 9.0.1 and compileSdk 36.

### Fixed

- Fixed Android Studio AAR metadata sync failure caused by Compose 1.12.x and AndroidX Core 1.19.x requiring API 37 and newer Android Gradle Plugin than the Phase 1 baseline.

### Known gaps

- Firebase sync requires the owner to add a real `app/google-services.json`, enable Anonymous Auth, create Firestore, and apply the included rules.
- Phase 2 client-side XP/streak writes are MVP-only and must not be treated as secure reward authority.
- Exact Android package/module structure is not yet audited.
- Exact Solana SDK/wallet integration choice is not yet locked.
- No build/test evidence exists yet for this new Android project.
