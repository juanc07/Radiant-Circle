# Phase 12 Weekly Cup — End-to-End Operator Command Playbook

**Product:** Radiant Circle
**Game:** Radiant Rush
**Firebase project:** `radiant-rush-10a9c`
**Audience:** trusted operator only
**Scope:** start a sponsored Weekly Cup, verify funding and ranked runs, close/finalize trusted winners, prepare/approve a payout manifest, transfer SKR, reconcile finalized transactions, and mark the payout batch paid.

This playbook is the linear command sequence. The detailed trust model and failure cases remain in `PHASE_12_ADMIN_OPERATOR_RUNBOOK.md`.

## Non-negotiable safety rules

- Admin tools are dry-run by default. Use `--apply` only after reviewing the dry-run output.
- Keep Firebase Admin credentials outside the repository.
- Never place a Solana private key, seed phrase, Seed Vault secret, service-account JSON, or keystore in Git, Firestore, Android, chat, or this document.
- Player-created receipts begin `UNVERIFIED`. Do not mark them `VERIFIED` by copying Firestore values and pretending they are independent evidence.
- Funding must be truthfully verified on Solana Mainnet while the Cup is still `OPEN`.
- Finalization must use distinct full wallet addresses and must refuse to invent missing winners.
- Phase 12E approval does not send tokens.
- Phase 12F sends one placement at a time and requires finalized on-chain proof before marking that placement paid.
- If a transaction outcome is uncertain, reconcile it. Do not automatically resend.

## 0. Enter the Admin workspace and load temporary credentials

```bash
cd /c/2026/SolanaHackaton/RadiantRushPhase1Android/scripts/firebase-admin

CRED="/c/2026/firebase-secrets/YOUR-TEMPORARY-SERVICE-ACCOUNT.json"
export GOOGLE_APPLICATION_CREDENTIALS="$(cygpath -w "$CRED")"

ls -l "$CRED"
echo "$GOOGLE_APPLICATION_CREDENTIALS"
```

Run the Admin gate before live writes:

```bash
npm ci
npm test
```

## 1. Configure/open the Weekly Cup — Phase 12B

Dry-run first:

```bash
node manage-weekly-cup.mjs \
  --project radiant-rush-10a9c \
  --week 2026-WXX \
  --status OPEN \
  --sponsor "ThinkBloxPH" \
  --prize-skr 300 \
  --funding-wallet "PUBLIC_SPONSOR_WALLET" \
  --placements "1:50,2:30,3:20"
```

Review the exact week, prize, funding wallet, placements, start/end boundaries, trusted-results requirement, and `payoutEnabled=false`.

Apply only after review:

```bash
node manage-weekly-cup.mjs \
  --project radiant-rush-10a9c \
  --week 2026-WXX \
  --status OPEN \
  --sponsor "ThinkBloxPH" \
  --prize-skr 300 \
  --funding-wallet "PUBLIC_SPONSOR_WALLET" \
  --placements "1:50,2:30,3:20" \
  --apply \
  --confirm-project radiant-rush-10a9c
```

## 2. Fund and verify the sponsor wallet — Phase 12C

The sponsor wallet must contain at least the configured liquid transferable SKR amount. It also needs enough SOL for transaction fees and, when applicable, recipient token-account creation.

Funding verification dry-run:

```bash
node verify-weekly-cup-funding.mjs \
  --project radiant-rush-10a9c \
  --week 2026-WXX \
  --funding-wallet "PUBLIC_SPONSOR_WALLET"
```

Proceed only if the output truthfully says:

```text
Verification result: VERIFIED
```

Persist the funding evidence while the Cup is still `OPEN`:

```bash
node verify-weekly-cup-funding.mjs \
  --project radiant-rush-10a9c \
  --week 2026-WXX \
  --funding-wallet "PUBLIC_SPONSOR_WALLET" \
  --apply \
  --confirm-project radiant-rush-10a9c
```

Do not close the Cup if the persisted status is `NOT_VERIFIED` when a real payout proof is intended.

## 3. Inspect Ranked receipts

Read-only candidate listing:

```bash
node --input-type=module -e 'import { applicationDefault, initializeApp } from "firebase-admin/app"; import { getFirestore } from "firebase-admin/firestore"; initializeApp({credential:applicationDefault(),projectId:"radiant-rush-10a9c"}); const db=getFirestore(); const week="2026-WXX"; const snaps=await db.collection("competitionRunSubmissions").where("utcWeekKey","==",week).get(); const wallets=new Set(); console.log(`${week} receipts:`,snaps.size); for(const doc of snaps.docs){const d=doc.data(); if(d.walletAddress) wallets.add(d.walletAddress); console.log({receiptId:doc.id,walletAddress:d.walletAddress,ownerUid:d.ownerUid,score:d.score,maxCombo:d.maxCombo,perfectHits:d.perfectHits,radiantHits:d.radiantHits,corruptedHits:d.corruptedHits,clientCompletedAtEpochMillis:d.clientCompletedAtEpochMillis,verificationStatus:d.verificationStatus,trustedPlacementEligible:d.trustedPlacementEligible});} console.log("Distinct wallets:",wallets.size);'
```

