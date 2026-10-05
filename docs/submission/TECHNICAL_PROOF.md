# Radiant Circle — Technical Proof Index

> Judge-facing implementation map for the CLOCK IN submission.
>
> This document points to the concrete source, tests, trusted tooling, and
> release evidence behind the submission claims.

## Frozen submission build

- **App:** Radiant Circle
- **Package:** `com.thinkblox.radiantrush`
- **Version:** `1.2.19`
- **VersionCode:** `44`
- **Frozen tag:** `clock-in-submission-v4`
- **Frozen source commit:** `d2f66c63d2860c4179af84284a2b7453f9991f46`
- **APK:** `RadiantCircle-v1.2.19-clock-in.apk`
- **APK SHA-256:** `07c577ca058587964e7ffa2a28e3043814cdede2832baa2709cdb8dfa45e10e3`
- **Release signer SHA-256:** `1f579b2703386ea0c34a5b70db165371b2f7564a0c9b404e9b191f2e0abc7973`

The v4 tag remains immutable. Later documentation-only commits on `main` do not
change the frozen APK/source checkpoint.

---

## 1. AI Spark Starter

### What is implemented

AI Spark Starter is available only after two users become an accepted Circle
connection. It creates a contextual first-message suggestion from the users'
Shared Sparks/public interests.

The generated text is never auto-sent. `Use this` fills the message composer;
the user still chooses whether to send it.

Private Circle chat history is not sent to the AI provider.

### Server-side proof

Primary backend location:

- `functions/`
- callable export/symbol: `generateSparkStarter`

Useful repository search:

```bash
git grep -n "generateSparkStarter" -- functions app
git grep -n "OPENAI_API_KEY" -- functions
git grep -n "gpt-6-luna" -- functions
```

The implementation proves these boundaries:

1. Firebase Authentication is required.
2. The caller supplies the peer UID, not trusted relationship context.
3. The server validates the Circle edge and requires `ACCEPTED` status.
4. The server verifies that both UIDs belong to the relationship.
5. Shared-interest context is derived server-side.
6. The OpenAI credential is read server-side through Firebase Secret Manager.
7. The deployed model is `gpt-6-luna`.
8. Generation is bounded by per-user limits.
9. Pair-level sequencing / anti-repetition reduces duplicate starters.
10. Private chat history is excluded from the AI context.

### Client/social proof

Relevant Android source:

- `app/src/main/java/com/thinkblox/radiantrush/firebase/AiSparkStarterRepository.kt`
- `app/src/main/java/com/thinkblox/radiantrush/firebase/FirebaseRadiantRepository.kt`
- `app/src/main/java/com/thinkblox/radiantrush/logic/SharedSparkRules.kt`
- `app/src/main/java/com/thinkblox/radiantrush/logic/CircleChatRules.kt`
- `app/src/main/java/com/thinkblox/radiantrush/ui/screens/CircleScreen.kt`

Relevant unit tests include:

- `app/src/test/java/com/thinkblox/radiantrush/logic/SharedSparkRulesTest.kt`
- `app/src/test/java/com/thinkblox/radiantrush/logic/CircleChatRulesTest.kt`

### Manual/device validation

The final build was manually validated with:

- accepted two-user Circle relationship
- AI generation inside Circle chat
- `Use this` filling the composer without auto-send
- simultaneous generation from both sides of the same Circle relationship
- distinct generation sequencing / reduced repetition
- reconnect behavior where a new accepted session does not expose the prior
  visible chat history

---

## 2. SKR Passport and ranked Weekly Cup

### Mainnet SKR read

Relevant Android source:

- `app/src/main/java/com/thinkblox/radiantrush/solana/SkrBalanceRepository.kt`
- `app/src/main/java/com/thinkblox/radiantrush/logic/SkrTierRules.kt`
- `app/src/main/java/com/thinkblox/radiantrush/logic/SkrPassportRules.kt`
- `app/src/main/java/com/thinkblox/radiantrush/logic/SkrStakingRules.kt`
- `app/src/main/java/com/thinkblox/radiantrush/logic/QuestInteractionRules.kt`

