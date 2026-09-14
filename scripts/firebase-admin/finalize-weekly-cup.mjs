#!/usr/bin/env node
import { applicationDefault, initializeApp } from "firebase-admin/app";
import { FieldValue, Timestamp, getFirestore } from "firebase-admin/firestore";
import {
  RESULT_FINALIZATION_AUTHORITY,
  RESULT_SCHEMA_VERSION,
  RESULT_VERSION,
  WINNER_PAYOUT_STATUS,
  buildFinalizationPlan,
} from "./weekly-cup-finalization.mjs";

function readArgs(argv) {
  const values = {};
  const flags = new Set();
  for (let i = 0; i < argv.length; i += 1) {
    const token = argv[i];
    if (!token.startsWith("--")) throw new Error(`Unexpected argument: ${token}`);
    const key = token.slice(2);
    if (key === "apply") { flags.add(key); continue; }
    const value = argv[i + 1];
    if (!value || value.startsWith("--")) throw new Error(`Missing value for --${key}`);
    values[key] = value;
    i += 1;
  }
  return { values, flags };
}

function usage() {
  return `
Radiant Circle Phase 12D trusted Weekly Cup close + winners

Dry run:
  node finalize-weekly-cup.mjs \\
    --project radiant-rush-10a9c \\
    --week 2026-W37

Apply only after every intended winning run has been independently attested with
verify-competition-run.mjs and after the configured Cup end time:
  node finalize-weekly-cup.mjs \\
    --project radiant-rush-10a9c \\
    --week 2026-W37 \\
    --apply \\
    --confirm-project radiant-rush-10a9c

Funding may be NOT_VERIFIED; competitive results can still freeze, but this tool
never marks payout ready and never transfers SKR.
`;
}

async function loadPlan(db, weekKey, nowEpochMillis) {
  const cupRef = db.collection("weeklyCupConfigs").doc(weekKey);
  const [cupSnap, receiptQuery, verificationQuery, existingResult] = await Promise.all([
    cupRef.get(),
    db.collection("competitionRunSubmissions").where("utcWeekKey", "==", weekKey).get(),
    db.collection("competitionRunVerifications").where("weekKey", "==", weekKey).get(),
    db.collection("weeklyCupResults").doc(weekKey).get(),
  ]);
  if (existingResult.exists) throw new Error(`weeklyCupResults/${weekKey} already exists; finalization is immutable.`);
  const plan = buildFinalizationPlan({
    cup: cupSnap.exists ? cupSnap.data() : null,
    weekKey,
    receiptDocuments: receiptQuery.docs.map((doc) => ({ id: doc.id, data: doc.data() })),
    verificationDocuments: verificationQuery.docs.map((doc) => ({ id: doc.id, data: doc.data() })),
    nowEpochMillis,
  });
  return { cupRef, plan };
}

