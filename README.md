# Radiant Rush — Phase 5 SKR Balance Tier

Radiant Rush is a native Android / Jetpack Compose app for Solana Mobile daily quests.

Current working milestone:

- Firebase anonymous profile and quest progress
- Mobile Wallet Adapter wallet connect
- Daily proof message signing
- Devnet Memo transaction proof
- Read-only mainnet SKR Passport scan
- SKR tier and XP multiplier display


## Phase 5 UX tap reliability fix

This patch clarifies expected behavior and makes quest buttons safer on phones:

- Sign Proof and Send Memo open Phantom / MWA.
- Scan SKR Passport is read-only and should finish inside Radiant Rush without Phantom.
- Buttons lock immediately after one accepted tap.
- Active quests show Syncing with clearer labels such as `Waiting…`, `Opening…`, or `Scanning…`.
- Refresh and disconnect actions are disabled where they could interrupt an in-flight wallet or Firebase save.

## Important network split

Phase 4 proof quests use Solana **Devnet** because they submit test memo transactions.

Phase 5 SKR scanning uses **mainnet-beta** because the official SKR token is a live mainnet SPL token. Devnet does not need an SKR test token for this phase.

Official SKR mint used by the app:

```text
SKRbvo6Gf7GondiT3BbTfuRDPqLWei4j2Qy2NPGZhW3
```

## Phase 5 behavior

`Check SKR Balance` reads the connected wallet public address and calls Solana JSON-RPC `getTokenAccountsByOwner` with the official SKR mint and `jsonParsed` encoding.

The app stores only public snapshot data:

- wallet public address
- SKR mint
- network
- balance
- token account count
- tier
- XP multiplier label
- RPC slot
- client check timestamp

The app does **not** request seed phrases, private keys, token transfers, mint authority, or reward authority.

## Build

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

1. Keep Phantom on Devnet for Sign Proof and Send Memo.
2. Connect wallet in Radiant Rush.
3. Complete Sign Proof.
4. Complete Send Memo.
5. Tap Check SKR Balance.
6. If the wallet has no mainnet SKR, the app should save Explorer tier with `0 SKR`.
7. If the wallet holds SKR, the app should show a boosted SKR tier.
8. Check Firestore under `users/{uid}` and `users/{uid}/completedQuests/skr-holder_<today>`.

## Do not commit secrets

Do not commit `app/google-services.json` to a public repository. Use `app/google-services.json.example` for documentation only.
