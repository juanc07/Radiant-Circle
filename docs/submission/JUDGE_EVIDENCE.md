# Radiant Circle — CLOCK IN Judge Evidence

This document is the judge-facing evidence map for the final Radiant Circle CLOCK IN submission.

It is intentionally organized around **what works now**, **where it exists in source**, and **what should be shown in the demo**.

> **Product thesis:** Most crypto apps begin with finance. Radiant Circle begins with a person.

---

## 1. Final Judge Build

| Item | Evidence |
|---|---|
| Product | Radiant Circle |
| Platform | Native Android / Solana Mobile |
| Package | `com.thinkblox.radiantrush` |
| Version | `1.2.18-ai1` |
| VersionCode | `43` |
| APK | `RadiantCircle-v1.2.18-clock-in.apk` |
| Release tag | `clock-in-submission-v3` |
| Frozen APK source | `0a181171194403b8ec95aebfb069c6800f491fbd` |
| SHA-256 | `52ecd83059f78a5d8d25175834ee125ef42deb510f2ec8aefea39f40bba281ee` |

**Direct APK download:**  
https://github.com/juanc07/Radiant-Circle/releases/download/clock-in-submission-v3/RadiantCircle-v1.2.18-clock-in.apk

**GitHub Release:**  
https://github.com/juanc07/Radiant-Circle/releases/tag/clock-in-submission-v3

The release tag points to the exact source checkpoint used for the final judge APK. Later README/evidence-document commits on `main` do not move the frozen release tag.

---

## 2. Problem and Impact

Most crypto mobile apps are transaction-, balance-, or speculation-first. That gives ordinary users little reason to return when they are not actively trading.

Mainstream social and mobile-game products solve daily engagement, but normally do not provide portable wallet identity, verifiable signing, or onchain ownership/reward rails.

Radiant Circle bridges those two worlds:

```text
Daily habit
    ↓
Real people
    ↓
Shared interests
    ↓
AI-assisted conversation
    ↓
Play and progression
    ↓
Optional Solana ownership / signing / verification
```

The goal is to make Solana useful **inside a repeatable consumer-mobile loop**, rather than making blockchain knowledge the entry requirement.

---

## 3. Core Product Loop

The current product flow is:

```text
Daily Radiance
    ↓
Shake-to-Discover
    ↓
Shared Sparks
    ↓
Send Spark
    ↓
Accept
    ↓
Circle
    ↓
AI Spark Starter + Private Chat
    ↓
Radiant Rush
    ↓
Radiant Vault / Weekly Radiant Cup
    ↓
Optional real Solana Mainnet participation
```

### Primary source locations

- `app/src/main/java/com/thinkblox/radiantrush/ui/screens/CircleScreen.kt`
- `app/src/main/java/com/thinkblox/radiantrush/firebase/FirebaseRadiantRepository.kt`
- `app/src/main/java/com/thinkblox/radiantrush/data/CircleModels.kt`
- `app/src/main/java/com/thinkblox/radiantrush/data/CircleChatModels.kt`
- `app/src/main/java/com/thinkblox/radiantrush/logic/CircleDiscoveryRules.kt`
- `app/src/main/java/com/thinkblox/radiantrush/logic/CircleDiscoverySafetyRules.kt`
- `app/src/main/java/com/thinkblox/radiantrush/logic/CircleChatRules.kt`
- `firebase/firestore.rules`

---

# 4. Social / Circle Evidence

## What works

Radiant Circle supports:

- profile-based discovery
- Shared Sparks derived from common interests
- Spark request
- recipient Accept / Ignore
- accepted Circle relationship
- private 1-to-1 chat
- typing presence
- Sent / Read state
- unread state
- Remove
- Block
- Report

Private chat is intentionally gated behind mutual acceptance.

### Firestore evidence

The application uses relationship records under:

- `circleProfiles`
- `circleEdges`
- `circleChats`
- per-chat `messages`

`firebase/firestore.rules` checks authenticated membership and accepted relationship state before allowing protected Circle chat behavior.

### Demo proof to capture

1. Phone A discovers Phone B.
2. Shared Sparks are visible.
3. Phone A sends Spark.
4. Phone B accepts.
5. The accepted relationship appears in Circle.
6. Both users can enter private chat.
7. Send a real message and show it arrive on the second device.

