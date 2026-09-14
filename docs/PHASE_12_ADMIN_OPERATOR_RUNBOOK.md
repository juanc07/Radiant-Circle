# Phase 12 Admin Operator Runbook

**Project:** Radiant Circle
**Game:** Radiant Rush
**Firebase project:** `radiant-rush-10a9c`
**Purpose:** trusted Weekly Radiant Cup configuration, funding verification, run attestation, and season finalization.

This document is an operator guide for the Phase 12 Firebase Admin tools. It is not player-facing documentation.

## 1. Safety rules

- Firebase Admin tools are **dry-run by default** unless a command explicitly includes `--apply`.
- Every destructive/trusted write requires exact `--confirm-project radiant-rush-10a9c` confirmation.
- Keep Firebase service-account JSON outside the repository.
- Never put a private key, treasury key, service-account JSON, or `app/google-services.json` in a patch/commit.
- Android remains read-only for trusted Cup configuration, run verification, funding-check receipts, and final results.
- `payoutEnabled` remains `false` throughout Phase 12D.
- Phase 12D never transfers SKR.
- A client-created run receipt is not trusted merely because it exists in Firestore.
- Do not copy client receipt values and call them independent evidence. `VERIFIED` requires a separate evidence review.
- Never weaken a three-place Cup merely to force a finalization. If there are fewer than three trusted eligible wallets, finalization should fail closed.

## 2. Working directory and temporary credentials

From Git Bash:

```bash
cd /c/2026/SolanaHackaton/RadiantRushPhase1Android/scripts/firebase-admin

CRED="/c/2026/firebase-secrets/YOUR-TEMPORARY-SERVICE-ACCOUNT.json"
export GOOGLE_APPLICATION_CREDENTIALS="$(cygpath -w "$CRED")"
```

The credential file must remain outside the repository. Revoke the temporary key and delete the local JSON after the live Admin session.

## 3. Admin source test gate

Run before live writes:

```bash
cd /c/2026/SolanaHackaton/RadiantRushPhase1Android/scripts/firebase-admin
npm ci
npm test
```

Phase 12D acceptance currently expects the complete Firebase Admin suite to pass before any commit/tag.

Do not blindly run `npm audit fix` during a release proof. Dependency upgrades should be reviewed as a separate source change.

## 4. Android/source gate

From repository root:

```bash
cd /c/2026/SolanaHackaton/RadiantRushPhase1Android

./gradlew --stop
./gradlew :app:testDebugUnitTest
./gradlew :app:assembleDebug
./gradlew :app:connectedDebugAndroidTest
./gradlew :app:installDebug
```

If a connected-device smoke test times out once because the Activity/device was still settling, rerun `:app:connectedDebugAndroidTest`. Record both the initial failure and the successful rerun; do not hide the first result.

## 5. Deploy Firestore rules

Only when the checked source includes intentional rule changes:

```bash
cd /c/2026/SolanaHackaton/RadiantRushPhase1Android

npx.cmd firebase-tools deploy \
  --only firestore:rules \
  --project radiant-rush-10a9c
```

Successful deployment must report that the rules compiled and were released.

## 6. Phase 12B — configure/open a Weekly Cup

`manage-weekly-cup.mjs` is dry-run by default.

Example dry run:

```bash
cd /c/2026/SolanaHackaton/RadiantRushPhase1Android/scripts/firebase-admin

node manage-weekly-cup.mjs \
  --project radiant-rush-10a9c \
  --week 2026-W37 \
  --status OPEN \
  --sponsor "ThinkBloxPH" \
  --prize-skr 1000 \
  --funding-wallet "PUBLIC_SOLANA_FUNDING_WALLET" \
  --placements "1:50,2:30,3:20"
```

Review the proposed document carefully. In particular, verify:

- correct `weekKey` and status,
- exact SKR prize amount,
- exact public funding wallet,
- expected placement split,
- `trustedResultsRequired=true`,
- `payoutEnabled=false`,
- existing Phase 12C funding evidence is shown as preserved when applicable,
- existing `sponsorNote` is not accidentally replaced.

