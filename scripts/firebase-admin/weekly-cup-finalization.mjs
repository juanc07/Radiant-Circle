import { createHash } from "node:crypto";
import {
  CONFIGURATION_AUTHORITY,
  OFFICIAL_SKR_MINT,
  SCHEMA_VERSION as CUP_SCHEMA_VERSION,
  SKR_DECIMALS,
} from "./weekly-cup-config.mjs";
import {
  RUN_RECEIPT_SCHEMA_VERSION,
  RUN_VERIFICATION_AUTHORITY,
  RUN_VERIFICATION_METHOD,
  RUN_VERIFICATION_SCHEMA_VERSION,
} from "./competition-run-verification.mjs";

export const RESULT_SCHEMA_VERSION = 1;
export const RESULT_VERSION = 1;
export const RESULT_FINALIZATION_AUTHORITY = "trusted-admin-phase12d";
export const RESULT_STATUS_FINALIZED = "FINALIZED";
export const WINNER_PAYOUT_STATUS = "NOT_ENABLED";
export const MAX_ELIGIBLE_RECEIPTS_PER_FINALIZATION = 450;

const SOLANA_ADDRESS = /^[1-9A-HJ-NP-Za-km-z]{32,44}$/;
const ATOMIC = /^[0-9]{1,30}$/;

function timestampMillis(value, label, { allowNull = false } = {}) {
  if (value == null && allowNull) return null;
  if (value == null) throw new Error(`${label} is required.`);
  if (typeof value === "number") {
    if (!Number.isSafeInteger(value) || value <= 0) throw new Error(`${label} must be a positive timestamp.`);
    return value;
  }
  if (value instanceof Date) return value.getTime();
  if (typeof value?.toMillis === "function") return value.toMillis();
  if (typeof value?.toDate === "function") return value.toDate().getTime();
  throw new Error(`${label} must be a Firestore Timestamp, Date, or epoch milliseconds.`);
}

function integer(value, label, { min = 0 } = {}) {
  const number = Number(value);
  if (!Number.isSafeInteger(number) || number < min) throw new Error(`${label} must be an integer >= ${min}.`);
  return number;
}

function atomic(value, label) {
  const raw = String(value ?? "").trim();
  if (!ATOMIC.test(raw)) throw new Error(`${label} must be a decimal-free atomic amount string.`);
  return BigInt(raw);
}

function stringValue(value, label, max = 240) {
  const clean = String(value ?? "").trim();
  if (!clean || clean.length > max) throw new Error(`${label} is required and must be at most ${max} characters.`);
  return clean;
}

function placementMap(raw) {
  const entries = Object.entries(raw ?? {}).map(([rankRaw, bpsRaw]) => {
    const rank = Number(rankRaw);
    const bps = Number(bpsRaw);
    if (!Number.isInteger(rank) || rank < 1 || rank > 10) throw new Error("Placement ranks must be integers from 1 to 10.");
    if (!Number.isInteger(bps) || bps <= 0 || bps > 10_000) throw new Error(`Placement ${rank} bps is invalid.`);
    return [rank, bps];
  });
  if (entries.length === 0) throw new Error("Cup placementAllocationsBps is empty.");
  const map = new Map(entries.sort((a, b) => a[0] - b[0]));
  if (map.size !== entries.length) throw new Error("Cup contains duplicate placement ranks.");
  const ranks = [...map.keys()];
  if (ranks.some((rank, index) => rank !== index + 1)) {
    throw new Error("Cup placement ranks must be contiguous starting at #1.");
  }
  if ([...map.values()].reduce((sum, value) => sum + value, 0) !== 10_000) {
    throw new Error("Cup placement allocations must total exactly 10000 bps.");
  }
  return map;
}