`SkrBalanceRepository` performs Mainnet RPC reads for the official SKR mint and
builds the wallet snapshot used by the Passport.

`SkrPassportRules` converts verified liquid/staked SKR state into Passport tier
and perk state. Unverified stake is not invented or counted.

### Ranked wallet lock

Relevant client rule:

- `app/src/main/java/com/thinkblox/radiantrush/logic/Phase12CompetitionWalletLockRules.kt`

Trusted operator implementation:

- `scripts/firebase-admin/competition-wallet-lock.mjs`

The ranked flow is designed so that the first trusted competition wallet for an
account/week becomes the immutable Cup wallet. A later attempt with a different
wallet fails closed.

### Trusted run verification

Relevant trusted tooling/tests:

- `scripts/firebase-admin/verify-competition-run.test.mjs`
- `scripts/firebase-admin/competition-wallet-lock.test.mjs`
- `app/src/test/java/com/thinkblox/radiantrush/logic/Phase12CompetitionVerificationRulesTest.kt`
- `app/src/test/java/com/thinkblox/radiantrush/logic/Phase12CompetitionWalletLockRulesTest.kt`

The verification tests cover:

- exact independent run-evidence matching
- Cup-window validation
- immutable competition-wallet matching
- rejected runs remaining placement-ineligible
- payout eligibility staying disabled at verification time

### SKR funding / payout trust boundary

Relevant trusted tooling:

- `scripts/firebase-admin/skr-funding-verification.mjs`
- `scripts/firebase-admin/verify-weekly-cup-funding.test.mjs`
- `scripts/firebase-admin/manage-weekly-cup.test.mjs`
- `scripts/firebase-admin/finalize-weekly-cup.test.mjs`
- `scripts/firebase-admin/weekly-cup-payout-lifecycle.test.mjs`
- `scripts/firebase-admin/prepare-weekly-cup-skr-transfer-intent.mjs`
- `scripts/firebase-admin/execute-weekly-cup-skr-transfer.mjs`
- `scripts/firebase-admin/reconcile-weekly-cup-skr-transfer.mjs`
- `scripts/firebase-admin/weekly-cup-skr-transfer.test.mjs`

Important claim boundary:

> Client-reported gameplay alone cannot authorize a trusted sponsored payout.

Funding verification checks the official SKR mint and Mainnet wallet evidence.
Payout preparation/approval/transfer are kept in trusted admin tooling rather
than in the Android client.

### SKR unit tests

Relevant tests include:

- `app/src/test/java/com/thinkblox/radiantrush/logic/SkrPassportRulesTest.kt`
- `app/src/test/java/com/thinkblox/radiantrush/logic/SkrTierRulesTest.kt`
- `app/src/test/java/com/thinkblox/radiantrush/logic/SkrStakingRulesTest.kt`
- `app/src/test/java/com/thinkblox/radiantrush/logic/Phase12WeeklyCupConfigRulesTest.kt`
- `app/src/test/java/com/thinkblox/radiantrush/logic/Phase12WeeklyCupResultRulesTest.kt`
- `app/src/test/java/com/thinkblox/radiantrush/logic/RewardLoopRulesTest.kt`

Examples covered by these tests include Passport tiers/perks, verified stake
contribution, unverified stake exclusion, idempotent ticket entitlement,
official SKR mint/config validation, and Weekly Cup result/prize presentation.

---

## 3. ORE Mainnet portfolio and transactions

### Mainnet portfolio read

Relevant Android source:

- `app/src/main/java/com/thinkblox/radiantrush/solana/OrePortfolioRepository.kt`
- `app/src/main/java/com/thinkblox/radiantrush/data/OrePortfolioModels.kt`
- `app/src/main/java/com/thinkblox/radiantrush/logic/OreStakingRules.kt`
- `app/src/main/java/com/thinkblox/radiantrush/ui/screens/OrePortfolioScreen.kt`

`OrePortfolioRepository` reads live Mainnet state and derives the connected
wallet's liquid, staked, and claimable ORE position.

