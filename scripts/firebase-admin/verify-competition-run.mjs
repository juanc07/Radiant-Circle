#!/usr/bin/env node
import { applicationDefault, initializeApp } from "firebase-admin/app";
import { FieldValue, getFirestore } from "firebase-admin/firestore";
import {
  RUN_VERIFICATION_AUTHORITY,
  RUN_VERIFICATION_METHOD,
  RUN_VERIFICATION_SCHEMA_VERSION,
  buildRejectedRunDecision,
  buildVerifiedRunDecision,
} from "./competition-run-verification.mjs";

function readArgs(argv) {
  const values = {};
  const flags = new Set();
  for (let i = 0; i < argv.length; i += 1) {
    const token = argv[i];
    if (!token.startsWith("--")) throw new Error(`Unexpected argument: ${token}`);
    const key = token.slice(2);
    if (key === "apply") {
      flags.add(key);
      continue;
    }
    const value = argv[i + 1];
    if (!value || value.startsWith("--")) throw new Error(`Missing value for --${key}`);
    values[key] = value;
    i += 1;
  }
  return { values, flags };
}

function usage() {
  return `
Radiant Circle Phase 12D trusted run verification prerequisite

This tool NEVER auto-verifies a client score. VERIFIED requires an operator to
compare the receipt with independent evidence and enter the checked facts.

Dry-run VERIFIED example:
  node verify-competition-run.mjs \\
    --project radiant-rush-10a9c \\
    --week 2026-W37 \\
    --receipt <RECEIPT_ID> \\
    --decision VERIFIED \\
    --evidence-ref "device-capture-2026-w37-run-001" \\
    --wallet <PUBLIC_WALLET> \\
    --score <SCORE> \\
    --max-combo <MAX_COMBO> \\
    --perfect-hits <PERFECT_HITS> \\
    --radiant-hits <RADIANT_HITS> \\
    --corrupted-hits <CORRUPTED_HITS> \\
    --completed-at-ms <EPOCH_MILLIS>

Reject a receipt:
  node verify-competition-run.mjs ... \\
    --decision REJECTED \\
    --evidence-ref "review-2026-w37-run-001" \\
    --reason "Independent evidence did not match the submitted run."

Apply only after reviewing the dry run:
  ...same arguments... --apply --confirm-project radiant-rush-10a9c
`;
}

function numberArg(values, key) {
  const raw = values[key];
  if (raw == null || !/^\d+$/.test(raw)) throw new Error(`--${key} must be a non-negative integer.`);
  const value = Number(raw);
  if (!Number.isSafeInteger(value)) throw new Error(`--${key} is outside the safe integer range.`);
  return value;
}

