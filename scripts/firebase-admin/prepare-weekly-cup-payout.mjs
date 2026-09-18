#!/usr/bin/env node
import { applicationDefault, initializeApp } from "firebase-admin/app";
import { FieldValue, getFirestore } from "firebase-admin/firestore";
import {
  PAYOUT_AUTHORITY,
  PAYOUT_EVENT_PREPARED,
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
Radiant Circle Phase 12E trusted payout lifecycle — PREPARE

Dry run:
  node prepare-weekly-cup-payout.mjs \\
    --project radiant-rush-10a9c \\
    --week 2026-W38

Apply only after reviewing the finalized winners, exact amounts, funding wallet,
and manifest digest:
  node prepare-weekly-cup-payout.mjs \\
    --project radiant-rush-10a9c \\
    --week 2026-W38 \\
    --apply \\
    --confirm-project radiant-rush-10a9c

This command creates a review-only payout manifest. It never signs or transfers SKR.
Funding must have been VERIFIED by Phase 12C before the Cup was finalized.
`;
}

async function sourceSnapshots(db, weekKey) {
  const cupRef = db.collection("weeklyCupConfigs").doc(weekKey);
  const resultRef = db.collection("weeklyCupResults").doc(weekKey);
  const payoutRef = db.collection("weeklyCupPayouts").doc(weekKey);
  const lockAccountsRef = db.collection("weeklyCupCompetitionWalletLocks").doc(weekKey).collection("accounts");
  const [cupSnap, resultSnap, winnerQuery, lockQuery, payoutSnap] = await Promise.all([
    cupRef.get(),
    resultRef.get(),
    resultRef.collection("winners").orderBy("placement").get(),
    lockAccountsRef.get(),
    payoutRef.get(),
  ]);
  if (payoutSnap.exists) throw new Error(`weeklyCupPayouts/${weekKey} already exists. Payout preparation is immutable and cannot be repeated.`);
  const plan = buildPayoutPreparationPlan({
    cup: cupSnap.exists ? cupSnap.data() : null,
    result: resultSnap.exists ? resultSnap.data() : null,
    winnerDocuments: winnerQuery.docs.map((doc) => ({ id: doc.id, data: doc.data() })),
    competitionWalletLockDocuments: lockQuery.docs.map((doc) => ({ id: doc.id, data: doc.data() })),
    weekKey,
  });
  return { cupRef, resultRef, payoutRef, lockAccountsRef, plan };
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
  const { cupRef, resultRef, payoutRef, lockAccountsRef, plan } = await sourceSnapshots(db, weekKey);

  console.log("Radiant Circle Phase 12E trusted payout lifecycle — PREPARE");
  console.log(`Project: ${projectId}`);
  console.log(`Mode: ${apply ? "APPLY" : "DRY RUN"}`);
  console.log(`Cup: ${weekKey}`);
  console.log(`Funding at close: ${plan.batch.fundingVerificationStatusAtClose}`);
  console.log(`Funding wallet: ${plan.batch.fundingWalletAddress}`);
  console.log(`Total manifest: ${plan.batch.totalAmountAtomic} atomic SKR`);
  console.log(`Winners: ${plan.batch.winnerCount}`);
  for (const item of plan.items) {
    console.log(`  #${item.placement} ${item.walletAddress} amountAtomic=${item.amountAtomic} receipt=${item.receiptId}`);
  }
  console.log(`Payout manifest SHA-256: ${plan.manifestDigestSha256}`);
  console.log("Transfer enabled: false");
  console.log("SKR transfer attempted: NO");

  if (!apply) {
    console.log("\nDRY RUN ONLY — no payout lifecycle documents were written.");
    return;
  }

  const preparedEventRef = payoutRef.collection("events").doc("prepared");
  await db.runTransaction(async (tx) => {
    const freshCupSnap = await tx.get(cupRef);
    const freshResultSnap = await tx.get(resultRef);
    const freshPayoutSnap = await tx.get(payoutRef);
    const freshWinnerQuery = await tx.get(resultRef.collection("winners").orderBy("placement"));
    const freshLockQuery = await tx.get(lockAccountsRef);
    if (freshPayoutSnap.exists) throw new Error("Payout batch appeared during preparation. Refusing duplicate prepare.");

    const freshPlan = buildPayoutPreparationPlan({
      cup: freshCupSnap.exists ? freshCupSnap.data() : null,
      result: freshResultSnap.exists ? freshResultSnap.data() : null,
      winnerDocuments: freshWinnerQuery.docs.map((doc) => ({ id: doc.id, data: doc.data() })),
      competitionWalletLockDocuments: freshLockQuery.docs.map((doc) => ({ id: doc.id, data: doc.data() })),
      weekKey,
    });
    if (freshPlan.manifestDigestSha256 !== plan.manifestDigestSha256) {
      throw new Error("Trusted winner inputs changed during payout preparation. Rerun and review the dry run.");
    }

    tx.create(payoutRef, {
      ...freshPlan.batch,
      preparedAt: FieldValue.serverTimestamp(),
      createdAt: FieldValue.serverTimestamp(),
      updatedAt: FieldValue.serverTimestamp(),
    });
    for (const item of freshPlan.items) {
      tx.create(payoutRef.collection("items").doc(String(item.placement)), {
        ...item,
        preparedAt: FieldValue.serverTimestamp(),
        createdAt: FieldValue.serverTimestamp(),
        updatedAt: FieldValue.serverTimestamp(),
      });
    }
    tx.create(preparedEventRef, {
      schemaVersion: 1,
      lifecycleVersion: 1,
      weekKey,
      eventType: PAYOUT_EVENT_PREPARED,
      authority: PAYOUT_AUTHORITY,
      payoutManifestDigestSha256: freshPlan.manifestDigestSha256,
      payoutEnabled: false,
      transferEnabled: false,
      createdAt: FieldValue.serverTimestamp(),
    });
  });

  console.log(`\nAPPLY COMPLETE — weeklyCupPayouts/${weekKey} created in READY_FOR_REVIEW state.`);
  console.log("No private key was used. No transaction was signed. No SKR was transferred.");
}

main().catch((error) => {
  console.error(`\nFATAL: ${error?.stack || error}`);
  process.exitCode = 1;
});
