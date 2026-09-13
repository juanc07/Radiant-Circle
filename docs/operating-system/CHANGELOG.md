# 2026-09-12 — Phase 11F.1 Home compile hotfix

- Added the missing public Compose `androidx.compose.foundation.layout.padding` import used by the new retention cards in `HomeScreen.kt`.
- Fixes `Unresolved reference 'padding' on receiver of type 'Modifier'` at the Phase 11F Home retention card call sites.
- No retention economics, leaderboard behavior, profile identity, wallet/SKR logic, Firestore rules, or ranked competition behavior changed.
- Documentation decision: CHANGELOG and TESTING_AND_RELEASE updated. ARCHITECTURE, AGENTS, MOBILE_UI_UX_STANDARDS, SOLANA_SECURITY_AND_DATA_RULES, and VERSION unchanged.
- Version remains `versionCode = 24`, `versionName = "1.1.9-phase11f"`.

# 2026-09-12 — Phase 11F retention + public identity

- Added rotating UTC-daily goals and weekly activity goals built from persisted Radiant Run/chest progress.
- Added Home “What’s next” guidance, streak milestone, and collection milestone presentation.
- Added player-chosen display name + bundled animal avatar editing.
- Added animal avatars and a clear `YOU` marker to Weekly Cup, Run All-Time, and XP leaderboard rows.
- Added daily/weekly activity counters without changing Ranked scoring, Ranked attempt limits, SKR perks, or payout authority.
- Version: `24 / 1.1.9-phase11f`.

# 2026-09-12 — Phase 11E final reward / chest polish

- Reworked Daily Radiant Chest presentation into a faster, rarity-aware reveal with stable geometry, soft rays, rarity-scaled particles/shockwaves, a persistent reward orb, centered reward tray, and stronger procedural reveal audio/haptics.
- Fixed chest rarity presentation mapping so the real reward ladder (`Spark`, `Pulse`, `Flare`, `Aurora`, `Legendary`) now drives escalating VFX/audio instead of most tiers falling back to the same rank.
- Firestore chest claim now starts immediately. Only very fast saves are held to a 520 ms minimum opening pose; slow saves reveal as soon as the real result arrives, removing the previous fixed pre-request delay.
- Claimed reward UI now separates total XP, standard Rush Tickets, SKR casual tickets, and SKR XP boost into readable responsive blocks instead of one dense line.
- No deterministic reward odds, XP/ticket amounts, SKR perk amounts, Ranked attempts/score, wallet signing, sponsor payout authority, or Firestore rules changed.
- Added JVM tests for the presentation-only rarity ladder and visual-intensity progression.
- Android version bumped to `versionCode = 23`, `versionName = "1.1.8-phase11e"`.
- Documentation decision: CHANGELOG, ARCHITECTURE, TESTING_AND_RELEASE, SOLANA_SECURITY_AND_DATA_RULES, MOBILE_UI_UX_STANDARDS, and Phase 11E notes updated. AGENTS unchanged.

# 2026-09-12 — Phase 11D Weekly Radiant Cup + sponsor-ready architecture

- Reframed the existing weekly Ranked Radiant Run board as the Weekly Radiant Cup with UTC season countdown, Top 3 podium, standings, participant count, connected-wallet rank, and weekly PB.
- Added weekly cosmetic placement rewards: Champion Crest, Podium Crest, Top 10 Ribbon, and Finisher Mark. Previous-week Top 100 data can surface the connected wallet's prior-season accolade without granting economic value.
- Added optional read-only `weeklyCupConfigs/{ISO_WEEK}` sponsor metadata. Android clients can never write sponsor configuration.
- Sponsor metadata is display-only. `payoutEnabled` is hard-coded false in domain rules; no client score, sponsor document, SKR balance, or stake balance can authorize token payout.
- No player entry fee, wagering, treasury key, automatic SKR transfer, or payout claim flow was added.
- Added JVM tests for UTC season close, prior-week key, placement tiers, sponsor safety, and wallet-deduped personal rank.
- Firestore rules changed and must be redeployed before sponsor metadata can be read from production.
- Android version bumped to `versionCode = 22`, `versionName = "1.1.7-phase11d"`.
- Documentation decision: CHANGELOG, ARCHITECTURE, TESTING_AND_RELEASE, SOLANA_SECURITY_AND_DATA_RULES, MOBILE_UI_UX_STANDARDS, and Phase 11D notes updated. AGENTS unchanged.

