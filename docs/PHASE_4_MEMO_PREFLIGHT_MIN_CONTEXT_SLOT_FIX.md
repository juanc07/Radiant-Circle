# Phase 4 Memo Preflight + Min Context Slot Fix

## Problem

`Sign Daily Proof` works because it only asks Phantom to sign a message. `Send Memo` still opens Phantom but does not reach a real transaction approval / done state.

Observed Logcat patterns:

- MWA session eventually connects after normal local WebSocket retries.
- The app reaches `Starting devnet memo proof on solana:devnet`.
- The app returns with `Memo proof crashed before MWA returned a result` or an authorization failure before a transaction signature is returned.
- Firestore/network logs also show DNS/name resolution warnings while the wallet flow is active.

## Fix

`MobileWalletRepository.kt` now:

1. Fetches the devnet blockhash before opening Phantom.
   - If devnet RPC/DNS fails, the app fails in Radiant Rush first instead of bouncing the player into Phantom with no transaction prompt.
2. Parses the `context.slot` from the same `getLatestBlockhash` response.
3. Passes that slot into MWA `signAndSendTransactions` as `TransactionParams.minContextSlot`.
   - This follows the known Phantom workaround where `signAndSendTransactions` can fail or show no approval UI if `minContextSlot` is omitted.
4. Reuses the active connected wallet adapter for memo submission.
   - Memo should go to transaction approval instead of asking for another fresh connection every tap.
5. Clears the stored auth token if Phantom returns an authorization/auth-token failure.
   - The user can reconnect once, then submit again.
6. Adds clearer user-facing failure text for devnet RPC DNS and timeout failures.

## Expected Logcat

Good path:

```text
RadiantRushWallet: Starting devnet memo proof on solana:devnet
RadiantRushWallet: Devnet chain context ready. minContextSlot=...
RadiantRushWallet: Memo proof using active MWA session on solana:devnet
RadiantRushWallet: Memo proof MWA success. signatureReturned=true
```

RPC/DNS failure path:

```text
RadiantRushWallet: Memo proof crashed before MWA returned a result
```

The app should show a message similar to:

```text
Radiant Rush could not reach devnet RPC. Check phone internet/DNS, then try Send Memo again.
```

## Test Steps

1. Open Phantom.
2. Confirm Developer Mode / Devnet is selected.
3. Confirm the Devnet wallet has SOL for gas.
4. Open Radiant Rush.
5. Tap Connect Wallet once.
6. Tap Send Memo once.
7. Approve transaction in Phantom.
8. Return to Radiant Rush.
9. Confirm the quest becomes Done.
10. Confirm Profile shows Last Memo Transaction.
11. Confirm Firestore contains `lastOnChainTxSignature` and the completed quest document.

If the app says wallet authorization expired, tap Connect Wallet once again and submit the memo once.
