#!/usr/bin/env node
import { randomUUID } from "node:crypto";
import { applicationDefault, initializeApp } from "firebase-admin/app";
import { FieldValue, getFirestore } from "firebase-admin/firestore";
import { DEFAULT_MAINNET_RPC_URL } from "./skr-funding-verification.mjs";
import {
  BATCH_STATUS_PAYMENT_PENDING,
  ITEM_STATUS_PAYMENT_PENDING,
  TRANSFER_EVENT_INTENT_CREATED,
  TRANSFER_EXECUTION_AUTHORITY,
  TRANSFER_NETWORK,
  TRANSFER_STATUS_AWAITING_EXTERNAL_SIGNATURE,
  TRANSFER_STATUS_IN_PROGRESS,
  assertRecipientSystemWallet,
  assertSkrMintProgram,
  buildGetAccountInfoRequest,
  buildGetTokenAccountsByOwnerRequest,
  buildTransferItemPlan,
  parseTransferableTokenAccounts,
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

async function postRpc(rpcUrl, body) {
  const response = await fetch(rpcUrl, {
    method: "POST",
    headers: { "content-type": "application/json", accept: "application/json" },
    body: JSON.stringify(body),
  });
  const text = await response.text();
  if (!response.ok) throw new Error(`Solana RPC HTTP ${response.status}: ${text.slice(0, 200)}`);
  return text;
}

async function main() {
  const { values, flags } = readArgs(process.argv.slice(2));
  const projectId = values.project;
  const weekKey = values.week;
  const placement = Number(values.placement);
  if (!projectId || !weekKey || !Number.isInteger(placement)) throw new Error("--project, --week, and --placement are required.");
  const apply = flags.has("apply");
  if (apply && values["confirm-project"] !== projectId) throw new Error("--apply requires --confirm-project to exactly match --project.");

  initializeApp({ credential: applicationDefault(), projectId });
  const db = getFirestore();
  const payoutRef = db.collection("weeklyCupPayouts").doc(weekKey);
  const [batchSnap, itemsSnap] = await Promise.all([
    payoutRef.get(),
    payoutRef.collection("items").orderBy("placement").get(),
  ]);
  const items = itemsSnap.docs.map((doc) => ({ id: doc.id, data: doc.data() }));
  const plan = buildTransferItemPlan({ batch: batchSnap.exists ? batchSnap.data() : null, itemDocuments: items, weekKey, placement });
  const rpcUrl = process.env.SOLANA_MAINNET_RPC_URL?.trim() || DEFAULT_MAINNET_RPC_URL;
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

  console.log("Radiant Circle Phase 12F external-wallet transfer intent");
  console.log(`Project: ${projectId}`);
  console.log(`Mode: ${apply ? "APPLY INTENT" : "DRY RUN"}`);
  console.log(`Cup: ${weekKey}`);
  console.log(`Placement: #${placement}`);
  console.log(`Funding wallet: ${plan.fundingWalletAddress}`);
  console.log(`Winner wallet: ${plan.recipientWalletAddress}`);
  console.log(`Amount: ${plan.amountUi} SKR (${plan.amountAtomic} atomic)`);
  console.log(`Official SKR mint: ${OFFICIAL_SKR_MINT}`);
  console.log(`Payout manifest SHA-256: ${plan.manifestDigestSha256}`);
  console.log(`Current liquid funding: ${totalLiquid} atomic SKR`);
  console.log("No private key is needed. This command does not send SKR.");

  if (!apply) {
    console.log("\nDRY RUN ONLY — review the exact wallet, amount, mint, and manifest digest before creating the intent.");
    return;
  }
  if (String(values["confirm-digest"] ?? "").toLowerCase() !== plan.manifestDigestSha256) throw new Error("--confirm-digest mismatch.");
  if (values["confirm-funding-wallet"] !== plan.fundingWalletAddress) throw new Error("--confirm-funding-wallet mismatch.");
  if (values["confirm-recipient"] !== plan.recipientWalletAddress) throw new Error("--confirm-recipient mismatch.");
  if (String(values["confirm-amount-atomic"] ?? "") !== plan.amountAtomic) throw new Error("--confirm-amount-atomic mismatch.");

  const executionId = randomUUID();
  const itemRef = payoutRef.collection("items").doc(String(placement));
  const eventRef = payoutRef.collection("events").doc(`transfer-intent-${placement}-${executionId}`);
  await db.runTransaction(async (tx) => {
    const freshBatch = await tx.get(payoutRef);
    const freshItems = await tx.get(payoutRef.collection("items").orderBy("placement"));
    const freshPlan = buildTransferItemPlan({
      batch: freshBatch.exists ? freshBatch.data() : null,
      itemDocuments: freshItems.docs.map((doc) => ({ id: doc.id, data: doc.data() })),
      weekKey,
      placement,
    });
    if (freshPlan.manifestDigestSha256 !== plan.manifestDigestSha256 || freshPlan.amountAtomic !== plan.amountAtomic || freshPlan.recipientWalletAddress !== plan.recipientWalletAddress) {
      throw new Error("Approved payout manifest changed before intent creation.");
    }
    tx.update(payoutRef, {
      status: BATCH_STATUS_PAYMENT_PENDING,
      transferStatus: TRANSFER_STATUS_IN_PROGRESS,
      transferAuthority: TRANSFER_EXECUTION_AUTHORITY,
      payoutEnabled: false,
      transferEnabled: false,
      updatedAt: FieldValue.serverTimestamp(),
    });
    tx.update(itemRef, {
      status: ITEM_STATUS_PAYMENT_PENDING,
      transferStatus: TRANSFER_STATUS_AWAITING_EXTERNAL_SIGNATURE,
      transferAuthority: TRANSFER_EXECUTION_AUTHORITY,
      transferExecutionId: executionId,
      transferNetwork: TRANSFER_NETWORK,
      transferStartedAt: FieldValue.serverTimestamp(),
      updatedAt: FieldValue.serverTimestamp(),
    });
    tx.create(eventRef, {
      schemaVersion: 1,
      weekKey,
      placement,
      eventType: TRANSFER_EVENT_INTENT_CREATED,
      authority: TRANSFER_EXECUTION_AUTHORITY,
      transferExecutionId: executionId,
      payoutManifestDigestSha256: plan.manifestDigestSha256,
      fundingWalletAddress: plan.fundingWalletAddress,
      recipientWalletAddress: plan.recipientWalletAddress,
      amountAtomic: plan.amountAtomic,
      mint: OFFICIAL_SKR_MINT,
      network: TRANSFER_NETWORK,
      createdAt: FieldValue.serverTimestamp(),
    });
  });

  console.log("\nINTENT CREATED — this payout item is now locked against duplicate automatic execution.");
  console.log("Use the configured funding wallet (for example Seed Vault Wallet) to send EXACTLY the amount above to the exact winner wallet using the official SKR mint.");
  console.log("Then capture the real Solana transaction signature and run reconcile-weekly-cup-skr-transfer.mjs --signature <TX_SIGNATURE>.");
  console.log("Do not create a second transfer if the first transaction is pending or its outcome is uncertain.");
}

main().catch((error) => {
  console.error(`\nFATAL: ${error?.stack || error}`);
  process.exitCode = 1;
});