async function main() {
  const { values, flags } = readArgs(process.argv.slice(2));
  const projectId = values.project;
  const weekKey = values.week;
  const receiptId = values.receipt;
  const decisionName = String(values.decision ?? "").trim().toUpperCase();
  if (!projectId || !weekKey || !receiptId || !decisionName) throw new Error(`Missing required arguments.\n${usage()}`);
  if (!["VERIFIED", "REJECTED"].includes(decisionName)) throw new Error("--decision must be VERIFIED or REJECTED.");

  const apply = flags.has("apply");
  if (apply && values["confirm-project"] !== projectId) {
    throw new Error("--apply requires --confirm-project to exactly match --project.");
  }

  initializeApp({ credential: applicationDefault(), projectId });
  const db = getFirestore();
  const cupRef = db.collection("weeklyCupConfigs").doc(weekKey);
  const receiptRef = db.collection("competitionRunSubmissions").doc(receiptId);
  const verificationRef = db.collection("competitionRunVerifications").doc(receiptId);

  const [cupSnap, receiptSnap, verificationSnap] = await Promise.all([
    cupRef.get(),
    receiptRef.get(),
    verificationRef.get(),
  ]);
  if (verificationSnap.exists) throw new Error("An immutable trusted verification record already exists for this receipt.");

  const args = {
    cup: cupSnap.exists ? cupSnap.data() : null,
    weekKey,
    receiptId,
    receipt: receiptSnap.exists ? receiptSnap.data() : null,
    evidenceRef: values["evidence-ref"],
  };
  const decision = decisionName === "VERIFIED"
    ? buildVerifiedRunDecision({
        ...args,
        expected: {
          walletAddress: values.wallet,
          score: numberArg(values, "score"),
          maxCombo: numberArg(values, "max-combo"),
          perfectHits: numberArg(values, "perfect-hits"),
          radiantHits: numberArg(values, "radiant-hits"),
          corruptedHits: numberArg(values, "corrupted-hits"),
          clientCompletedAtEpochMillis: numberArg(values, "completed-at-ms"),
        },
      })
    : buildRejectedRunDecision({ ...args, reason: values.reason });

  console.log("Radiant Circle Phase 12D trusted run verification prerequisite");
  console.log(`Project: ${projectId}`);
  console.log(`Mode: ${apply ? "APPLY" : "DRY RUN"}`);
  console.log(`Cup: ${weekKey}`);
  console.log(`Receipt: ${receiptId}`);
  console.log(`Decision: ${decision.decision}`);
  console.log(`Wallet: ${decision.normalized.walletAddress}`);
  console.log(`Score: ${decision.normalized.score}`);
  console.log(`Evidence ref: ${decision.update.trustedEvidenceRef}`);
  console.log("Payout enabled: false");

  if (!apply) {
    console.log("\nDRY RUN ONLY — no receipt or verification record was written.");
    return;
  }

  await db.runTransaction(async (transaction) => {
    const [freshCupSnap, freshReceiptSnap, freshVerificationSnap] = await Promise.all([
      transaction.get(cupRef),
      transaction.get(receiptRef),
      transaction.get(verificationRef),
    ]);
    if (freshVerificationSnap.exists) throw new Error("Trusted verification already exists; refusing duplicate decision.");

    const freshArgs = {
      cup: freshCupSnap.exists ? freshCupSnap.data() : null,
      weekKey,
      receiptId,
      receipt: freshReceiptSnap.exists ? freshReceiptSnap.data() : null,
      evidenceRef: values["evidence-ref"],
    };
    const freshDecision = decisionName === "VERIFIED"
      ? buildVerifiedRunDecision({
          ...freshArgs,
          expected: {
            walletAddress: values.wallet,
            score: numberArg(values, "score"),
            maxCombo: numberArg(values, "max-combo"),
            perfectHits: numberArg(values, "perfect-hits"),
            radiantHits: numberArg(values, "radiant-hits"),
            corruptedHits: numberArg(values, "corrupted-hits"),
            clientCompletedAtEpochMillis: numberArg(values, "completed-at-ms"),
          },
        })
      : buildRejectedRunDecision({ ...freshArgs, reason: values.reason });

    const trustedAtField = freshDecision.decision === "VERIFIED" ? "trustedVerifiedAt" : "trustedRejectedAt";
    transaction.update(receiptRef, {
      ...freshDecision.update,
      [trustedAtField]: FieldValue.serverTimestamp(),
    });
    transaction.create(verificationRef, {
      schemaVersion: RUN_VERIFICATION_SCHEMA_VERSION,
      receiptId,
      weekKey,
      decision: freshDecision.decision,
      walletAddress: freshDecision.normalized.walletAddress,
      ownerUid: freshDecision.normalized.ownerUid,
      score: freshDecision.normalized.score,
      maxCombo: freshDecision.normalized.maxCombo,
      perfectHits: freshDecision.normalized.perfectHits,
      radiantHits: freshDecision.normalized.radiantHits,
      corruptedHits: freshDecision.normalized.corruptedHits,
      clientCompletedAtEpochMillis: freshDecision.normalized.clientCompletedAtEpochMillis,
      evidenceRef: freshDecision.update.trustedEvidenceRef,
      rejectionReason: freshDecision.update.trustedRejectionReason ?? null,
      verificationAuthority: RUN_VERIFICATION_AUTHORITY,
      verificationMethod: RUN_VERIFICATION_METHOD,
      payoutEnabled: false,
      decidedAt: FieldValue.serverTimestamp(),
    });
  });

  console.log("\nAPPLY COMPLETE — trusted receipt decision and immutable verification audit record written.");
}

main().catch((error) => {
  console.error(`\nFATAL: ${error?.stack || error}`);
  process.exitCode = 1;
});
