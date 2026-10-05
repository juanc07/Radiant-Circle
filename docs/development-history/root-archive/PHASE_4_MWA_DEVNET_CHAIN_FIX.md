# Phase 4 MWA Devnet Chain Fix

## Problem

The app uses Solana devnet for Phase 4 proof quests:

- devnet blockhash RPC
- devnet memo transaction
- devnet explorer URL
- Phantom developer/test wallet flow

However, the Mobile Wallet Adapter instance did not explicitly declare the devnet chain. Some wallets can treat an unset chain as mainnet, which creates a mismatch between wallet context and the devnet transaction built by the app.

## Fix

`MobileWalletRepository` now sets:

```kotlin
blockchain = Solana.Devnet
```

This aligns:

```text
MWA chain: solana:devnet
RPC:       https://api.devnet.solana.com
Explorer:  https://explorer.solana.com/tx/<signature>?cluster=devnet
Wallet:    Phantom developer/test mode Devnet
```

## Added logging

The wallet repository now logs app-level wallet outcomes under:

```text
RadiantRushWallet
```

Useful filters:

```text
RadiantRushWallet
Daily proof
Memo proof
MWA success
MWA failure
signatureReturned
```

## Testing rule

For Phase 4, use Phantom on **Devnet**. Do not use mainnet or Solana testnet for this build.

Devnet and testnet are different Solana clusters. A devnet blockhash/transaction should be signed and submitted against devnet.
