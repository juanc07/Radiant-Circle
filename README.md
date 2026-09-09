# Radiant Rush — Phase 6 Demo Polish + Retention

Radiant Rush is a native Android / Jetpack Compose app for Solana Mobile daily quests.

Phase 6 keeps the working Phase 4/5 proof stack and adds a judge-friendly demo layer:

- Firebase anonymous profile, XP, streaks, quests, and leaderboard
- Mobile Wallet Adapter wallet connect
- Daily proof message signing
- Devnet Memo transaction proof
- Read-only mainnet SKR Passport scan
- SKR tier and XP multiplier display
- New `Demo` tab with a 3-minute hackathon walkthrough

## Important network split

- Sign Proof and Send Memo use Phantom / MWA on Solana Devnet.
- SKR Passport is a read-only Solana mainnet-beta balance scan by public wallet address.
- SKR Passport should not open Phantom and should still honestly complete as `Explorer` with `0 SKR`.

Official SKR mint used by the app:

```text
SKRbvo6Gf7GondiT3BbTfuRDPqLWei4j2Qy2NPGZhW3
```

## Phase 6 behavior

The new Demo tab is for your recording and judge walkthrough. It summarizes:

1. Firebase sync
2. Wallet connect
3. Message signature proof
4. Devnet memo proof
5. Mainnet SKR Passport scan
6. Profile proof summary
7. Security boundaries

The patch does not add fake balances, fake transactions, token transfers, or backend reward authority.

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

1. Open the app on Android.
2. Confirm the bottom navigation has `Demo`.
3. Complete or review Today quests.
4. Open the Demo tab and confirm the 3-minute flow is readable on phone.
5. Confirm Connect Wallet / Sign Proof / Send Memo still behave like Phase 5.
6. Confirm SKR Passport still scans inside the app with no Phantom popup.
7. Open Profile and confirm proof/status fields are still present.

## Do not commit secrets

Do not commit `app/google-services.json` to a public repository. Use `app/google-services.json.example` for documentation only.