async function main() {
  const { values, flags } = readArgs(process.argv.slice(2));
  const projectId = values.project;
  const weekKey = values.week;
  if (!projectId || !weekKey) throw new Error(`--project and --week are required.\n${usage()}`);
  const apply = flags.has("apply");
  if (apply && values["confirm-project"] !== projectId) {
    throw new Error("--apply requires --confirm-project to exactly match --project.");
  }

  initializeApp({ credential: applicationDefault(), projectId });
  const db = getFirestore();
  const cutoffMs = Date.now();
  const { cupRef, plan } = await loadPlan(db, weekKey, cutoffMs);

  console.log("Radiant Circle Phase 12D trusted Weekly Cup finalization");
  console.log(`Project: ${projectId}`);
  console.log(`Mode: ${apply ? "APPLY" : "DRY RUN"}`);
  console.log(`Cup: ${weekKey}`);
  console.log(`Source receipts: ${plan.sourceReceiptCount}`);
  console.log(`Trusted eligible receipts: ${plan.eligibleReceiptCount}`);
  console.log(`Eligible wallets after best-result dedupe: ${plan.eligibleWalletCount}`);
  console.log(`Funding at close: ${plan.fundingVerificationStatusAtClose}`);
  console.log("Payout ready: false");
  console.log("Winners:");
  for (const winner of plan.winners) {
    console.log(`  #${winner.placement} ${winner.walletAddress} score=${winner.score} prizeAtomic=${winner.prizeAmountAtomic}`);
  }
  console.log(`Eligible receipt snapshot SHA-256: ${plan.eligibleReceiptDigestSha256}`);
  console.log(`Ranking snapshot SHA-256: ${plan.rankingDigestSha256}`);

  if (!apply) {
    console.log("\nDRY RUN ONLY — no Cup, result, winner, or snapshot document was written.");
    return;
  }

  const resultRef = db.collection("weeklyCupResults").doc(weekKey);
  const receiptQueryRef = db.collection("competitionRunSubmissions").where("utcWeekKey", "==", weekKey);
  const verificationQueryRef = db.collection("competitionRunVerifications").where("weekKey", "==", weekKey);
  await db.runTransaction(async (transaction) => {
    // Re-read every authoritative finalization input inside the same transaction
    // before any writes. If anything changed after the displayed plan, fail closed.
    const freshCupSnap = await transaction.get(cupRef);
    const freshResultSnap = await transaction.get(resultRef);
    const freshReceiptQuery = await transaction.get(receiptQueryRef);
    const freshVerificationQuery = await transaction.get(verificationQueryRef);
    if (freshResultSnap.exists) throw new Error("Result appeared during finalization; refusing duplicate close.");

    const freshPlan = buildFinalizationPlan({
      cup: freshCupSnap.exists ? freshCupSnap.data() : null,
      weekKey,
      receiptDocuments: freshReceiptQuery.docs.map((doc) => ({ id: doc.id, data: doc.data() })),
      verificationDocuments: freshVerificationQuery.docs.map((doc) => ({ id: doc.id, data: doc.data() })),
      nowEpochMillis: cutoffMs,
    });
    if (
      freshPlan.eligibleReceiptDigestSha256 !== plan.eligibleReceiptDigestSha256 ||
      freshPlan.rankingDigestSha256 !== plan.rankingDigestSha256
    ) {
      throw new Error("Trusted competition inputs changed during finalization; rerun and review the dry-run plan.");
    }

    transaction.create(resultRef, {
      ...freshPlan.result,
      finalizationCutoffAt: Timestamp.fromMillis(cutoffMs),
      finalizedAt: FieldValue.serverTimestamp(),
      closedAt: FieldValue.serverTimestamp(),
    });

    for (const receipt of freshPlan.frozenReceipts) {
      transaction.create(resultRef.collection("eligibleReceipts").doc(receipt.receiptId), {
        schemaVersion: RESULT_SCHEMA_VERSION,
        resultVersion: RESULT_VERSION,
        weekKey,
        receiptId: receipt.receiptId,
        ownerUid: receipt.ownerUid,
        walletAddress: receipt.walletAddress,
        score: receipt.score,
        maxCombo: receipt.maxCombo,
        perfectHits: receipt.perfectHits,
        radiantHits: receipt.radiantHits,
        corruptedHits: receipt.corruptedHits,
        clientCompletedAtEpochMillis: receipt.clientCompletedAtEpochMillis,
        submittedAt: Timestamp.fromMillis(receipt.submittedAtEpochMillis),
        trustedVerifiedAt: Timestamp.fromMillis(receipt.trustedVerifiedAtEpochMillis),
        trustedEvidenceRef: receipt.trustedEvidenceRef,
        trustedVerificationAuditRef: receipt.trustedVerificationAuditRef,
        trustedVerificationDecidedAt: Timestamp.fromMillis(receipt.trustedVerificationDecidedAtEpochMillis),
        selectedForWalletRanking: receipt.selectedForWalletRanking,
        finalRank: receipt.finalRank,
        snapshotAuthority: RESULT_FINALIZATION_AUTHORITY,
        payoutEnabled: false,
      });
    }

    for (const winner of freshPlan.winners) {
      transaction.create(resultRef.collection("winners").doc(String(winner.placement)), {
        schemaVersion: RESULT_SCHEMA_VERSION,
        resultVersion: RESULT_VERSION,
        weekKey,
        placement: winner.placement,
        walletAddress: winner.walletAddress,
        receiptId: winner.receiptId,
        score: winner.score,
        maxCombo: winner.maxCombo,
        perfectHits: winner.perfectHits,
        clientCompletedAtEpochMillis: winner.clientCompletedAtEpochMillis,
        prizeAmountAtomic: winner.prizeAmountAtomic,
        prizeAssetSymbol: winner.prizeAssetSymbol,
        payoutStatus: WINNER_PAYOUT_STATUS,
        payoutEnabled: false,
        payoutReady: false,
        fundingVerificationStatusAtClose: freshPlan.fundingVerificationStatusAtClose,
        resultAuthority: RESULT_FINALIZATION_AUTHORITY,
        finalizedAt: FieldValue.serverTimestamp(),
      });
    }

    transaction.update(cupRef, {
      status: "CLOSED",
      finalizationStatus: "FINALIZED",
      trustedResultVersion: RESULT_VERSION,
      trustedResultAuthority: RESULT_FINALIZATION_AUTHORITY,
      trustedResultRef: `weeklyCupResults/${weekKey}`,
      trustedResultFinalizedAt: FieldValue.serverTimestamp(),
      closedAt: FieldValue.serverTimestamp(),
      payoutEnabled: false,
      updatedAt: FieldValue.serverTimestamp(),
    });
  });

  console.log(`\nAPPLY COMPLETE — weeklyCupResults/${weekKey} frozen and Cup marked CLOSED.`);
  console.log("No SKR transfer was attempted. payoutEnabled=false and payoutReady=false remain enforced.");
}

main().catch((error) => {
  console.error(`\nFATAL: ${error?.stack || error}`);
  process.exitCode = 1;
});
