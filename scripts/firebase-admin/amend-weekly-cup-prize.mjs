#!/usr/bin/env node
import { randomUUID } from "node:crypto";
import { applicationDefault, initializeApp } from "firebase-admin/app";
import { FieldValue, getFirestore } from "firebase-admin/firestore";
import {
  FUNDING_EVIDENCE_FIELDS_TO_RESET,
  PRIZE_AMENDMENT_AUTHORITY,
  PRIZE_AMENDMENT_SCHEMA_VERSION,
  assertAmendmentDigest,
  buildPrizeAmendmentPlan,
} from "./weekly-cup-prize-amendment.mjs";
import { formatAtomicSkr } from "./skr-funding-verification.mjs";

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
Radiant Circle trusted Weekly Cup prize amendment

Dry run first:
  node amend-weekly-cup-prize.mjs \\
    --project radiant-rush-10a9c \\
    --week 2026-W38 \\
    --prize-skr 300 \\
    --funding-wallet <PUBLIC_SOLANA_WALLET> \\
    --reason "Operator-authorized reduced-value live payout test"

Apply only after reviewing the exact dry-run digest:
  ...same arguments... \\
    --apply \\
    --confirm-project radiant-rush-10a9c \\
    --confirm-week 2026-W38 \\
    --confirm-amendment-digest <EXACT_SHA256_FROM_DRY_RUN>

