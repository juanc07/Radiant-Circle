# Radiant Rush — Phase 7 Final QA + Automated Tests

Radiant Rush is a native Android / Jetpack Compose app for Solana Mobile daily quests.

Phase 7 keeps the working Phase 4/5/6 proof stack and adds final release QA support:

- Firebase anonymous profile, XP, streaks, quests, and leaderboard
- Mobile Wallet Adapter wallet connect
- Daily proof message signing
- Devnet Memo transaction proof
- Read-only mainnet SKR Passport scan
- SKR tier and XP multiplier display
- Demo tab with a 3-minute hackathon walkthrough
- New JVM unit tests for SKR tier rules and quest interaction rules
- New Compose instrumented smoke tests for Welcome → shell → Demo navigation
- Local test scripts and GitHub Actions CI for repeatable checks

## Important test boundary

Most app logic can now be tested automatically, but real wallet approval still needs a human because Phantom/MWA is an external wallet authorization flow.

Automated without phone/wallet:

```text
./gradlew :app:testDebugUnitTest
./gradlew :app:assembleDebug
```

Needs an emulator or Android phone:

```text
./gradlew :app:connectedDebugAndroidTest
```

Still manual for final demo QA:

```text
Connect Wallet
Sign Daily Proof
Send Memo Proof
Scan SKR Passport on a real wallet address
```

## Important network split

- Sign Proof and Send Memo use Phantom / MWA on Solana Devnet.
- SKR Passport is a read-only Solana mainnet-beta balance scan by public wallet address.
- SKR Passport should not open Phantom and should still honestly complete as `Explorer` with `0 SKR`.

Official SKR mint used by the app:

```text
SKRbvo6Gf7GondiT3BbTfuRDPqLWei4j2Qy2NPGZhW3
```

## Run Phase 7 tests

Git Bash / macOS / Linux:

```bash
./scripts/run_phase7_tests.sh
```

PowerShell:

```powershell
.\scripts\run_phase7_tests.ps1
```

Manual emulator/phone smoke test:

```bash
./gradlew :app:connectedDebugAndroidTest
```

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

## Final QA checklist

1. Fresh install app.
2. Open Radiant Rush.
3. Complete Firebase check-in.
4. Connect wallet with Phantom/MWA.
5. Sign daily proof.
6. Send devnet memo proof. A second MWA attempt is acceptable if the first authorization handoff expires.
7. Scan SKR Passport. It should not open Phantom.
8. Open Profile and confirm wallet, signature, memo tx, explorer link, SKR tier, XP, and streak.
9. Open Demo tab and use it for the 3-minute video walkthrough.

## Do not commit secrets

Do not commit `app/google-services.json` to a public repository. Use `app/google-services.json.example` for documentation only.
