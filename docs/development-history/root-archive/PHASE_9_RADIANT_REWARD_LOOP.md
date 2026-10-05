# Phase 9 — Radiant Reward Loop

## Goal

Phase 9 makes Radiant Rush feel less like a technical demo and more like a daily Solana Mobile quest game.

The core loop is now:

```text
Open app
Complete daily proofs
Scan SKR Passport
Open Daily Radiant Chest
Gain bonus XP
Come back tomorrow
```

## Why no XP betting

A gacha-style reward reveal can be fun, but letting users bet or lose earned XP is not the right fit for this app.

Phase 9 keeps the excitement of a reveal while avoiding the negative parts:

```text
No XP stake
No XP loss
No SOL transfer
No SKR transfer
No wallet approval
No paid randomness
```

## Daily Radiant Chest

The chest unlocks only when all daily proof tasks are complete:

```text
Daily Check-In
Wallet Ready
Sign Daily Proof
On-Chain Memo Proof
Scan SKR Passport
```

When claimed, Firebase writes a `completedQuests/daily-radiant-chest_<date>` document and adds bonus XP to the user profile and leaderboard.

## Reward rule

`RewardLoopRules` selects a deterministic once-per-day reward from a stable seed:

```text
todayKey + Firebase uid + current streak + hasSkr
```

This prevents re-rolling by retrying a failed write while still feeling like a daily reveal.

Reward tiers:

```text
Spark
Pulse
Flare
Aurora
Legendary
```

The total bonus is:

```text
base reward XP + streak bonus XP + optional SKR holder bonus XP
```

## Firebase fields

User profile additions:

```text
lastChestClaimDate
lastChestRewardTitle
lastChestRewardRarity
lastChestRewardXp
totalChestXp
```

Chest proof document fields:

```text
questId = daily-radiant-chest
proofType = daily_reward_loop_no_stake_no_loss
rewardRarity
rewardTitle
baseXp
streakBonusXp
skrBonusXp
xpEarned
noStake = true
noLoss = true
```

## Safety boundary

Daily Radiant Chest is an in-app Firebase reward loop. It is not a blockchain transaction and does not move tokens.

Wallet behavior remains unchanged:

```text
Connect Wallet -> opens wallet
Sign Daily Proof -> opens wallet
Send Memo Proof -> opens wallet
Scan SKR Passport -> no wallet popup
Daily Radiant Chest -> no wallet popup
```