Safety contract:
- Cup must still be DRAFT/ANNOUNCED/OPEN and not finalized.
- Existing funding evidence is invalidated/reset because it was tied to the old prize/wallet.
- A fresh Phase 12C funding verification is mandatory after amendment.
- payoutEnabled remains false and no SKR is transferred.
`;
}

function printPlan(plan) {
  console.log(`Cup status: ${plan.cupStatus}`);
  console.log(`Old prize: ${formatAtomicSkr(plan.previousPrizeAmountAtomic)}`);
  console.log(`New prize: ${formatAtomicSkr(plan.newPrizeAmountAtomic)}`);
  console.log(`Old funding wallet: ${plan.previousFundingWalletAddress ?? "NONE"}`);
  console.log(`New funding wallet: ${plan.newFundingWalletAddress}`);
  console.log(`Previous funding status: ${plan.previousFundingVerificationStatus}`);
  console.log("Funding evidence after amendment: RESET / NOT_VERIFIED");
  console.log("Payout enabled: false");
  console.log(`Reason: ${plan.reason}`);
  console.log(`Amendment SHA-256: ${plan.amendmentDigest}`);
}

async function main() {
  const { values, flags } = readArgs(process.argv.slice(2));
  const projectId = String(values.project ?? "").trim();
  const weekKey = String(values.week ?? "").trim();
  const newPrizeSkr = values["prize-skr"];
  const fundingWalletAddress = values["funding-wallet"];
  const reason = values.reason;
  if (!projectId || !weekKey || !newPrizeSkr || !fundingWalletAddress || !reason) {
    throw new Error(`--project, --week, --prize-skr, --funding-wallet, and --reason are required.\n${usage()}`);
  }

  const apply = flags.has("apply");
  if (apply && values["confirm-project"] !== projectId) {
    throw new Error("--apply requires --confirm-project to exactly match --project.");
  }
  if (apply && values["confirm-week"] !== weekKey) {
    throw new Error("--apply requires --confirm-week to exactly match --week.");
  }

  initializeApp({ credential: applicationDefault(), projectId });
  const db = getFirestore();
  const cupRef = db.collection("weeklyCupConfigs").doc(weekKey);
  const initialSnapshot = await cupRef.get();
  if (!initialSnapshot.exists) throw new Error(`weeklyCupConfigs/${weekKey} does not exist.`);

  const initialCup = initialSnapshot.data();
  const initialPlan = buildPrizeAmendmentPlan({
    cup: initialCup,
    weekKey,
    newPrizeSkr,
    fundingWalletAddress,
    reason,
  });

  console.log("Radiant Circle trusted Weekly Cup prize amendment");
  console.log(`Project: ${projectId}`);
  console.log(`Cup: weeklyCupConfigs/${weekKey}`);
  console.log(`Mode: ${apply ? "APPLY" : "DRY RUN"}`);
  console.log("");
  printPlan(initialPlan);

  if (!apply) {
    console.log("\nDRY RUN ONLY — no Firestore document was written.");
    console.log("Copy the exact Amendment SHA-256 only after reviewing all fields above.");
    return;
  }

  const confirmedDigest = assertAmendmentDigest(values["confirm-amendment-digest"]);
  if (confirmedDigest !== initialPlan.amendmentDigest) {
    throw new Error("--confirm-amendment-digest does not match the reviewed dry-run amendment digest.");
  }

  const amendmentId = randomUUID();
  const amendmentRef = db
    .collection("weeklyCupPrizeAmendments")
    .doc(weekKey)
    .collection("amendments")
    .doc(amendmentId);

  await db.runTransaction(async (tx) => {
    const freshSnapshot = await tx.get(cupRef);
    if (!freshSnapshot.exists) throw new Error("Cup config disappeared before amendment could be committed.");
    const freshCup = freshSnapshot.data();
    const freshPlan = buildPrizeAmendmentPlan({
      cup: freshCup,
      weekKey,
      newPrizeSkr,
      fundingWalletAddress,
      reason,
    });
    if (freshPlan.amendmentDigest !== initialPlan.amendmentDigest) {
      throw new Error("Cup configuration changed after dry-run review. Nothing was written; rerun the dry run.");
    }

    const patch = {
      prizeAmountAtomic: freshPlan.newPrizeAmountAtomic,
      fundingWalletAddress: freshPlan.newFundingWalletAddress,
      fundingVerificationStatus: "NOT_VERIFIED",
      payoutEnabled: false,
      updatedAt: FieldValue.serverTimestamp(),
    };
    for (const field of FUNDING_EVIDENCE_FIELDS_TO_RESET) {
      patch[field] = FieldValue.delete();
    }

    tx.update(cupRef, patch);
    tx.create(amendmentRef, {
      schemaVersion: PRIZE_AMENDMENT_SCHEMA_VERSION,
      amendmentId,
      authority: PRIZE_AMENDMENT_AUTHORITY,
      weekKey,
      cupStatusAtAmendment: freshPlan.cupStatus,
      previousPrizeAmountAtomic: freshPlan.previousPrizeAmountAtomic,
      newPrizeAmountAtomic: freshPlan.newPrizeAmountAtomic,
      previousFundingWalletAddress: freshPlan.previousFundingWalletAddress,
      newFundingWalletAddress: freshPlan.newFundingWalletAddress,
      previousFundingVerificationStatus: freshPlan.previousFundingVerificationStatus,
      fundingEvidenceReset: true,
      reason: freshPlan.reason,
      amendmentDigest: freshPlan.amendmentDigest,
      payoutEnabled: false,
      amendedAt: FieldValue.serverTimestamp(),
    });
  });

  console.log(`\nAPPLY COMPLETE — weeklyCupConfigs/${weekKey} prize amended.`);
  console.log(`Immutable audit: weeklyCupPrizeAmendments/${weekKey}/amendments/${amendmentId}`);
  console.log(`New prize: ${formatAtomicSkr(initialPlan.newPrizeAmountAtomic)}`);
  console.log(`Funding wallet: ${initialPlan.newFundingWalletAddress}`);
  console.log("Funding status: NOT_VERIFIED (fresh Phase 12C verification REQUIRED)." );
  console.log("No SKR was transferred. Payout remains disabled.");
}

main().catch((error) => {
  console.error(`\nFATAL: ${error?.stack || error}`);
  process.exitCode = 1;
});