If the existing Cup has a non-empty sponsor note, pass the exact note using `--note "..."` on both dry-run and apply.

Apply only after review:

```bash
node manage-weekly-cup.mjs \
  --project radiant-rush-10a9c \
  --week 2026-W37 \
  --status OPEN \
  --sponsor "ThinkBloxPH" \
  --prize-skr 1000 \
  --funding-wallet "PUBLIC_SOLANA_FUNDING_WALLET" \
  --placements "1:50,2:30,3:20" \
  --apply \
  --confirm-project radiant-rush-10a9c
```

## 7. Read current Cup state safely

Some Git Bash/terminal combinations can print `stdin is not a tty` for a heredoc. The one-line `node -e` form below avoids that issue.

```bash
node --input-type=module -e 'import { applicationDefault, initializeApp } from "firebase-admin/app"; import { getFirestore } from "firebase-admin/firestore"; initializeApp({credential:applicationDefault(),projectId:"radiant-rush-10a9c"}); const snap=await getFirestore().collection("weeklyCupConfigs").doc("2026-W37").get(); if(!snap.exists) throw new Error("W37 config missing"); const d=snap.data(); console.log({status:d.status,sponsorName:d.sponsorName,sponsorNote:d.sponsorNote??null,prizeAmountAtomic:d.prizeAmountAtomic,fundingWalletAddress:d.fundingWalletAddress,fundingVerificationStatus:d.fundingVerificationStatus,payoutEnabled:d.payoutEnabled,finalizationStatus:d.finalizationStatus??null,trustedResultRef:d.trustedResultRef??null,startsAt:d.startsAt,endsAt:d.endsAt});'
```

This command is read-only.

## 8. List candidate competition receipts

Use this to inspect the source set for a week. It does **not** verify any run.

```bash
node --input-type=module -e 'import { applicationDefault, initializeApp } from "firebase-admin/app"; import { getFirestore } from "firebase-admin/firestore"; initializeApp({credential:applicationDefault(),projectId:"radiant-rush-10a9c"}); const db=getFirestore(); const snaps=await db.collection("competitionRunSubmissions").where("utcWeekKey","==","2026-W37").get(); const wallets=new Set(); console.log("W37 receipts:",snaps.size); for(const doc of snaps.docs){const d=doc.data(); if(d.walletAddress) wallets.add(d.walletAddress); console.log({receiptId:doc.id,walletAddress:d.walletAddress,ownerUid:d.ownerUid,score:d.score,maxCombo:d.maxCombo,perfectHits:d.perfectHits,radiantHits:d.radiantHits,corruptedHits:d.corruptedHits,clientCompletedAtEpochMillis:d.clientCompletedAtEpochMillis,verificationStatus:d.verificationStatus,trustedPlacementEligible:d.trustedPlacementEligible});} console.log("Distinct wallets:",wallets.size);'
```

Important interpretation:

- `UNVERIFIED` + `trustedPlacementEligible=false` is the expected Phase 12A client-created state.
- Multiple receipts from one wallet are allowed.
- Final ranking later keeps only the best trusted-eligible receipt per exact wallet.
- The receipt query is candidate discovery only; it is not independent evidence.

## 9. Phase 12C — verify sponsor funding

The funding verifier reads Solana Mainnet liquid SKR from the configured public wallet. No private key is required and no transfer occurs.

Dry-run first:

```bash
node verify-weekly-cup-funding.mjs \
  --project radiant-rush-10a9c \
  --week 2026-W37
```

Apply only after reviewing the RPC result and exact atomic amounts:

```bash
node verify-weekly-cup-funding.mjs \
  --project radiant-rush-10a9c \
  --week 2026-W37 \
  --apply \
  --confirm-project radiant-rush-10a9c
```

`NOT_VERIFIED` is a valid truthful result when liquid transferable SKR is below the configured prize requirement. Do not convert it into `VERIFIED` manually.

## 10. Phase 12D — attest an independently verified run

