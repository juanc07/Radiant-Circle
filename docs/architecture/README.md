# Radiant Circle — Product & Technical Architecture

This document explains how Radiant Circle is structured, how its major systems interact, and why the application was designed this way.

> **Most crypto apps begin with finance. Radiant Circle begins with a person.**

Radiant Circle combines daily engagement, social discovery, AI-assisted conversation, native gameplay, competition, and optional Solana participation.

---

## 1. High-Level Architecture

```text
Android Application
        |
        +-- Daily Radiance
        +-- Shake-to-Discover
        +-- Shared Sparks
        +-- Circle / Private Chat
        +-- AI Spark Starter
        +-- Radiant Rush
        +-- Radiant Vault
        +-- SKR Passport / Weekly Cup
        +-- ORE Mainnet Portfolio
                |
                v
        Firebase + Solana Services
             /             \
        Firebase          Solana
          |                 |
          + Auth            + Mobile Wallet Adapter
          + Firestore       + Mainnet RPC
          + Functions       + ORE
          + Secret Manager  + wallet-owned signing
```

The Android client owns presentation and user interaction.

Firebase provides social identity, persistence, authorization rules, real-time data, and trusted server-side AI functionality.

Solana provides wallet ownership, signing, verifiable Mainnet state, and token-related participation.

---

## 2. Social Identity vs Wallet Identity

Radiant Circle intentionally keeps social identity separate from wallet identity.

```text
Firebase UID
    |
    v
Radiant Circle social identity

Solana wallet
    |
    v
Optional ownership / signing / reward identity
```

A user can use the social product without first connecting a crypto wallet.

The wallet becomes relevant only when ownership, competition eligibility, signing, or onchain participation adds value.

---

## 3. Android Application Structure

Primary source:

```text
app/src/main/java/com/thinkblox/radiantrush/
```

Major areas:

```text
ui/        Jetpack Compose screens and UI components
data/      application models
logic/     deterministic product/game rules
firebase/  Firebase repositories and callable clients
solana/    wallet, SKR, and ORE integrations
audio/     game and interaction audio
```

Jetpack Compose is used for the native Android UI.

Business rules are separated from UI code where practical so important behavior can be tested and reasoned about independently.

---

## 4. Firebase Architecture

Firebase provides the primary social backend:

- Firebase Authentication
- user profiles
- discovery data
- Circle relationships
- private chat
- presence and typing state
- reports
- competition persistence
- server-side AI
- Firestore authorization rules

Primary source:

```text
app/src/main/java/com/thinkblox/radiantrush/firebase/
firebase/firestore.rules
functions/
```

---

## 5. Circle Relationship Model

The relationship lifecycle is:

```text
Discover
   |
Shared Sparks
   |
Send Spark
   |
Recipient Accepts
   |
Accepted Circle Connection
   |
Private Chat
```

Private chat is available only after mutual acceptance.

Primary source:

```text
app/src/main/java/com/thinkblox/radiantrush/data/CircleModels.kt
app/src/main/java/com/thinkblox/radiantrush/logic/CircleDiscoveryRules.kt
app/src/main/java/com/thinkblox/radiantrush/ui/screens/CircleScreen.kt
firebase/firestore.rules
```

---

## 6. Shared Sparks

Shared Sparks are interests two users have in common.

They support:

1. explaining why another profile may be relevant
2. providing safe contextual input to AI Spark Starter

The AI backend derives Shared Sparks server-side instead of accepting arbitrary prompt context from Android.

---

## 7. AI Spark Starter

Architecture:

```text
Android Circle Chat
        |
        v
peer UID only
        |
        v
Firebase Callable Function
        |
        +-- verify Firebase user
        +-- verify accepted Circle relationship
        +-- load both profiles
        +-- derive Shared Sparks
        +-- apply rate limits / anti-repetition
        |
        v
OpenAI
        |
        v
one short starter
        |
        v
user chooses whether to use/send it
```

Primary source:

```text
app/src/main/java/com/thinkblox/radiantrush/firebase/AiSparkStarterRepository.kt
app/src/main/java/com/thinkblox/radiantrush/ui/screens/CircleScreen.kt
functions/index.js
```

Important design decisions:

- private chat history is not sent to OpenAI
- OpenAI credentials stay server-side
- accepted relationships are verified server-side
- suggestions never auto-send
- users remain in control
- per-user limits reduce cost/abuse
- pair-level sequencing reduces duplicate simultaneous generations

---

## 8. Private Chat

Accepted Circle members can use private 1-to-1 chat.

Current capabilities include:

- real-time messages
- Sent / Read state
- unread state
- typing presence
- profile/avatar presentation
- AI Spark Starter
- Remove / Block / Report

Primary source:

```text
app/src/main/java/com/thinkblox/radiantrush/data/CircleChatModels.kt
app/src/main/java/com/thinkblox/radiantrush/logic/CircleChatRules.kt
app/src/main/java/com/thinkblox/radiantrush/ui/screens/CircleScreen.kt
firebase/firestore.rules
```

Firestore rules are part of the access-control boundary; security does not rely only on hidden UI controls.

---

## 9. Radiant Rush

