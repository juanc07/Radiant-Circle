#!/usr/bin/env node
import { applicationDefault, initializeApp } from "firebase-admin/app";
import { FieldValue, getFirestore } from "firebase-admin/firestore";
import {
  PAYOUT_AUTHORITY,
  PAYOUT_EVENT_APPROVED,
  buildPayoutApprovalPlan,
  buildPayoutPreparationPlan,
} from "./weekly-cup-payout-lifecycle.mjs";

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
Radiant Circle Phase 12E trusted payout lifecycle — APPROVE

Dry run:
  node approve-weekly-cup-payout.mjs \\
    --project radiant-rush-10a9c \\
    --week 2026-W38 \\
    --review-ref "operator-review-w38"

Apply requires both the project confirmation and the exact digest printed by the dry run:
  node approve-weekly-cup-payout.mjs \\
    --project radiant-rush-10a9c \\
    --week 2026-W38 \\
    --review-ref "operator-review-w38" \\
    --confirm-digest <64-char-sha256> \\
    --apply \\
    --confirm-project radiant-rush-10a9c

Approval freezes the reviewed manifest for a future trusted transfer phase.
It does NOT enable transfer, sign a transaction, or move SKR.
`;
}

async function loadEverything(db, weekKey, reviewRef) {
  const cupRef = db.collection("weeklyCupConfigs").doc(weekKey);
  const resultRef = db.collection("weeklyCupResults").doc(weekKey);
  const payoutRef = db.collection("weeklyCupPayouts").doc(weekKey);
  const [cupSnap, resultSnap, winnerQuery, payoutSnap, itemQuery] = await Promise.all([
    cupRef.get(),
    resultRef.get(),
    resultRef.collection("winners").orderBy("placement").get(),
    payoutRef.get(),
    payoutRef.collection("items").orderBy("placement").get(),
  ]);
  if (!payoutSnap.exists) throw new Error(`weeklyCupPayouts/${weekKey} does not exist. Prepare it first.`);

  const sourcePlan = buildPayoutPreparationPlan({
    cup: cupSnap.exists ? cupSnap.data() : null,
    result: resultSnap.exists ? resultSnap.data() : null,
    winnerDocuments: winnerQuery.docs.map((doc) => ({ id: doc.id, data: doc.data() })),
    weekKey,
  });
  if (String(payoutSnap.data().payoutManifestDigestSha256 ?? "") !== sourcePlan.manifestDigestSha256) {
    throw new Error("Prepared payout batch no longer matches the trusted Phase 12D result. Refusing approval.");
  }

  const approval = buildPayoutApprovalPlan({
    batch: payoutSnap.data(),
    itemDocuments: itemQuery.docs.map((doc) => ({ id: doc.id, data: doc.data() })),
    weekKey,
    reviewRef,
  });
  return { cupRef, resultRef, payoutRef, sourcePlan, approval };
}

async function main() {
  const { values, flags } = readArgs(process.argv.slice(2));
  const projectId = values.project;
  const weekKey = values.week;
  const reviewRef = values["review-ref"];
  if (!projectId || !weekKey || !reviewRef) {
    throw new Error(`--project, --week, and --review-ref are required.\n${usage()}`);
  }
  const apply = flags.has("apply");
  if (apply && values["confirm-project"] !== projectId) {
    throw new Error("--apply requires --confirm-project to exactly match --project.");
  }

  initializeApp({ credential: applicationDefault(), projectId });
  const db = getFirestore();
  const { cupRef, resultRef, payoutRef, approval } = await loadEverything(db, weekKey, reviewRef);

  console.log("Radiant Circle Phase 12E trusted payout lifecycle — APPROVE");
  console.log(`Project: ${projectId}`);
  console.log(`Mode: ${apply ? "APPLY" : "DRY RUN"}`);
  console.log(`Cup: ${weekKey}`);
  console.log(`Review reference: ${approval.reviewRef}`);
  console.log(`Payout manifest SHA-256: ${approval.manifestDigestSha256}`);
  console.log("Reviewed payout items:");
  for (const item of approval.items) {
    console.log(`  #${item.placement} ${item.walletAddress} amountAtomic=${item.amountAtomic}`);
  }
  console.log("Resulting lifecycle status: APPROVED");
  console.log("Transfer enabled: false");
  console.log("SKR transfer attempted: NO");

  if (!apply) {
    console.log("\nDRY RUN ONLY — no payout lifecycle state was changed.");
    return;
  }
  if (String(values["confirm-digest"] ?? "").toLowerCase() !== approval.manifestDigestSha256) {
    throw new Error("--apply requires --confirm-digest to exactly match the dry-run payout manifest SHA-256.");
  }

  const approvedEventRef = payoutRef.collection("events").doc("approved");
  await db.runTransaction(async (tx) => {
    const freshCupSnap = await tx.get(cupRef);
    const freshResultSnap = await tx.get(resultRef);
    const freshWinnerQuery = await tx.get(resultRef.collection("winners").orderBy("placement"));
    const freshPayoutSnap = await tx.get(payoutRef);
    const freshItemQuery = await tx.get(payoutRef.collection("items").orderBy("placement"));
    const freshApprovedEvent = await tx.get(approvedEventRef);
    if (!freshPayoutSnap.exists) throw new Error("Payout batch disappeared during approval.");
    if (freshApprovedEvent.exists) throw new Error("Payout approval audit already exists. Duplicate approval refused.");

    const freshSource = buildPayoutPreparationPlan({
      cup: freshCupSnap.exists ? freshCupSnap.data() : null,
      result: freshResultSnap.exists ? freshResultSnap.data() : null,
      winnerDocuments: freshWinnerQuery.docs.map((doc) => ({ id: doc.id, data: doc.data() })),
      weekKey,
    });
    const freshApproval = buildPayoutApprovalPlan({
      batch: freshPayoutSnap.data(),
      itemDocuments: freshItemQuery.docs.map((doc) => ({ id: doc.id, data: doc.data() })),
      weekKey,
      reviewRef,
    });
    if (freshSource.manifestDigestSha256 !== approval.manifestDigestSha256 ||
        freshApproval.manifestDigestSha256 !== approval.manifestDigestSha256) {
      throw new Error("Payout source or manifest changed during approval. Rerun and review the dry run.");
    }

    tx.update(payoutRef, {
      ...freshApproval.batchPatch,
      approvedAt: FieldValue.serverTimestamp(),
      updatedAt: FieldValue.serverTimestamp(),
    });
    for (const item of freshApproval.items) {
      tx.update(payoutRef.collection("items").doc(String(item.placement)), {
        ...freshApproval.itemPatch,
        approvedAt: FieldValue.serverTimestamp(),
        updatedAt: FieldValue.serverTimestamp(),
      });
    }
    tx.create(approvedEventRef, {
      schemaVersion: 1,
      lifecycleVersion: 1,
      weekKey,
      eventType: PAYOUT_EVENT_APPROVED,
      authority: PAYOUT_AUTHORITY,
      reviewRef: freshApproval.reviewRef,
      payoutManifestDigestSha256: freshApproval.manifestDigestSha256,
      payoutEnabled: false,
      transferEnabled: false,
      createdAt: FieldValue.serverTimestamp(),
    });
  });

  console.log(`\nAPPLY COMPLETE — weeklyCupPayouts/${weekKey} is APPROVED for a future transfer phase.`);
  console.log("No private key was used. No transaction was signed. No SKR was transferred.");
}

main().catch((error) => {
  console.error(`\nFATAL: ${error?.stack || error}`);
  process.exitCode = 1;
});