### User-approved transaction path

Relevant wallet code:

- `app/src/main/java/com/thinkblox/radiantrush/solana/MobileWalletRepository.kt`
- method/symbol: `sendOreStakeAction`

The transaction path:

```text
Read current Mainnet state
        ↓
Validate requested amount/action
        ↓
Build ORE transaction
        ↓
Open Solana Mobile Wallet Adapter
        ↓
User approves/signs in wallet
        ↓
Submit transaction to Mainnet
        ↓
Read refreshed Mainnet state
        ↓
Verify observed state transition
        ↓
Show transaction receipt
```

Radiant Circle never requests or stores seed phrases/private keys.

### Post-state verification

Relevant rule:

- `app/src/main/java/com/thinkblox/radiantrush/logic/OreStakeActionRules.kt`
- method/symbol: `verifiedAfterRefresh`

The rule verifies action success using refreshed observed state rather than
treating the wallet request itself as proof of success.

Relevant tests:

- `app/src/test/java/com/thinkblox/radiantrush/logic/OreStakeActionRulesTest.kt`
- `app/src/test/java/com/thinkblox/radiantrush/logic/OreStakeInstructionRulesTest.kt`
- `app/src/test/java/com/thinkblox/radiantrush/logic/OreStakingRulesTest.kt`
- `app/src/test/java/com/thinkblox/radiantrush/logic/OrePortfolioPresentationRulesTest.kt`

`OreStakeActionRulesTest` covers observed state changes for Stake, Withdraw, and
Claim.

### Receipt behavior

The final v4 submission also improves receipt handling so a successfully
submitted transaction is still surfaced when fresh Mainnet verification is
temporarily propagating.

The UI distinguishes:

- **verified** — refreshed Mainnet state satisfies the expected transition
- **verification pending** — submission/signature exists but fresh post-state
  has not yet satisfied the strict verification rule

This avoids falsely presenting an unverified transaction as verified.

### Real-device validation

The submission build was tested on physical Android hardware with real Solana
wallet flows. ORE Mainnet approval and before/after receipt behavior were
confirmed on Solana Seeker.

---

## 4. Wallet / identity separation

Radiant Circle deliberately separates:

```text
Firebase UID
    ↓
social identity / profile / Circle

Solana wallet
    ↓
optional proof / ownership / ranked / ORE identity
```

Relevant wallet source:

- `app/src/main/java/com/thinkblox/radiantrush/solana/MobileWalletRepository.kt`
- `app/src/main/java/com/thinkblox/radiantrush/logic/WalletFeatureAccessRules.kt`

The social product does not require users to begin with a wallet.

---

## 5. Security proof

### Private credentials

The public client does not contain:

- Firebase Admin service-account private keys
- OpenAI API key
- Android release keystore/private signing key
- wallet private keys or seed phrases
- treasury private keys

The OpenAI credential is held server-side via Firebase Secret Manager.

### Firebase Android client API key

`google-services.json` contains a Firebase Android client API key, not a
Firebase Admin/private server credential.

The key is cloud-restricted with:

- Firebase-related API restrictions
- Android application restriction: `com.thinkblox.radiantrush`
- production release SHA-1:
  `F4:C8:35:74:A7:F6:33:6E:8A:22:1B:E5:F3:30:EE:27:F1:B9:88:62`

After the restriction was applied, the already-frozen v1.2.19 APK was re-tested:
Google Sign-In and Firebase-backed profile loading continued to work.

This cloud-side security remediation does not move or modify
`clock-in-submission-v4`.

### Firestore boundary

Relevant source:

- `firebase/firestore.rules`

The application uses Firestore rules plus server/trusted tooling boundaries for
ownership, Circle access, competition state, and admin-only operations.

---

## 6. Automated verification

Final submission validation included:

```bash
./gradlew.bat test
./gradlew.bat assembleRelease
```

Both passed before the final v4 release was frozen.

High-value test files include:

### Social / Circle

- `SharedSparkRulesTest.kt`
- `CircleChatRulesTest.kt`
- `CircleDiscoveryRulesTest.kt`
- `CircleDiscoverySafetyRulesTest.kt`