# 2026-09-12 — Phase 11C.5.2 centered-content polish

- Fixed badge cards whose centered children could still appear off-center because the inner `Column` and text measured at wrap-content width instead of the full card width.
- Fixed Radiant Run completion/reward cards so headline, score, save/reward copy, and summary text center against the full card width.
- Centered Quest detail dialog title, body, and acknowledgement action for consistent popup presentation.
- No gameplay, wallet, SKR, staking, Firestore rules, competition logic, reward authority, or payout eligibility changed.
- Documentation decision: CHANGELOG, TESTING_AND_RELEASE, MOBILE_UI_UX_STANDARDS, and Phase 11C.5 notes updated. ARCHITECTURE, AGENTS, SOLANA_SECURITY_AND_DATA_RULES, and VERSION unchanged.
- Version remains `versionCode = 21`, `versionName = "1.1.6-phase11c5"`.

# 2026-09-12 — Phase 11C.5.1 Compose compile hotfix

- Removed the explicit `androidx.compose.foundation.layout.weight` import from `RadiantIdentityComponents.kt`.
- `Modifier.weight(...)` is invoked inside `Row` scope and resolves through the public `RowScope` API; the explicit import was resolving to an internal Compose implementation symbol under the project's current Compose/Kotlin toolchain.
- No UI behavior, SKR logic, staking logic, competition rules, Firestore rules, payout authority, or reward calculations changed.
- Documentation decision: CHANGELOG and TESTING_AND_RELEASE updated. ARCHITECTURE, AGENTS, SOLANA_SECURITY_AND_DATA_RULES, MOBILE_UI_UX_STANDARDS, and VERSION unchanged.
- Version remains `versionCode = 21`, `versionName = "1.1.6-phase11c5"`.

# 2026-09-11 — Phase 11C.5 visual identity + badge polish

- Replaced generic badge check/bolt marks with distinct scalable Radiant Rush crests for launch, daily, wallet, proof, on-chain, SKR, chest, run, combo, collection, and streak achievements.
- Added a responsive SKR Passport crest card showing tier, eligible SKR, stake boost, frame, and aura without changing SKR or ranked-game authority.
- Tightened badge copy and kept all badge/Passport text wrapping instead of clipping or ellipsizing on compact/large-font layouts.
- Android version bumped to `versionCode = 21`, `versionName = "1.1.6-phase11c5"`.
- Firestore rules, wallet signing, SKR reads/staking logic, ranked attempts, gameplay-XP caps, chest reward authority, and payout eligibility are unchanged.
- Documentation decision: CHANGELOG, ARCHITECTURE, TESTING_AND_RELEASE, and MOBILE_UI_UX_STANDARDS updated; AGENTS and SOLANA_SECURITY_AND_DATA_RULES unchanged.

## Phase 11C.4.1 — connected-device smoke-test restoration fix

- Fixed physical-device Compose smoke tests that could time out when Android restored the shell on a non-Home tab.
- The test now detects the stable bottom navigation shell, explicitly selects Home, and only then asserts Home content.
- Radiant Run route visibility is now transient rather than saveable, matching the in-memory run state and preventing restoration into a half-reset run after Activity recreation.

# 2026-09-11 — Phase 11C.4 My Stats wallet-sync fix

- Fixed `My Stats` resetting to empty/zero values when the same Solana wallet is used under a different Firebase Anonymous Auth UID on another device or reinstall.
- Weekly PB, All-Time PB, best combo, PERFECT hits at PB, and weekly ranked-run count now resolve by connected wallet across matching run rows.
- Moved the 300 gameplay-XP daily cap mirror into the existing wallet/day competition document so daily XP cannot reset per device/UID.
- Kept `users/{uid}` competition fields as migration/fallback mirrors.
- Made My Stats metric rows stack on tiny/large-text layouts to avoid clipping and bleed.
- Firestore rules updated for monotonic wallet/day gameplay-XP updates capped at 300; `payoutEligible` remains false.
- Documentation decision: CHANGELOG, ARCHITECTURE, TESTING_AND_RELEASE, SOLANA_SECURITY_AND_DATA_RULES, and MOBILE_UI_UX_STANDARDS updated. AGENTS unchanged. VERSION unchanged at `versionCode = 20`, `versionName = "1.1.5-phase11c3"` because this is a focused regression hotfix inside the current Phase 11C milestone.

# 2026-09-11 — Phase 11C.3 chest response timing hotfix

