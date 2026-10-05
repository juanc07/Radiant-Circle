# Phase 4 Memo Proof State Fix

## Problem

After Phantom approved and submitted the devnet memo request, the app could still show the memo quest as actionable. This made the user think the memo proof did not finish and encouraged repeated submissions.

The Logcat capture showed the Mobile Wallet Adapter session establishing and closing normally, with no app crash. The repeated `ECONNREFUSED` entries were wallet handoff retries before the session connected; they were not the final failure.

## Fix

- Keep wallet proof actions locked while the wallet/Firebase flow is still saving.
- Mark the active proof quest as `Syncing` immediately after the wallet returns a signed proof or memo transaction signature.
- Do not replace the full UI with preview/default state during the Firebase proof write.
- Stop completed or syncing quests from being submitted again.
- Add defensive profile-level completion fallback using:
  - `lastSignedProofDate`
  - `lastOnChainProofDate`
- Add small `RadiantRushWallet` Logcat markers when MWA returns success and whether a signature was returned.

## What this does not fake

The app still only marks the on-chain memo proof completed after Mobile Wallet Adapter returns a transaction signature and Firebase stores that proof. It does not treat a wallet app switch or a closed session as success by itself.

## Test checklist

1. Open app on a real Android phone.
2. Connect Phantom in devnet/test mode.
3. Tap `Submit Memo Proof` once.
4. Approve the transaction in Phantom.
5. Return to Radiant Rush.
6. Confirm the memo quest shows `Done` / `Memo Submitted`.
7. Confirm tapping the same quest again does not open Phantom again.
8. Confirm Firestore has:
   - `users/{uid}.lastOnChainProofDate`
   - `users/{uid}.lastOnChainTxSignature`
   - `users/{uid}/completedQuests/on-chain-proof_<today>`

## Useful Logcat filters

```text
RadiantRushWallet
AndroidRuntime
FirebaseFirestore
MobileWallet
```
