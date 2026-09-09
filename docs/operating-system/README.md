# RadiantSolanaHackatonAndroid — Development Documentation

This documentation set was created from an audit of the Blox Monster Adventure source tree and maintained docs. The Roblox code is not being ported. The transferable value is the development discipline: source authority, mobile-first UI, server/client ownership, persistence safety, release gates, and honest testing.

## Source identity

- Project name: `RadiantSolanaHackatonAndroid`
- Target: Android app for a Solana hackathon.
- Current state: `Radiant Rush` Phase 2 Firebase foundation is underway: native Compose shell plus Firebase Auth/Firestore profile and progress persistence, with Mobile Wallet Adapter next.
- BMA source role: reference project only. Do not copy Roblox/Luau/Studio-specific code into this Android app.

## Canonical documentation map

Keep this project lean. The maintained operating-system documentation set should stay small. App phase docs may live one level above this folder, such as `docs/PHASE_2_FIREBASE_FOUNDATION.md`.

1. `README.md` — entry point, document map, read order.
2. `AGENTS.md` — AI/developer workflow and source-control contract.
3. `ARCHITECTURE.md` — Android architecture, module ownership, app boundaries.
4. `SOLANA_SECURITY_AND_DATA_RULES.md` — wallet, transaction, key, RPC, and persistence rules.
5. `MOBILE_UI_UX_STANDARDS.md` — mobile-first layout and interaction standards.
6. `TESTING_AND_RELEASE.md` — test matrix, acceptance language, release gates.
7. `BACKLOG_AND_ROADMAP.md` — current build plan and deferred work.
8. `PHASED_DELIVERY_PLAN.md` — phase-by-phase plan from foundation to final APK, video, repo, and pitch deck.
9. `DOCUMENTATION_AND_VERSIONING_RULES.md` — rules for updating docs, architecture, agent workflow, changelog, app versions, and Git-metadata-free handoff packages.
10. `CHANGELOG.md` — forward-only change record.
11. `BMA_SOURCE_AUDIT_FOR_ANDROID.md` — what was learned from the BMA audit.
12. `NEXT_CHAT_START_PROMPT.md` — ready prompt for continuing with another AI session.

Phase-specific implementation docs outside this folder:

- `docs/PHASE_1_NATIVE_ANDROID_APP.md`
- `docs/PHASE_2_FIREBASE_FOUNDATION.md`

## Required read order for new work

1. Read `README.md`.
2. Read `AGENTS.md`.
3. Read `ARCHITECTURE.md`.
4. Read `SOLANA_SECURITY_AND_DATA_RULES.md` for any wallet, balance, token, transaction, RPC, storage, or backend work.
5. Read `MOBILE_UI_UX_STANDARDS.md` for any UI work.
6. Read `PHASED_DELIVERY_PLAN.md` before adding or reprioritizing major features.
7. Read `DOCUMENTATION_AND_VERSIONING_RULES.md` before delivering any code, build, dependency, or release patch.
8. Read `TESTING_AND_RELEASE.md` before claiming a fix is complete.
9. Update `CHANGELOG.md` and, when relevant, `BACKLOG_AND_ROADMAP.md`.

## Operating principle

Make the smallest safe change that improves the current app. Do not rebuild, rename, restructure, add new dependencies, or invent backend behavior unless the task requires it.

## Environment policy

Separate hackathon/demo behavior from production behavior:

- `dev`: local development, mocked/fake data allowed only when clearly labeled.
- `staging/demo`: public demo candidate, no fake claims about real transactions.
- `production`: only when real wallet, RPC, backend, analytics, and privacy rules are approved.

Never mix fake Solana balances or simulated transactions with UI copy that suggests real on-chain state. For Radiant Rush, SKR tier and quest proof must be either real on-chain state or visibly labeled demo data.