- Reduced the artificial Daily Radiant Chest `Opening` hold from 1.8 seconds to 850 ms so feedback remains immediate while the ~760 ms charge/open animation still has time to land before the claim result replaces it.
- Reward authority, Firestore behavior, Solana/SKR/staking logic, ranked competition rules, chest reward amounts, and chest visual geometry are unchanged.
- Documentation decision: CHANGELOG and TESTING_AND_RELEASE updated. ARCHITECTURE, AGENTS, SOLANA_SECURITY_AND_DATA_RULES, and MOBILE_UI_UX_STANDARDS unchanged because this is presentation timing only. VERSION unchanged at `versionCode = 20`, `versionName = "1.1.5-phase11c3"`; this is a hotfix inside the same Phase 11C.3 milestone.

# 2026-09-11 — Phase 11C.3 responsive text + pastel game + chest smoothness

- Fixed phone-width classification so common ~400–430dp Android devices use compact layouts before text is forced into narrow multi-column cards.
- Added measured single-line shrinking for single-token metric values such as `Explorer`; required values must fit rather than split, clip, or bleed.
- Restored the original pastel Radiant Run palette: normal Radiant targets use the theme primary pastel, FEVER targets use the theme tertiary pastel, and Corruption keeps the error/pastel-red treatment.
- Kept FEVER decoration non-circular so it cannot be mistaken for a tappable target; removed the ambiguous in-canvas BEST/combo affordance from the target area.
- Reworked chest anticipation motion from charge-driven high-frequency oscillation to a short damped keyframed shake, and removed whole-chest breathing scale to prevent visible jitter.
- Continued player-copy cleanup by removing remaining implementation-facing wallet/phase wording from normal UI messages.
- Firestore rules: unchanged. Solana proof, SKR/staking reads, competition rules, payout authority, and reward authority: unchanged.
- Android version bumped to `versionCode = 20`, `versionName = "1.1.5-phase11c3"`.
- ARCHITECTURE: unchanged; this is presentation/responsiveness polish only. AGENTS: unchanged. SOLANA_SECURITY_AND_DATA_RULES: unchanged.

# 2026-09-11 — Phase 11C.2 product-copy + chest/game clarity polish

- Removed developer/judge/architecture commentary from normal user-facing screens and replaced the Demo tab copy with a concise user Guide.
- Fixed compact text behavior so single-token values such as `Explorer` cannot orphan the final character onto a new line; compact/large-font Profile cards now stack for more usable width.
- Rebuilt the Daily Radiant Chest presentation with a Compose-drawn chest, ready glow, shake/charge sequence, lid/lock burst, light beam, particles, shockwaves, haptics, procedural chest SFX, temporary music, and rarity reveal.
- Held the chest in `Opening` for 1.8 seconds before the repository claim so the animation is actually visible; reward authority remains in Firebase.
- Removed ambiguous cyan FEVER circles from Radiant Run. Valid targets stay green, Corruption stays red, and FEVER is now a gold rectangular screen-state glow.
- Increased the target hit tolerance slightly and replaced implementation-facing game copy with player-facing instructions.
- Firestore rules: unchanged. Solana/MWA/SKR/staking/ranked/payout authority: unchanged.
- Android version bumped to `versionCode = 19`, `versionName = "1.1.4-phase11c2"`.
- AGENTS: unchanged; contributor workflow did not change.

# 2026-09-10 — Phase 11C SKR Passport v2 + fair holder perks

- Upgraded the existing Mainnet read-only SKR scan into Passport v2 without changing the stable MWA/Phantom signing path.
- Added deterministic SKR tier perks: daily casual-only Rush Tickets, fixed Daily Radiant Chest enhancements, and profile frame/aura/badge status.
- Split holder bonus tickets into `skrCasualRushTickets`; they can never fund ranked play or increase the three ranked attempts per UTC day. Ranked runs always require a standard ticket; Casual runs prefer an SKR casual ticket.
- Made the completed SKR Passport quest safely refreshable and same-day ticket grants idempotent/tier-up aware: repeat scans do not duplicate grants, while a higher tier receives only the missing difference.
- Kept the Daily Radiant Chest base rarity roll independent from SKR tier; holder benefits are fixed post-roll XP/casual-ticket enhancements only.
- Staked SKR remains explicitly not verified/not counted because Phase 11C has no trustworthy staking source. No staking state is inferred or faked.
- Passport cosmetics are off-chain app status, not NFTs/on-chain assets.
- Added JVM tests for Passport tiers, daily grant idempotence and ranked eligibility when only an SKR casual ticket remains.
- Firestore rules decision: unchanged; Passport fields use existing owner-writable user/completed-quest documents and Phase 11C adds no public collection.
- Android version bumped to `versionCode = 17`, `versionName = "1.1.2-phase11c"`.
- AGENTS decision: unchanged; contributor/source-control workflow did not change.

