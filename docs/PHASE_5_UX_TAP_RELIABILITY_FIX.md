# Phase 5 UX Tap Reliability Fix

## Purpose

This patch fixes the Phase 5 polish issue where some quest actions felt like they needed several taps and the SKR Passport behavior looked confusing.

## Expected wallet popup behavior

- `Connect Wallet` opens Phantom or another MWA-compatible wallet.
- `Sign Daily Proof` opens Phantom for a message signature.
- `Submit Memo Proof` opens Phantom for a devnet memo transaction.
- `Scan SKR Passport` does **not** open Phantom. It reads the already connected public wallet address and checks mainnet SKR balance through Solana RPC.

## Code changes

- Added `activeQuestId` to `RushUiState`.
- Added `beginQuestAction` / `failQuestAction` flow in `RadiantRushApp` so actions lock immediately after the first accepted tap.
- Kept wallet/proof actions disabled while Firebase is saving.
- Added a 900 ms tap debounce inside `QuestCard`.
- Added visible syncing labels and a small progress spinner for running quest actions.
- Clarified SKR Passport copy in Home, Quests, Welcome, and Profile screens.

## Why this matters

The app should feel reliable during demo testing:

1. Tap once.
2. See immediate feedback.
3. Wait for Phantom only when a wallet signature or transaction is actually needed.
4. See Done after Firebase saves the proof.

The patch does not fake SKR, signatures, or memo transactions.
