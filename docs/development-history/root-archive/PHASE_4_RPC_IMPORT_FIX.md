# Phase 4 RPC Import Fix

## Problem

`MobileWalletRepository.kt` imported `com.solana.rpc.SolanaRpcClient`, but the resolved Phase 4 dependency set did not expose that package to the Kotlin compiler in the current AGP 9 build.

Build symptom:

```text
Unresolved reference 'rpc'
Location: app/src/main/java/com/thinkblox/radiantrush/solana/MobileWalletRepository.kt
```

## Fix

The devnet blockhash fetch no longer depends on `SolanaRpcClient`.

`MobileWalletRepository` now fetches `getLatestBlockhash` with a small JSON-RPC POST using Android/Java standard networking:

```text
POST https://api.devnet.solana.com
method: getLatestBlockhash
commitment: confirmed
```

The transaction is still built with Solana Mobile `web3-solana` primitives and still sent through Mobile Wallet Adapter.

## Files changed

```text
app/src/main/java/com/thinkblox/radiantrush/solana/MobileWalletRepository.kt
app/build.gradle.kts
gradle/libs.versions.toml
docs/PHASE_4_RPC_IMPORT_FIX.md
docs/operating-system/CHANGELOG.md
```

## Validation required

Run:

```bash
./gradlew :app:assembleDebug
```

Then test on a phone with Phantom test/devnet mode:

1. Connect wallet.
2. Sign Daily Proof.
3. Run On-Chain Memo Proof.
4. Confirm Firebase stores the signature/proof result.