# 2026-09-10 — Phase 11B.1 ranked-wallet sync + empty-board diagnostics

- Fixed the confusing case where a completed Radiant Run could award gameplay XP while Weekly/All-Time stayed empty: the UI now states clearly when the run is Casual because no wallet is connected or the daily ranked wallet allowance is exhausted.
- Ranked-attempt usage is now persisted in `runWalletDaily/{utcDay}/wallets/{walletAddress}` so the same connected Solana wallet shares one 3-attempt UTC-daily allowance across phones/reinstalls instead of receiving 3 attempts per Firebase Anonymous UID.
- Existing Phase 11B per-UID attempt counters are migrated into the shared wallet/day document on the next saved run.
- `completeRadiantRun` can recover a canonical wallet address from the existing public leaderboard row when an older profile mirror is missing, then backfills the user profile.
- Weekly/All-Time score rows remain Firebase-UID-owned and public display still collapses duplicate UIDs by wallet.
- Client-produced score and wallet/day counters remain prototype competition data only and cannot authorize real SKR payout.
- Android version bumped to `versionCode = 16`, `versionName = "1.1.1-phase11b1"`.
- AGENTS decision: unchanged; workflow rules did not change.

# 2026-09-10 — Phase 11A SKR Arena competition foundation

## Phase 11B — Radiant Run competition persistence + visible ranks

- Wired the Phase 11A competition rules into real Radiant Run completion.
- Added Firebase-backed UTC Weekly and All-Time ranked Personal Best boards, with public rows deduplicated by connected Solana wallet.
- Added Personal Stats UI, daily ranked-attempt status, and gameplay-XP-cap progress.
- Added real PERFECT-hit counting to submitted run results.
- Ranked attempts are equal for every connected wallet: 3 per UTC day; later ticket-backed runs are Casual and cannot replace ranked PBs.
- Replaced uncapped run-performance XP with the Phase 11 controlled award and 300 XP/day cap. Collectible/shard rewards remain non-token app progression.
- Added `runWeekly/{weekKey}/entries/{uid}` and `runAllTime/{uid}` Firestore architecture. Client rows are explicitly marked prototype-only and `payoutEligible=false`.
- Updated Firestore rules so users may write only their own competition rows while ranks remain public-readable.
- Preserved existing XP leaderboard, MWA/Phantom proof paths, and Mainnet read-only SKR scan.
- Android version decision: visible milestone patch, so bumped to `versionCode = 15`, `versionName = "1.1.0-phase11b"`.
- AGENTS decision: unchanged; contributor/patch workflow rules did not change.


## Added

- Added pure `Phase11CompetitionRules` models/rules for UTC Weekly and All-Time Radiant Run competition.
- Added wallet-deduplicated run ranking candidates that preserve the Phase 9.1 disconnect/legacy-short-wallet compatibility behavior.
- Added equal daily ranked-attempt rules: 3 ranked attempts per connected wallet per UTC day; additional ticket-backed runs are casual.
- Added controlled run-performance XP conversion with a 300 XP UTC-daily cap. Ranked competition continues to use raw run score, not XP.
- Added `RunScoreRecord`, `WeeklyRunStats`, `RunPersonalBest`, ranked-attempt, and gameplay-XP decision models for Phase 11B persistence/UI wiring.
- Added JVM unit-test coverage for UTC boundaries, XP caps/resets, ranked/casual decisions, personal-best ordering, wallet deduplication, disconnect tombstones, and payout-safety metadata.

## Safety

- `RunScoreRecord` is explicitly `ClientReportedPrototype`; its `payoutEligible` property is hard-coded `false`.
- Phase 11A adds no SKR transfer, wager, staking, treasury key, payout transaction, wallet signing, or RPC behavior.
- SKR ownership is deliberately absent from ranked-score and ranked-attempt calculations.

## Version

- Android `versionCode` / `versionName` are unchanged in Phase 11A because this patch is a pure competition-domain foundation and not an APK/release handoff.

# 2026-09-10 — Phase 10.1 Radiant Run procedural audio + VFX juice

