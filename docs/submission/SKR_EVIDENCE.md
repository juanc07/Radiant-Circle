# Radiant Circle — SKR / Weekly Radiant Cup Evidence

This document focuses on Radiant Circle's SKR, SKR Passport, Weekly Radiant Cup, wallet-lock, funding-verification, and payout architecture.

It deliberately separates **wallet state**, **competition eligibility**, and **trusted payout authority** so judges can see what is implemented without overclaiming.

---

## 1. Final Release Context

- **APK:** `RadiantCircle-v1.2.18-clock-in.apk`
- **Release tag:** `clock-in-submission-v3`
- **Frozen source:** `0a181171194403b8ec95aebfb069c6800f491fbd`
- **SHA-256:** `52ecd83059f78a5d8d25175834ee125ef42deb510f2ec8aefea39f40bba281ee`

Direct APK:  
https://github.com/juanc07/Radiant-Circle/releases/download/clock-in-submission-v3/RadiantCircle-v1.2.18-clock-in.apk

---

## 2. SKR Passport — What Is Implemented

Primary source:

- `app/src/main/java/com/thinkblox/radiantrush/solana/SkrBalanceRepository.kt`
- `app/src/main/java/com/thinkblox/radiantrush/logic/SkrPassportRules.kt`
- `app/src/main/java/com/thinkblox/radiantrush/logic/SkrStakingRules.kt`
- `app/src/main/java/com/thinkblox/radiantrush/logic/SkrTierRules.kt`
- `app/src/main/java/com/thinkblox/radiantrush/logic/WalletFeatureAccessRules.kt`

`SkrPassportRules.kt` calculates Passport eligibility from:

```text
eligible SKR =
    liquid Mainnet SKR
    +
    verified active staked SKR
```

Important behavior:

- active staked SKR counts only when the staking read is verified
- cooldown/unstaking SKR may be displayed but is excluded from eligible Passport balance
- Passport benefits are off-chain/non-ranked
- SKR stake does not change raw ranked score, ranked tie-breakers, or the ranked-attempt cap

Examples of Passport benefits in source include casual-play tickets, Daily Radiant Chest enhancements, and cosmetic/status benefits.

This design keeps holder utility separate from raw competitive score.

---

## 3. Weekly Radiant Cup

Primary Android/client rule files:

- `app/src/main/java/com/thinkblox/radiantrush/logic/Phase11CompetitionRules.kt`
- `app/src/main/java/com/thinkblox/radiantrush/logic/WeeklyRadiantCupRules.kt`
- `app/src/main/java/com/thinkblox/radiantrush/logic/Phase12CompetitionVerificationRules.kt`
- `app/src/main/java/com/thinkblox/radiantrush/logic/Phase12CompetitionWalletLockRules.kt`
- `app/src/main/java/com/thinkblox/radiantrush/ui/screens/LeaderboardScreen.kt`
- `firebase/firestore.rules`

`WeeklyRadiantCupRules.kt` is competition/presentation logic. It does **not** grant token payout authority by itself.

Trusted result and reward handling are intentionally separated from the Android client.

---

## 4. Competition Wallet Lock

A key anti-abuse control is the account/week wallet lock.

### Android contract

`app/src/main/java/com/thinkblox/radiantrush/logic/Phase12CompetitionWalletLockRules.kt`

The rule is:

1. the first successfully persisted ranked Cup receipt for a Firebase UID/week establishes the competition wallet
2. later ranked entries for that same account/week must use that exact wallet

### Firestore authority

`firebase/firestore.rules`

The rules contain a dedicated lock path:

```text
weeklyCupCompetitionWalletLocks/{weekKey}/accounts/{userId}
```

The lock binds the owner, week, wallet address, and trusted schema/authority shape.

### Trusted operator validation

`scripts/firebase-admin/competition-wallet-lock.mjs`

The trusted parser rejects malformed or mismatched locks, including wrong owner, week, schema, authority, wallet, or required receipt evidence.

This means wallet-lock behavior is represented in:

- Android logic
- Firestore authorization
- trusted admin verification

rather than relying on one client-side flag.

---

## 5. Trusted Run Verification

Relevant files:

- `scripts/firebase-admin/competition-run-verification.mjs`
- `scripts/firebase-admin/verify-competition-run.mjs`
- `scripts/firebase-admin/competition-wallet-lock.mjs`
- `scripts/firebase-admin/radiant-cup-operator.mjs`

Trust flow:

```text
Android gameplay
    ↓
submitted competition evidence
    ↓
trusted verification/admin layer
    ↓
trusted competition result
```

Client-reported score data is not treated as direct payout authority.

---

## 6. Cup Configuration and Funding Verification

Relevant files:

- `scripts/firebase-admin/weekly-cup-config.mjs`
- `scripts/firebase-admin/manage-weekly-cup.mjs`
- `scripts/firebase-admin/skr-funding-verification.mjs`
- `scripts/firebase-admin/verify-weekly-cup-funding.mjs`
- `scripts/firebase-admin/verify-weekly-cup-funding.test.mjs`