`verify-competition-run.mjs` is an explicit Admin-only evidence attestation tool. It does not automatically trust Firestore score values.

For a run that has been independently reviewed, dry-run:

```bash
node verify-competition-run.mjs \
  --project radiant-rush-10a9c \
  --week 2026-W37 \
  --receipt "REAL_RECEIPT_ID" \
  --decision VERIFIED \
  --evidence-ref "INDEPENDENT_EVIDENCE_REFERENCE" \
  --wallet "REAL_PUBLIC_WALLET" \
  --score REAL_SCORE \
  --max-combo REAL_MAX_COMBO \
  --perfect-hits REAL_PERFECT_HITS \
  --radiant-hits REAL_RADIANT_HITS \
  --corrupted-hits REAL_CORRUPTED_HITS \
  --completed-at-ms REAL_COMPLETION_EPOCH_MS
```

Every supplied fact must exactly match the immutable receipt. The evidence values must come from the independent review, not from copying the Firestore receipt into the command.

After reviewing the dry run, repeat the exact command with:

```bash
  --apply \
  --confirm-project radiant-rush-10a9c
```

A successful VERIFIED apply creates/updates the trusted receipt state and creates the matching immutable Admin-only:

```text
competitionRunVerifications/{receiptId}
```

Finalization requires both records to agree.

### Reject a run

For independently reviewed evidence showing a run should not be trusted:

```bash
node verify-competition-run.mjs \
  --project radiant-rush-10a9c \
  --week 2026-W37 \
  --receipt "REAL_RECEIPT_ID" \
  --decision REJECTED \
  --evidence-ref "INDEPENDENT_EVIDENCE_REFERENCE" \
  --reason "SHORT_OPERATOR_REASON"
```

Dry-run first, then repeat with `--apply --confirm-project radiant-rush-10a9c` only after review.

## 11. Phase 12D — dry-run season finalization

Finalization is allowed only after the configured `endsAt` and while the Cup is still `OPEN`.

```bash
node finalize-weekly-cup.mjs \
  --project radiant-rush-10a9c \
  --week 2026-W37
```

Review:

- source receipt count,
- trusted eligible receipt count,
- distinct trusted eligible wallet count,
- exact-wallet dedupe result,
- deterministic tiebreak ordering,
- #1/#2/#3 wallet and source receipt,
- exact atomic SKR allocation,
- funding state at close,
- `payoutEnabled=false`,
- `payoutReady=false`,
- eligible-receipt and ranking SHA-256 digests.

If a three-place Cup has fewer than three trusted eligible wallets, the command must refuse to invent winners. That refusal is the expected fail-closed behavior.

## 12. Apply finalization once

Only after the dry run is correct:

```bash
node finalize-weekly-cup.mjs \
  --project radiant-rush-10a9c \
  --week 2026-W37 \
  --apply \
  --confirm-project radiant-rush-10a9c
```

A successful apply creates:

```text
weeklyCupResults/{weekKey}
weeklyCupResults/{weekKey}/eligibleReceipts/{receiptId}
weeklyCupResults/{weekKey}/winners/{placement}
```

and marks the trusted Cup CLOSED. It does not enable payout and does not transfer SKR.

## 13. Prove duplicate finalization is rejected

After one successful close:

```bash
if node finalize-weekly-cup.mjs \
  --project radiant-rush-10a9c \
  --week 2026-W37
then
  echo "ERROR: duplicate finalization unexpectedly succeeded"
  exit 1
else
  echo "PASS: duplicate finalization was refused"
fi
```

## 14. Post-close Firestore proof

Use read-only Admin inspection to confirm:

- Cup status is `CLOSED`,
- result status is `FINALIZED`,
- finalization authority is Phase 12D,
- winner count equals configured placements,
- each exact prize allocation is correct,
- funding truth at close is preserved,
- `payoutEnabled=false`,
- `payoutReady=false`,
- each winner has `payoutStatus=NOT_ENABLED`,
- frozen eligible receipt count matches the result summary.