For a three-place Cup, the trusted close needs at least three distinct full wallet addresses after verification and wallet dedupe.

## 4. Independently attest trusted Ranked runs — Phase 12D

For each independently reviewed run, dry-run first:

```bash
node verify-competition-run.mjs \
  --project radiant-rush-10a9c \
  --week 2026-WXX \
  --receipt "REAL_RECEIPT_ID" \
  --decision VERIFIED \
  --evidence-ref "INDEPENDENT_EVIDENCE_REFERENCE" \
  --wallet "FULL_PUBLIC_WINNER_WALLET" \
  --score REAL_SCORE \
  --max-combo REAL_MAX_COMBO \
  --perfect-hits REAL_PERFECT_HITS \
  --radiant-hits REAL_RADIANT_HITS \
  --corrupted-hits REAL_CORRUPTED_HITS \
  --completed-at-ms REAL_COMPLETION_EPOCH_MS
```

After reviewing the dry run, repeat the exact command with:

```bash
  --apply \
  --confirm-project radiant-rush-10a9c
```

Repeat for enough distinct trusted wallets to fill every configured placement.

## 5. Close and freeze the trusted ranking — Phase 12D

### Normal scheduled close

After the configured `endsAt`:

```bash
node finalize-weekly-cup.mjs \
  --project radiant-rush-10a9c \
  --week 2026-WXX
```

Review source/trusted counts, exact wallet dedupe, placement order, funding at close, exact atomic allocations, and both SHA-256 snapshot digests. Then apply:

```bash
node finalize-weekly-cup.mjs \
  --project radiant-rush-10a9c \
  --week 2026-WXX \
  --apply \
  --confirm-project radiant-rush-10a9c
```

### Exceptional operator-authorized early close

Use only when intentionally ending an `OPEN` Cup before `endsAt`.

Dry-run:

```bash
node finalize-weekly-cup.mjs \
  --project radiant-rush-10a9c \
  --week 2026-WXX \
  --force-close-early \
  --early-close-reason "Operator-authorized reason"
```

Review the exact winners and copy the exact `Ranking snapshot SHA-256`. Apply only after review:

```bash
node finalize-weekly-cup.mjs \
  --project radiant-rush-10a9c \
  --week 2026-WXX \
  --force-close-early \
  --early-close-reason "Operator-authorized reason" \
  --apply \
  --confirm-project radiant-rush-10a9c \
  --confirm-early-close 2026-WXX \
  --confirm-ranking-digest "EXACT_RANKING_SHA256_FROM_DRY_RUN"
```

A successful close writes the immutable result/winner snapshot and keeps payout disabled.

## 6. Exceptional pre-close prize/funding-wallet amendment

This path is optional and only valid while the Cup is still `OPEN`. It is used when a prize or sponsor wallet must be intentionally corrected before close. The command records an immutable amendment and resets old funding evidence, so Phase 12C must be run again afterward.

Dry-run:

```bash
node amend-weekly-cup-prize.mjs \
  --project radiant-rush-10a9c \
  --week 2026-WXX \
  --prize-skr 300 \
  --funding-wallet "NEW_PUBLIC_SPONSOR_WALLET" \
  --reason "Operator-authorized amendment reason"
```

Copy the exact `Amendment SHA-256`, then apply:

```bash
node amend-weekly-cup-prize.mjs \
  --project radiant-rush-10a9c \
  --week 2026-WXX \
  --prize-skr 300 \
  --funding-wallet "NEW_PUBLIC_SPONSOR_WALLET" \
  --reason "Operator-authorized amendment reason" \
  --apply \
  --confirm-project radiant-rush-10a9c \
  --confirm-week 2026-WXX \
  --confirm-amendment-digest "EXACT_AMENDMENT_SHA256_FROM_DRY_RUN"
```

Immediately repeat Phase 12C funding verification. Do not close until funding is freshly `VERIFIED`.

## 7. Prepare the payout manifest — Phase 12E

Dry-run:

```bash
node prepare-weekly-cup-payout.mjs \
  --project radiant-rush-10a9c \
  --week 2026-WXX
```

Review:

- `Funding at close: VERIFIED`
- exact sponsor wallet
- exact winner wallets
- exact atomic allocations
- total manifest amount
- payout manifest SHA-256
- `Transfer enabled: false`
- `SKR transfer attempted: NO`

Apply:

