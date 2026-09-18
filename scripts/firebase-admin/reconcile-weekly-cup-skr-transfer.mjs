#!/usr/bin/env node
import { applicationDefault, initializeApp } from "firebase-admin/app";
import { FieldValue, getFirestore } from "firebase-admin/firestore";
import { PAYOUT_ITEM_STATUS_APPROVED, TRANSFER_STATUS_NOT_STARTED } from "./weekly-cup-payout-lifecycle.mjs";
import { DEFAULT_MAINNET_RPC_URL } from "./skr-funding-verification.mjs";
import {
  ITEM_STATUS_PAID,
  TRANSFER_COMMITMENT,
  TRANSFER_EVENT_FINALIZED,
  TRANSFER_EVENT_FAILED,
  TRANSFER_EXECUTION_AUTHORITY,
  TRANSFER_NETWORK,
  TRANSFER_STATUS_FINALIZED,
  TRANSFER_STATUS_AWAITING_EXTERNAL_SIGNATURE,
  TRANSFER_STATUS_RECONCILIATION_REQUIRED,
  TRANSFER_STATUS_SUBMITTED,
  buildGetSignatureStatusesRequest,
  buildGetTransactionRequest,
  parseSignatureStatus,
  validateTransferManifest,
  verifyFinalizedTransferTransaction,
} from "./weekly-cup-skr-transfer.mjs";

