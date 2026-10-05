# Phase 4 — MWA Reauthorization and Signature Action Fix

## Problem

Physical-device testing showed that `Sign Daily Proof` could succeed, but `On-Chain Memo Proof` failed after wallet handoff with:

```text
Memo transaction failed on solana:devnet
JsonRpc20RemoteException: -1/authorization request failed
LocalAdapterOperations.reauthorize
```

This means the app reached the wallet association flow, but the failure happened in Mobile Wallet Adapter authorization/reauthorization before a usable transaction signature was returned.

The UI also had proof/status chips that looked clickable because they were implemented as `AssistChip(onClick = {})`, causing labels like `Signature` to feel like dead buttons.

## Fix

- `MobileWalletRepository` now keeps connect/disconnect separate from proof actions.
- `Sign Daily Proof` uses a fresh devnet `MobileWalletAdapter` session for the message-signing request.
- `On-Chain Memo Proof` uses a fresh devnet `MobileWalletAdapter` session for the memo transaction request.
- MWA remains explicitly set to `Solana.Devnet`.
- The app still requires a real signature/transaction signature before marking a quest complete.
- Proof/status chips are passive UI pills, not fake buttons.
- Profile rows can copy wallet address, message signature, and memo explorer/transaction values.

## Test

```bash
./gradlew --stop
./gradlew :app:assembleDebug
```

Then test on a physical Android device with Phantom Devnet:

1. Connect wallet.
2. Tap `Sign Daily Proof`.
3. Approve and confirm the quest completes.
4. Go to Profile and tap `Copy Signature`.
5. Tap `Submit Memo Proof`.
6. Approve the wallet prompt.
7. Confirm Firestore gets `lastOnChainTxSignature` and `completedQuests/on-chain-proof_<date>`.

## Known risk

This patch avoids stale MWA reauthorization failures, but Phantom can still reject the memo if the wallet is not on Devnet, has no devnet SOL, or rejects the sign-and-send request. Those states must remain recoverable failures, not fake success.