export function trustedFundingStatusAtClose(cup) {
  const requested = String(cup?.fundingVerificationStatus ?? "").trim().toUpperCase();
  if (requested === "REJECTED") return "REJECTED";
  if (requested !== "VERIFIED") {
    return String(cup?.fundingWalletAddress ?? "").trim() ? "NOT_VERIFIED" : "NOT_CONFIGURED";
  }

  try {
    const required = atomic(cup.fundingRequiredAmountAtomic, "fundingRequiredAmountAtomic");
    const observed = atomic(cup.fundingObservedAmountAtomic, "fundingObservedAmountAtomic");
    const prize = atomic(cup.prizeAmountAtomic, "prizeAmountAtomic");
    const checkedAt = timestampMillis(cup.fundingCheckedAt, "fundingCheckedAt", { allowNull: true });
    const verifiedAt = timestampMillis(cup.fundingVerifiedAt, "fundingVerifiedAt", { allowNull: true });
    const slot = Number(cup.fundingVerificationSlot);
    const evidenceValid =
      String(cup.fundingVerificationAuthority ?? "") === "trusted-admin-phase12c" &&
      Number(cup.fundingVerificationSchemaVersion) === 1 &&
      String(cup.fundingVerificationNetwork ?? "") === "mainnet-beta" &&
      String(cup.fundingVerificationCommitment ?? "").toLowerCase() === "finalized" &&
      String(cup.fundingVerificationMint ?? "") === OFFICIAL_SKR_MINT &&
      required === prize && observed >= prize &&
      Number.isSafeInteger(slot) && slot > 0 &&
      checkedAt != null && checkedAt > 0 && verifiedAt != null && verifiedAt > 0;
    return evidenceValid ? "VERIFIED" : "NOT_VERIFIED";
  } catch {
    // A forged/broken VERIFIED label must never block competitive close or become
    // payout-ready. Treat incomplete Phase 12C evidence as NOT_VERIFIED.
    return "NOT_VERIFIED";
  }
}

export function assertCloseableCup(cup, weekKey, nowEpochMillis) {
  if (!cup) throw new Error("Weekly Cup config does not exist.");
  if (Number(cup.schemaVersion) !== CUP_SCHEMA_VERSION) throw new Error(`Cup schemaVersion must be ${CUP_SCHEMA_VERSION}.`);
  if (String(cup.configurationAuthority ?? "") !== CONFIGURATION_AUTHORITY) throw new Error("Cup configurationAuthority is not trusted Phase 12B.");
  if (String(cup.weekKey ?? "").trim() !== weekKey) throw new Error("Cup weekKey does not match requested week.");
  if (String(cup.status ?? "").toUpperCase() !== "OPEN") throw new Error("Cup must be OPEN before trusted finalization.");
  if (cup.trustedResultsRequired !== true) throw new Error("Cup must require trusted results.");
  if (cup.payoutEnabled === true) throw new Error("Cup has payoutEnabled=true. Phase 12D refuses to finalize it.");
  if (String(cup.finalizationStatus ?? "").toUpperCase() === RESULT_STATUS_FINALIZED || cup.trustedResultRef) {
    throw new Error("Cup is already finalized.");
  }
  if (String(cup.prizeAssetSymbol ?? "").toUpperCase() !== "SKR") throw new Error("Cup prize asset must be SKR.");
  if (String(cup.prizeMint ?? "") !== OFFICIAL_SKR_MINT) throw new Error("Cup prize mint is not the official SKR mint.");
  if (Number(cup.prizeDecimals) !== SKR_DECIMALS) throw new Error(`Cup prizeDecimals must be ${SKR_DECIMALS}.`);
  const prizeAmountAtomic = atomic(cup.prizeAmountAtomic, "Cup prizeAmountAtomic");
  if (prizeAmountAtomic <= 0n) throw new Error("Cup prizeAmountAtomic must be greater than zero.");
  const startsAtMs = timestampMillis(cup.startsAt, "Cup startsAt");
  const endsAtMs = timestampMillis(cup.endsAt, "Cup endsAt");
  if (endsAtMs <= startsAtMs) throw new Error("Cup end must be after start.");
  const now = Number(nowEpochMillis);
  if (!Number.isSafeInteger(now) || now <= 0) throw new Error("Finalization time is invalid.");
  if (now < endsAtMs) throw new Error(`Cup cannot be finalized before its configured end (${new Date(endsAtMs).toISOString()}).`);
  return {
    startsAtMs,
    endsAtMs,
    prizeAmountAtomic,
    placements: placementMap(cup.placementAllocationsBps),
    fundingStatusAtClose: trustedFundingStatusAtClose(cup),
  };
}