Radiant Rush is the native game layer inside Radiant Circle.

Primary source:

```text
app/src/main/java/com/thinkblox/radiantrush/ui/screens/RadiantRunScreen.kt
app/src/main/java/com/thinkblox/radiantrush/logic/RadiantGameRules.kt
app/src/main/java/com/thinkblox/radiantrush/logic/RewardLoopRules.kt
```

Its role is retention, progression, collectibles, rewards, and competition.

It gives users another reason to return even when they are not performing a wallet transaction.

---

## 10. SKR Passport

Primary source:

```text
app/src/main/java/com/thinkblox/radiantrush/solana/SkrBalanceRepository.kt
app/src/main/java/com/thinkblox/radiantrush/logic/SkrPassportRules.kt
app/src/main/java/com/thinkblox/radiantrush/logic/SkrStakingRules.kt
app/src/main/java/com/thinkblox/radiantrush/logic/SkrTierRules.kt
```

Passport eligibility is based on:

```text
liquid Mainnet SKR
+
verified active staked SKR
```

Unstaking/cooldown state is treated separately from verified active stake.

SKR perks do not directly increase raw ranked gameplay scores.

---

## 11. Weekly Radiant Cup

Competition architecture:

```text
Android gameplay
        |
        v
competition evidence
        |
        v
trusted verification
        |
        v
wallet lock
        |
        v
funding verification
        |
        v
trusted finalization
        |
        v
payout lifecycle
```

Trusted competition tooling lives under:

```text
scripts/firebase-admin/
```

Android client state is not treated as payout authority.

---

## 12. Competition Wallet Lock

The first ranked entry for a Firebase account/week establishes the competition wallet.

Later ranked entries for the same account/week must use that wallet.

Primary source:

```text
app/src/main/java/com/thinkblox/radiantrush/logic/Phase12CompetitionWalletLockRules.kt
firebase/firestore.rules
scripts/firebase-admin/competition-wallet-lock.mjs
```

---

## 13. Solana Mobile Wallet Adapter

Primary source:

```text
app/src/main/java/com/thinkblox/radiantrush/solana/MobileWalletRepository.kt
```

Flow:

```text
Radiant Circle
    |
    v
Open MWA session
    |
    v
Wallet selects account
    |
    v
Verify expected wallet
    |
    v
Build transaction
    |
    v
User approves in wallet
    |
    v
Wallet signs / submits
    |
    v
Verify result
```

Radiant Circle does not need access to wallet seed phrases or private keys.

---

## 14. ORE Mainnet

Primary source:

```text
app/src/main/java/com/thinkblox/radiantrush/solana/OrePortfolioRepository.kt
app/src/main/java/com/thinkblox/radiantrush/solana/MobileWalletRepository.kt
app/src/main/java/com/thinkblox/radiantrush/ui/screens/OrePortfolioScreen.kt
```

Supported actions include:

- Stake
- Withdraw
- Claim

Flow:

```text
Read Mainnet state
    |
    v
User chooses action
    |
    v
Validate wallet / amount / state
    |
    v
Build transaction
    |
    v
MWA approval
    |
    v
Submit to Solana
    |
    v
Wait for confirmation
    |
    v
Refresh Mainnet state
    |
    v
Show before -> after result
```

The app does not treat opening the wallet UI as proof of success. It refreshes Mainnet state after execution.

---

## 15. Trust Boundaries

### Android client

Responsible for:

- presentation
- interaction
- gameplay
- requesting wallet operations
- requesting backend operations

Not trusted for:

- wallet private keys
- OpenAI credentials
- Firebase Admin credentials
- competition payout authority

### Firebase backend

Responsible for:

- authenticated social state
- authorization
- server-side AI validation
- OpenAI secret handling

### Solana wallet

Responsible for:

- private-key custody
- transaction approval
- transaction signing

### Trusted operator tooling

Responsible for:

- competition verification
- funding verification
- finalization
- payout workflow
- transfer reconciliation

---

## 16. Why It Is Designed This Way

Radiant Circle is intentionally not a crypto dashboard with social features added afterward.

The architecture follows the product thesis:

```text
person first
    |
daily habit
    |
social relationship
    |
AI-assisted conversation
    |
play
    |
wallet / ownership only where useful
```

Firebase is used where a responsive social product benefits from authenticated real-time state.

Solana is used where decentralized ownership, user-controlled signing, and verifiable Mainnet state add meaningful value.

AI is used narrowly for a specific user problem rather than being inserted as a generic chatbot.

---

## 17. Related Documentation

Judge evidence:

- [`../submission/JUDGE_EVIDENCE.md`](../submission/JUDGE_EVIDENCE.md)

AI implementation:

- [`../submission/AI_SPARK_STARTER.md`](../submission/AI_SPARK_STARTER.md)

SKR / Weekly Radiant Cup:

- [`../submission/SKR_EVIDENCE.md`](../submission/SKR_EVIDENCE.md)

Security:

- [`../submission/SECURITY.md`](../submission/SECURITY.md)

Engineering workflow:

- [`../operating-system/`](../operating-system/)

Development history:

- [`../development-history/`](../development-history/)