For W37's 1000 SKR / 50-30-20 configuration, expected atomic allocation is:

```text
#1 500000000
#2 300000000
#3 200000000
```

## 15. Current W37 development proof checkpoint

As observed during Phase 12D live testing on 2026-09-14:

- Cup: `2026-W37`
- status was moved to `OPEN` through the Phase 12B Admin tool,
- prize: 1000 SKR,
- funding state: `NOT_VERIFIED`, preserved from the real Phase 12C check,
- payout remains disabled,
- source receipts: 5,
- distinct source wallets: 2,
- all five source receipts were still `UNVERIFIED` / `trustedPlacementEligible=false`,
- configured placements: 3.

Therefore W37 cannot truthfully produce three trusted winners from the current receipt set. The Phase 12D finalization dry run was executed after the configured end boundary and correctly failed closed with:

```text
FATAL: Error: No trusted-placement-eligible VERIFIED receipts exist for this Cup. Refusing to invent winners.
```

This refusal is the expected live security proof: no result/winner snapshot was written, no client receipt was promoted, and no payout state was enabled. Do not fabricate a third wallet, reduce trust requirements, or manually promote receipts.

For a successful three-winner proof, use a later development Cup with at least three controlled participants/distinct wallets and independently captured run evidence. The next active ISO Cup after W37 is `2026-W38`; keep its full weekly lifecycle intact rather than backdating or fabricating receipts.

## 16. End the Admin session

When finished:

```bash
unset GOOGLE_APPLICATION_CREDENTIALS
```

Then:

1. revoke the temporary service-account key in Firebase / Google Cloud,
2. delete the local JSON credential,
3. confirm it was never copied into the repository.

## 17. Before commit/tag

Do not checkpoint Phase 12D until the intended live proof is complete.

At minimum retain evidence for:

- Firebase Admin test suite,
- Android unit/build/device/install gate,
- Firestore rule deployment,
- dry-run/apply Admin operations,
- fail-closed behavior for untrusted/insufficient receipts,
- successful trusted finalization on a valid controlled Cup if the phase is being declared fully live-tested,
- duplicate-finalization rejection,
- Firestore frozen result inspection,
- Seeker UI verification (`Live standings`, `Final winners`, clean funding copy, no payout-ready claim).

---

## Phase 12E — trusted payout lifecycle

Phase 12E starts only after a Cup has a successful immutable Phase 12D final result **and** funding was VERIFIED before the Cup closed.

### Prepare payout manifest

Dry run:

```bash
node prepare-weekly-cup-payout.mjs \
  --project radiant-rush-10a9c \
  --week 2026-W38
```

Review:

- full funding wallet,
- full winner wallets,
- exact atomic SKR amounts,
- total atomic SKR,
- payout manifest SHA-256,
- `Transfer enabled: false`,
- `SKR transfer attempted: NO`.

Apply only when exact:

```bash
node prepare-weekly-cup-payout.mjs \
  --project radiant-rush-10a9c \
  --week 2026-W38 \
  --apply \
  --confirm-project radiant-rush-10a9c
```

A second prepare must be refused.

### Approve reviewed manifest

Dry run:

```bash
node approve-weekly-cup-payout.mjs \
  --project radiant-rush-10a9c \
  --week 2026-W38 \
  --review-ref "operator-review-w38"
```

`--review-ref` is a non-secret operator/audit reference. Do not put private keys, seed phrases, service-account contents, or confidential material in it.

For apply, copy the exact manifest digest printed by the dry run:

```bash
node approve-weekly-cup-payout.mjs \
  --project radiant-rush-10a9c \
  --week 2026-W38 \
  --review-ref "operator-review-w38" \
  --confirm-digest "<EXACT_SHA256>" \
  --apply \
  --confirm-project radiant-rush-10a9c
```

Approval keeps:

```text
payoutEnabled = false
transferEnabled = false
transferStatus = NOT_STARTED
```

It creates no transaction and moves no SKR. Real transfer belongs to Phase 12F only after another explicit safety review.
