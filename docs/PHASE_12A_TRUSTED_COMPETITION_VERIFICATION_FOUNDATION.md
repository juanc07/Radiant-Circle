# Phase 12A — Trusted Competition Verification Foundation

## Goal

Phase 12A adds the trust boundary required before a Sponsored SKR Cup can ever
pay real value. It does **not** add a payout system and it does **not** make an
Android-produced score trustworthy.

The existing Weekly / All-Time / XP flows remain prototype/client-reported and
continue to work independently of the new trusted receipt inbox.

## New receipt flow

A finished `RadiantRunResult` now receives a stable UUID receipt identity and a
client completion timestamp when the result object is created. Copies/retries of
that same result keep the same receipt id.

After the existing Ranked-run transaction succeeds, Android best-effort creates:

```text
/competitionRunSubmissions/{receiptId}
```

with the client-create-only state:

```text
verificationStatus       = UNVERIFIED
trustedPlacementEligible = false
payoutEligible           = false
payoutStatus              = NOT_ELIGIBLE
scoreAuthority            = client-reported-prototype-not-payout-authority
```

This receipt is **not** payout proof. The score, timing, wallet string, day key,
and week key all originate from client-visible state and must be independently
validated by trusted infrastructure before any future `VERIFIED` decision.

## Firestore trust boundary

Client rules for `/competitionRunSubmissions/{receiptId}`:

- authenticated client may create only an exact allow-listed schema,
- `ownerUid` must equal the authenticated Firebase UID,
- receipt/document id must be a bounded client receipt string and identical,
- wallet/day/week/score fields remain client assertions and are **not** treated as proof,
- mode must be `Ranked`,
- score counters must be non-negative integers,
- `submittedAt` must use Firestore server timestamp,
- verification must start as `UNVERIFIED`,
- trusted placement eligibility must be `false`,
- payout eligibility must be `false`,
- payout status must be `NOT_ELIGIBLE`,
- Android cannot update or delete the receipt.

A future trusted verifier must use Firebase Admin/server credentials. Admin SDK
writes are outside normal client security rules and can later add trusted fields
such as `VERIFIED` / `REJECTED`, `trustedScore`, `verifiedAt`, and a reason.

## Availability behavior

Receipt persistence is intentionally outside the existing game/progression
transaction. If the receipt write fails or trusted infrastructure is temporarily
unavailable:

- the Radiant Rush result still saves,
- Weekly / All-Time prototype boards still update,
- XP/reward/chest/profile/retention behavior is unchanged,
- the result screen reports the receipt failure instead of hiding it,
- the affected run simply has no trusted receipt to verify.

Retries reuse the same receipt id. If the immutable receipt already exists, the
client reads it back and treats that as an idempotent success rather than creating
a duplicate.

This prevents Phase 12A infrastructure from making the existing game unusable.

## No payout authority added

Phase 12A does not add:

- player wagering,
- an entry fee,
- a treasury/private key in Android,
- a fake SKR pool,
- a fake transaction confirmation,
- automatic winner selection,
- automatic token transfer,
- Android authority to mark a run verified or payout eligible.

Mainnet SKR remains read-only and existing signed/on-chain proof flows remain on
their intended networks.

## Files

Changed/new files for this phase:

```text
app/src/main/java/com/thinkblox/radiantrush/firebase/FirebaseRadiantRepository.kt
app/src/main/java/com/thinkblox/radiantrush/logic/RadiantGameRules.kt
app/src/main/java/com/thinkblox/radiantrush/logic/Phase12CompetitionVerificationRules.kt
app/src/main/java/com/thinkblox/radiantrush/ui/screens/LeaderboardScreen.kt
app/src/test/java/com/thinkblox/radiantrush/logic/Phase12CompetitionVerificationRulesTest.kt
firebase/firestore.rules
docs/PHASE_12A_TRUSTED_COMPETITION_VERIFICATION_FOUNDATION.md
```

## Required deployment order

Because the Android patch creates the new receipt collection, deploy the updated
Firestore rules before testing the new app build:

```powershell
npx.cmd firebase-tools deploy --only firestore:rules --project radiant-rush-10a9c
```

Then run the normal Android gate:

```powershell
.\gradlew.bat --stop
.\gradlew.bat :app:testDebugUnitTest
.\gradlew.bat :app:assembleDebug
.\gradlew.bat :app:connectedDebugAndroidTest
.\gradlew.bat :app:installDebug
```

## Manual Phase 12A QA

1. Connect a wallet.
2. Finish one Ranked Radiant Rush.
3. Confirm Weekly and All-Time boards still update.
4. Confirm XP/reward/capsule behavior still works.
5. Confirm the result message explicitly says the competition receipt was created
   as `UNVERIFIED` (or reports a concrete failure instead of silently succeeding).
6. In Firestore, confirm one `/competitionRunSubmissions/{receiptId}` document exists.
7. Confirm it is `UNVERIFIED`, `trustedPlacementEligible=false`,
   `payoutEligible=false`, and `payoutStatus=NOT_ELIGIBLE`.
8. From a normal client, attempts to update that receipt must fail with permission denied.
9. Retry the same completed result if available; it must not create a duplicate receipt.
10. Confirm Casual runs continue working and explicitly report that no trusted-Cup receipt is created.
11. Recheck wallet, SKR Passport/staking, Daily Radiant Chest, Profile, leaderboards,
   and retention surfaces for regressions.

## Next phase

Phase 12B should introduce trusted sponsor/Cup configuration only after this
boundary is tested. Phase 12A alone must never authorize a real SKR payout.