### SKR / Weekly Cup

- `SkrPassportRulesTest.kt`
- `SkrTierRulesTest.kt`
- `SkrStakingRulesTest.kt`
- `Phase12CompetitionWalletLockRulesTest.kt`
- `Phase12CompetitionVerificationRulesTest.kt`
- `Phase12WeeklyCupConfigRulesTest.kt`
- `Phase12WeeklyCupResultRulesTest.kt`
- trusted `scripts/firebase-admin/*.test.mjs` competition/funding/payout tests

### ORE

- `OreStakeActionRulesTest.kt`
- `OreStakeInstructionRulesTest.kt`
- `OreStakingRulesTest.kt`
- `OrePortfolioPresentationRulesTest.kt`

### Reward / daily loop

- `RewardLoopRulesTest.kt`
- `QuestInteractionRulesTest.kt`
- `DailyPlanRefreshRulesTest.kt`
- `DailyRadianceRulesTest.kt`

---

## 7. Fast judge verification commands

From the repository root:

```bash
# AI
git grep -n "generateSparkStarter" -- functions app
git grep -n "OPENAI_API_KEY" -- functions
git grep -n "gpt-6-luna" -- functions

# SKR / ranked wallet
git grep -n "OFFICIAL_SKR_MINT" -- app scripts
git grep -n "CompetitionWalletLock" -- app scripts
git grep -n "competition-wallet" -- scripts/firebase-admin

# ORE Mainnet
git grep -n "sendOreStakeAction" -- app
git grep -n "verifiedAfterRefresh" -- app
git grep -n "ORE_MINT" -- app

# Security
git grep -n "google-services.json" -- README.md docs
git grep -n "Circle" -- firebase/firestore.rules

# Android tests
./gradlew.bat test
```

---

## 8. Evidence map

| Claim | Primary evidence |
|---|---|
| AI is relationship-gated | `functions/` → `generateSparkStarter` |
| AI key stays server-side | Firebase Secret Manager / `OPENAI_API_KEY` |
| Private chat is excluded from AI context | AI callable + submission/security docs |
| Shared Sparks are real profile overlap | `SharedSparkRules.kt` + tests |
| SKR state comes from Mainnet | `SkrBalanceRepository.kt` |
| SKR perks use verified state | `SkrPassportRules.kt` + tests |
| Ranked wallet cannot silently switch | `Phase12CompetitionWalletLockRules.kt` + trusted wallet-lock tooling/tests |
| Client gameplay cannot authorize payout | trusted verification / payout lifecycle scripts |
| ORE is Mainnet and wallet-approved | `MobileWalletRepository.sendOreStakeAction` |
| ORE post-state is verified | `OreStakeActionRules.verifiedAfterRefresh` + tests |
| Wallet secrets are non-custodial | MWA flow; no seed/private-key storage |
| Firebase client key is restricted | README security remediation + Google Cloud restriction |
| Frozen submission is reproducibly identified | v4 tag + APK SHA-256 + signer SHA-256 |

---

## 9. Claim boundaries

- AI Spark Starter is an assistive conversation feature, not an autonomous agent.
- AI suggestions are not automatically sent.
- Private chat history is not sent to OpenAI.
- SKR Passport is a wallet/Mainnet verification feature; ranked eligibility is
  separate from any promise of automatic token payout.
- Weekly Cup trusted payout infrastructure is admin/server controlled; client
  gameplay alone cannot authorize transfer.
- ORE success is not inferred merely from opening a wallet request.
- The v4 APK/tag remains frozen even though later documentation/security
  configuration improvements may appear on `main`.

---

## 10. Submission links

- Repository: `https://github.com/juanc07/Radiant-Circle`
- Release: `https://github.com/juanc07/Radiant-Circle/releases/tag/clock-in-submission-v4`
- APK: `https://github.com/juanc07/Radiant-Circle/releases/download/clock-in-submission-v4/RadiantCircle-v1.2.19-clock-in.apk`
