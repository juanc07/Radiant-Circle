# Phase 4 Memo Auth Auto-Retry Fix

## Problem

After the memo preflight/min-context-slot fix, the memo transaction can succeed, but Phantom/MWA may reject the first memo request with an authorization failure before the user sees a transaction approval prompt.

Observed Logcat pattern:

```text
Memo transaction failed on solana:devnet
JsonRpc20RemoteException: -1/authorization request failed
LocalAdapterOperations.reauthorize
```

A later manual tap can succeed with:

```text
Memo proof MWA success. signatureReturned=true
```

## Fix

`MobileWalletRepository` now retries the memo proof once when the first attempt fails with a wallet authorization/reauthorization error.

The retry flow:

1. Runs the normal devnet memo attempt.
2. If Phantom rejects stale authorization, clears the cached MWA auth token.
3. Waits briefly so the wallet/app handoff can settle.
4. Rebuilds the memo transaction with a fresh devnet blockhash/context slot.
5. Reopens Phantom one more time for the transaction approval.
6. Marks the quest complete only if a real transaction signature is returned.

## Non-goals

This does not fake quest completion. If Phantom still rejects authorization after the retry, Radiant Rush shows a user-facing fix message asking the user to force close Phantom, reopen Devnet, connect wallet, and tap Send Memo again.

## Test checklist

1. Open Phantom and select Devnet.
2. Open Radiant Rush.
3. Connect wallet.
4. Tap Send Memo once.
5. If the first MWA authorization fails, keep the app open and allow the automatic retry.
6. Approve the Phantom transaction prompt.
7. Confirm `Memo proof MWA success. signatureReturned=true` in Logcat.
8. Confirm the memo quest becomes Done and the transaction signature appears in Profile.
