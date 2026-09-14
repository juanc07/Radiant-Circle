import { createHash } from "node:crypto";
import {
  OFFICIAL_SKR_MINT,
  SKR_DECIMALS,
} from "./weekly-cup-config.mjs";
import {
  PAYOUT_AUTHORITY,
  PAYOUT_ITEM_STATUS_APPROVED,
  PAYOUT_LIFECYCLE_VERSION,
  PAYOUT_SCHEMA_VERSION,
  PAYOUT_STATUS_APPROVED,
  TRANSFER_STATUS_NOT_STARTED,
} from "./weekly-cup-payout-lifecycle.mjs";

export const TRANSFER_EXECUTION_SCHEMA_VERSION = 1;
export const TRANSFER_EXECUTION_AUTHORITY = "trusted-admin-phase12f";
export const TRANSFER_NETWORK = "mainnet-beta";
export const TRANSFER_COMMITMENT = "finalized";
export const TOKEN_PROGRAM_ID = "TokenkegQfeZyiNwAJbNbGKPFXCWuBvf9Ss623VQ5DA";
export const SYSTEM_PROGRAM_ID = "11111111111111111111111111111111";
export const BATCH_STATUS_PAYMENT_PENDING = "PAYMENT_PENDING";
export const BATCH_STATUS_PAID = "PAID";
export const ITEM_STATUS_PAYMENT_PENDING = "PAYMENT_PENDING";
export const ITEM_STATUS_PAID = "PAID";
export const TRANSFER_STATUS_IN_PROGRESS = "IN_PROGRESS";
export const TRANSFER_STATUS_SUBMITTING = "SUBMITTING";
export const TRANSFER_STATUS_AWAITING_EXTERNAL_SIGNATURE = "AWAITING_EXTERNAL_SIGNATURE";
export const TRANSFER_STATUS_SUBMITTED = "SUBMITTED";
export const TRANSFER_STATUS_FINALIZED = "FINALIZED";
export const TRANSFER_STATUS_RECONCILIATION_REQUIRED = "RECONCILIATION_REQUIRED";
export const TRANSFER_EVENT_STARTED = "TRANSFER_STARTED";
export const TRANSFER_EVENT_INTENT_CREATED = "TRANSFER_INTENT_CREATED";
export const TRANSFER_EVENT_SUBMITTED = "TRANSFER_SUBMITTED";
export const TRANSFER_EVENT_FINALIZED = "TRANSFER_FINALIZED";
export const TRANSFER_EVENT_FAILED = "TRANSFER_FAILED";
export const TRANSFER_EVENT_PAYOUT_PAID = "PAYOUT_PAID";

const SOLANA_ADDRESS = /^[1-9A-HJ-NP-Za-km-z]{32,44}$/;
const ATOMIC = /^[0-9]{1,30}$/;
const SHA256 = /^[0-9a-f]{64}$/;
const SIGNATURE = /^[1-9A-HJ-NP-Za-km-z]{64,100}$/;

function requiredString(value, label, max = 240) {
  const clean = String(value ?? "").trim();
  if (!clean || clean.length > max) throw new Error(`${label} is required and must be at most ${max} characters.`);
  return clean;
}

export function canonicalWallet(value, label = "wallet") {
  const clean = String(value ?? "").trim();
  if (!SOLANA_ADDRESS.test(clean)) throw new Error(`${label} must be a Solana public address.`);
  return clean;
}

export function atomic(value, label = "atomic amount") {
  const raw = String(value ?? "").trim();
  if (!ATOMIC.test(raw)) throw new Error(`${label} must be a decimal-free non-negative atomic amount string.`);
  return BigInt(raw);
}

function digestLines(lines) {
  return createHash("sha256").update(lines.join("\n"), "utf8").digest("hex");
}

function normalizeItem(doc) {
  return doc?.data ? { id: String(doc.id ?? doc.data?.placement ?? ""), data: doc.data } : {
    id: String(doc?.id ?? doc?.placement ?? ""),
    data: doc,
  };
}

