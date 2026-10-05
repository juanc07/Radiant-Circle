# Radiant Rush — Submission Form Copy

Use or adapt this text for the hackathon submission form.

## Project name

Radiant Rush

## Tagline

Daily SKR quests and proof streaks for Solana Mobile.

## Short description

Radiant Rush is a native Android app that turns Solana Mobile wallet activity into a daily quest loop. Users connect a wallet through Mobile Wallet Adapter, complete signed and on-chain proof quests, scan their SKR passport status, and build XP, streaks, badges, and leaderboard progress.

## Long description

Radiant Rush is built for Solana Mobile users who need a simple reason to return every day. The app creates a mobile-first participation loop: connect a wallet, sign a daily proof, submit a devnet memo proof, scan SKR balance as a read-only passport signal, and build streak/XP progress.

The app uses Firebase Auth and Firestore for profile, quest, streak, badge, and leaderboard persistence. It uses Solana Mobile Wallet Adapter for wallet approval flows, Solana devnet for demo-safe memo transactions, and mainnet read-only RPC for SKR balance scanning.

Radiant Rush does not store private keys, seed phrases, or custodial token balances. SKR is used as a visible holder tier and multiplier signal only.

## What is working

- Native Android app built with Kotlin and Jetpack Compose.
- Firebase anonymous user profile.
- Mobile Wallet Adapter wallet connection.
- Daily message signature proof.
- Devnet memo transaction proof.
- Mainnet read-only SKR balance scan.
- XP, streak, badges, profile, and leaderboard fields.
- Demo tab for judge walkthrough.
- Automated unit/build tests.
- Real phone QA completed.

## SKR integration summary

Radiant Rush treats SKR as a passport/status signal. The app scans the connected wallet's SKR balance on mainnet using read-only RPC, maps the balance to a holder tier, and reflects that tier in the user's profile and quest progress. The app does not move or spend SKR.

## Technical stack

- Android native.
- Kotlin.
- Jetpack Compose.
- Solana Mobile Wallet Adapter.
- Solana RPC.
- Firebase Auth.
- Firebase Firestore.
- Gradle automated tests.

## Demo instructions

1. Install the APK on Android.
2. Open Radiant Rush.
3. Connect a supported Solana wallet.
4. Complete Sign Daily Proof.
5. Complete Send Memo Proof.
6. Tap Scan SKR Passport.
7. Open Profile to see proof data.
8. Open Demo tab for walkthrough.

## Known limitation

Wallet consent is external to the app. On some fresh Phantom/MWA sessions, the devnet memo proof may require one retry. The app handles this with clearer status/retry behavior. The SKR passport scan is read-only and should not open the wallet.
