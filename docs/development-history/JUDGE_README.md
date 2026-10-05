# Radiant Rush — Judge README

## Project

Radiant Rush is a native Android app for Solana Mobile users. It turns daily wallet activity into a lightweight quest, streak, proof, and SKR passport experience.

The goal is to make on-chain participation feel simple, repeatable, and mobile-first.

## What the app demonstrates

Radiant Rush demonstrates:

- Native Android / Kotlin / Jetpack Compose app experience.
- Solana Mobile Wallet Adapter wallet connection.
- Daily message signing as lightweight proof of participation.
- Devnet memo transaction as visible on-chain proof.
- Mainnet SKR read-only balance scan.
- Firebase-backed user profile, quest state, streak, XP, badges, and leaderboard data.
- Mobile-first proof dashboard and demo walkthrough.

## Core user flow

1. User opens Radiant Rush.
2. App creates/signs in a Firebase anonymous profile.
3. User connects a Solana wallet through Mobile Wallet Adapter.
4. User completes a daily signed proof quest.
5. User sends a devnet memo proof transaction.
6. User scans their wallet for SKR holdings on mainnet.
7. App updates quest state, profile proof data, streak, XP, tier, and leaderboard fields.
8. User can open the Demo tab to see a judge-friendly walkthrough.

## Why Solana Mobile

Radiant Rush is designed around a mobile-first Solana habit loop. The user should not need a desktop browser, private-key import, or manual copy/paste flow. Wallet approval happens through the phone wallet using Mobile Wallet Adapter.

## Why SKR matters in the app

SKR is treated as a read-only passport signal. Radiant Rush does not transfer, spend, lock, or stake SKR. The app checks the connected wallet's SKR balance and maps it to a visible holder tier and XP multiplier.

This makes SKR useful as:

- Identity/status signal.
- Progress multiplier.
- Community passport.
- Future reward targeting signal.

## Safety and data rules

Radiant Rush stores safe app data only:

- Firebase anonymous user id.
- Public wallet address.
- Quest completion state.
- Message signature string.
- Devnet memo transaction signature / explorer URL.
- SKR balance snapshot and tier.
- XP, streak, badges, leaderboard fields.

Radiant Rush does not store:

- Private keys.
- Seed phrases.
- Wallet auth secrets for long-term use.
- SKR transfer instructions.
- Custodial balances.

## Networks used

- Devnet is used for memo proof transactions.
- Mainnet-beta is used for read-only SKR balance checks.

## Known demo note

The memo proof may sometimes require a retry because Phantom/MWA authorization handoff can be timing-sensitive on a fresh install or fresh wallet session. The app includes a retry path and clearer status states. For demo recording, connect wallet first, then sign proof, then send memo proof.

## Build commands

```bash
./gradlew --stop
./gradlew :app:testDebugUnitTest
./gradlew :app:assembleDebug
```

Debug APK output:

```text
app/build/outputs/apk/debug/app-debug.apk
```

Optional copy command:

```bash
bash scripts/copy_debug_apk_to_submission.sh
```

Copied APK location:

```text
release/submission/RadiantRush-debug.apk
```

## Manual test path

1. Install APK on Android phone.
2. Install/configure Phantom or supported Solana wallet.
3. Open Radiant Rush.
4. Connect wallet.
5. Sign Daily Proof.
6. Send Memo Proof.
7. Scan SKR Passport.
8. Open Profile and verify proof information.
9. Open Demo tab and follow the walkthrough.

## Judge demo checklist

A good demo should show:

- App starts cleanly.
- Wallet connects through MWA.
- Signature proof completes.
- Memo proof opens wallet and returns a transaction signature.
- SKR scan completes without wallet popup because it is read-only RPC.
- Profile displays public proof data.
- Demo tab explains the product clearly.