export function computePayoutManifestDigest(batch, itemDocuments, weekKey) {
  const cleanWeek = requiredString(weekKey, "weekKey", 16);
  if (!batch) throw new Error("Payout batch does not exist.");
  const total = atomic(batch.totalAmountAtomic, "Payout batch totalAmountAtomic");
  const rankingDigest = String(batch.sourceRankingDigestSha256 ?? "").trim().toLowerCase();
  if (!SHA256.test(rankingDigest)) throw new Error("Payout batch source ranking digest is invalid.");
  const fundingWallet = canonicalWallet(batch.fundingWalletAddress, "Payout batch fundingWalletAddress");
  const items = [...(itemDocuments ?? [])]
    .map(normalizeItem)
    .map((entry) => entry.data)
    .sort((a, b) => Number(a?.placement) - Number(b?.placement));
  return digestLines([
    `week=${cleanWeek}`,
    `mint=${OFFICIAL_SKR_MINT}`,
    `decimals=${SKR_DECIMALS}`,
    `total=${total}`,
    `fundingWallet=${fundingWallet}`,
    `rankingDigest=${rankingDigest}`,
    ...items.map((item) => [
      Number(item?.placement),
      String(item?.walletAddress ?? "").trim(),
      String(item?.receiptId ?? "").trim(),
      String(item?.amountAtomic ?? "").trim(),
    ].join("|")),
  ]);
}

export function validateTransferManifest({ batch, itemDocuments, weekKey }) {
  const cleanWeek = requiredString(weekKey, "weekKey", 16);
  if (!batch) throw new Error("Payout batch does not exist.");
  if (Number(batch.schemaVersion) !== PAYOUT_SCHEMA_VERSION || Number(batch.lifecycleVersion) !== PAYOUT_LIFECYCLE_VERSION) {
    throw new Error("Payout batch schema/version is invalid.");
  }
  if (String(batch.weekKey ?? "").trim() !== cleanWeek) throw new Error("Payout batch weekKey mismatch.");
  if (String(batch.authority ?? "") !== PAYOUT_AUTHORITY) throw new Error("Payout batch authority is not trusted Phase 12E.");
  const batchStatus = String(batch.status ?? "").toUpperCase();
  if (![PAYOUT_STATUS_APPROVED, BATCH_STATUS_PAYMENT_PENDING, BATCH_STATUS_PAID].includes(batchStatus)) {
    throw new Error("Phase 12F requires an APPROVED, PAYMENT_PENDING, or PAID payout batch.");
  }
  if (batch.payoutEnabled === true || batch.transferEnabled === true) {
    throw new Error("Client/automatic payout flags must remain disabled during Phase 12F.");
  }
  if (String(batch.fundingVerificationStatusAtClose ?? "").toUpperCase() !== "VERIFIED") {
    throw new Error("Phase 12F requires funding VERIFIED at Cup close.");
  }
  if (String(batch.prizeAssetSymbol ?? "").toUpperCase() !== "SKR" ||
      String(batch.prizeMint ?? "") !== OFFICIAL_SKR_MINT ||
      Number(batch.prizeDecimals) !== SKR_DECIMALS) {
    throw new Error("Payout batch SKR identity is invalid.");
  }
  const expectedDigest = String(batch.payoutManifestDigestSha256 ?? "").trim().toLowerCase();
  if (!SHA256.test(expectedDigest)) throw new Error("Payout batch manifest digest is invalid.");
  const recomputed = computePayoutManifestDigest(batch, itemDocuments, cleanWeek);
  if (recomputed !== expectedDigest) throw new Error("Payout manifest digest mismatch. Refusing transfer.");

  const items = [...(itemDocuments ?? [])]
    .map(normalizeItem)
    .map((entry) => entry.data)
    .sort((a, b) => Number(a?.placement) - Number(b?.placement));
  if (items.length !== Number(batch.winnerCount) || items.length === 0) {
    throw new Error("Payout item count does not match batch winnerCount.");
  }

  const seenPlacements = new Set();
  const seenWallets = new Set();
  let total = 0n;
  for (const item of items) {
    const placement = Number(item?.placement);
    if (!Number.isInteger(placement) || placement < 1 || placement > 10 || seenPlacements.has(placement)) {
      throw new Error("Payout items contain invalid or duplicate placements.");
    }
    seenPlacements.add(placement);
    if (Number(item.schemaVersion) !== PAYOUT_SCHEMA_VERSION || Number(item.lifecycleVersion) !== PAYOUT_LIFECYCLE_VERSION) {
      throw new Error(`Payout item #${placement} schema/version is invalid.`);
    }
    if (String(item.weekKey ?? "").trim() !== cleanWeek || String(item.authority ?? "") !== PAYOUT_AUTHORITY) {
      throw new Error(`Payout item #${placement} authority/week mismatch.`);
    }
    const wallet = canonicalWallet(item.walletAddress, `Payout item #${placement} walletAddress`);
    if (seenWallets.has(wallet)) throw new Error("Payout items must use distinct full wallet addresses.");
    seenWallets.add(wallet);
    requiredString(item.receiptId, `Payout item #${placement} receiptId`, 100);
    if (String(item.prizeAssetSymbol ?? "").toUpperCase() !== "SKR") {
      throw new Error(`Payout item #${placement} is not SKR.`);
    }
    total += atomic(item.amountAtomic, `Payout item #${placement} amountAtomic`);
  }
  if (total !== atomic(batch.totalAmountAtomic, "Payout batch totalAmountAtomic")) {
    throw new Error("Payout item amounts do not sum to the batch total.");
  }

  return {
    weekKey: cleanWeek,
    manifestDigestSha256: expectedDigest,
    fundingWalletAddress: canonicalWallet(batch.fundingWalletAddress, "Payout batch fundingWalletAddress"),
    totalAmountAtomic: total.toString(),
    batchStatus,
    items,
  };
}

