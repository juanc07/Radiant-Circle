#!/usr/bin/env node
import { randomUUID } from "node:crypto";
import { applicationDefault, initializeApp } from "firebase-admin/app";
import { FieldValue, getFirestore } from "firebase-admin/firestore";
import {
  DEFAULT_MAINNET_RPC_URL,
  FUNDING_COMMITMENT,
  FUNDING_NETWORK,
  FUNDING_VERIFICATION_AUTHORITY,
  assertCupFundingTarget,
  buildFundingEvidence,
  buildGetTokenAccountsByOwnerRequest,
  formatAtomicSkr,
  parseTokenAccountsByOwnerResponse,
} from "./skr-funding-verification.mjs";

function readArgs(argv) {
  const values = {};
  const flags = new Set();
  for (let i = 0; i < argv.length; i += 1) {
    const token = argv[i];
    if (!token.startsWith("--")) throw new Error(`Unexpected argument: ${token}`);
    const key = token.slice(2);
    if (["apply", "replace-funding-wallet"].includes(key)) {
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
Radiant Circle Phase 12C trusted SKR funding verification

Dry run (read-only on-chain check; no Firestore write):
  node verify-weekly-cup-funding.mjs \\
    --project radiant-rush-10a9c \\
    --week 2026-W37 \\
    --funding-wallet <PUBLIC_SOLANA_WALLET>

Optional custom mainnet RPC (prefer environment variable so API keys do not enter shell history):
  export SOLANA_MAINNET_RPC_URL="https://your-provider.example/..."

Apply only after reviewing the dry run:
  ...same arguments... \\
    --apply \\
    --confirm-project radiant-rush-10a9c

If intentionally replacing an already configured wallet:
  --replace-funding-wallet

This tool reads liquid SKR only. Staked SKR is not counted as prize funding because it is not immediately transferable.
`;
}

async function postRpc(rpcUrl, body) {
  const controller = new AbortController();
  const timeout = setTimeout(() => controller.abort(), 15_000);
  try {
    const response = await fetch(rpcUrl, {
      method: "POST",
      headers: { "content-type": "application/json", accept: "application/json" },
      body: JSON.stringify(body),
      signal: controller.signal,
    });
    const text = await response.text();
    if (!response.ok) {
      throw new Error(`Solana mainnet RPC HTTP ${response.status}: ${text.slice(0, 180)}`);
    }
    return text;
  } catch (error) {
    if (error?.name === "AbortError") throw new Error("Solana mainnet RPC timed out after 15 seconds.");
    throw error;
  } finally {
    clearTimeout(timeout);
  }
}

function sameCriticalTarget(a, b) {
  return String(a?.prizeMint ?? "") === String(b?.prizeMint ?? "") &&
    Number(a?.prizeDecimals) === Number(b?.prizeDecimals) &&
    String(a?.prizeAmountAtomic ?? "") === String(b?.prizeAmountAtomic ?? "") &&
    String(a?.weekKey ?? "") === String(b?.weekKey ?? "");
}

async function main() {
  const { values, flags } = readArgs(process.argv.slice(2));
  const projectId = values.project;
  const weekKey = values.week;
  const fundingWallet = values["funding-wallet"];
  if (!projectId || !weekKey || !fundingWallet) throw new Error(`--project, --week, and --funding-wallet are required.\n${usage()}`);

  const apply = flags.has("apply");
  const allowWalletReplace = flags.has("replace-funding-wallet");
  if (apply && values["confirm-project"] !== projectId) {
    throw new Error("--apply requires --confirm-project to exactly match --project.");
  }

  const rpcUrl = process.env.SOLANA_MAINNET_RPC_URL?.trim() || DEFAULT_MAINNET_RPC_URL;
  console.log("Radiant Circle Phase 12C trusted SKR funding verification");
  console.log(`Project: ${projectId}`);
  console.log(`Cup: weeklyCupConfigs/${weekKey}`);
  console.log(`Mode: ${apply ? "APPLY" : "DRY RUN"}`);
  console.log(`Network: ${FUNDING_NETWORK} (${rpcUrl === DEFAULT_MAINNET_RPC_URL ? "public RPC" : "custom RPC"})`);
  console.log(`Commitment: ${FUNDING_COMMITMENT}`);

  initializeApp({ credential: applicationDefault(), projectId });
  const db = getFirestore();
  const cupRef = db.collection("weeklyCupConfigs").doc(weekKey);
  const initialSnapshot = await cupRef.get();
  if (!initialSnapshot.exists) throw new Error(`weeklyCupConfigs/${weekKey} does not exist.`);
  const initialCup = initialSnapshot.data();
  const target = assertCupFundingTarget(initialCup, fundingWallet, { allowWalletReplace });
  if (target.weekKey !== weekKey) throw new Error(`Cup document weekKey=${target.weekKey} does not match requested ${weekKey}.`);

  console.log("\n[1/2] Reading liquid SKR from Solana mainnet...");
  const rpcResponse = await postRpc(rpcUrl, buildGetTokenAccountsByOwnerRequest(target.wallet));
  const onChain = parseTokenAccountsByOwnerResponse(rpcResponse, target.wallet);
  const evidence = buildFundingEvidence({ cup: initialCup, walletAddress: target.wallet, snapshot: onChain });

  console.log(`Funding wallet: ${target.wallet}`);
  console.log(`Required: ${formatAtomicSkr(evidence.fundingRequiredAmountAtomic)}`);
  console.log(`Observed transferable liquid SKR: ${formatAtomicSkr(evidence.fundingObservedAmountAtomic)}`);
  console.log(`Token accounts: ${evidence.fundingTokenAccountCount} (${evidence.fundingFrozenTokenAccountCount} frozen/excluded)`);
  console.log(`RPC slot: ${evidence.fundingVerificationSlot}`);
  console.log(`Verification result: ${evidence.fundingVerificationStatus}`);
  console.log("Payout remains disabled.");

  if (!apply) {
    console.log("\nDRY RUN ONLY — on-chain state was read, but no Firestore document was written.");
    return;
  }

  console.log("\n[2/2] Persisting trusted funding snapshot and immutable check receipt...");
  const checkId = randomUUID();
  const checkRef = db
    .collection("weeklyCupFundingChecks")
    .doc(weekKey)
    .collection("checks")
    .doc(checkId);

  await db.runTransaction(async (tx) => {
    const freshSnapshot = await tx.get(cupRef);
    if (!freshSnapshot.exists) throw new Error("Cup config disappeared before verification could be committed.");
    const freshCup = freshSnapshot.data();
    assertCupFundingTarget(freshCup, target.wallet, { allowWalletReplace });
    if (!sameCriticalTarget(initialCup, freshCup)) {
      throw new Error("Cup prize configuration changed during verification. Nothing was written; run the check again.");
    }

    const fundingPatch = {
      fundingWalletAddress: evidence.fundingWalletAddress,
      fundingVerificationStatus: evidence.fundingVerificationStatus,
      fundingRequiredAmountAtomic: evidence.fundingRequiredAmountAtomic,
      fundingObservedAmountAtomic: evidence.fundingObservedAmountAtomic,
      fundingTokenAccountCount: evidence.fundingTokenAccountCount,
      fundingFrozenTokenAccountCount: evidence.fundingFrozenTokenAccountCount,
      fundingVerificationSlot: evidence.fundingVerificationSlot,
      fundingVerificationNetwork: evidence.fundingVerificationNetwork,
      fundingVerificationMint: evidence.fundingVerificationMint,
      fundingVerificationCommitment: evidence.fundingVerificationCommitment,
      fundingVerificationAuthority: evidence.fundingVerificationAuthority,
      fundingVerificationSchemaVersion: evidence.fundingVerificationSchemaVersion,
      fundingCheckedAt: FieldValue.serverTimestamp(),
      payoutEnabled: false,
      updatedAt: FieldValue.serverTimestamp(),
    };
    if (evidence.fundingVerificationStatus === "VERIFIED") {
      fundingPatch.fundingVerifiedAt = FieldValue.serverTimestamp();
    } else {
      fundingPatch.fundingVerifiedAt = FieldValue.delete();
    }
    tx.update(cupRef, fundingPatch);

    tx.create(checkRef, {
      schemaVersion: 1,
      checkId,
      weekKey,
      fundingWalletAddress: evidence.fundingWalletAddress,
      prizeMint: evidence.fundingVerificationMint,
      requiredAmountAtomic: evidence.fundingRequiredAmountAtomic,
      observedAmountAtomic: evidence.fundingObservedAmountAtomic,
      tokenAccountCount: evidence.fundingTokenAccountCount,
      frozenTokenAccountCount: evidence.fundingFrozenTokenAccountCount,
      rpcSlot: evidence.fundingVerificationSlot,
      network: evidence.fundingVerificationNetwork,
      commitment: evidence.fundingVerificationCommitment,
      verificationStatus: evidence.fundingVerificationStatus,
      verificationAuthority: FUNDING_VERIFICATION_AUTHORITY,
      payoutEnabled: false,
      checkedAt: FieldValue.serverTimestamp(),
    });
  });

  console.log(`\nAPPLY COMPLETE — funding snapshot written to weeklyCupConfigs/${weekKey}.`);
  console.log(`Immutable funding check: weeklyCupFundingChecks/${weekKey}/checks/${checkId}`);
  console.log(`Status: ${evidence.fundingVerificationStatus}`);
  console.log("No SKR was transferred. Payout remains disabled.");
}

main().catch((error) => {
  console.error(`\nFATAL: ${error?.stack || error}`);
  process.exitCode = 1;
});
