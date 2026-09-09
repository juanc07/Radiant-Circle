# RadiantSolanaHackatonAndroid — Changelog

All meaningful changes should be recorded here. Keep exactly one `[Unreleased]` section at the top.

## [Unreleased]

### Added

- Added `GIT_INITIAL_PUSH_FIX.md` with the correct first Git commit/push flow for the Radiant Rush repository.
- Added Git metadata delivery rules forbidding `.git/` folders and internal Git files in patch/full-source ZIPs.
- Initial Android/Solana development documentation generated from the Blox Monster Adventure source audit.
- Source authority, mobile UI, Solana security, architecture, testing, and roadmap rules.
- `PHASED_DELIVERY_PLAN.md` covering Radiant Rush phases from repository foundation through final APK, demo video, GitHub repo, and pitch deck.
- `DOCUMENTATION_AND_VERSIONING_RULES.md` covering when to update changelog, architecture, agent rules, testing rules, UI standards, Solana security rules, and Android app versions.
- `GRADLE_AAR_METADATA_FIX_COMPOSE_202608.md` documenting the Compose/Core dependency compatibility fix.

### Changed

- Updated `.gitignore` to exclude accidental root-level Gradle/Kotlin cache folders that blocked `git add .`.
- Updated `AGENTS.md` and `DOCUMENTATION_AND_VERSIONING_RULES.md` to require Git-metadata-free packaging and no-Git checks before delivery.
- Updated `README.md` to include the Radiant Rush product direction and new phased delivery plan.
- Updated `BACKLOG_AND_ROADMAP.md` to prioritize the wallet connect -> quest proof -> Firebase save -> XP/streak MVP flow.
- Updated `AGENTS.md`, `ARCHITECTURE.md`, `TESTING_AND_RELEASE.md`, and docs `README.md` to enforce documentation/versioning decisions for future patches.
- Downgraded Phase 1 AndroidX/Compose dependency pins to avoid API 37 / AGP 9.1+ AAR metadata requirements while staying on AGP 9.0.1 and compileSdk 36.

### Fixed

- Fixed Android Studio AAR metadata sync failure caused by Compose 1.12.x and AndroidX Core 1.19.x requiring API 37 and newer Android Gradle Plugin than the Phase 1 baseline.

### Known gaps

- Exact Android package/module structure is not yet audited.
- Exact Solana SDK/wallet integration choice is not yet locked.
- No build/test evidence exists yet for this new Android project.