## Added

- Added `ProceduralGameAudioEngine`, a runtime PCM synth/mixer using Android `AudioTrack`; no MP3/WAV assets are required.
- Added procedural BGM plus synthesized countdown, GO, hit, PERFECT, corruption, miss, FEVER, final-five-second, run-complete, capsule, and reward-reveal cues.
- Added in-game mute/unmute.
- Added moving Canvas grid, pulsing targets, impact particles/rings, FEVER rings, corruption flash, and PERFECT flash.
- Added a visible center-hit PERFECT zone and +50 skill score bonus.

## Changed

- Radiant Run presentation now escalates during FEVER and the final five seconds.
- Replaced the deprecated game-screen back icon with the AutoMirrored variant.
- Bumped Android app version to `versionCode = 13`, `versionName = "1.0.1-phase10.1"`.

## Safety

- Audio/VFX are presentation-only and cannot block the run if device audio initialization fails.
- No wallet/Solana/token/economic authority changed.

# 2026-09-10 — Phase 10 Radiant native game layer

## Phase 10 Android UI test v2 launch stability fix

- Migrated the physical-device Compose smoke test to `androidx.compose.ui.test.junit4.v2.createAndroidComposeRule`.
- Added startup readiness that waits for either Welcome or the already-restored Home shell before assertions.
- Removed the assumption that a specific lazy-list game CTA must already be composed.
- Production app behavior is unchanged.

## Phase 10 Android smoke-test Home anchor fix

- Added a stable `screen_home` semantics anchor to the Home `LazyColumn`.
- Fixed the instrumentation smoke test so it no longer expects the below-the-fold `radiant_run_play` lazy item to be composed immediately after entering the shell.
- Production gameplay, wallet, Firebase, SKR, rewards, and version metadata are unchanged.


## Added

- Added free Rush Tickets as the bridge from real daily proof actions into gameplay.
- Added a 20-second native Jetpack Compose `Radiant Run` reflex game with target taps, Corruption penalties, combo scoring, FEVER mode, haptics, and a score-based capsule reveal.
- Added six persistent Radiant collectibles plus duplicate-to-Radiant-Shards conversion.
- Added Home Radiant Run launcher and Vault collection strip, plus Profile run/ticket/reward details.
- Added Firebase persistence for tickets, run score/best score, run count, latest capsule, collection counts, shards, and run XP.
- Added `RadiantGameRulesTest` coverage for ticket defaults, deterministic rewards, duplicate shards, capsule tiers, and collection preview.

## Changed

- New daily proof completions award +1 Rush Ticket; the Daily Radiant Chest awards +2. Existing profiles without the field use a 3-ticket compatibility default.
- Bumped Android app version to `versionCode = 12`, `versionName = "1.0.0-phase10"`.
- Firebase profile phase writes now identify Phase 10.

## Safety

- Rush Tickets, Radiant Shards, capsules, and collectibles are app-only progression with no SOL/SKR spend, no XP wagering, no token transfer, and no cash/token redemption.
- Radiant Run scoring remains client-side for this hackathon slice and must not be treated as economic authority.

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

## Phase 10.1.1 — Game juice polish
- Added pastel, high-contrast hit/miss feedback colors for Radiant Run.
- Changed successful-hit procedural SFX to a musical combo pitch climb.
- Added x10+ combo shake, stronger particles/shockwaves, and procedural milestone burst cues.
- Scaled reward particle explosions and procedural reveal audio by rarity; Epic+ rewards now receive a substantially stronger celebration.
- Version: `1.0.2-phase10.1.1` (`versionCode 14`).

## 1.1.3-phase11c1 — Phase 11C.1 verified staked SKR

- SKR Passport v2 now reads official Solana Mobile active SKR staking state from Mainnet by public wallet address.
- Passport tier eligibility uses verified liquid + verified active staked SKR; unstaking/cooldown SKR is shown but excluded from Stake Boost.
- Added Guardian Stake Boost: +1 casual-only ticket/day, +25 Daily Radiant Chest XP, and cosmetic Guardian Glow status.
- Staking read failures fail honest: no inferred/fake stake balance, while the liquid scan can remain usable.
- Daily chest uses the exact perk values persisted by today's Passport scan, preserving staking bonus without changing the deterministic base rarity roll.
- Ranked raw score and three daily ranked attempts remain unchanged by liquid/staked SKR wealth.
- Version bumped to `1.1.3-phase11c1` (`versionCode 18`).
