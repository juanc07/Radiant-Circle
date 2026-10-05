# Phase 4 — Solana Proof Quests

## Goal

Phase 4 makes Radiant Rush meaningfully interact with Solana through Mobile Wallet Adapter.

The app now supports two wallet-backed proof quests:

1. **Sign Daily Proof** — asks the connected wallet to sign a daily proof message.
2. **On-Chain Memo Proof** — builds and submits a lightweight Solana devnet Memo transaction through the wallet.

This is the first milestone where the app goes beyond Firebase-only progress and wallet authorization.

## What changed

### Mobile Wallet Adapter

`MobileWalletRepository` now owns:

- Wallet connect/disconnect.
- Daily message signing through `signMessagesDetached`.
- Devnet Memo transaction building.
- `signAndSendTransactions` submission through the wallet.
- Public result parsing: wallet address, message signature, transaction signature, explorer URL.

The repository still never asks for seed phrases, private keys, or reward authority.

### Solana transaction

The memo proof transaction uses:

```text
Network: devnet
RPC: https://api.devnet.solana.com
Program: MemoSq4gqABAXKb96qnH8TysNcWxMyWCqXgDLGmfcHr
Memo shape: radiant-rush:daily-memo-proof:<date>:<wallet>
```

### Firebase persistence

Firestore now stores:

```text
users/{uid}
  lastSignedMessage
  lastSignedMessageSignature
  lastSignedProofDate
  lastOnChainTxSignature
  lastOnChainExplorerUrl
  lastOnChainProofDate
  phase = 4

users/{uid}/completedQuests/{questId_date}
  questId
  questTitle
  date
  proofType
  walletAddress
  walletAddressShort
  xpEarned
  signedMessage / signedMessageSignature
  txSignature / explorerUrl
```

Leaderboard rows continue to update from client-side MVP data. This is acceptable for hackathon demo progress only; it is not trusted reward authority.

## User flow

```text
Open app
Connect wallet
Tap Sign Daily Proof
Approve message signature in wallet
Return to app
Firebase saves signed proof
Tap On-Chain Memo Proof
Approve devnet memo transaction in wallet
Return to app
Firebase saves transaction signature
Profile shows latest proof signatures
```

## Testing notes

Use Phantom test/devnet mode first. Mainnet/live mode is not the Phase 4 target.

The memo transaction may fail if:

- Phantom is not in test/devnet mode.
- The test wallet has no devnet SOL for fees.
- RPC is down or blocked.
- The user rejects the wallet request.
- The wallet does not support the requested MWA method.

Failures must show a recoverable message and must not grant quest completion.

## Acceptance checklist

- App builds.
- App opens without crash.
- Firebase profile still loads.
- Wallet connect still works.
- Sign Daily Proof opens the wallet and returns a signature.
- Signed proof is saved to Firestore.
- On-Chain Memo Proof opens the wallet and returns a devnet transaction signature.
- Memo proof is saved to Firestore.
- Duplicate same-day proof quests do not award XP twice.
- No secrets are stored or logged.

## Known limits

- Transaction confirmation polling is not implemented yet.
- Phase 4 stores submitted transaction signatures, but does not independently verify finalization.
- SKR token balance and XP multipliers are still Phase 5.
- MWA auth token persistence is still intentionally deferred.