---

# 5. Radiant AI Spark Starter

Radiant AI Spark Starter is a real server-backed AI feature designed for one narrow social problem:

> Two people found a real shared interest. What is a natural first thing to say?

It is **not** an autonomous chatbot and does not auto-send messages.

## Client evidence

### Android callable client

`app/src/main/java/com/thinkblox/radiantrush/firebase/AiSparkStarterRepository.kt`

The client:

- requires a signed-in Firebase user
- sends only the selected accepted Circle peer UID
- calls the Firebase callable function `generateSparkStarter`
- receives a generated starter
- exposes friendly permission/rate-limit/error states

### UI

`app/src/main/java/com/thinkblox/radiantrush/ui/screens/CircleScreen.kt`

The accepted Circle chat provides:

- collapsible **AI Spark Starter** panel
- **Generate AI icebreaker**
- **Another**
- **Use this**
- user-controlled insertion into the chat composer
- no automatic sending

## Server evidence

`functions/index.js`

`generateSparkStarter` performs the important trust checks server-side:

1. requires Firebase Authentication
2. validates `peerUid`
3. loads deterministic `circleEdges/<pairId>`
4. requires relationship status `ACCEPTED`
5. requires both authenticated member UIDs to be in the relationship
6. reads both `circleProfiles` server-side
7. derives Shared Sparks from selected profile fields
8. rejects generation when no Shared Sparks exist
9. applies a per-user daily generation limit
10. reserves pair-level generation sequencing in a Firestore transaction
11. sends only the Shared Sparks context to OpenAI
12. stores a short pair history to reduce repetition
13. returns one short conversation starter

### Current limits

- `MAX_GENERATIONS_PER_DAY = 20`
- maximum Shared Sparks included per request: `5`
- pair starter history used for anti-repetition: `6`
- output is clipped to a short starter
- private Circle chat history is **not** sent to OpenAI

### Secret handling

`functions/index.js` uses:

```text
defineSecret("OPENAI_API_KEY")
```

The key is supplied server-side through Firebase Secret Manager. It is not stored in Android source or the APK.

### AI model

Current deployed function uses:

```text
gpt-6-luna
```

### Concurrency evidence

Pair generation uses a Firestore transaction to reserve a unique generation sequence **before** the OpenAI request.

This was added specifically so two users pressing **Another** at nearly the same time do not start from the same pair generation state.

Physical-device test logs showed simultaneous requests receiving different generation sequence values and different focus/angle combinations.

### Additional documentation

- `docs/submission/AI_SPARK_STARTER.md`

### Demo proof to capture

1. Open an accepted Circle chat.
2. Expand AI Spark Starter.
3. Generate a starter.
4. Press **Another** and show a different result.
5. Press **Use this**.
6. Show the text appear in the normal composer.
7. Manually press Send.
8. Show the message arrive on the second phone.

This demonstrates that AI assists conversation but the human remains in control.

---

# 6. Radiant Rush Evidence

Radiant Rush is the native skill-game layer inside Radiant Circle.

### Primary source locations

- `app/src/main/java/com/thinkblox/radiantrush/ui/screens/RadiantRunScreen.kt`
- `app/src/main/java/com/thinkblox/radiantrush/ui/components/RadiantGameComponents.kt`
- `app/src/main/java/com/thinkblox/radiantrush/logic/RadiantGameRules.kt`
- `app/src/main/java/com/thinkblox/radiantrush/logic/RewardLoopRules.kt`
- `app/src/main/java/com/thinkblox/radiantrush/audio/ProceduralGameAudioEngine.kt`
- `app/src/main/java/com/thinkblox/radiantrush/ui/screens/LeaderboardScreen.kt`
- `app/src/main/java/com/thinkblox/radiantrush/logic/LeaderboardRules.kt`

### What to prove in demo

- native gameplay
- score / combo feedback
- progression/reward feedback
- connection to the broader Radiant Circle retention loop

The demo does not need to show every game mechanic. A short, clean gameplay sequence is stronger than an exhaustive walkthrough.

---

# 7. SKR Passport Evidence

SKR is integrated as wallet-aware state and eligibility/perk infrastructure.

### Primary source locations