export function buildTransferItemPlan({ batch, itemDocuments, weekKey, placement }) {
  const manifest = validateTransferManifest({ batch, itemDocuments, weekKey });
  if (manifest.batchStatus === BATCH_STATUS_PAID) throw new Error("Payout batch is already PAID.");
  const requestedPlacement = Number(placement);
  if (!Number.isInteger(requestedPlacement) || requestedPlacement < 1 || requestedPlacement > 10) {
    throw new Error("--placement must be an integer from 1 to 10.");
  }

  const unpaid = manifest.items.filter((item) => String(item.status ?? "").toUpperCase() !== ITEM_STATUS_PAID);
  if (unpaid.length === 0) throw new Error("All payout items are already PAID.");
  const nextPlacement = Math.min(...unpaid.map((item) => Number(item.placement)));
  if (requestedPlacement !== nextPlacement) {
    throw new Error(`Transfers must execute in placement order. Next unpaid placement is #${nextPlacement}.`);
  }
  const item = manifest.items.find((entry) => Number(entry.placement) === requestedPlacement);
  if (!item) throw new Error(`Payout item #${requestedPlacement} does not exist.`);
  if (String(item.status ?? "").toUpperCase() !== PAYOUT_ITEM_STATUS_APPROVED ||
      String(item.transferStatus ?? "").toUpperCase() !== TRANSFER_STATUS_NOT_STARTED ||
      item.payoutEnabled === true || item.transferEnabled === true) {
    throw new Error(`Payout item #${requestedPlacement} is not in pristine APPROVED/NOT_STARTED state.`);
  }
  if (item.transactionSignature || item.transferExecutionId) {
    throw new Error(`Payout item #${requestedPlacement} already contains transfer execution evidence.`);
  }

  const remainingAtomic = unpaid.reduce((sum, entry) => sum + atomic(entry.amountAtomic, `Payout item #${entry.placement} amountAtomic`), 0n);
  return {
    schemaVersion: TRANSFER_EXECUTION_SCHEMA_VERSION,
    executionAuthority: TRANSFER_EXECUTION_AUTHORITY,
    weekKey: manifest.weekKey,
    placement: requestedPlacement,
    fundingWalletAddress: manifest.fundingWalletAddress,
    recipientWalletAddress: canonicalWallet(item.walletAddress, `Payout item #${requestedPlacement} walletAddress`),
    amountAtomic: atomic(item.amountAtomic, `Payout item #${requestedPlacement} amountAtomic`).toString(),
    amountUi: atomicToUiString(item.amountAtomic),
    remainingAmountAtomic: remainingAtomic.toString(),
    manifestDigestSha256: manifest.manifestDigestSha256,
    mint: OFFICIAL_SKR_MINT,
    decimals: SKR_DECIMALS,
    network: TRANSFER_NETWORK,
  };
}

