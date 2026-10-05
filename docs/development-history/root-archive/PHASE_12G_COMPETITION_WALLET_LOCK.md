# Phase 12G — Competition Wallet Lock

## Goal

Close the controlled-test production gap discovered during the W38 live payout proof:

```text
one Radiant Circle account
→ one competition wallet per Weekly Cup
```

Firebase UID remains the Radiant Circle account/social identity. Wallet switching remains allowed for normal Solana usage; the lock applies only to trusted Weekly Cup competition for a specific `weekKey`.

## Authoritative lock

The first successfully persisted Ranked receipt for a Firebase UID/week atomically creates:

```text
weeklyCupCompetitionWalletLocks/{weekKey}/accounts/{firebaseUid}
```

Fields:

```text
schemaVersion
weekKey
ownerUid
walletAddress
firstReceiptId
lockAuthority
lockedAt
```

Firestore Security Rules enforce the create contract with `getAfter()` / `existsAfter()` and deny client update/delete. Every later client competition receipt for the same account/week must match that immutable wallet.

The normal Android run-save path also reads the existing lock. If the same account has switched to another wallet, the completed run is downgraded to **Casual** before Weekly/All-Time Ranked boards or ranked-attempt counters are changed. This is UX/defense-in-depth only; Firestore + Admin validation remain authoritative against modified clients.

## Defense in depth

Phase 12G does not trust the Android lock alone. The Admin trust path independently checks the actual lock document at each critical boundary:

1. `verify-competition-run.mjs` — VERIFIED promotion requires the receipt wallet to match the account/week lock.
2. `finalize-weekly-cup.mjs` — only verified receipts with matching immutable lock evidence are eligible.
3. `prepare-weekly-cup-payout.mjs` — every frozen winner must still match the actual lock.
4. `weekly-cup-skr-transfer.mjs` — Phase 12G payout manifests bind owner UID and lock evidence into the manifest digest.

This does not claim to eliminate all Sybil behavior across separately created Firebase accounts. It specifically closes the known same-account/multi-wallet prize-entry gap.

## Compatibility

- Do not modify the already-paid W38 history.
- New Phase 12G trusted verifications use verification schema v2.
- Historical Phase 12F payout batches without `competitionWalletLockRequired=true` continue to use the legacy manifest digest, preserving W38 reconciliation/auditability.
- Deploy the Phase 12G Android build and Firestore rules before opening the next public prize Cup. Older clients cannot create a valid competition receipt under the new rules because they do not atomically create the account/week lock.

## Required proof before commit/tag

Automated:

```bash
cd scripts/firebase-admin
npm test
cd ../..

./gradlew --stop
./gradlew :app:testDebugUnitTest
./gradlew :app:assembleDebug
./gradlew :app:connectedDebugAndroidTest
./gradlew :app:installDebug
```

Rules:

```bash
npx firebase-tools deploy --only firestore:rules --project radiant-rush-10a9c
```

Live proof must use a future/dev OPEN Cup, never W38:

1. Same Firebase/Google-backed Radiant Circle account connects Wallet A.
2. Complete a Ranked run; the receipt and immutable Cup lock are created.
3. Switch that same account to Wallet B.
4. Complete another Ranked run for the same Cup.
5. Receipt persistence must fail closed with the Cup-wallet-lock message.
6. Trusted verification with Wallet B must also fail closed.
7. Wallet A receipts remain eligible for ordinary verification/finalization if all existing Phase 12 checks pass.