- `app/src/main/java/com/thinkblox/radiantrush/solana/SkrBalanceRepository.kt`
- `app/src/main/java/com/thinkblox/radiantrush/logic/SkrPassportRules.kt`
- `app/src/main/java/com/thinkblox/radiantrush/logic/SkrStakingRules.kt`
- `app/src/main/java/com/thinkblox/radiantrush/logic/SkrTierRules.kt`
- `app/src/main/java/com/thinkblox/radiantrush/logic/WalletFeatureAccessRules.kt`

### Important implementation behavior

`SkrPassportRules.kt` distinguishes:

- liquid Mainnet SKR
- verified active staked SKR
- unstaking/cooldown SKR

Only verified active stake is included in the eligible Passport balance.

The Passport perks are intentionally off-chain/non-ranked benefits. Staking does **not** modify the raw ranked score, tie-breakers, or ranked-attempt limit.

Detailed SKR/competition evidence is in:

- `docs/submission/SKR_EVIDENCE.md`

---

# 8. Weekly Radiant Cup / Competition Evidence

The competition system is split between Android participation and trusted server/admin authority.

### Android/client-side rules

- `app/src/main/java/com/thinkblox/radiantrush/logic/Phase11CompetitionRules.kt`
- `app/src/main/java/com/thinkblox/radiantrush/logic/WeeklyRadiantCupRules.kt`
- `app/src/main/java/com/thinkblox/radiantrush/logic/Phase12CompetitionVerificationRules.kt`
- `app/src/main/java/com/thinkblox/radiantrush/logic/Phase12CompetitionWalletLockRules.kt`
- `firebase/firestore.rules`

### Trusted operator/admin tooling

- `scripts/firebase-admin/competition-run-verification.mjs`
- `scripts/firebase-admin/competition-wallet-lock.mjs`
- `scripts/firebase-admin/skr-funding-verification.mjs`
- `scripts/firebase-admin/weekly-cup-finalization.mjs`
- `scripts/firebase-admin/weekly-cup-payout-lifecycle.mjs`
- `scripts/firebase-admin/weekly-cup-skr-transfer.mjs`
- `scripts/firebase-admin/execute-weekly-cup-skr-transfer.mjs`
- `scripts/firebase-admin/reconcile-weekly-cup-skr-transfer.mjs`
- `scripts/firebase-admin/radiant-cup-operator.mjs`

### Wallet-lock design

For a Firebase UID/week, the first persisted ranked Cup entry establishes the competition wallet lock.

Later ranked entries for the same account/week must use that same wallet.

Relevant code:

- `Phase12CompetitionWalletLockRules.kt`
- `firebase/firestore.rules`
- `scripts/firebase-admin/competition-wallet-lock.mjs`

### Reward/payout authority

The Android app is **not** trusted to authorize SKR payouts.

Trusted tools verify:

- competition evidence
- funding state
- result/finalization state
- wallet-lock evidence
- payout manifest/state
- transfer/reconciliation state

This prevents a client-reported score from directly becoming payout authority.

### Important claim boundary

The repository contains a real transfer/reconciliation path, but this evidence document does **not** claim that a live sponsored SKR prize payout was executed unless a real transaction signature/explorer link is added.

If a real payout is used for judging, add it to `SKR_EVIDENCE.md`.

---

# 9. ORE Mainnet + Mobile Wallet Adapter Evidence

Radiant Circle contains a real non-custodial ORE Mainnet portfolio/action flow.

### Main source locations

- `app/src/main/java/com/thinkblox/radiantrush/solana/OrePortfolioRepository.kt`
- `app/src/main/java/com/thinkblox/radiantrush/solana/MobileWalletRepository.kt`
- `app/src/main/java/com/thinkblox/radiantrush/logic/OreStakingRules.kt`
- `app/src/main/java/com/thinkblox/radiantrush/logic/OreStakeActionRules.kt`
- `app/src/main/java/com/thinkblox/radiantrush/logic/OreStakeInstructionRules.kt`
- `app/src/main/java/com/thinkblox/radiantrush/ui/screens/OrePortfolioScreen.kt`

### Portfolio read

`OrePortfolioRepository.kt` reads ORE Mainnet state and presents values including:

- liquid/available ORE
- staked ORE
- unclaimed/claimable rewards
- stake-account verification information

### Supported actions

