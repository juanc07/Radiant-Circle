# Radiant Rush Phase 4 MWA Devnet Chain Fix

Changed-files-only patch.

## Fix

Phase 4 uses Solana devnet for its proof quests. This patch explicitly tells Mobile Wallet Adapter to use `Solana.Devnet` so Phantom receives the same chain context as the app's devnet RPC and devnet explorer URL.

Without this, MWA can default to mainnet while the app builds a devnet memo transaction. That can look like Phantom approves something, but the app never receives a usable signature/proof result.

## Files

- `app/src/main/java/com/thinkblox/radiantrush/solana/MobileWalletRepository.kt`
- `docs/PHASE_4_MWA_DEVNET_CHAIN_FIX.md`
- `docs/operating-system/PATCH_LOG_PHASE_4_MWA_DEVNET_CHAIN_FIX.md`

## Apply

Copy the contents of this folder into your project root and overwrite existing files.

Then build:

```bash
./gradlew --stop
./gradlew :app:assembleDebug
```

PowerShell:

```powershell
.\gradlew.bat --stop
.\gradlew.bat :app:assembleDebug
```

## Test

Use Phantom mobile in developer/test mode with the wallet set to **Devnet**.

Do not use Solana testnet unless the app RPC and MWA chain are also changed to testnet.

## Packaging check

- No `.git` metadata
- No `google-services.json`
- Changed/new files only