export function normalizeTrustedPlacementReceipt({
  docId,
  data,
  verification,
  verificationDocId,
  weekKey,
  cupStartMs,
  cupEndMs,
  cutoffMs,
}) {
  if (!data || Number(data.schemaVersion) !== RUN_RECEIPT_SCHEMA_VERSION) return null;
  if (String(data.scoreAuthority ?? "") !== "client-reported-prototype-not-payout-authority") return null;
  const receiptId = String(data.receiptId ?? "").trim();
  if (!receiptId || receiptId !== String(docId)) return null;
  if (String(data.utcWeekKey ?? "") !== weekKey || String(data.mode ?? "") !== "Ranked") return null;
  if (String(data.verificationStatus ?? "") !== "VERIFIED" || data.trustedPlacementEligible !== true) return null;
  if (data.payoutEligible !== false || String(data.payoutStatus ?? "") !== "NOT_ELIGIBLE") return null;
  if (Number(data.trustedVerificationSchemaVersion) !== RUN_VERIFICATION_SCHEMA_VERSION) return null;
  if (String(data.trustedVerificationAuthority ?? "") !== RUN_VERIFICATION_AUTHORITY) return null;
  if (String(data.trustedVerificationMethod ?? "") !== RUN_VERIFICATION_METHOD) return null;
  const evidenceRef = String(data.trustedEvidenceRef ?? "").trim();
  if (!evidenceRef) return null;
  if (String(data.trustedVerificationRef ?? "") !== `competitionRunVerifications/${receiptId}`) return null;
  const walletAddress = String(data.walletAddress ?? "").trim();
  if (!SOLANA_ADDRESS.test(walletAddress)) return null;
  const ownerUid = String(data.ownerUid ?? "").trim();
  if (!ownerUid) return null;

  const completedAt = Number(data.clientCompletedAtEpochMillis);
  const submittedAt = timestampMillis(data.submittedAt, "Receipt submittedAt", { allowNull: true });
  const verifiedAt = timestampMillis(data.trustedVerifiedAt, "Receipt trustedVerifiedAt", { allowNull: true });
  if (!Number.isSafeInteger(completedAt) || completedAt < cupStartMs || completedAt >= cupEndMs) return null;
  if (submittedAt == null || submittedAt < cupStartMs || submittedAt > cupEndMs) return null;
  if (verifiedAt == null || verifiedAt > cutoffMs) return null;

  let normalized;
  try {
    normalized = {
      receiptId,
      ownerUid,
      walletAddress,
      score: integer(data.score, "score"),
      maxCombo: integer(data.maxCombo, "maxCombo"),
      perfectHits: integer(data.perfectHits, "perfectHits"),
      radiantHits: integer(data.radiantHits, "radiantHits"),
      corruptedHits: integer(data.corruptedHits, "corruptedHits"),
      clientCompletedAtEpochMillis: completedAt,
      submittedAtEpochMillis: submittedAt,
      trustedVerifiedAtEpochMillis: verifiedAt,
      trustedEvidenceRef: evidenceRef,
    };
  } catch {
    return null;
  }

  // The promoted receipt is not sufficient by itself. Eligibility also requires
  // the immutable Admin-only verification decision written by the verifier.
  if (!verification || String(verificationDocId ?? "") !== receiptId) return null;
  const auditDecidedAt = timestampMillis(verification.decidedAt, "Verification decidedAt", { allowNull: true });
  if (
    Number(verification.schemaVersion) !== RUN_VERIFICATION_SCHEMA_VERSION ||
    String(verification.receiptId ?? "") !== receiptId ||
    String(verification.weekKey ?? "") !== weekKey ||
    String(verification.decision ?? "") !== "VERIFIED" ||
    String(verification.walletAddress ?? "") !== walletAddress ||
    String(verification.ownerUid ?? "") !== ownerUid ||
    Number(verification.score) !== normalized.score ||
    Number(verification.maxCombo) !== normalized.maxCombo ||
    Number(verification.perfectHits) !== normalized.perfectHits ||
    Number(verification.radiantHits) !== normalized.radiantHits ||
    Number(verification.corruptedHits) !== normalized.corruptedHits ||
    Number(verification.clientCompletedAtEpochMillis) !== completedAt ||
    String(verification.evidenceRef ?? "").trim() !== evidenceRef ||
    String(verification.verificationAuthority ?? "") !== RUN_VERIFICATION_AUTHORITY ||
    String(verification.verificationMethod ?? "") !== RUN_VERIFICATION_METHOD ||
    verification.payoutEnabled !== false ||
    auditDecidedAt == null || auditDecidedAt > cutoffMs
  ) return null;

  return {
    ...normalized,
    trustedVerificationAuditRef: `competitionRunVerifications/${receiptId}`,
    trustedVerificationDecidedAtEpochMillis: auditDecidedAt,
  };
}

