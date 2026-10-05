# Radiant Circle

**Connect. Play. Participate.**

Radiant Circle is a social-first Android experience built for Solana Mobile.

Instead of starting with a wallet or trading screen, Radiant Circle starts with people: a daily ritual, social discovery, shared interests, private connections, AI-assisted conversation, a native mobile game, and optional onchain participation through Solana Mobile Wallet Adapter.

> **Radiant Circle** is the product.  
> **Radiant Rush** is the native skill game built inside Radiant Circle.

---

## CLOCK IN Submission — Final Judge Release

For judging and full product testing, use the **official signed Android APK** below.

### 📱 Download the Official Signed APK

**Recommended for judges — one-click APK download:**

👉 **[Download RadiantCircle-v1.2.18-clock-in.apk](https://github.com/juanc07/Radiant-Circle/releases/download/clock-in-submission-v3/RadiantCircle-v1.2.18-clock-in.apk)**

**GitHub Release page:**

👉 [Radiant Circle v1.2.18 — CLOCK IN Submission](https://github.com/juanc07/Radiant-Circle/releases/tag/clock-in-submission-v3)

### Radiant Circle v1.2.18

- **APK:** `RadiantCircle-v1.2.18-clock-in.apk`
- **Version:** `1.2.18-ai1`
- **VersionCode:** `43`
- **Package:** `com.thinkblox.radiantrush`
- **Release tag:** `clock-in-submission-v3`
- **Frozen source commit:** `0a181171194403b8ec95aebfb069c6800f491fbd`
- **SHA-256:** `52ecd83059f78a5d8d25175834ee125ef42deb510f2ec8aefea39f40bba281ee`

> The public repository intentionally excludes `app/google-services.json`. A fresh local build therefore requires the developer's own Firebase Android configuration and matching OAuth credentials for production Google Sign-In. For complete judge testing, use the official signed APK above.

---

## The Idea

Most crypto apps begin with finance.

**Radiant Circle begins with a person.**

The product is built around a repeatable mobile loop:

```text
Open Radiant Circle
        ↓
Daily Radiance
        ↓
Shake to Discover
        ↓
Find Shared Sparks
        ↓
Send a Spark
        ↓
Accept → Build Your Circle
        ↓
AI Spark Starter + Private Chat
        ↓
Play Radiant Rush
        ↓
Progress + Compete
        ↓
Participate on Solana when you choose
```

Solana is the ownership, verification, and value layer underneath the experience — not a barrier users must understand before they can participate.

---

# Product Highlights

## Daily Radiance

Radiant Circle gives users a lightweight reason to return each day.

- Daily Radiance message/reveal
- dedicated Radiance streak
- Daily Plan
- Daily Radiant Chest
- progression and reward feedback

The goal is to establish a daily habit before asking users to think about wallets or blockchain activity.

---

## Shake to Discover

Users can physically shake their Android device to discover another active Radiant Circle member.

Discovery profiles can surface:

- display name
- avatar
- profile information
- interests
- Shared Sparks

The interaction is designed to feel mobile-native rather than like another traditional search or wallet interface.

---

## Shared Sparks

Shared Sparks highlight interests two members have in common.

Examples include:

- music
- games
- food
- hobbies
- books
- pets
- weekend interests
- conversation topics

A member can send a **Spark** to another person.

The recipient can accept the Spark before the two users become Circle connections.

Radiant Circle is designed as a **friend-first social experience**, not a dating application.

---

## Your Circle

Accepted connections become part of each user's Circle.

The relationship model supports:

- Spark
- Accept
- Remove
- reconnect after removal
- Block
- Report

A deterministic pair relationship model prevents separate conflicting relationship records for the same two users.

---

## Radiant AI Spark Starter

Radiant Circle includes **AI Spark Starter**, an AI-assisted social feature designed to help two newly connected users start a natural conversation.

After two users become accepted Circle connections, the app can generate a contextual starter from their verified **Shared Sparks**.

```text
Accepted Circle relationship
        ↓
Verified Shared Sparks
        ↓
Authenticated Firebase callable function
        ↓
OpenAI GPT-6 Luna
        ↓
Contextual conversation starter
        ↓
User chooses whether to use it
```

### Privacy and control

- AI uses shared profile-interest fields rather than private chat history
- OpenAI requests are made through a server-side Firebase Cloud Function
- the OpenAI API key is stored server-side and is not embedded in the APK
- accepted Circle relationships are validated server-side
- generated suggestions are never automatically sent
- the user always chooses whether to insert and send a suggestion
- pair-level generation sequencing reduces duplicate results when both users generate at nearly the same time
- previous starter context is used to reduce repetition

The feature is intentionally an **AI conversation assistant**, not an autonomous chatbot.

---

## Private Circle Chat

Accepted Circle members can continue directly into private 1-to-1 chat.

Current chat capabilities include:

- profile avatars
- real-time messages
- optimistic sending
- unread state
- Sent / Read state
- typing presence
- incoming-message feedback
- AI Spark Starter
- Remove / Block / Report controls

Firestore authorization rules enforce relationship and ownership boundaries.

---

# Radiant Rush

Radiant Rush is a short-form native skill game built directly into Radiant Circle.

Players can:

- build score
- create combos
- earn progression
- collect rewards
- compete in weekly activity
- grow their Radiant collection

The game provides another reason to return to Radiant Circle beyond social discovery.

---

## Radiant Vault

Radiant Circle includes a collectible progression system with items across multiple rarity tiers.

Duplicate rewards can convert into **Radiant Shards**, allowing continued progression even after receiving repeated collectibles.

---

# Weekly Radiant Cup

Radiant Circle contains infrastructure for weekly skill competition.

The trusted competition architecture separates:

```text
Android gameplay
        ↓
submitted run evidence
        ↓
trusted server/admin verification
        ↓
competition result
        ↓
controlled reward lifecycle
```

Client-reported game data alone cannot authorize trusted sponsored payouts.

Competition infrastructure includes wallet-aware eligibility and wallet-lock safeguards for trusted competition flows.

---

# Real Solana Mainnet Participation

Radiant Circle uses Solana where blockchain adds meaningful ownership and verification.

The current release contains a real **ORE Mainnet portfolio and staking experience**.

Users can view:

- Available ORE
- Staked ORE
- Claimable ORE
- total position

Supported Mainnet actions include:

- **Stake**
- **Withdraw**
- **Claim**

Transactions are non-custodial and user-approved through **Solana Mobile Wallet Adapter**.

The app does not display a successful result simply because a wallet request was initiated.

The flow is:

```text
Read live Mainnet state
        ↓
User chooses an action
        ↓
Build transaction
        ↓
Mobile Wallet Adapter approval
        ↓
Wallet signs
        ↓
Submit to Solana
        ↓
Verify resulting onchain state
        ↓
Refresh portfolio
        ↓
Show before → after receipt
```

---

# Solana Mobile Integration

Radiant Circle currently uses Solana Mobile capabilities including:

- Solana Mobile Wallet Adapter
- non-custodial wallet authorization
- Mainnet transaction signing
- real ORE portfolio reads
- real ORE staking
- real ORE withdrawal
- real ORE claiming
- post-transaction onchain verification
- wallet-aware SKR Passport state
- trusted Weekly Radiant Cup infrastructure

The social experience remains usable without forcing users to connect a wallet first.

---

# Identity Model

Radiant Circle deliberately separates **social identity** from **wallet identity**.

```text
Firebase UID
    ↓
Radiant Circle social account

Solana wallet
    ↓
Optional ownership / proof / reward identity
```

Connecting a wallet does not replace the user's social account.

Firebase UID remains the primary social identity.

---

# Privacy and Security

Radiant Circle follows several core security rules:

- no treasury private key is embedded in the APK
- no Firebase Admin service-account private key is embedded in the APK
- no OpenAI API key is embedded in the APK or committed to the repository
- Android release signing keys and passwords are not committed
- `app/google-services.json` is intentionally excluded from the public repository
- social identity and wallet identity remain separate
- Circle chat is restricted to accepted connections
- Block / Remove terminate social access as appropriate
- Firestore rules enforce ownership and relationship boundaries
- client-reported game data alone cannot authorize trusted sponsored payouts
- Solana transactions remain user-approved and non-custodial
- ORE success states are verified against fresh Mainnet state
- AI Spark Starter does not send private chat history to OpenAI

---

# Technology

Radiant Circle is a native Android application.

### Android

- Kotlin
- Jetpack Compose
- Android foreground/device APIs
- native Android navigation and state management

### Backend

- Firebase Authentication
- Cloud Firestore
- Firebase Cloud Functions
- Firebase Secret Manager integration
- Firebase Admin trusted operator tooling

### AI

- OpenAI API
- GPT-6 Luna
- server-side prompt generation from Shared Sparks
- pair-level generation sequencing and anti-repetition logic

### Solana

- Solana Mobile Stack
- Mobile Wallet Adapter
- Solana Mainnet RPC
- ORE Mainnet protocol integration
- transaction construction and signing
- post-transaction onchain verification

---

# Repository Structure

```text
app/
    Native Android / Jetpack Compose application

functions/
    Firebase Cloud Function for AI Spark Starter

firebase/
    Firestore security rules and Firebase configuration

scripts/firebase-admin/
    Trusted competition, verification and payout tooling

docs/
    Architecture, QA, submission evidence and technical documentation
```

---

# Build

From Git Bash or another compatible shell:

```bash
./gradlew.bat test
./gradlew.bat :app:assembleDebug
```

Debug APK:

```text
app/build/outputs/apk/debug/app-debug.apk
```

For production Google Sign-In, a local build requires the developer's own:

```text
app/google-services.json
```

with matching Firebase Android/OAuth configuration.

> For complete judging, use the official signed release APK.

---

# Verified Final Release

```text
App:         Radiant Circle
Package:     com.thinkblox.radiantrush

Version:     1.2.18-ai1
VersionCode: 43

APK:
RadiantCircle-v1.2.18-clock-in.apk

SHA-256:
52ecd83059f78a5d8d25175834ee125ef42deb510f2ec8aefea39f40bba281ee
```

Frozen source checkpoint:

```text
Commit:
0a181171194403b8ec95aebfb069c6800f491fbd

Tag:
clock-in-submission-v3
```

The release tag points to the exact source used for the final judge APK.

---

# Verification

Before the final submission source was frozen, the project successfully passed:

```bash
./gradlew.bat test
./gradlew.bat :app:assembleDebug
./gradlew.bat :app:assembleRelease
```

The final signed APK was also:

- zipaligned
- signed with the permanent release certificate
- cryptographically verified
- verified with APK Signature Scheme v2
- verified with APK Signature Scheme v3
- installed successfully on physical Android devices
- tested with Google Sign-In
- tested with the live Radiant Circle social experience
- tested with AI Spark Starter
- tested with simultaneous AI generation from both sides of a Circle connection
- tested with real Solana mobile flows

---

# Judge Demo Flow

The short demo focuses on the core product loop:

```text
Daily Radiance
        ↓
Shake to Discover
        ↓
Shared Sparks
        ↓
Send Spark
        ↓
Accept
        ↓
AI Spark Starter
        ↓
Use This → Private Circle Chat
        ↓
Radiant Rush
        ↓
SKR / Weekly Radiant Cup
        ↓
ORE Mainnet Portfolio
        ↓
Real MWA transaction
        ↓
Verified before → after state
```

The product story is:

> **Daily habit → real people → AI-assisted connection → play → real onchain participation.**

---

# Product Vision

Radiant Circle explores a different direction for crypto-native mobile products.

Instead of asking:

> “How do we put another financial dashboard on a phone?”

Radiant Circle asks:

> **“What would make someone actually want to open a Solana app every day?”**

Our answer is:

**people, daily rituals, AI-assisted connection, play, progression, and optional ownership — with Solana underneath when it adds real value.**

---

**Radiant Circle**

**Connect. Play. Participate.**