function readArgs(argv) {
  const values = {};
  const flags = new Set();
  for (let i = 0; i < argv.length; i += 1) {
    const token = argv[i];
    if (!token.startsWith("--")) throw new Error(`Unexpected argument: ${token}`);
    const key = token.slice(2);
    if (["apply", "reset-failed"].includes(key)) { flags.add(key); continue; }
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
  const manifest = validateTransferManifest({ batch: batchSnap.exists ? batchSnap.data() : null, itemDocuments: items, weekKey });
  const item = manifest.items.find((entry) => Number(entry.placement) === placement);
  if (!item) throw new Error(`Payout item #${placement} does not exist.`);
  if (String(item.status ?? "").toUpperCase() === ITEM_STATUS_PAID && String(item.transferStatus ?? "").toUpperCase() === TRANSFER_STATUS_FINALIZED) {
    console.log(`Payout item #${placement} is already PAID and FINALIZED. Nothing to reconcile.`);
    return;
  }

  const storedSignature = String(item.transactionSignature ?? "").trim();
  const signature = String(values.signature ?? storedSignature).trim();
  if (!signature) {
    throw new Error(
      `Payout item #${placement} has no recorded transaction signature. Automatic retry is intentionally blocked. ` +
      "Find the original transaction through the signer wallet/activity history, then rerun with --signature so this tool can verify it.",
    );
  }
  if (storedSignature && values.signature && values.signature !== storedSignature) {
    throw new Error("Provided --signature does not match the already recorded transaction signature.");
  }

  const rpcUrl = process.env.SOLANA_MAINNET_RPC_URL?.trim() || DEFAULT_MAINNET_RPC_URL;
  const status = parseSignatureStatus(await postRpc(rpcUrl, buildGetSignatureStatusesRequest(signature)), signature);
  if (!status.found) throw new Error("Transaction signature was not found in Solana history yet. Do not resend automatically.");
  if (status.error) {
    console.log("Radiant Circle Phase 12F transfer reconciliation");
    console.log(`Cup: ${weekKey}`);
    console.log(`Placement: #${placement}`);
    console.log(`Signature: ${signature}`);
    console.log(`On-chain result: FAILED (${status.error})`);
    console.log("Because the transaction failed atomically, no SKR transfer from this transaction succeeded.");
    if (!flags.has("reset-failed")) {
      throw new Error("Failed transaction is safely retryable only after an explicit reset. Rerun with --reset-failed; add --apply --confirm-project only after reviewing the same failed signature.");
    }
    if (!apply) {
      console.log("DRY RUN ONLY — --reset-failed would return this item to APPROVED/NOT_STARTED for a new execution attempt.");
      return;
    }
    const itemRef = payoutRef.collection("items").doc(String(placement));
    const currentExecutionId = String(item.transferExecutionId ?? "unknown");
    const failedEventRef = payoutRef.collection("events").doc(`transfer-failed-${placement}-${currentExecutionId}`);
    await db.runTransaction(async (tx) => {
      const freshItem = await tx.get(itemRef);
      const existingEvent = await tx.get(failedEventRef);
      if (!freshItem.exists) throw new Error("Payout item disappeared during failed-transfer reset.");
      if (existingEvent.exists) throw new Error("Failed-transfer reset audit already exists.");
      const current = freshItem.data();
      const currentSig = String(current.transactionSignature ?? "").trim();
      if (currentSig && currentSig !== signature) throw new Error("Stored signature changed before failed-transfer reset.");
      tx.update(itemRef, {
        status: PAYOUT_ITEM_STATUS_APPROVED,
        transferStatus: TRANSFER_STATUS_NOT_STARTED,
        transferExecutionId: FieldValue.delete(),
        transferAuthority: FieldValue.delete(),
        sourceTokenAccount: FieldValue.delete(),
        transferNetwork: FieldValue.delete(),
        transferCommitment: FieldValue.delete(),
        transferStartedAt: FieldValue.delete(),
        transactionSignature: FieldValue.delete(),
        transactionSubmittedAt: FieldValue.delete(),
        reconciliationReason: FieldValue.delete(),
        updatedAt: FieldValue.serverTimestamp(),
      });
      tx.create(failedEventRef, {
        schemaVersion: 1,
        weekKey,
        placement,
        eventType: TRANSFER_EVENT_FAILED,
        authority: TRANSFER_EXECUTION_AUTHORITY,
        transferExecutionId: currentExecutionId,
        transactionSignature: signature,
        onChainError: status.error,
        resetForRetry: true,
        createdAt: FieldValue.serverTimestamp(),
      });
    });
    console.log(`APPLY COMPLETE — failed payout item #${placement} reset to APPROVED/NOT_STARTED for a fresh reviewed attempt.`);
    return;
  }
  if (!status.finalized) throw new Error("Transaction is not finalized yet. Wait and rerun reconciliation; do not resend.");

  const plan = {
    amountAtomic: String(item.amountAtomic),
    fundingWalletAddress: manifest.fundingWalletAddress,
    recipientWalletAddress: String(item.walletAddress),
  };
  const verification = verifyFinalizedTransferTransaction(
    await postRpc(rpcUrl, buildGetTransactionRequest(signature)),
    plan,
    signature,
  );

  console.log("Radiant Circle Phase 12F transfer reconciliation");
  console.log(`Cup: ${weekKey}`);
  console.log(`Placement: #${placement}`);
  console.log(`Signature: ${signature}`);
  console.log(`Finalized slot: ${verification.slot}`);
  console.log(`Exact SKR amount verified: ${verification.amountAtomic}`);
  console.log(`Mode: ${apply ? "APPLY" : "DRY RUN"}`);
  if (!apply) {
    console.log("DRY RUN ONLY — Firestore payout state was not changed.");
    return;
  }

  const itemRef = payoutRef.collection("items").doc(String(placement));
  const finalizedEventRef = payoutRef.collection("events").doc(`transfer-finalized-${placement}`);
  await db.runTransaction(async (tx) => {
    const [freshItem, existingEvent] = await Promise.all([tx.get(itemRef), tx.get(finalizedEventRef)]);
    if (!freshItem.exists) throw new Error("Payout item disappeared during reconciliation.");
    if (existingEvent.exists) throw new Error("Finalized transfer event already exists; duplicate reconciliation refused.");
    const current = freshItem.data();
    const transferStatus = String(current.transferStatus ?? "").toUpperCase();
    if (![TRANSFER_STATUS_SUBMITTED, TRANSFER_STATUS_RECONCILIATION_REQUIRED, "SUBMITTING", TRANSFER_STATUS_AWAITING_EXTERNAL_SIGNATURE].includes(transferStatus)) {
      throw new Error(`Payout item cannot be reconciled from transferStatus=${transferStatus}.`);
    }
    const currentSignature = String(current.transactionSignature ?? "").trim();
    if (currentSignature && currentSignature !== signature) throw new Error("Stored transaction signature changed during reconciliation.");

    tx.update(itemRef, {
      status: ITEM_STATUS_PAID,
      transferStatus: TRANSFER_STATUS_FINALIZED,
      transferAuthority: TRANSFER_EXECUTION_AUTHORITY,
      transactionSignature: signature,
      transactionSlot: verification.slot,
      transactionBlockTime: verification.blockTime,
      paidAt: FieldValue.serverTimestamp(),
      reconciliationReason: FieldValue.delete(),
      updatedAt: FieldValue.serverTimestamp(),
    });
    tx.create(finalizedEventRef, {
      schemaVersion: 1,
      weekKey,
      placement,
      eventType: TRANSFER_EVENT_FINALIZED,
      authority: TRANSFER_EXECUTION_AUTHORITY,
      transferExecutionId: String(current.transferExecutionId ?? "manual-reconciliation"),
      transactionSignature: signature,
      transactionSlot: verification.slot,
      amountAtomic: verification.amountAtomic,
      fundingWalletAddress: manifest.fundingWalletAddress,
      recipientWalletAddress: String(current.walletAddress),
      network: TRANSFER_NETWORK,
      commitment: TRANSFER_COMMITMENT,
      reconciled: true,
      createdAt: FieldValue.serverTimestamp(),
    });
  });
  console.log(`APPLY COMPLETE — payout item #${placement} marked PAID from verified finalized transaction.`);
}

main().catch((error) => {
  console.error(`\nFATAL: ${error?.stack || error}`);
  process.exitCode = 1;
});