export function compareTrustedRuns(a, b) {
  return (b.score - a.score) ||
    (b.maxCombo - a.maxCombo) ||
    (b.perfectHits - a.perfectHits) ||
    (a.clientCompletedAtEpochMillis - b.clientCompletedAtEpochMillis) ||
    a.receiptId.localeCompare(b.receiptId);
}

export function bestEligibleResultPerWallet(receipts) {
  const byWallet = new Map();
  for (const receipt of receipts) {
    const current = byWallet.get(receipt.walletAddress);
    if (!current || compareTrustedRuns(receipt, current) < 0) byWallet.set(receipt.walletAddress, receipt);
  }
  return [...byWallet.values()].sort(compareTrustedRuns);
}

export function exactPrizeAllocations(prizeAmountAtomic, placements) {
  const result = new Map();
  let total = 0n;
  for (const [rank, bps] of placements.entries()) {
    const numerator = prizeAmountAtomic * BigInt(bps);
    if (numerator % 10_000n !== 0n) {
      throw new Error(`Prize amount cannot represent placement #${rank} (${bps} bps) exactly in atomic SKR.`);
    }
    const amount = numerator / 10_000n;
    result.set(rank, amount);
    total += amount;
  }
  if (total !== prizeAmountAtomic) throw new Error("Placement prize allocation does not sum exactly to the configured prize.");
  return result;
}

function canonicalReceipt(receipt) {
  return [
    receipt.receiptId,
    receipt.ownerUid,
    receipt.walletAddress,
    receipt.score,
    receipt.maxCombo,
    receipt.perfectHits,
    receipt.radiantHits,
    receipt.corruptedHits,
    receipt.clientCompletedAtEpochMillis,
    receipt.submittedAtEpochMillis,
    receipt.trustedVerifiedAtEpochMillis,
    receipt.trustedEvidenceRef,
    receipt.trustedVerificationAuditRef,
    receipt.trustedVerificationDecidedAtEpochMillis,
  ].join("|");
}

function sha256(lines) {
  return createHash("sha256").update(lines.join("\n"), "utf8").digest("hex");
}

