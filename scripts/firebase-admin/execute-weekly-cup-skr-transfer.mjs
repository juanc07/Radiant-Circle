#!/usr/bin/env node
import { randomUUID } from "node:crypto";
import { access } from "node:fs/promises";
import { spawn } from "node:child_process";
import { applicationDefault, initializeApp } from "firebase-admin/app";
import { FieldValue, getFirestore } from "firebase-admin/firestore";
import { DEFAULT_MAINNET_RPC_URL } from "./skr-funding-verification.mjs";
import {
  BATCH_STATUS_PAYMENT_PENDING,
  ITEM_STATUS_PAYMENT_PENDING,
  ITEM_STATUS_PAID,
  TRANSFER_COMMITMENT,
  TRANSFER_EVENT_FINALIZED,
  TRANSFER_EVENT_STARTED,
  TRANSFER_EVENT_SUBMITTED,
  TRANSFER_EXECUTION_AUTHORITY,
  TRANSFER_NETWORK,
  TRANSFER_STATUS_FINALIZED,
  TRANSFER_STATUS_IN_PROGRESS,
  TRANSFER_STATUS_SUBMITTED,
  TRANSFER_STATUS_SUBMITTING,
  assertRecipientSystemWallet,
  assertSkrMintProgram,
  buildGetAccountInfoRequest,
  buildGetSignatureStatusesRequest,
  buildGetTokenAccountsByOwnerRequest,
  buildGetTransactionRequest,
  buildTransferItemPlan,
  parseSignatureStatus,
  parseSplTokenTransferSignature,
  parseTransferableTokenAccounts,
  selectSourceTokenAccount,
  validateTransferManifest,
  verifyFinalizedTransferTransaction,
} from "./weekly-cup-skr-transfer.mjs";
import { OFFICIAL_SKR_MINT } from "./weekly-cup-config.mjs";

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
Radiant Circle Phase 12F — execute ONE approved SKR winner transfer

Dry run (no signer required, no transaction):
  node execute-weekly-cup-skr-transfer.mjs \\
    --project radiant-rush-10a9c \\
    --week 2026-W38 \\
    --placement 1

Apply requires the exact values printed by the dry run plus an external signer path:
  export RADIANT_PAYOUT_SIGNER_KEYPAIR="C:\\secure\\sponsor-keypair.json"

  node execute-weekly-cup-skr-transfer.mjs \\
    --project radiant-rush-10a9c \\
    --week 2026-W38 \\
    --placement 1 \\
    --confirm-digest <SHA256> \\
    --confirm-funding-wallet <FUNDING_WALLET> \\
    --confirm-recipient <WINNER_WALLET> \\
    --confirm-amount-atomic <ATOMIC_AMOUNT> \\
    --apply \\
    --confirm-project radiant-rush-10a9c

