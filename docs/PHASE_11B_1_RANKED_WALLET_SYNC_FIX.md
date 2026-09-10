# Phase 11B.1 — Ranked Wallet Sync Fix

## Why this patch exists

Two-device testing exposed an important Phase 11B behavior. A Radiant Run can still award controlled gameplay XP even when it is Casual, while Weekly and All-Time boards only receive Ranked runs. That is intentional, but the UI did not make the reason obvious enough.

The same test also exposed a real fairness bug: Phase 11B stored the three daily ranked attempts on `users/{firebaseUid}`. Firebase Anonymous Auth creates a different UID per installation, so the same wallet could receive another three ranked attempts on another phone.

## Fix

- Ranked eligibility still requires a connected Solana wallet.
- The three daily ranked attempts are now shared by wallet + UTC day at `runWalletDaily/{utcDay}/wallets/{walletAddress}`.
- The write occurs inside the same Firestore transaction as the run result.
- Existing Phase 11B per-UID counters are used as a migration fallback until the shared wallet/day document exists.
- Weekly and All-Time leaderboard rows remain UID-owned but are deduplicated by wallet for public display.
- The UI now explains exactly why a run is Ranked or Casual.
- Gameplay XP remains available on Casual runs and remains capped separately.

## Security

This is prototype competition fairness only. Client-produced run scores and client-established wallet/profile state are not payout authority. `payoutEligible` remains false and no real SKR payout is enabled.

## Documentation decisions

- CHANGELOG: updated.
- ARCHITECTURE: updated for wallet/day shared attempt state.
- AGENTS: unchanged; engineering workflow did not change.
- TESTING_AND_RELEASE: updated with two-device QA.
- SOLANA_SECURITY_AND_DATA_RULES: updated to describe prototype-only wallet fairness.
- MOBILE_UI_UX_STANDARDS: updated for explicit Ranked/Casual reason copy.
- VERSION: bumped to `versionCode 16`, `versionName 1.1.1-phase11b1`.