export function buildFinalizationPlan({ cup, weekKey, receiptDocuments, verificationDocuments, nowEpochMillis }) {
  const close = assertCloseableCup(cup, weekKey, nowEpochMillis);
  const sourceDocs = Array.isArray(receiptDocuments) ? receiptDocuments : [];
  const verificationDocs = Array.isArray(verificationDocuments) ? verificationDocuments : [];
  const verificationByReceipt = new Map(verificationDocs.map(({ id, data }) => [String(id), data]));
  const eligible = sourceDocs.map(({ id, data }) => normalizeTrustedPlacementReceipt({
    docId: id,
    data,
    verificationDocId: String(id),
    verification: verificationByReceipt.get(String(id)),
    weekKey,
    cupStartMs: close.startsAtMs,
    cupEndMs: close.endsAtMs,
    cutoffMs: nowEpochMillis,
  })).filter(Boolean);

  if (eligible.length === 0) {
    throw new Error("No trusted-placement-eligible VERIFIED receipts exist for this Cup. Refusing to invent winners.");
  }
  if (eligible.length > MAX_ELIGIBLE_RECEIPTS_PER_FINALIZATION) {
    throw new Error(`Eligible receipt count ${eligible.length} exceeds safe single-transaction limit ${MAX_ELIGIBLE_RECEIPTS_PER_FINALIZATION}.`);
  }

  const rankedWallets = bestEligibleResultPerWallet(eligible);
  const requiredWinnerCount = close.placements.size;
  if (rankedWallets.length < requiredWinnerCount) {
    throw new Error(`Cup requires ${requiredWinnerCount} configured placements but only ${rankedWallets.length} eligible wallet(s) exist.`);
  }
  const allocations = exactPrizeAllocations(close.prizeAmountAtomic, close.placements);
  const rankByReceipt = new Map(rankedWallets.map((receipt, index) => [receipt.receiptId, index + 1]));
  const selectedReceiptIds = new Set(rankedWallets.map((receipt) => receipt.receiptId));
  const frozenReceipts = [...eligible].sort((a, b) => a.receiptId.localeCompare(b.receiptId)).map((receipt) => ({
    ...receipt,
    selectedForWalletRanking: selectedReceiptIds.has(receipt.receiptId),
    finalRank: rankByReceipt.get(receipt.receiptId) ?? null,
  }));
  const winners = [...close.placements.entries()].map(([placement]) => {
    const receipt = rankedWallets[placement - 1];
    return {
      placement,
      walletAddress: receipt.walletAddress,
      ownerUid: receipt.ownerUid,
      receiptId: receipt.receiptId,
      score: receipt.score,
      maxCombo: receipt.maxCombo,
      perfectHits: receipt.perfectHits,
      clientCompletedAtEpochMillis: receipt.clientCompletedAtEpochMillis,
      prizeAmountAtomic: allocations.get(placement).toString(),
      prizeAssetSymbol: "SKR",
      payoutStatus: WINNER_PAYOUT_STATUS,
      payoutEnabled: false,
      payoutReady: false,
    };
  });

  const eligibleReceiptDigestSha256 = sha256(frozenReceipts.map(canonicalReceipt));
  const rankingDigestSha256 = sha256(rankedWallets.map(canonicalReceipt));
  return {
    sourceReceiptCount: sourceDocs.length,
    eligibleReceiptCount: eligible.length,
    eligibleWalletCount: rankedWallets.length,
    winnerCount: winners.length,
    fundingVerificationStatusAtClose: close.fundingStatusAtClose,
    payoutEnabled: false,
    payoutReady: false,
    eligibleReceiptDigestSha256,
    rankingDigestSha256,
    frozenReceipts,
    rankedWallets,
    winners,
    result: {
      schemaVersion: RESULT_SCHEMA_VERSION,
      resultVersion: RESULT_VERSION,
      weekKey,
      finalizationStatus: RESULT_STATUS_FINALIZED,
      finalizationAuthority: RESULT_FINALIZATION_AUTHORITY,
      sourceReceiptCount: sourceDocs.length,
      eligibleReceiptCount: eligible.length,
      eligibleWalletCount: rankedWallets.length,
      winnerCount: winners.length,
      prizeAssetSymbol: "SKR",
      prizeMint: OFFICIAL_SKR_MINT,
      prizeDecimals: SKR_DECIMALS,
      prizeAmountAtomic: close.prizeAmountAtomic.toString(),
      placementAllocationsBps: Object.fromEntries(close.placements.entries()),
      fundingVerificationStatusAtClose: close.fundingStatusAtClose,
      eligibleReceiptDigestSha256,
      rankingDigestSha256,
      payoutEnabled: false,
      payoutReady: false,
    },
  };
}