```bash
node prepare-weekly-cup-payout.mjs \
  --project radiant-rush-10a9c \
  --week 2026-WXX \
  --apply \
  --confirm-project radiant-rush-10a9c
```

The payout batch becomes `READY_FOR_REVIEW`. No token transfer occurs.

## 8. Review and approve the exact payout manifest — Phase 12E

Dry-run:

```bash
node approve-weekly-cup-payout.mjs \
  --project radiant-rush-10a9c \
  --week 2026-WXX \
  --review-ref "operator-review-2026-WXX"
```

Copy the exact payout manifest SHA-256 and verify every full wallet and amount.

Apply:

```bash
node approve-weekly-cup-payout.mjs \
  --project radiant-rush-10a9c \
  --week 2026-WXX \
  --review-ref "operator-review-2026-WXX" \
  --confirm-digest "EXACT_PAYOUT_MANIFEST_SHA256" \
  --apply \
  --confirm-project radiant-rush-10a9c
```

The batch becomes `APPROVED`. This still signs nothing and sends no SKR.

## 9. Pay each placement — Phase 12F preferred external-wallet / Seed Vault path

Use this path when the sponsor funds are held by Seed Vault, a hardware wallet, or another wallet whose private key must never be exported.

### 9.1 Placement #1 — create intent

Dry-run:

```bash
node prepare-weekly-cup-skr-transfer-intent.mjs \
  --project radiant-rush-10a9c \
  --week 2026-WXX \
  --placement 1
```

Review the exact funding wallet, recipient wallet, amount, official SKR mint, current liquid funding, and manifest digest.

Apply the intent with exact confirmations from the dry-run output:

```bash
node prepare-weekly-cup-skr-transfer-intent.mjs \
  --project radiant-rush-10a9c \
  --week 2026-WXX \
  --placement 1 \
  --confirm-digest "EXACT_PAYOUT_MANIFEST_SHA256" \
  --confirm-funding-wallet "PUBLIC_SPONSOR_WALLET" \
  --confirm-recipient "FULL_WINNER_1_WALLET" \
  --confirm-amount-atomic "WINNER_1_ATOMIC_AMOUNT" \
  --apply \
  --confirm-project radiant-rush-10a9c
```

The item is now locked in `AWAITING_EXTERNAL_SIGNATURE`. Do not create another transfer for that placement.

### 9.2 Send the exact transfer from the sponsor wallet

Using the configured sponsor wallet itself, send exactly the reviewed SKR amount to the exact winner wallet using the official SKR mint. Capture the resulting Solana transaction signature.

Do not paste or export any seed phrase/private key into the Admin tool.

### 9.3 Reconcile the real transaction signature

Dry-run first:

```bash
node reconcile-weekly-cup-skr-transfer.mjs \
  --project radiant-rush-10a9c \
  --week 2026-WXX \
  --placement 1 \
  --signature "REAL_SOLANA_TRANSACTION_SIGNATURE"
```

Proceed only if the tool verifies the transaction is finalized and the exact SKR debit/credit matches the approved manifest.

Apply:

```bash
node reconcile-weekly-cup-skr-transfer.mjs \
  --project radiant-rush-10a9c \
  --week 2026-WXX \
  --placement 1 \
  --signature "REAL_SOLANA_TRANSACTION_SIGNATURE" \
  --apply \
  --confirm-project radiant-rush-10a9c
```

Placement #1 becomes `PAID` / `FINALIZED` only after exact finalized on-chain evidence.

### 9.4 Repeat sequentially for #2 and #3

Repeat the same dry-run → intent apply → real wallet send → reconciliation dry-run → reconciliation apply flow for placement `2`, then placement `3`.

The transfer plan enforces placement order. Do not batch or skip placements.

## 10. Optional trusted local CLI signer path

Only use this when the sponsor wallet is intentionally backed by a dedicated local operational keypair outside the repository. Never export Seed Vault merely to use this path.

```bash
export RADIANT_PAYOUT_SIGNER_KEYPAIR="C:\\secure\\sponsor-keypair.json"
```

Dry-run one placement:

```bash
node execute-weekly-cup-skr-transfer.mjs \
  --project radiant-rush-10a9c \
  --week 2026-WXX \
  --placement 1
```

Apply requires the exact dry-run manifest digest, funding wallet, recipient wallet, atomic amount, and project confirmation:

```bash
node execute-weekly-cup-skr-transfer.mjs \
  --project radiant-rush-10a9c \
  --week 2026-WXX \
  --placement 1 \
  --confirm-digest "EXACT_PAYOUT_MANIFEST_SHA256" \
  --confirm-funding-wallet "PUBLIC_SPONSOR_WALLET" \
  --confirm-recipient "FULL_WINNER_WALLET" \
  --confirm-amount-atomic "ATOMIC_AMOUNT" \
  --apply \
  --confirm-project radiant-rush-10a9c
```

