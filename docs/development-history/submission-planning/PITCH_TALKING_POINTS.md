# Radiant Rush — Pitch Talking Points

## One-liner

Radiant Rush is a native Android daily quest app that turns Solana Mobile wallet activity, on-chain proof, and SKR holder status into a repeatable streak and passport experience.

## Problem

Many crypto apps are transactional and forgettable. Users connect a wallet once, perform a task, then leave. There is often no lightweight daily habit, no simple proof history, and no clear reason to return.

## Solution

Radiant Rush creates a daily loop:

1. Connect wallet.
2. Complete proof quests.
3. Submit on-chain memo proof.
4. Scan SKR Passport.
5. Earn XP and streak progress.
6. Return tomorrow.

## Why users come back

- Daily quest reset.
- Streak progress.
- XP and badge progression.
- Proof history.
- SKR tier/multiplier.
- Leaderboard status.

## Why this fits Solana Mobile

- Built as a native Android app.
- Wallet connection through Mobile Wallet Adapter.
- Mobile-first UI and proof flow.
- Uses Solana devnet for demo-safe transactions.
- Uses mainnet read-only SKR scan for real passport/status signal.

## SKR integration

SKR is used as a read-only community/passport signal:

- App scans connected wallet for SKR balance.
- App maps balance to holder tier.
- Tier becomes visible status.
- Tier can drive XP multiplier and future reward logic.
- No SKR movement, custody, transfer, or spending.

## Technical stack

- Android native app.
- Kotlin.
- Jetpack Compose.
- Solana Mobile Wallet Adapter.
- Solana RPC.
- Firebase Auth.
- Firebase Firestore.
- Gradle automated unit/build tests.

## Demo proof points

- Wallet connect works.
- Daily message signing works.
- Devnet memo transaction proof works.
- Mainnet SKR read-only scan works.
- Firebase profile/streak/quest persistence works.
- Automated tests pass.
- Physical phone QA passed.

## Future roadmap

Near-term:

- Better confirmation polling for memo transactions.
- Richer SKR tier visuals.
- Daily reset countdown.
- Friend/referral proof quests.
- Push reminders for streak continuation.

Longer-term:

- Seasonal campaigns.
- Partner quests.
- More Solana Mobile-native actions.
- SKR holder campaigns.
- Seeker-focused achievements.

## What makes it different

Radiant Rush is not just a wallet checker. It packages wallet actions into a repeatable, gamified, mobile-first habit loop with visible proof and status.
