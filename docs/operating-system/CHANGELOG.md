# 2026-09-10 — Phase 9.1.1 Leaderboard duplicate compatibility fix

## Fixed

- Exclude legacy leaderboard rows explicitly marked `No wallet` even when an older build left a stale full `walletAddress` field behind.
- Normalize legacy `AAAA…BBBB` / `AAAA...BBBB` wallet representations before deduplication.
- Show the shortened wallet identity on each rank row so genuinely different wallets can be distinguished from duplicate Firebase anonymous UIDs.

## Changed

- Bumped Android app version to `versionCode = 11`, `versionName = "0.9.2-phase9.1.1"`.

# 2026-09-10 — Phase 9.1 Leaderboard wallet identity fix

## Fixed

- Public ranks now collapse duplicate Firebase Anonymous Auth rows by connected Solana wallet identity.
- Leaderboard writes now persist the full public wallet address in addition to the shortened display address.
- Legacy anonymous rows can no longer crowd the visible Top 20 because reads fetch a wider candidate set before wallet deduplication.
- Anonymous/no-wallet rows are excluded from public ranks.

## Changed

- Bumped Android app version to `versionCode = 10`, `versionName = "0.9.1-phase9.1"`.
- Firebase UID remains the internal owner key; no Firestore ownership/security relaxation was introduced.

# 2026-09-10 — Phase 9 Radiant reward loop
## 2026-09-10 — Phase 9 Welcome compile fix

- Fixed a Kotlin named-argument syntax error in `WelcomeScreen.kt` by adding the missing comma before the `trailing` lambda on the Phase 9 hero card.
- No wallet, Firebase, SKR, quest, or reward logic changed.

## Added

- Added Daily Radiant Chest reward loop after all daily proof quests are complete.
- Added deterministic no-loss reward reveal with Spark, Pulse, Flare, Aurora, and Legendary reward tiers.
- Added Firebase persistence for chest claim date, reward rarity, reward title, bonus XP, and total chest XP.
- Added Profile and Demo tab support for showing the latest chest reward.
- Added `RewardLoopRules` and automated unit tests for chest gating and deterministic rewards.

## Changed

- Bumped Android app version to `versionCode = 9`, `versionName = "0.9.0-phase9"`.
- Updated app copy from pure demo/submission mode toward a daily quest game loop.
- Updated Firebase profile `phase` writes to `9`.

## Safety

- Chose a no-loss chest reveal instead of XP betting. The chest never spends XP, SOL, SKR, or tokens and never opens the wallet.

# Changelog

## 2026-09-09 — Phase 7 Final QA + Automated Testing

- Bumped Android app version to `0.7.0-phase7`.
- Added pure JVM unit tests for SKR tiering and quest interaction rules.
- Added stable Compose UI test tags and instrumented smoke tests for Welcome → app shell → Demo tab.
- Added local test scripts for Git Bash/macOS/Linux and PowerShell.
- Added GitHub Actions Android CI to run unit tests and debug APK build.
- Kept wallet/MWA proof flows unchanged to avoid destabilizing Phase 4/5 behavior.

## 0.6.0-phase6 — Demo Polish + Retention

- Added a new `Demo` bottom navigation tab for hackathon judging and recording.
- Added a 3-minute demo walkthrough covering Firebase, MWA wallet connect, message signing, devnet Memo proof, SKR Passport, Profile proof summary, and security boundaries.
- Added a demo readiness card with quest completion count, XP, streak, wallet, signed proof, memo proof, and SKR scan state.
- Updated Welcome, Home, Quests, and Profile copy to explain the judge flow more clearly.
- Bumped Android app version to `versionCode = 6`, `versionName = "0.6.0-phase6"`.


## 0.5.1-phase5 — UX Tap Reliability Fix

- Clarified SKR Passport behavior: the SKR scan is read-only mainnet RPC and should not open Phantom.
- Added active quest tracking so one accepted tap immediately locks the running quest.
- Added syncing labels for check-in, wallet connect, proof signing, memo submission, and SKR scanning.
- Added a short tap debounce on quest action buttons to reduce accidental duplicate submissions.
- Kept wallet actions disabled while Firebase saves proof results.
- Disabled Firebase refresh while a wallet/proof/SKR action is running.

## 0.4.5-phase4 — Memo Preflight + Min Context Slot Fix

- Fixed Phase 4 memo proof flow where tapping `Send Memo` could only open Phantom connection and never reach transaction approval.
- Fetch devnet blockhash before wallet handoff so RPC/DNS failures are caught before Phantom opens.
- Parse devnet context slot and pass it as MWA `TransactionParams.minContextSlot` for Phantom compatibility.
- Reuse the active wallet authorization for memo submission instead of forcing a fresh connection-only prompt.
- Clear wallet auth token on authorization/auth-token failure so reconnecting can recover cleanly.
- Added clearer Logcat and user-facing errors for devnet RPC/DNS/timeout issues.

# RadiantSolanaHackatonAndroid — Changelog

All meaningful changes should be recorded here. Keep exactly one `[Unreleased]` section at the top.

## [Unreleased]

### Added