The trusted payout lifecycle checks that funding verification is:

```text
VERIFIED
```

before later payout stages can proceed.

The trusted layer also validates SKR asset identity/mint/decimals rather than accepting arbitrary client metadata.

---

## 7. Trusted Finalization

Relevant files:

- `scripts/firebase-admin/weekly-cup-finalization.mjs`
- `scripts/firebase-admin/finalize-weekly-cup.mjs`
- `scripts/firebase-admin/finalize-weekly-cup.test.mjs`

Finalization creates a trusted result snapshot.

The Android client does not get to declare an authoritative winner/payout state on its own.

---

## 8. Payout Lifecycle

Relevant files:

- `scripts/firebase-admin/weekly-cup-payout-lifecycle.mjs`
- `scripts/firebase-admin/prepare-weekly-cup-payout.mjs`
- `scripts/firebase-admin/approve-weekly-cup-payout.mjs`
- `scripts/firebase-admin/complete-weekly-cup-payout.mjs`
- `scripts/firebase-admin/weekly-cup-payout-lifecycle.test.mjs`

The payout path validates items such as:

- trusted result reference
- VERIFIED funding at Cup close
- SKR identity
- competition-wallet lock evidence
- winner state
- payout manifest integrity
- payout status transitions

The lifecycle intentionally separates preparation, review, approval, and completion.

---

## 9. Real SKR Transfer / Reconciliation Tooling

Relevant files:

- `scripts/firebase-admin/weekly-cup-skr-transfer.mjs`
- `scripts/firebase-admin/prepare-weekly-cup-skr-transfer-intent.mjs`
- `scripts/firebase-admin/execute-weekly-cup-skr-transfer.mjs`
- `scripts/firebase-admin/reconcile-weekly-cup-skr-transfer.mjs`
- `scripts/firebase-admin/weekly-cup-skr-transfer.test.mjs`

The tooling includes support for:

- reviewed transfer intent preparation
- transaction signature recording
- Solana signature/transaction verification
- exact SKR transfer verification
- finalized transfer evidence
- reconciliation after uncertain/interrupted submission
- refusing a blind automatic resend when a prior transaction may already exist

This transfer authority is deliberately outside the Android APK.

---

## 10. Signer / Secret Boundary

The Android app does not contain the trusted payout signer.

Repository guidance prohibits placing seed phrases/private keys in:

- Git
- Android
- Firestore
- documentation
- chat
- shell arguments

Trusted Firebase Admin credentials are expected outside the repository.

This keeps payout authority out of ordinary mobile-client code.

---

## 11. What Judges Can Verify Quickly

| Evidence | Source |
|---|---|
| liquid + verified staked SKR eligibility | `logic/SkrPassportRules.kt` |
| SKR balance/stake reads | `solana/SkrBalanceRepository.kt` |
| competition wallet-lock contract | `logic/Phase12CompetitionWalletLockRules.kt` |
| Firestore wallet-lock enforcement | `firebase/firestore.rules` |
| trusted lock validation | `scripts/firebase-admin/competition-wallet-lock.mjs` |
| funding verification | `scripts/firebase-admin/skr-funding-verification.mjs` |
| trusted finalization | `scripts/firebase-admin/weekly-cup-finalization.mjs` |
| payout lifecycle | `scripts/firebase-admin/weekly-cup-payout-lifecycle.mjs` |
| transfer implementation | `scripts/firebase-admin/weekly-cup-skr-transfer.mjs` |
| execution | `scripts/firebase-admin/execute-weekly-cup-skr-transfer.mjs` |
| reconciliation | `scripts/firebase-admin/reconcile-weekly-cup-skr-transfer.mjs` |

---

## 12. Demo Evidence

For the short demo, show the visual parts:

1. connected wallet / SKR Passport state
2. Weekly Radiant Cup / competition surface
3. wallet-aware identity/eligibility where visible

Do not spend much of the short demo explaining backend payout internals; source evidence is stronger for those details.

---

## 13. Live SKR Payout Evidence

**Status in this document: NOT CLAIMED.**

If a real judge-safe sponsored SKR payout has actually been executed and verified, add:

```text
Week:
Placement:
Amount:
Transaction signature:
Explorer URL:
Trusted payout record:
```

If no live payout exists, keep this section unchanged and describe the current work accurately as **trusted payout/transfer infrastructure**.

Do not fabricate transaction evidence.

---

## 14. Claim Boundary

Radiant Circle currently claims:

- real SKR read/Passport logic
- wallet-aware competition logic
- account/week competition wallet lock
- trusted verification/funding/finalization/payout infrastructure
- real transfer/reconciliation tooling

Radiant Circle does **not** claim a completed live sponsored SKR payout unless a real transaction signature/explorer link is added here.
