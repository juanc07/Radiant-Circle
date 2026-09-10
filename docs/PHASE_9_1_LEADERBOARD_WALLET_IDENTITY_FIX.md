# Phase 9.1 — Leaderboard Wallet Identity Fix

## Problem

Firebase Anonymous Auth is intentionally used for lightweight app sessions. Reinstalling the app or clearing app data can create a new Firebase UID. The old leaderboard implementation stored one row at `leaderboard/{uid}`, so the same physical user could appear several times after reconnecting the same Solana wallet under multiple anonymous UIDs.

## Fix

- Firebase UID remains the owner/storage key. Existing Firestore security assumptions are not weakened.
- Every new leaderboard write now also stores the full public `walletAddress` alongside `walletAddressShort`.
- Public leaderboard reads fetch up to 100 XP-sorted rows, collapse duplicates by wallet identity, and then show the top 20 unique wallets.
- Legacy rows that only have `walletAddressShort` can still collapse into a current exact wallet row when the short form maps unambiguously.
- Rows with no connected wallet are excluded from public ranks.
- Duplicate selection keeps the highest XP row; ties prefer the most recently updated row.

## Why not change the Firestore document id to the wallet address now?

Current Firebase ownership is based on Anonymous Auth UID. Moving client-writable documents directly to `leaderboard/{walletAddress}` would require a new authorization model that proves the signed-in Firebase user owns that wallet. This patch fixes ranking correctness without weakening Firestore ownership rules or touching Mobile Wallet Adapter proof code.

## Existing duplicate documents

This patch hides/collapses existing duplicates in the app. It does not delete historical documents owned by older anonymous UIDs. Those can be cleaned manually in Firebase Console later if desired, but cleanup is not required for correct ranking display.

## QA

1. Run `./gradlew :app:testDebugUnitTest`.
2. Run `./gradlew :app:assembleDebug`.
3. Run `./gradlew :app:connectedDebugAndroidTest` on a connected Android device.
4. Open Ranks and confirm the same wallet appears only once.
5. Reconnect the wallet, earn XP, refresh, and confirm the same single row updates/reranks.
6. Confirm an app session with no wallet is not presented as a public rank.

## Scope

No Solana transaction, MWA authorization, SKR balance, chest reward, or private-key handling behavior changed.
