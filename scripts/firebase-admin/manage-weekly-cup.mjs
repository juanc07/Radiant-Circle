#!/usr/bin/env node
import { applicationDefault, initializeApp } from "firebase-admin/app";
import { FieldValue, Timestamp, getFirestore } from "firebase-admin/firestore";
import {
  assertSafeExistingConfig,
  buildWeeklyCupConfig,
} from "./weekly-cup-config.mjs";

function readArgs(argv) {
  const values = {};
  const flags = new Set();

  for (let i = 0; i < argv.length; i += 1) {
    const token = argv[i];
    if (!token.startsWith("--")) throw new Error(`Unexpected argument: ${token}`);
    const key = token.slice(2);
    if (["apply"].includes(key)) {
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
Radiant Circle Phase 12B trusted Weekly Cup config

Dry run:
  node manage-weekly-cup.mjs \\
    --project radiant-rush-10a9c \\
    --week 2026-W37 \\
    --status ANNOUNCED \\
    --sponsor "ThinkBloxPH" \\
    --prize-skr 1000 \\
    --note "Sponsored results require trusted verification after Cup close."

Optional:
  --funding-wallet <public Solana address>
  --placements "1:50,2:30,3:20"

Apply only after reviewing dry run:
  ...same arguments... \\
    --apply \\
    --confirm-project radiant-rush-10a9c
`;
}

async function main() {
  const { values, flags } = readArgs(process.argv.slice(2));
  const projectId = values.project;
  if (!projectId) throw new Error(`--project is required.\n${usage()}`);

  const config = buildWeeklyCupConfig({
    weekKey: values.week,
    status: values.status,
    sponsorName: values.sponsor,
    sponsorNote: values.note,
    prizeSkr: values["prize-skr"],
    fundingWalletAddress: values["funding-wallet"],
    placements: values.placements,
  });

  const apply = flags.has("apply");
  if (apply && values["confirm-project"] !== projectId) {
    throw new Error("--apply requires --confirm-project to exactly match --project.");
  }

  console.log("Radiant Circle Phase 12B trusted Weekly Cup configuration");
  console.log(`Project: ${projectId}`);
  console.log(`Mode: ${apply ? "APPLY" : "DRY RUN"}`);
  console.log(`Cup: weeklyCupConfigs/${config.weekKey}`);

  initializeApp({
    credential: applicationDefault(),
    projectId,
  });

  const db = getFirestore();
  const ref = db.collection("weeklyCupConfigs").doc(config.weekKey);
  const existingSnapshot = await ref.get();
  const existing = existingSnapshot.exists ? existingSnapshot.data() : null;
  const safety = assertSafeExistingConfig(existing, config);
  const effectiveConfig = safety.preservePhase12cEvidence
    ? {
        ...config,
        fundingWalletAddress: existing.fundingWalletAddress ?? null,
        fundingVerificationStatus: existing.fundingVerificationStatus ?? "NOT_VERIFIED",
      }
    : config;

  const review = {
    ...effectiveConfig,
    startsAt: new Date(config.startsAtEpochMillis).toISOString(),
    endsAt: new Date(config.endsAtEpochMillis).toISOString(),
  };
  delete review.startsAtEpochMillis;
  delete review.endsAtEpochMillis;

  console.log("\nProposed trusted config:");
  console.log(JSON.stringify(review, null, 2));
  console.log(`\nExisting document: ${existingSnapshot.exists ? "YES" : "NO"}`);
  console.log("Android write access: NONE (Firestore rules keep weeklyCupConfigs read-only)." );
  console.log("Funding verification: NOT performed by Phase 12B.");
  if (safety.preservePhase12cEvidence) {
    console.log("Phase 12C funding evidence: PRESERVED.");
  }
  console.log("Payout enabled: false.");

  if (!apply) {
    console.log("\nDRY RUN ONLY — no Firestore document was written.");
    return;
  }

  const firestoreData = {
    schemaVersion: effectiveConfig.schemaVersion,
    weekKey: effectiveConfig.weekKey,
    status: effectiveConfig.status,
    sponsorName: effectiveConfig.sponsorName,
    sponsorNote: effectiveConfig.sponsorNote,
    prizeAssetSymbol: effectiveConfig.prizeAssetSymbol,
    prizeMint: effectiveConfig.prizeMint,
    prizeDecimals: effectiveConfig.prizeDecimals,
    prizeAmountAtomic: effectiveConfig.prizeAmountAtomic,
    placementAllocationsBps: effectiveConfig.placementAllocationsBps,
    startsAt: Timestamp.fromMillis(effectiveConfig.startsAtEpochMillis),
    endsAt: Timestamp.fromMillis(effectiveConfig.endsAtEpochMillis),
    fundingWalletAddress: effectiveConfig.fundingWalletAddress,
    fundingVerificationStatus: effectiveConfig.fundingVerificationStatus,
    trustedResultsRequired: true,
    payoutEnabled: false,
    configurationAuthority: config.configurationAuthority,
    createdAt: existing?.createdAt ?? FieldValue.serverTimestamp(),
    updatedAt: FieldValue.serverTimestamp(),
  };

  if (safety.preservePhase12cEvidence) {
    await ref.set(firestoreData, { merge: true });
  } else {
    await ref.set(firestoreData);
  }
  console.log(`\nAPPLY COMPLETE — weeklyCupConfigs/${config.weekKey} written.`);
  console.log(
    safety.preservePhase12cEvidence
      ? "Existing Phase 12C funding evidence was preserved; payout remains disabled."
      : "Funding remains unverified and Android payout remains disabled.",
  );
}

main().catch((error) => {
  console.error(`\nFATAL: ${error?.stack || error}`);
  process.exitCode = 1;
});