export function atomicToUiString(value) {
  const amount = atomic(value, "SKR amount");
  const divisor = 10n ** BigInt(SKR_DECIMALS);
  const whole = amount / divisor;
  const fraction = (amount % divisor).toString().padStart(SKR_DECIMALS, "0").replace(/0+$/, "");
  return `${whole}${fraction ? `.${fraction}` : ""}`;
}

export function buildGetTokenAccountsByOwnerRequest(walletAddress) {
  return {
    jsonrpc: "2.0",
    id: 1,
    method: "getTokenAccountsByOwner",
    params: [
      canonicalWallet(walletAddress),
      { mint: OFFICIAL_SKR_MINT },
      { encoding: "jsonParsed", commitment: TRANSFER_COMMITMENT },
    ],
  };
}

export function parseTransferableTokenAccounts(payload, expectedOwner) {
  const root = typeof payload === "string" ? JSON.parse(payload) : payload;
  if (root?.error) throw new Error(`Solana RPC error: ${root.error?.message ?? "unknown error"}`);
  if (!Array.isArray(root?.result?.value)) throw new Error("Solana RPC token-account response is invalid.");
  const owner = canonicalWallet(expectedOwner, "expected token owner");
  const accounts = [];
  for (const entry of root.result.value) {
    const pubkey = canonicalWallet(entry?.pubkey, "token account");
    const info = entry?.account?.data?.parsed?.info;
    const tokenAmount = info?.tokenAmount;
    if (!info || !tokenAmount) throw new Error("SKR token account was not jsonParsed as expected.");
    if (String(info.owner ?? "") !== owner) throw new Error("RPC returned an SKR token account owned by a different wallet.");
    if (String(info.mint ?? "") !== OFFICIAL_SKR_MINT) throw new Error("RPC returned an unexpected mint.");
    if (Number(tokenAmount.decimals) !== SKR_DECIMALS) throw new Error("RPC returned unexpected SKR decimals.");
    const state = String(info.state ?? "").toLowerCase();
    if (state === "frozen") continue;
    if (state && state !== "initialized") throw new Error(`Unsupported SKR token-account state=${state}.`);
    accounts.push({ pubkey, amountAtomic: atomic(tokenAmount.amount ?? "0", "token balance") });
  }
  accounts.sort((a, b) => a.amountAtomic === b.amountAtomic ? a.pubkey.localeCompare(b.pubkey) : (a.amountAtomic > b.amountAtomic ? -1 : 1));
  return accounts;
}

export function selectSourceTokenAccount(accounts, requiredAmountAtomic) {
  const required = atomic(requiredAmountAtomic, "required transfer amount");
  const total = (accounts ?? []).reduce((sum, item) => sum + BigInt(item.amountAtomic), 0n);
  if (total < required) throw new Error("Funding wallet no longer has enough liquid SKR for this transfer.");
  const selected = (accounts ?? []).find((item) => BigInt(item.amountAtomic) >= required);
  if (!selected) {
    throw new Error("Liquid SKR is split across token accounts; consolidate into one source token account before payout.");
  }
  return {
    sourceTokenAccount: selected.pubkey,
    sourceBalanceAtomic: selected.amountAtomic.toString(),
    totalLiquidAtomic: total.toString(),
  };
}

