## Phase 10 — Radiant game layer

**Implemented vertical slice:** proof quest → free Rush Ticket → 20-second Radiant Run → score/combo → capsule → collectible/duplicate shards → persisted progression.

**Phase 10.1 implemented:** procedural BGM/SFX, hit particles, PERFECT center hits, FEVER/final-seconds escalation, mute control, and stronger native Canvas feedback without imported media assets.

**After Phase 10.1 is stable:** tune target cadence and score curve on real phones; expand capsule reveal staging/collection presentation; consider weekly run-score ranking only after anti-cheat/server validation exists; add optional original art without making gameplay depend on asset downloads. Do not add paid/random token wagering.

# RadiantSolanaHackatonAndroid — Backlog and Roadmap

## Current phase

Radiant Rush is now in **Phase 10.1: game juice / procedural audio + VFX**. The stable foundation includes native Compose, Firebase progression, MWA proofs, devnet Memo proof, read-only mainnet SKR Passport, wallet-deduplicated ranks, Daily Radiant Chest, and the Phase 10 Radiant Run/capsule/collection loop.

Immediate owner task: run the automated gate, then physically QA synthesized audio, particles, PERFECT/FEVER feedback, mute behavior, and persistence on the target phone before merging Phase 10.1.

## Owner-ordered near-term work

1. Create or audit the Android project skeleton. — done for Phase 1
2. Add native mobile-first Compose shell for Radiant Rush. — done for Phase 1
3. Add Firebase Auth and Firestore profile/quest persistence. — done for Phase 2
4. Add Mobile Wallet Adapter connection flow. — done for Phase 3
5. Add signed daily proof quest. — delivered in Phase 4 patch
6. Add devnet memo transaction proof quest. — delivered in Phase 4 patch, needs device/RPC test
7. Add transaction confirmation polling and status refresh.
8. Add SKR balance/tier detection and XP multiplier. — delivered in Phase 5 patch
9. Add streaks, badges, and leaderboard polish.
10. Polish mobile UI for small phones and Seeker device demo.
11. Prepare APK, demo video, GitHub repo, and pitch deck.

## Phase 5 delivered scope

- Read SKR token balance on mainnet-beta using the connected wallet public address.
- Keep SKR read-only; no paid transaction, staking, or transfer is required.
- Add SKR tiers and XP multipliers.
- Add SKR badge state and Profile/Home display.
- Clearly distinguish devnet quest proofs from mainnet SKR balance reads.

## Phase 6 delivered scope

- Added judge-friendly Demo tab and 3-minute walkthrough.
- Added demo readiness state using existing quest, wallet, Firebase, and SKR data.
- Updated onboarding and screen copy for the hackathon recording flow.

## Phase 7 delivered scope

- Added JVM unit tests for SKR tiering and quest interaction boundaries.
- Added Compose smoke tests for Welcome → app shell → Demo navigation.
- Added local test scripts and GitHub Actions CI.
- Bumped app version to `0.7.0-phase7`.

## Remaining release scope

- Add transaction confirmation polling and explorer status refresh if time allows.
- Add richer SKR Passport visuals/profile frame if time allows.
- Add app icon/splash polish and final demo screenshots.
- Prepare APK, demo video, GitHub README, and pitch deck.

## Deferred until core proof flow is stable

- Token launch/minting logic.
- NFT/SFT reward minting.
- Custom on-chain program integration.
- Cloud Functions or paid backend services.
- Push notifications beyond optional local reminder.
- Production mainnet write transactions.
- Real reward distribution requiring server authority.

## Execution rule

Do not start broad features before the primary demo flow works. A small complete flow is better than many half-working screens. Follow `PHASED_DELIVERY_PLAN.md` for phase order and acceptance checks.


## Phase 7 status

Current priority is final QA and release readiness. Automated unit tests and Compose smoke tests are now part of the checklist. Remaining work should be limited to submission polish unless a blocker appears.

Next release-focused tasks:

- Run `:app:testDebugUnitTest` before every commit.
- Run `:app:connectedDebugAndroidTest` on an emulator or phone before final recording.
- Do one manual wallet proof pass on the target phone.
- Record the 3-minute demo from the Demo tab.
- Prepare final APK and GitHub README for judges.

## Phase 9 completed

- Added no-loss Daily Radiant Chest to improve fun, retention, and demo excitement.
- Added deterministic reward-loop rules and unit tests.

## Later ideas, not in Phase 9

- Chest animation/VFX polish.
- Daily countdown to next chest.
- Cosmetic-only collectible badges from chests.
- Real reward mechanics only after security/legal review.
