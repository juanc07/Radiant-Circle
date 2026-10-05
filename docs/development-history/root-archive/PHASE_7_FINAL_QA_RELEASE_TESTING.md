# Phase 7 — Final QA + Automated Testing

Date: 2026-09-09

## Goal

Phase 7 moves Radiant Rush from feature-building into final hackathon readiness. It keeps the working wallet/proof/SKR behavior and adds repeatable test coverage so basic regressions can be caught without repeatedly testing everything by hand.

## What changed

- Bumped app version to `0.7.0-phase7`.
- Added JVM unit tests for pure SKR tiering rules.
- Added JVM unit tests that lock the expected quest interaction boundary:
  - Wallet Ready opens an external MWA wallet.
  - Sign Daily Proof opens an external MWA wallet.
  - On-Chain Memo Proof opens an external MWA wallet.
  - SKR Passport is read-only RPC and must not open Phantom.
- Added stable Compose test tags for the welcome button, bottom nav, and Demo screen.
- Added instrumented Compose smoke tests for opening the shell and reaching the Demo tab.
- Added local test scripts for Git Bash/macOS/Linux and PowerShell.
- Added GitHub Actions CI that runs unit tests and builds the debug APK.

## Automated test commands

Fast local / CI checks:

```bash
./gradlew :app:testDebugUnitTest
./gradlew :app:assembleDebug
```

Convenience script:

```bash
./scripts/run_phase7_tests.sh
```

PowerShell:

```powershell
.\scripts\run_phase7_tests.ps1
```

Emulator or physical Android smoke test:

```bash
./gradlew :app:connectedDebugAndroidTest
```

## What cannot be fully automated yet

The following flows still require a real wallet app and user approval:

- Connect Wallet through MWA.
- Sign Daily Proof in Phantom or another MWA wallet.
- Approve the devnet Memo transaction.

This is expected because wallet apps should not allow the Radiant Rush app to bypass user consent.

## Manual final pass

Before hackathon submission:

1. Fresh install the app.
2. Confirm Welcome and Today tabs are readable on the target phone.
3. Complete Firebase check-in.
4. Connect wallet.
5. Sign daily proof.
6. Send memo proof. If the first MWA authorization attempt expires, a second attempt is acceptable, but the UI must stay clear.
7. Scan SKR Passport and confirm Phantom does not open.
8. Open Profile and confirm proof fields are present.
9. Open Demo and record the 3-minute walkthrough.

## Safety boundary

Tests and CI must not include `app/google-services.json`, wallet private keys, seed phrases, reward authority keys, or fake SKR balances.