export function buildGetAccountInfoRequest(address) {
  return {
    jsonrpc: "2.0",
    id: 1,
    method: "getAccountInfo",
    params: [canonicalWallet(address, "account"), { encoding: "jsonParsed", commitment: TRANSFER_COMMITMENT }],
  };
}

export function assertRecipientSystemWallet(payload, expectedWallet) {
  const root = typeof payload === "string" ? JSON.parse(payload) : payload;
  if (root?.error) throw new Error(`Solana RPC error: ${root.error?.message ?? "unknown error"}`);
  const value = root?.result?.value;
  const wallet = canonicalWallet(expectedWallet, "recipient wallet");
  if (!value) {
    throw new Error(`Recipient ${wallet} has no funded mainnet account. Refusing automatic payout; review the wallet manually.`);
  }
  if (value.executable === true || String(value.owner ?? "") !== SYSTEM_PROGRAM_ID) {
    throw new Error(`Recipient ${wallet} is not a standard System Program wallet. Refusing automatic payout.`);
  }
  return wallet;
}

export function assertSkrMintProgram(payload) {
  const root = typeof payload === "string" ? JSON.parse(payload) : payload;
  if (root?.error) throw new Error(`Solana RPC error: ${root.error?.message ?? "unknown error"}`);
  const value = root?.result?.value;
  if (!value) throw new Error("Official SKR mint account was not found on mainnet.");
  if (value.executable === true || String(value.owner ?? "") !== TOKEN_PROGRAM_ID) {
    throw new Error("Official SKR mint is not owned by the expected SPL Token Program.");
  }
}

export function buildGetSignatureStatusesRequest(signature) {
  const sig = requiredString(signature, "transaction signature", 110);
  if (!SIGNATURE.test(sig)) throw new Error("transaction signature is not valid base58.");
  return {
    jsonrpc: "2.0",
    id: 1,
    method: "getSignatureStatuses",
    params: [[sig], { searchTransactionHistory: true }],
  };
}

export function parseSignatureStatus(payload, signature) {
  const root = typeof payload === "string" ? JSON.parse(payload) : payload;
  if (root?.error) throw new Error(`Solana RPC error: ${root.error?.message ?? "unknown error"}`);
  const value = root?.result?.value?.[0] ?? null;
  if (!value) return { found: false, finalized: false, error: null };
  if (value.err != null) return { found: true, finalized: false, error: JSON.stringify(value.err) };
  return {
    found: true,
    finalized: String(value.confirmationStatus ?? "").toLowerCase() === "finalized",
    error: null,
    slot: Number(value.slot),
  };
}

export function buildGetTransactionRequest(signature) {
  const sig = requiredString(signature, "transaction signature", 110);
  if (!SIGNATURE.test(sig)) throw new Error("transaction signature is not valid base58.");
  return {
    jsonrpc: "2.0",
    id: 1,
    method: "getTransaction",
    params: [sig, { encoding: "jsonParsed", commitment: TRANSFER_COMMITMENT, maxSupportedTransactionVersion: 0 }],
  };
}

function tokenOwnerAmountMap(entries, mint) {
  const map = new Map();
  for (const entry of entries ?? []) {
    if (String(entry?.mint ?? "") !== mint) continue;
    const owner = String(entry?.owner ?? "").trim();
    if (!owner) continue;
    const amount = atomic(entry?.uiTokenAmount?.amount ?? "0", "transaction token amount");
    map.set(owner, (map.get(owner) ?? 0n) + amount);
  }
  return map;
}