The current Mainnet action flow supports:

- Stake
- Withdraw
- Claim

### Wallet signing

`MobileWalletRepository.kt`:

- creates a Solana **Mainnet** Mobile Wallet Adapter session for ORE actions
- verifies the wallet selected by the wallet app matches the wallet shown in Radiant Circle
- builds the ORE transaction
- sends it through `signAndSendTransactions`
- receives the transaction signature
- waits for Mainnet confirmation

The repository explicitly states that it never asks for or stores seed phrases/private keys.

### Post-state verification

After a successful transaction, the product refreshes the ORE portfolio and shows a before/after receipt.

`OrePortfolioScreen.kt` labels those values as fresh Mainnet reads rather than estimated UI balances.

### Demo proof to capture

Best evidence sequence:

1. show ORE pre-state
2. choose one supported action
3. show Mobile Wallet Adapter / wallet approval
4. return to Radiant Circle
5. show verified receipt
6. show refreshed before → after state
7. optionally show transaction signature/explorer link

Never fake a transaction or onchain balance for the demo.

---

# 10. Security Evidence

Detailed security evidence is in:

- `docs/submission/SECURITY.md`

Key points:

- no wallet seed/private key collection
- non-custodial Mobile Wallet Adapter signing
- OpenAI key is server-side only
- Firebase Admin credentials are not shipped in Android
- Android release keystore is outside the public repository
- current public source excludes `app/google-services.json`
- accepted Circle relationship is checked before private chat/AI behavior
- Firestore rules restrict relationship/message access
- trusted competition payout authority is separated from Android client state

---

# 11. Submission / Release Evidence

### Frozen source

```text
0a181171194403b8ec95aebfb069c6800f491fbd
```

### Release tag

```text
clock-in-submission-v3
```

### APK

```text
RadiantCircle-v1.2.18-clock-in.apk
```

### SHA-256

```text
52ecd83059f78a5d8d25175834ee125ef42deb510f2ec8aefea39f40bba281ee
```

### Direct download

https://github.com/juanc07/Radiant-Circle/releases/download/clock-in-submission-v3/RadiantCircle-v1.2.18-clock-in.apk

### Release page

https://github.com/juanc07/Radiant-Circle/releases/tag/clock-in-submission-v3

---

# 12. Demo Video

**Status:** Pending final recording.

Add the final public/unlisted video URL and exact timestamps here before submission.

## Suggested timestamp map

| Target time | Evidence |
|---|---|
| `00:00–00:12` | Radiant Circle thesis / opening |
| `00:12–00:30` | Daily Radiance |
| `00:30–00:55` | Shake-to-Discover + Shared Sparks |
| `00:55–01:15` | Spark → Accept on second phone |
| `01:15–01:45` | Circle chat + AI Spark Starter + Use This + send |
| `01:45–02:05` | Radiant Rush |
| `02:05–02:20` | SKR Passport / Weekly Cup / Vault |
| `02:20–02:45` | ORE Mainnet + MWA + verified receipt |
| `02:45–02:55` | closing thesis |

### Final video URL

```text
PENDING
```

### Final timestamps

```text
PENDING
```

---

# 13. Judge Claim Boundaries

Radiant Circle intentionally avoids claiming more than the current build proves.

The submission does **not** claim:

- exact physical proximity between users
- automatic AI messaging
- AI access to private chat history
- custodial wallet control
- possession of user seed phrases/private keys
- that Android client state alone can authorize sponsored payouts
- a live SKR payout transaction unless an actual verified transaction link is supplied
- dApp Store publication unless publication is actually completed
- Firebase App Check enforcement for AI callable requests in the current release

This distinction is intentional: the judge should be able to separate **working implementation**, **trusted infrastructure**, and **future/product vision**.

---

## Final Judge Summary

Radiant Circle's strongest evidence is the combination of:

1. a real social-first Android product loop,
2. real two-user Circle interaction,
3. a real authenticated server-side AI feature grounded in Shared Sparks,
4. native Radiant Rush gameplay and competition infrastructure,
5. SKR wallet-aware Passport / Cup logic with trusted payout separation,
6. and real non-custodial Solana Mainnet ORE actions through Mobile Wallet Adapter.

The submission should be judged on what is implemented and demonstrable in the final signed APK and source tag above.
