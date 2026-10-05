# Phase 11C.4 — My Stats wallet-sync fix

## Problem

Radiant Run public Weekly and All-Time boards already deduplicate by connected Solana wallet, but the `My Stats` card still read Personal Best, combo, PERFECT-hit, weekly-run, and gameplay-XP values primarily from the current Firebase Anonymous Auth UID.

Using the same wallet on another phone or after an anonymous-auth reinstall can create another Firebase UID. Public ranks still looked correct, while `My Stats` could appear empty or reset because the current UID did not own the older personal mirrors.

## Fix

- `My Stats` now resolves Weekly and All-Time personal competition rows by the connected full Solana wallet address, across all matching anonymous UIDs.
- Weekly PB, All-Time PB, best combo, PERFECT hits at PB, and weekly ranked-run count therefore follow the wallet across devices/reinstalls.
- Existing `users/{uid}` fields remain a migration/fallback mirror; they are not deleted.
- `runWalletDaily/{utcDay}/wallets/{walletAddress}` now also mirrors `gameplayXpEarnedToday`, so the 300 XP/day cap and the displayed daily XP value are wallet/day scoped instead of device/UID scoped.
- Ranked attempts remain three per connected wallet per UTC day.
- Public raw score is still client-reported prototype data and is not payout authority.

## Mobile UI

The My Stats metric rows stack vertically on tiny screens or large font scales and allow two display lines, preventing label/value clipping or bleed.

## Firebase rules

`runWalletDaily` accepts monotonic updates to either ranked attempts (maximum +1 per transaction) or gameplay XP (never decreasing and capped at 300). Existing Phase 11B.1 documents without the new XP field are supported using a zero migration default.

## Documentation decision

- CHANGELOG: updated.
- ARCHITECTURE: updated for wallet-scoped personal-stat/read model and daily XP state.
- AGENTS: unchanged; contributor workflow is unchanged.
- TESTING_AND_RELEASE: updated with regression checks.
- SOLANA_SECURITY_AND_DATA_RULES: updated because the shared wallet/day prototype state now also carries capped gameplay XP.
- MOBILE_UI_UX_STANDARDS: updated for My Stats compact/large-font metric layout.
- VERSION: unchanged at `versionCode 20` / `1.1.5-phase11c3`; this is a focused Phase 11C regression hotfix, not a new milestone APK handoff.