The tool derives the keypair public key and refuses to sign unless it exactly equals the configured sponsor wallet.

## 11. Uncertain or failed transfer handling

If a transaction may have been submitted but the command timed out or the outcome is uncertain, do not resend. Reconcile the original signature:

```bash
node reconcile-weekly-cup-skr-transfer.mjs \
  --project radiant-rush-10a9c \
  --week 2026-WXX \
  --placement N \
  --signature "ORIGINAL_SIGNATURE"
```

If the finalized transaction failed atomically, the tool will refuse automatic retry. Review the same signature and then explicitly reset only when the tool says it is safely retryable:

```bash
node reconcile-weekly-cup-skr-transfer.mjs \
  --project radiant-rush-10a9c \
  --week 2026-WXX \
  --placement N \
  --signature "FAILED_SIGNATURE" \
  --reset-failed
```

Apply the reset only after review:

```bash
node reconcile-weekly-cup-skr-transfer.mjs \
  --project radiant-rush-10a9c \
  --week 2026-WXX \
  --placement N \
  --signature "FAILED_SIGNATURE" \
  --reset-failed \
  --apply \
  --confirm-project radiant-rush-10a9c
```

## 12. Complete the payout batch after every winner is PAID

Dry-run:

```bash
node complete-weekly-cup-payout.mjs \
  --project radiant-rush-10a9c \
  --week 2026-WXX
```

Review the manifest digest, winner count, and every finalized transaction signature.

Apply:

```bash
node complete-weekly-cup-payout.mjs \
  --project radiant-rush-10a9c \
  --week 2026-WXX \
  --confirm-digest "EXACT_PAYOUT_MANIFEST_SHA256" \
  --apply \
  --confirm-project radiant-rush-10a9c
```

Expected final batch state:

```text
weeklyCupPayouts/{weekKey}.status = PAID
all payout items = PAID / FINALIZED
all transaction signatures unique and finalized
```

## 13. End the trusted Admin session

```bash
unset GOOGLE_APPLICATION_CREDENTIALS
```

Then revoke/delete the temporary Firebase service-account key used for the session.

---

# W38 live proof snapshot — 2026-09-14

This section records the controlled Phase 12 live test performed on `2026-W38` so the exact trust chain can be reproduced and audited.

## Cup amendment/funding

- Original configured prize: `1000 SKR`
- Audited amended prize: `300 SKR`
- Funding wallet: `EqLUDQpfZrCJcQ5obVWzjuBn1v4sHH8PxVfcCg4rppzj`
- Observed liquid transferable funding before close: `399.926445 SKR`
- Funding verification: `VERIFIED`
- Placement split: `50 / 30 / 20`

Expected 300 SKR allocation:

```text
#1 = 150000000 atomic = 150 SKR
#2 =  90000000 atomic =  90 SKR
#3 =  60000000 atomic =  60 SKR
```

## Trusted final winners

```text
#1 J86vtTs7twTUuS4xfo8H8zaUeFyMNHWPPeEXyL5DscPg  score=4310  150 SKR
#2 HbyQrE2N1V8TPs5HJ9wGDq3M85Zm1i21RmgbLFk39xkS  score=4215   90 SKR
#3 21jdTFKL5LS41dPRUZsZzFkceE7fBXvSCSwe6kDUk5ey  score=3735   60 SKR
```

Trusted finalization proof:

```text
Eligible receipt snapshot SHA-256:
3ec77e41fac067ad90e19874f27fabfa30c4a24a77f743c7486b9073dd951882

Ranking snapshot SHA-256:
bd0ff48ff5b4e9e4fd866c177a5544a90ec11eb471f5644283bfd5d4f9c322f5
```

W38 was early-closed through the explicit audited override. The original scheduled `endsAt` was preserved and the actual `competitionEndedAt` cutoff was recorded.

## Phase 12E manifest proof

Prepared payout manifest:

```text
Total: 300000000 atomic SKR
#1: 150000000
#2:  90000000
#3:  60000000

Payout manifest SHA-256:
d2a5cfeabcb83311af32ec68dd0460e2e345afb273a6394e2e61d72af0a9aea4
```

The prepare apply completed and created `weeklyCupPayouts/2026-W38` in `READY_FOR_REVIEW`.

The approval dry-run was reviewed with:

```text
review-ref: operator-review-w38-live-payout
resulting lifecycle status: APPROVED
transfer enabled: false
SKR transfer attempted: NO
```

At the time this snapshot was written, the approval **apply** and Phase 12F real transfers had not yet been executed. Continue from Step 8 above using the exact manifest digest shown here, then execute/reconcile placements sequentially.
