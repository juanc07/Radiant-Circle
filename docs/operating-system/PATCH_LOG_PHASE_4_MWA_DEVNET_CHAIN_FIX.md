# Patch Log — Phase 4 MWA Devnet Chain Fix

Date: 2026-09-09

## Change summary

- Explicitly set Mobile Wallet Adapter to `Solana.Devnet`.
- Kept Phase 4 proof quests devnet-only.
- Added clearer Logcat output through `RadiantRushWallet`.
- Prevented the hidden mainnet/default-chain mismatch from blocking proof completion.

## Architecture impact

The Solana wallet boundary now owns chain selection. App screens should not choose the Solana cluster directly. UI actions call repository methods; repository methods enforce devnet for Phase 4.

## Security/data impact

No private keys or seed phrases are handled by the app. Wallet signing remains user-approved through Mobile Wallet Adapter. Firebase should only save public wallet address, proof message, signature, memo text, transaction signature, explorer URL, and quest completion metadata.

## Release/test impact

Before committing this patch:

```bash
./gradlew :app:assembleDebug
```

Phone test:

1. Phantom installed.
2. Phantom developer/test mode enabled.
3. Phantom wallet set to Devnet.
4. Connect wallet.
5. Sign Daily Proof.
6. Submit Memo Proof.
7. Confirm `RadiantRushWallet` logs show signature returned.
8. Confirm Firestore quest proof documents are created.
