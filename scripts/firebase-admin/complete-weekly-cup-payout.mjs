#!/usr/bin/env node
import { applicationDefault, initializeApp } from "firebase-admin/app";
import { FieldValue, getFirestore } from "firebase-admin/firestore";
import {
  TRANSFER_EVENT_PAYOUT_PAID,
  TRANSFER_EXECUTION_AUTHORITY,
  buildCompletionPlan,
} from "./weekly-cup-skr-transfer.mjs";

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

async function load(db, weekKey) {
  const payoutRef = db.collection("weeklyCupPayouts").doc(weekKey);
  const [batchSnap, itemsSnap] = await Promise.all([
    payoutRef.get(),
    payoutRef.collection("items").orderBy("placement").get(),
  ]);
  return {
    payoutRef,
    batch: batchSnap.exists ? batchSnap.data() : null,
    items: itemsSnap.docs.map((doc) => ({ id: doc.id, data: doc.data() })),
  };
}

async function main() {
  const { values, flags } = readArgs(process.argv.slice(2));
  const projectId = values.project;
  const weekKey = values.week;
  if (!projectId || !weekKey) throw new Error("--project and --week are required.");
  const apply = flags.has("apply");
  if (apply && values["confirm-project"] !== projectId) throw new Error("--apply requires --confirm-project to exactly match --project.");

  initializeApp({ credential: applicationDefault(), projectId });
  const db = getFirestore();
  const loaded = await load(db, weekKey);
  const plan = buildCompletionPlan({ batch: loaded.batch, itemDocuments: loaded.items, weekKey });

  console.log("Radiant Circle Phase 12F payout completion");
  console.log(`Project: ${projectId}`);
  console.log(`Cup: ${weekKey}`);
  console.log(`Mode: ${apply ? "APPLY" : "DRY RUN"}`);
  console.log(`Manifest SHA-256: ${plan.manifestDigestSha256}`);
  console.log(`Paid winners: ${plan.winnerCount}`);
  for (const signature of plan.transactionSignatures) console.log(`  ${signature}`);
  console.log("Resulting batch status: PAID");

  if (!apply) {
    console.log("DRY RUN ONLY — payout batch was not marked PAID.");
    return;
  }
  if (String(values["confirm-digest"] ?? "").toLowerCase() !== plan.manifestDigestSha256) {
    throw new Error("--apply requires --confirm-digest to exactly match the reviewed payout manifest digest.");
  }

  const paidEventRef = loaded.payoutRef.collection("events").doc("paid");
  await db.runTransaction(async (tx) => {
    const freshBatchSnap = await tx.get(loaded.payoutRef);
    const freshItemsSnap = await tx.get(loaded.payoutRef.collection("items").orderBy("placement"));
    const existingEvent = await tx.get(paidEventRef);
    if (existingEvent.exists) throw new Error("PAYOUT_PAID event already exists. Duplicate completion refused.");
    const freshPlan = buildCompletionPlan({
      batch: freshBatchSnap.exists ? freshBatchSnap.data() : null,
      itemDocuments: freshItemsSnap.docs.map((doc) => ({ id: doc.id, data: doc.data() })),
      weekKey,
    });
    if (freshPlan.manifestDigestSha256 !== plan.manifestDigestSha256) {
      throw new Error("Payout evidence changed during completion. Rerun and review dry run.");
    }
    tx.update(loaded.payoutRef, {
      ...freshPlan.batchPatch,
      paidAt: FieldValue.serverTimestamp(),
      updatedAt: FieldValue.serverTimestamp(),
    });
    tx.create(paidEventRef, {
      schemaVersion: 1,
      weekKey,
      eventType: TRANSFER_EVENT_PAYOUT_PAID,
      authority: TRANSFER_EXECUTION_AUTHORITY,
      payoutManifestDigestSha256: freshPlan.manifestDigestSha256,
      winnerCount: freshPlan.winnerCount,
      transactionSignatures: freshPlan.transactionSignatures,
      createdAt: FieldValue.serverTimestamp(),
    });
  });

  console.log(`APPLY COMPLETE — weeklyCupPayouts/${weekKey} is PAID.`);
}

main().catch((error) => {
  console.error(`\nFATAL: ${error?.stack || error}`);
  process.exitCode = 1;
});
