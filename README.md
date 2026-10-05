# Radiant Circle

**Connect. Play. Participate.**

Radiant Circle is a social-first Android experience built for Solana Mobile.

Instead of starting with a wallet or a trading screen, Radiant Circle starts with people: a daily ritual, social discovery, shared interests, private connections, a native mobile game, and optional onchain participation through Solana Mobile Wallet Adapter.

> **Radiant Circle** is the product.
> **Radiant Rush** is the native skill game built inside Radiant Circle.

---

## Download the App

For judging and full product testing, use the **official signed Android APK**.

### Radiant Circle v1.2.17 — CLOCK IN Submission

- **Version:** `1.2.17-ore15b2a`
- **VersionCode:** `42`
- **Package:** `com.thinkblox.radiantrush`
- **Release tag:** `clock-in-submission-v2`
- **SHA-256:** `953f23eae76df225ac7f17e054f5131e8e24778f087262eafedbdebdc91ff006`

**[Download the official signed APK](https://github.com/juanc07/Radiant-Circle/releases/download/clock-in-submission-v2/RadiantCircle-v1.2.17-clock-in.apk)**

**[View the GitHub Release](https://github.com/juanc07/Radiant-Circle/releases/tag/clock-in-submission-v2)**

> [!IMPORTANT]
> ### Judge testing — use the official signed APK
>
> The public repository intentionally does **not** include `app/google-services.json`. This avoids publishing the production Firebase client configuration in the submission repository.
>
> The source can still be cloned and built for code review and local development. However, **Google Sign-In and production Firebase-connected authentication will not work from a fresh local build** unless the developer supplies their own Firebase Android configuration and matching OAuth/signing credentials.
>
> For complete judge testing, including the configured Google Sign-In and production Firebase flows, use the official signed APK linked above: **`RadiantCircle-v1.2.17-clock-in.apk`**.
>
> No Firebase Admin service-account credentials, wallet private keys, release signing keystores, OpenAI API keys, or other server-side secrets are included in this repository.

A sanitized template is provided at `app/google-services.json.example` to show the expected local configuration shape without publishing production values.

The source repository remains available for code review and local debug builds.

---

## The idea

Most crypto apps begin with finance.

**Radiant Circle begins with a person.**

The experience is built around a repeatable mobile loop:

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
Build Your Circle
        ↓
Private Chat
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

The recipient can then accept the Spark before the two users become Circle connections.

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

A deterministic relationship model prevents separate conflicting relationship records for the same user pair.

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

Radiant Circle also contains infrastructure for weekly skill competition.

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

Competition infrastructure includes wallet-lock safeguards so an account cannot generate multiple trusted entries through different wallets for the same Cup period.

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

This allows the UI to show the actual change in Available, Staked, or Claimable ORE after verification.

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
- onchain post-transaction verification
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

This means connecting a wallet does not replace the user's social account.

Firebase UID remains the primary social identity.

---

# Privacy and Security

Radiant Circle follows several core security rules:

- no treasury private key is embedded in the APK
- no Firebase service-account credential is embedded in the APK
- signing keys are not committed to the repository
- social identity and wallet identity remain separate
- Circle chat is restricted to accepted connections
- Block / Remove terminate social access as appropriate
- Firestore rules enforce ownership and relationship boundaries
- client-reported game data alone cannot authorize trusted sponsored payouts
- Solana transactions remain user-approved and non-custodial
- ORE success states are verified against fresh Mainnet state

`app/google-services.json` is intentionally tracked because it contains Firebase Android client configuration required by the app.

Server credentials, service-account files, signing keys, and other secrets remain outside Git.

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
- Firebase Admin trusted operator tooling

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

firebase/
    Firestore security rules and Firebase configuration

scripts/firebase-admin/
    Trusted competition, verification and payout tooling

docs/
    Architecture, phase documentation, QA and technical evidence
```

---

# Build

From Git Bash or another compatible shell:

```bash
./gradlew testDebugUnitTest
./gradlew assembleDebug
```

Debug APK:

```text
app/build/outputs/apk/debug/app-debug.apk
```

Install on a connected Android device:

```bash
./gradlew installDebug
```

Run connected-device tests:

```bash
./gradlew connectedDebugAndroidTest
```

> Local debug builds are useful for source verification and development. For Google Sign-In and full judging, use the official signed APK from the release above.

---

# Verified Release

Current submission release:

```text
App:         Radiant Circle
Package:     com.thinkblox.radiantrush

Version:     1.2.17-ore15b2a
VersionCode: 42

Release APK:
RadiantCircle-v1.2.17-clock-in.apk
```

Final release-code checkpoint:

```text
Commit:
d226d22a2e5094219c819c1730ef74602a809ae8

Tag:
clock-in-submission-v2
```

Current GitHub `main`, including submission documentation updates:

```text
902e66f09daa0f40e089592136157a7e9cf3ffa4
```

The documentation-only commits after the release checkpoint do not change the signed submission APK.

---

# Verification

Before the submission release was frozen, the project passed:

```bash
./gradlew testDebugUnitTest
./gradlew assembleDebug
./gradlew connectedDebugAndroidTest
```

Connected Android instrumentation result:

```text
7 tests
0 failed
0 skipped
```

The signed release APK was also:

- zipaligned
- signed with the permanent release certificate
- cryptographically verified
- installed on physical Android devices
- tested with Google Sign-In
- tested with the live Radiant Circle experience

---

# Judge Demo

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
Private Circle Chat
        ↓
Radiant Rush
        ↓
ORE Mainnet Portfolio
        ↓
Real MWA transaction
        ↓
Verified before → after receipt
```

The story is simple:

> **Daily habit → real people → play → real onchain participation.**

---

# Product Vision

Radiant Circle explores a different direction for crypto-native mobile products.

Instead of asking:

> “How do we put another financial dashboard on a phone?”

Radiant Circle asks:

> **“What would make someone actually want to open a Solana app every day?”**

Our answer is:

**people, daily rituals, play, progression, and optional ownership — with Solana underneath when it adds real value.**

---

**Radiant Circle**

**Connect. Play. Participate.**