Safety:
- exactly one placement per command
- placement order is enforced (#1, then #2, then #3)
- signer material stays outside Git, Firestore, Android, and command arguments
- any ambiguous post-submit failure stops for reconciliation instead of retrying
`;
}

async function postRpc(rpcUrl, body) {
  const controller = new AbortController();
  const timeout = setTimeout(() => controller.abort(), 20_000);
  try {
    const response = await fetch(rpcUrl, {
      method: "POST",
      headers: { "content-type": "application/json", accept: "application/json" },
      body: JSON.stringify(body),
      signal: controller.signal,
    });
    const text = await response.text();
    if (!response.ok) throw new Error(`Solana RPC HTTP ${response.status}: ${text.slice(0, 200)}`);
    return text;
  } catch (error) {
    if (error?.name === "AbortError") throw new Error("Solana mainnet RPC timed out after 20 seconds.");
    throw error;
  } finally {
    clearTimeout(timeout);
  }
}

function spawnCapture(command, args) {
  return new Promise((resolve, reject) => {
    const child = spawn(command, args, { windowsHide: true, shell: false });
    let stdout = "";
    let stderr = "";
    child.stdout.on("data", (chunk) => { stdout += chunk.toString(); });
    child.stderr.on("data", (chunk) => { stderr += chunk.toString(); });
    child.on("error", reject);
    child.on("close", (code) => resolve({ code, stdout, stderr }));
  });
}

async function signerPubkey(keypairPath) {
  await access(keypairPath);
  const binary = process.env.SOLANA_KEYGEN_BIN?.trim() || "solana-keygen";
  const result = await spawnCapture(binary, ["pubkey", keypairPath]);
  if (result.code !== 0) throw new Error(`solana-keygen pubkey failed: ${result.stderr || result.stdout}`);
  return result.stdout.trim();
}

async function loadPayout(db, weekKey) {
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

async function readOnChainPreflight(rpcUrl, plan) {
  const [mintRaw, recipientRaw, fundingRaw] = await Promise.all([
    postRpc(rpcUrl, buildGetAccountInfoRequest(OFFICIAL_SKR_MINT)),
    postRpc(rpcUrl, buildGetAccountInfoRequest(plan.recipientWalletAddress)),
    postRpc(rpcUrl, buildGetTokenAccountsByOwnerRequest(plan.fundingWalletAddress)),
  ]);
  assertSkrMintProgram(mintRaw);
  assertRecipientSystemWallet(recipientRaw, plan.recipientWalletAddress);
  const accounts = parseTransferableTokenAccounts(fundingRaw, plan.fundingWalletAddress);
  const totalLiquid = accounts.reduce((sum, entry) => sum + BigInt(entry.amountAtomic), 0n);
  if (totalLiquid < BigInt(plan.remainingAmountAtomic)) {
    throw new Error("Funding wallet no longer contains enough liquid SKR to finish all remaining approved payouts.");
  }
  const source = selectSourceTokenAccount(accounts, plan.amountAtomic);
  return { ...source, totalLiquidAtomic: totalLiquid.toString() };
}

async function waitForFinalized(rpcUrl, signature, timeoutMs = 75_000) {
  const started = Date.now();
  while (Date.now() - started < timeoutMs) {
    const raw = await postRpc(rpcUrl, buildGetSignatureStatusesRequest(signature));
    const status = parseSignatureStatus(raw, signature);
    if (status.error) throw new Error(`Solana transaction failed: ${status.error}`);
    if (status.finalized) return status;
    await new Promise((resolve) => setTimeout(resolve, 2_000));
  }
  return null;
}

async function markReconciliationRequired(db, payoutRef, placement, executionId, message) {
  const itemRef = payoutRef.collection("items").doc(String(placement));
  await db.runTransaction(async (tx) => {
    const snap = await tx.get(itemRef);
    if (!snap.exists || String(snap.data().transferExecutionId ?? "") !== executionId) return;
    tx.update(itemRef, {
      transferStatus: "RECONCILIATION_REQUIRED",
      reconciliationReason: String(message).slice(0, 500),
      updatedAt: FieldValue.serverTimestamp(),
    });
  });
}

async function main() {
  const { values, flags } = readArgs(process.argv.slice(2));
  const projectId = values.project;
  const weekKey = values.week;
  const placement = Number(values.placement);
  if (!projectId || !weekKey || !Number.isInteger(placement)) throw new Error(`--project, --week, and --placement are required.\n${usage()}`);
  const apply = flags.has("apply");
  if (apply && values["confirm-project"] !== projectId) throw new Error("--apply requires --confirm-project to exactly match --project.");

  const rpcUrl = process.env.SOLANA_MAINNET_RPC_URL?.trim() || DEFAULT_MAINNET_RPC_URL;
  initializeApp({ credential: applicationDefault(), projectId });
  const db = getFirestore();
  const loaded = await loadPayout(db, weekKey);
  const plan = buildTransferItemPlan({ batch: loaded.batch, itemDocuments: loaded.items, weekKey, placement });
  const chain = await readOnChainPreflight(rpcUrl, plan);

  console.log("Radiant Circle Phase 12F trusted SKR transfer");
  console.log(`Project: ${projectId}`);
  console.log(`Mode: ${apply ? "APPLY — REAL MAINNET TRANSFER" : "DRY RUN"}`);
  console.log(`Cup: ${weekKey}`);
  console.log(`Placement: #${plan.placement}`);
  console.log(`Network: ${TRANSFER_NETWORK}`);
  console.log(`Funding wallet: ${plan.fundingWalletAddress}`);
  console.log(`Source token account: ${chain.sourceTokenAccount}`);
  console.log(`Winner wallet: ${plan.recipientWalletAddress}`);
  console.log(`Amount: ${plan.amountUi} SKR (${plan.amountAtomic} atomic)`);
  console.log(`Remaining approved payouts: ${plan.remainingAmountAtomic} atomic SKR`);
  console.log(`Current liquid funding: ${chain.totalLiquidAtomic} atomic SKR`);
  console.log(`Payout manifest SHA-256: ${plan.manifestDigestSha256}`);

  if (!apply) {
    console.log("\nDRY RUN ONLY — no Firestore transfer intent was created, no signer was opened, and no transaction was sent.");
    return;
  }

  if (String(values["confirm-digest"] ?? "").toLowerCase() !== plan.manifestDigestSha256) {
    throw new Error("--apply requires --confirm-digest to exactly match the reviewed manifest digest.");
  }
  if (values["confirm-funding-wallet"] !== plan.fundingWalletAddress) throw new Error("--confirm-funding-wallet mismatch.");
  if (values["confirm-recipient"] !== plan.recipientWalletAddress) throw new Error("--confirm-recipient mismatch.");
  if (String(values["confirm-amount-atomic"] ?? "") !== plan.amountAtomic) throw new Error("--confirm-amount-atomic mismatch.");

  const signerPath = process.env.RADIANT_PAYOUT_SIGNER_KEYPAIR?.trim();
  if (!signerPath) throw new Error("RADIANT_PAYOUT_SIGNER_KEYPAIR must point to the sponsor signer outside the repository.");
  const signer = await signerPubkey(signerPath);
  if (signer !== plan.fundingWalletAddress) {
    throw new Error(`Signer pubkey ${signer} does not match configured funding wallet ${plan.fundingWalletAddress}.`);
  }

  const executionId = randomUUID();
  const itemRef = loaded.payoutRef.collection("items").doc(String(placement));
  const startedEventRef = loaded.payoutRef.collection("events").doc(`transfer-started-${placement}-${executionId}`);

  await db.runTransaction(async (tx) => {
    const freshBatchSnap = await tx.get(loaded.payoutRef);
    const freshItemsQuery = await tx.get(loaded.payoutRef.collection("items").orderBy("placement"));
    const freshEvent = await tx.get(startedEventRef);
    if (freshEvent.exists) throw new Error(`Transfer-start audit for this execution already exists. Duplicate execution refused.`);
    const freshItems = freshItemsQuery.docs.map((doc) => ({ id: doc.id, data: doc.data() }));
    const freshPlan = buildTransferItemPlan({ batch: freshBatchSnap.exists ? freshBatchSnap.data() : null, itemDocuments: freshItems, weekKey, placement });
    if (freshPlan.manifestDigestSha256 !== plan.manifestDigestSha256 ||
        freshPlan.recipientWalletAddress !== plan.recipientWalletAddress ||
        freshPlan.amountAtomic !== plan.amountAtomic) {
      throw new Error("Approved payout manifest changed before transfer start. Nothing was sent.");
    }
    tx.update(loaded.payoutRef, {
      status: BATCH_STATUS_PAYMENT_PENDING,
      transferStatus: TRANSFER_STATUS_IN_PROGRESS,
      transferAuthority: TRANSFER_EXECUTION_AUTHORITY,
      payoutEnabled: false,
      transferEnabled: false,
      updatedAt: FieldValue.serverTimestamp(),
    });
    tx.update(itemRef, {
      status: ITEM_STATUS_PAYMENT_PENDING,
      transferStatus: TRANSFER_STATUS_SUBMITTING,
      transferAuthority: TRANSFER_EXECUTION_AUTHORITY,
      transferExecutionId: executionId,
      sourceTokenAccount: chain.sourceTokenAccount,
      transferNetwork: TRANSFER_NETWORK,
      transferCommitment: TRANSFER_COMMITMENT,
      transferStartedAt: FieldValue.serverTimestamp(),
      updatedAt: FieldValue.serverTimestamp(),
    });
    tx.create(startedEventRef, {
      schemaVersion: 1,
      weekKey,
      placement,
      eventType: TRANSFER_EVENT_STARTED,
      authority: TRANSFER_EXECUTION_AUTHORITY,
      transferExecutionId: executionId,
      payoutManifestDigestSha256: plan.manifestDigestSha256,
      fundingWalletAddress: plan.fundingWalletAddress,
      recipientWalletAddress: plan.recipientWalletAddress,
      amountAtomic: plan.amountAtomic,
      sourceTokenAccount: chain.sourceTokenAccount,
      network: TRANSFER_NETWORK,
      createdAt: FieldValue.serverTimestamp(),
    });
  });

  const splTokenBin = process.env.SPL_TOKEN_BIN?.trim() || "spl-token";
  const args = [
    "--url", rpcUrl,
    "transfer",
    OFFICIAL_SKR_MINT,
    plan.amountUi,
    plan.recipientWalletAddress,
    "--from", chain.sourceTokenAccount,
    "--owner", signerPath,
    "--fee-payer", signerPath,
    "--fund-recipient",
  ];

  console.log("\nSubmitting exactly one mainnet SKR transfer...");
  let signature;
  try {
    const result = await spawnCapture(splTokenBin, args);
    if (result.code !== 0) {
      throw new Error(`spl-token exited ${result.code}: ${(result.stderr || result.stdout).trim().slice(0, 1200)}`);
    }
    signature = parseSplTokenTransferSignature(result.stdout, result.stderr);
  } catch (error) {
    await markReconciliationRequired(db, loaded.payoutRef, placement, executionId, error?.message ?? error);
    throw new Error(
      `Transfer command did not produce safely recordable success. Item #${placement} is locked for reconciliation; DO NOT RETRY automatically. ${error?.message ?? error}`,
    );
  }

  const submittedEventRef = loaded.payoutRef.collection("events").doc(`transfer-submitted-${placement}-${executionId}`);
  await db.runTransaction(async (tx) => {
    const itemSnap = await tx.get(itemRef);
    if (!itemSnap.exists || String(itemSnap.data().transferExecutionId ?? "") !== executionId) {
      throw new Error("Transfer item execution identity changed before signature persistence.");
    }
    tx.update(itemRef, {
      transferStatus: TRANSFER_STATUS_SUBMITTED,
      transactionSignature: signature,
      transactionSubmittedAt: FieldValue.serverTimestamp(),
      updatedAt: FieldValue.serverTimestamp(),
    });
    tx.create(submittedEventRef, {
      schemaVersion: 1,
      weekKey,
      placement,
      eventType: TRANSFER_EVENT_SUBMITTED,
      authority: TRANSFER_EXECUTION_AUTHORITY,
      transferExecutionId: executionId,
      transactionSignature: signature,
      amountAtomic: plan.amountAtomic,
      recipientWalletAddress: plan.recipientWalletAddress,
      network: TRANSFER_NETWORK,
      createdAt: FieldValue.serverTimestamp(),
    });
  });

  console.log(`Transaction signature: ${signature}`);
  console.log("Waiting for finalized confirmation and exact on-chain balance deltas...");
  const status = await waitForFinalized(rpcUrl, signature);
  if (!status) {
    console.log("Transaction was submitted but did not reach finalized before timeout.");
    console.log("DO NOT resend. Use reconcile-weekly-cup-skr-transfer.mjs for this placement.");
    process.exitCode = 2;
    return;
  }

  let verification;
  try {
    const txRaw = await postRpc(rpcUrl, buildGetTransactionRequest(signature));
    verification = verifyFinalizedTransferTransaction(txRaw, plan, signature);
  } catch (error) {
    await markReconciliationRequired(db, loaded.payoutRef, placement, executionId, error?.message ?? error);
    throw new Error(`Transaction finalized but exact payout verification needs reconciliation. DO NOT resend. ${error?.message ?? error}`);
  }

  const finalizedEventRef = loaded.payoutRef.collection("events").doc(`transfer-finalized-${placement}`);
  await db.runTransaction(async (tx) => {
    const itemSnap = await tx.get(itemRef);
    if (!itemSnap.exists) throw new Error("Payout item disappeared before final confirmation.");
    const item = itemSnap.data();
    if (String(item.transferExecutionId ?? "") !== executionId || String(item.transactionSignature ?? "") !== signature) {
      throw new Error("Payout item transfer evidence changed before final confirmation.");
    }
    tx.update(itemRef, {
      status: ITEM_STATUS_PAID,
      transferStatus: TRANSFER_STATUS_FINALIZED,
      transferAuthority: TRANSFER_EXECUTION_AUTHORITY,
      transactionSlot: verification.slot,
      transactionBlockTime: verification.blockTime,
      paidAt: FieldValue.serverTimestamp(),
      updatedAt: FieldValue.serverTimestamp(),
    });
    tx.create(finalizedEventRef, {
      schemaVersion: 1,
      weekKey,
      placement,
      eventType: TRANSFER_EVENT_FINALIZED,
      authority: TRANSFER_EXECUTION_AUTHORITY,
      transferExecutionId: executionId,
      transactionSignature: signature,
      transactionSlot: verification.slot,
      amountAtomic: verification.amountAtomic,
      fundingWalletAddress: plan.fundingWalletAddress,
      recipientWalletAddress: plan.recipientWalletAddress,
      network: TRANSFER_NETWORK,
      commitment: TRANSFER_COMMITMENT,
      createdAt: FieldValue.serverTimestamp(),
    });
  });

  console.log(`\nPAID #${placement} — finalized exact SKR transfer recorded.`);
  console.log(`Signature: ${signature}`);
  console.log("Run the dry run again for the next placement. Do not batch or skip placement order.");
}

main().catch((error) => {
  console.error(`\nFATAL: ${error?.stack || error}`);
  process.exitCode = 1;
});