export function verifyFinalizedTransferTransaction(payload, plan, expectedSignature) {
  const root = typeof payload === "string" ? JSON.parse(payload) : payload;
  if (root?.error) throw new Error(`Solana RPC error: ${root.error?.message ?? "unknown error"}`);
  const tx = root?.result;
  if (!tx) throw new Error("Finalized transaction is not available from RPC yet.");
  if (tx?.meta?.err != null) throw new Error(`Transfer transaction failed on-chain: ${JSON.stringify(tx.meta.err)}`);
  const signatures = tx?.transaction?.signatures ?? [];
  if (!signatures.includes(expectedSignature)) throw new Error("RPC transaction does not contain the expected signature.");
  const pre = tokenOwnerAmountMap(tx?.meta?.preTokenBalances, OFFICIAL_SKR_MINT);
  const post = tokenOwnerAmountMap(tx?.meta?.postTokenBalances, OFFICIAL_SKR_MINT);
  const amount = atomic(plan.amountAtomic, "transfer amount");
  const sourceBefore = pre.get(plan.fundingWalletAddress) ?? 0n;
  const sourceAfter = post.get(plan.fundingWalletAddress) ?? 0n;
  const recipientBefore = pre.get(plan.recipientWalletAddress) ?? 0n;
  const recipientAfter = post.get(plan.recipientWalletAddress) ?? 0n;
  if (sourceBefore - sourceAfter !== amount) {
    throw new Error("Finalized transaction does not debit the exact expected SKR amount from the funding wallet.");
  }
  if (recipientAfter - recipientBefore !== amount) {
    throw new Error("Finalized transaction does not credit the exact expected SKR amount to the winner wallet.");
  }
  const slot = Number(tx.slot);
  if (!Number.isSafeInteger(slot) || slot <= 0) throw new Error("Finalized transaction has no valid slot.");
  return {
    signature: expectedSignature,
    slot,
    blockTime: Number.isSafeInteger(Number(tx.blockTime)) ? Number(tx.blockTime) : null,
    amountAtomic: amount.toString(),
  };
}

export function parseSplTokenTransferSignature(stdout, stderr = "") {
  const combined = `${stdout ?? ""}\n${stderr ?? ""}`.trim();
  if (!combined) throw new Error("spl-token returned no output; transaction signature is unknown.");
  try {
    const parsed = JSON.parse(combined);
    for (const key of ["signature", "transactionSignature", "transaction_signature"]) {
      const candidate = String(parsed?.[key] ?? "").trim();
      if (SIGNATURE.test(candidate)) return candidate;
    }
  } catch {
    // Human-readable spl-token output is expected on many CLI versions.
  }
  const match = combined.match(/Signature:\s*([1-9A-HJ-NP-Za-km-z]{64,100})/i);
  if (!match || !SIGNATURE.test(match[1])) {
    throw new Error("Could not parse a Solana transaction signature from spl-token output.");
  }
  return match[1];
}

export function buildCompletionPlan({ batch, itemDocuments, weekKey }) {
  const manifest = validateTransferManifest({ batch, itemDocuments, weekKey });
  if (manifest.batchStatus !== BATCH_STATUS_PAYMENT_PENDING) {
    throw new Error("Payout completion requires batch status PAYMENT_PENDING.");
  }
  const signatures = new Set();
  for (const item of manifest.items) {
    const placement = Number(item.placement);
    if (String(item.status ?? "").toUpperCase() !== ITEM_STATUS_PAID ||
        String(item.transferStatus ?? "").toUpperCase() !== TRANSFER_STATUS_FINALIZED ||
        String(item.transferAuthority ?? "") !== TRANSFER_EXECUTION_AUTHORITY) {
      throw new Error(`Payout item #${placement} is not finalized PAID.`);
    }
    const signature = String(item.transactionSignature ?? "").trim();
    if (!SIGNATURE.test(signature)) throw new Error(`Payout item #${placement} has no valid transaction signature.`);
    if (signatures.has(signature)) throw new Error("Payout items must not reuse a transaction signature.");
    signatures.add(signature);
  }
  return {
    manifestDigestSha256: manifest.manifestDigestSha256,
    winnerCount: manifest.items.length,
    transactionSignatures: [...signatures],
    batchPatch: {
      status: BATCH_STATUS_PAID,
      transferStatus: TRANSFER_STATUS_FINALIZED,
      transferAuthority: TRANSFER_EXECUTION_AUTHORITY,
      payoutEnabled: false,
      transferEnabled: false,
    },
  };
}