- Added Phase 5 SKR Passport scan using Solana mainnet `getTokenAccountsByOwner` for the official SKR mint.
- Added read-only SKR balance snapshot persistence to Firebase.
- Added SKR tiers: Explorer, Radiant Scout, Radiant Holder, Radiant Elite, and Radiant Legend.
- Added SKR XP multiplier display on Home and Profile.
- Added zero-balance behavior so Devnet testing still works without fake SKR.

### Changed

- Bumped Android app version to `versionCode = 5`, `versionName = "0.5.0-phase5"`.
- Updated Firebase profile `phase` writes from `4` to `5`.
- Updated the SKR quest from locked Phase 5 placeholder to `Scan SKR Passport`.


### Fixed

- Fixed Phase 4 memo proof authorization failure caused by reusing a Mobile Wallet Adapter session that could enter a failing `reauthorize` path in Phantom.
- Proof signing and memo submission now use fresh devnet MWA authorization sessions while keeping wallet connect/disconnect state separate.
- Replaced fake-clickable proof/status chips with passive pills so `Signature` no longer looks like a dead button.
- Added copy actions in Profile for wallet address, signed proof signature, and memo explorer/transaction values.


### UI / Mobile Responsiveness

- Added shared responsive sizing helpers for screen padding, card padding, button height, button text size, and bottom navigation text size.
- Updated action buttons to use adaptive labels and two-line-safe text instead of clipped one-line labels.
- Shortened Phase 4 quest copy for small Android phones and accessibility font scaling.
- Updated Home, Quest, Welcome, Profile, Badges, and Leaderboard screens to reduce text clipping on compact devices.


### Added

- Added Phase 4 Solana proof quests:
  - `Sign Daily Proof` uses Mobile Wallet Adapter `signMessagesDetached` to request a real wallet signature.
  - `On-Chain Memo Proof` builds a devnet Memo transaction and submits it through Mobile Wallet Adapter `signAndSendTransactions`.
- Added devnet Solana Memo transaction builder in `MobileWalletRepository`.
- Added Firebase persistence for signed message signatures, memo transaction signatures, explorer URL, and Phase 4 proof quest completion documents.
- Added `docs/PHASE_4_SOLANA_PROOF_QUESTS.md` and `docs/PHASE_4_PATCH_APPLY_AND_TEST.md`.
- Added `rpc-ktordriver` dependency for Solana RPC HTTP transport.

### Changed

- Bumped Android app version to `versionCode = 4`, `versionName = "0.4.0-phase4"`.
- Updated quest list from Phase 3 wallet-only state to Phase 4 wallet proof state.
- Updated Home, Quests, and Profile screens to show signed proof and memo proof actions/results.
- Updated Firebase profile phase writes from `3` to `4`.

### Fixed

- Kept the Phase 3 lifecycle fix: `MobileWalletRepository` remains created in `MainActivity.onCreate()` before Compose content.

### Known gaps

- Memo transaction confirmation polling is not implemented yet; Phase 4 saves submitted transaction signatures returned by the wallet.
- SKR token balance detection and XP multipliers remain Phase 5.
- MWA auth token persistence is still deferred.
- Mainnet/live mode is not the Phase 4 target; test/devnet mode is expected.

## 2026-09-09 — Phase 3 MWA lifecycle crash fix

- Fixed startup crash caused by constructing `MobileWalletRepository` inside Compose after the Activity was already `RESUMED`.
- Moved `MobileWalletRepository` creation into `MainActivity.onCreate()` before `setContent`.
- Updated `RadiantRushApp` to receive the wallet repository instead of constructing lifecycle-sensitive MWA objects inside composition.
- Added `docs/PHASE_3_MWA_LIFECYCLE_CRASH_FIX.md`.
- Version unchanged: runtime crash fix only, no new milestone APK accepted yet.

## 2026-09-09 — Phase 4 RPC import fix

- Fixed Kotlin compile failure caused by unresolved `com.solana.rpc.SolanaRpcClient` import.
- Removed direct `rpc-core` / `rpc-ktordriver` dependency usage from Phase 4.
- Added local JSON-RPC `getLatestBlockhash` fetch inside the Solana boundary.
- Kept transaction building with `web3-solana` and transaction signing/submission through Mobile Wallet Adapter.
- Version not changed because this is a build fix inside the same Phase 4 milestone.

## 2026-09-09 - Phase 4 memo proof state fix

- Fixed a Phase 4 UX/data-state bug where `Submit Memo Proof` could remain actionable after wallet approval while Firebase proof persistence was still in progress.
- Added syncing state handling for signed proof and memo proof saves.
- Added profile-level completion fallback using `lastSignedProofDate` and `lastOnChainProofDate`.
- Added `RadiantRushWallet` Logcat markers for wallet signature-return diagnostics.
- No version bump; patch fixes Phase 4 behavior without changing the public milestone.

### Phase 4 memo auth auto-retry fix

- Added a one-time automatic retry for devnet memo proof when Phantom/MWA rejects a stale authorization token.
- The retry clears the cached wallet authorization, rebuilds a fresh devnet memo transaction, and reopens Phantom once.
- Memo proof still only completes after a real transaction signature is returned.
