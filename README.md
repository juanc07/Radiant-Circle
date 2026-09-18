# Radiant Circle

**Shake. Connect. Play. Prove.**

Radiant Circle is an Android-first social experience built for Solana Mobile. It combines a lightweight daily habit loop, proximity-aware social discovery, private Circle connections, a native skill game, collectible progression, and trusted weekly competition powered by optional Solana wallet proofs.

> **Radiant Circle** is the product/app. **Radiant Rush** is the native skill game inside Radiant Circle.

## Why Radiant Circle

Most crypto apps begin with a wallet. Radiant Circle begins with a person.

The core experience is designed around a simple human loop:

```text
Open Radiant Circle
        ↓
Daily Radiance + Daily Plan
        ↓
Shake your Seeker to discover someone
        ↓
See Shared Sparks
        ↓
Connect through Your Circle
        ↓
Private 1-to-1 chat
        ↓
Play Radiant Rush
        ↓
Open rewards + grow the Radiant Vault
        ↓
Compete in the Weekly Radiant Cup
        ↓
Use Solana for trusted identity / reward proof when needed
```

The goal is to make Solana useful without making blockchain the first thing a new user has to understand.

## Product highlights

### Daily habit
- **Daily Radiance** with a deterministic daily message and dedicated Radiance streak.
- **Daily Plan** and **Daily Radiant Chest** give players lightweight reasons to return.
- Progress and rewards feed into the broader Radiant Circle experience.

### Shake to Discover
- Physically shake a supported Android / Solana Mobile device to discover another active Radiant Circle member.
- Discovery begins nearby and can widen from local to regional, country, then global availability.
- Uses **approximate foreground location only**.
- Exact coordinates are not stored or shown to other members.

### Shared Sparks + Your Circle
- Profiles highlight interests such as food, music, games, hobbies, books, pets, weekend vibe, and topics people can talk about for hours.
- **Shared Sparks** surface interests two people have in common.
- Members can send and accept a Spark before becoming Circle connections.
- The social model is **friend-first**, not dating-first.

### Private Circle chat
- 1-to-1 chat is available only between accepted Circle connections.
- Includes avatars, optimistic sending, unread state, **Sent / Read** status, live typing presence, and subtle incoming-message feedback.
- Includes **Remove, Block, and Report** safety controls.
- Messages are immutable from the client and chat access is enforced by Firestore rules.

### Radiant Rush
- A native short-form skill game built directly into Radiant Circle.
- Players compete for score, progress, collectibles, and Weekly Radiant Cup placement.
- The game is intentionally fast enough to fit between social interactions and daily activities.

### Radiant Vault
- A **12-collectible** collection system across Common, Uncommon, Rare, Epic, and Legendary tiers.
- Duplicate rewards convert into **Radiant Shards**.
- Collection milestones reward continued participation.

### Weekly Radiant Cup
- Weekly skill competition with trusted competition receipts and controlled reward flows.
- Sponsored SKR reward operations use server/admin-side verification rather than trusting client score claims.
- Competition safeguards include an account-to-wallet lock for a Cup/week so one Firebase account cannot create trusted eligible entries through multiple wallets during the same Cup.

### SKR Passport + optional wallet
- The wallet is a **trust and reward layer**, not the user's social identity.
- Firebase UID remains the Radiant Circle account identity.
- A user may use different wallets over time, while competition policy can lock one wallet for a specific Cup.
- No treasury/private key is embedded in the APK.

## Solana Mobile integration

Radiant Circle uses Solana Mobile capabilities where they add meaningful trust or ownership:

- Mobile Wallet Adapter wallet connection/signing flow.
- On-chain memo/proof interactions from earlier project phases.
- Wallet-aware SKR Passport state.
- Trusted Weekly Radiant Cup reward and payout evidence.
- Real sponsored SKR transfer/reconciliation tooling kept outside the APK.

The social experience remains usable without forcing users to begin with a wallet.

## Privacy and security model

- Approximate location only for Shake-to-Discover.
- No raw latitude/longitude is intentionally stored for social discovery.
- Public profiles do not expose Firebase UID, email, exact location, or private wallet/account data.
- Circle chat is restricted to accepted connections.
- Block/remove actions terminate the social/chat relationship.
- Firestore rules enforce ownership and relationship boundaries.
- Client-reported game data alone cannot authorize sponsored SKR payouts.
- Treasury/private keys and service-account credentials are never embedded in the Android app.

## Current engineering status

Stable social/chat checkpoint:

```text
Tag:    phase-13d-social-chat-stable
Commit: 3ca0e11
```

Current automated/device verification includes:

```bash
./gradlew :app:testDebugUnitTest
./gradlew :app:assembleDebug
./gradlew :app:connectedDebugAndroidTest
./gradlew :app:installDebug
```

The Phase 12G competition-wallet safeguard also has extensive admin-side automated coverage. Final fresh-Cup live wallet-switch proof is intentionally tracked separately from automated verification.

## Naming contract

- Product/app: **Radiant Circle**
- Built-in game: **Radiant Rush**
- Weekly competition: **Weekly Radiant Cup**
- Daily reward: **Daily Radiant Chest**
- Wallet / reward identity surface: **SKR Passport**
- Android namespace/applicationId remains `com.thinkblox.radiantrush` for upgrade, Firebase, wallet, and persisted-data compatibility.

Internal names such as `RadiantRushApp` and legacy protocol identifiers are implementation details and do not change the user-facing Radiant Circle brand.

## Tech stack

- Kotlin
- Jetpack Compose
- Firebase Authentication
- Cloud Firestore
- Solana Mobile Stack / Mobile Wallet Adapter
- Solana RPC / transaction proof flows
- Firebase Admin tooling for trusted competition/reward operations

## Repository structure

```text
app/                     Android / Jetpack Compose application
firebase/                Firestore security rules and Firebase config
scripts/firebase-admin/  Trusted admin / competition / payout tooling
docs/                    Architecture, phase notes, QA and submission evidence
```

Sensitive local files such as `app/google-services.json`, service-account credentials, signing keys, and other secrets are intentionally excluded from Git.

## Build

From Git Bash / a compatible shell:

```bash
./gradlew :app:testDebugUnitTest
./gradlew :app:assembleDebug
```

Debug APK output:

```text
app/build/outputs/apk/debug/app-debug.apk
```

Install on a connected Android device:

```bash
./gradlew :app:installDebug
```

Run connected Android tests:

```bash
./gradlew :app:connectedDebugAndroidTest
```

## Judge demo flow

The intended short demo is:

```text
Daily Radiance
→ Shake to Discover
→ Shared Sparks
→ Connect
→ Private Circle chat
→ Radiant Rush
→ Radiant Vault reward
→ Weekly Radiant Cup
→ Solana / SKR trust layer
```

The product story is **human connection first, Solana trust when it matters**.

---

**Radiant Circle — Shake. Connect. Play. Prove.**
