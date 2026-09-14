import {
  CONFIGURATION_AUTHORITY,
  SCHEMA_VERSION as CUP_SCHEMA_VERSION,
} from "./weekly-cup-config.mjs";

export const RUN_RECEIPT_SCHEMA_VERSION = 1;
export const CLIENT_SCORE_AUTHORITY = "client-reported-prototype-not-payout-authority";
export const RUN_VERIFICATION_SCHEMA_VERSION = 1;
export const RUN_VERIFICATION_AUTHORITY = "trusted-admin-phase12d";
export const RUN_VERIFICATION_METHOD = "manual-independent-evidence-attestation-v1";

const SOLANA_ADDRESS = /^[1-9A-HJ-NP-Za-km-z]{32,44}$/;
const VERIFIABLE_CUP_STATUSES = new Set(["OPEN"]);

function timestampMillis(value, label) {
  if (value == null) throw new Error(`${label} is required.`);
  if (typeof value === "number") {
    if (!Number.isSafeInteger(value) || value <= 0) throw new Error(`${label} must be a positive timestamp.`);
    return value;
  }
  if (value instanceof Date) return value.getTime();
  if (typeof value?.toMillis === "function") {
    const millis = value.toMillis();
    if (!Number.isSafeInteger(millis) || millis <= 0) throw new Error(`${label} must be a positive timestamp.`);
    return millis;
  }
  if (typeof value?.toDate === "function") return value.toDate().getTime();
  throw new Error(`${label} must be a Firestore Timestamp, Date, or epoch milliseconds.`);
}

function nonNegativeInt(value, label) {
  const number = Number(value);
  if (!Number.isSafeInteger(number) || number < 0) throw new Error(`${label} must be a non-negative integer.`);
  return number;
}

function requiredString(value, label, max = 200) {
  const clean = String(value ?? "").trim();
  if (!clean || clean.length > max) throw new Error(`${label} is required and must be at most ${max} characters.`);
  return clean;
}

function assertBaseCup(cup, weekKey) {
  if (!cup) throw new Error("Weekly Cup config does not exist.");
  if (Number(cup.schemaVersion) !== CUP_SCHEMA_VERSION) {
    throw new Error(`Cup schemaVersion must be ${CUP_SCHEMA_VERSION}.`);
  }
  if (String(cup.configurationAuthority ?? "") !== CONFIGURATION_AUTHORITY) {
    throw new Error("Cup configurationAuthority is not the trusted Phase 12B authority.");
  }
  if (String(cup.weekKey ?? "").trim() !== weekKey) throw new Error("Cup weekKey does not match the requested week.");
  if (!VERIFIABLE_CUP_STATUSES.has(String(cup.status ?? "").toUpperCase())) {
    throw new Error("Run verification requires the Cup status to be OPEN.");
  }
  if (cup.trustedResultsRequired !== true) throw new Error("Cup must require trusted results.");
  if (cup.payoutEnabled === true) throw new Error("Cup has payoutEnabled=true. Refusing run verification.");

  const startMs = timestampMillis(cup.startsAt, "Cup startsAt");
  const endMs = timestampMillis(cup.endsAt, "Cup endsAt");
  if (endMs <= startMs) throw new Error("Cup endsAt must be after startsAt.");
  return { startMs, endMs };
}

export function assertUnverifiedReceipt({ cup, weekKey, receiptId, receipt }) {
  const cleanWeek = requiredString(weekKey, "weekKey", 16);
  const cleanReceiptId = requiredString(receiptId, "receiptId", 80);
  const { startMs, endMs } = assertBaseCup(cup, cleanWeek);

  if (!receipt) throw new Error("Competition run receipt does not exist.");
  if (Number(receipt.schemaVersion) !== RUN_RECEIPT_SCHEMA_VERSION) {
    throw new Error(`Receipt schemaVersion must be ${RUN_RECEIPT_SCHEMA_VERSION}.`);
  }
  if (String(receipt.receiptId ?? "") !== cleanReceiptId) throw new Error("Receipt id does not match its document id.");
  if (String(receipt.utcWeekKey ?? "") !== cleanWeek) throw new Error("Receipt week does not match the Cup week.");
  if (String(receipt.mode ?? "") !== "Ranked") throw new Error("Only Ranked receipts can be verified for Cup placement.");
  if (String(receipt.scoreAuthority ?? "") !== CLIENT_SCORE_AUTHORITY) {
    throw new Error("Receipt scoreAuthority is not the expected client submission authority.");
  }
  if (String(receipt.verificationStatus ?? "") !== "UNVERIFIED") {
    throw new Error(`Receipt is already ${String(receipt.verificationStatus ?? "UNKNOWN")}; trusted decisions are immutable.`);
  }
  if (receipt.trustedPlacementEligible !== false) throw new Error("UNVERIFIED receipt must have trustedPlacementEligible=false.");
  if (receipt.payoutEligible !== false || String(receipt.payoutStatus ?? "") !== "NOT_ELIGIBLE") {
    throw new Error("Receipt payout fields are not in the safe pre-verification state.");
  }

  const ownerUid = requiredString(receipt.ownerUid, "Receipt ownerUid", 128);
  const walletAddress = requiredString(receipt.walletAddress, "Receipt walletAddress", 64);
  if (!SOLANA_ADDRESS.test(walletAddress)) throw new Error("Receipt walletAddress is not a valid Solana public address.");

  const score = nonNegativeInt(receipt.score, "Receipt score");
  const maxCombo = nonNegativeInt(receipt.maxCombo, "Receipt maxCombo");
  const perfectHits = nonNegativeInt(receipt.perfectHits, "Receipt perfectHits");
  const radiantHits = nonNegativeInt(receipt.radiantHits, "Receipt radiantHits");
  const corruptedHits = nonNegativeInt(receipt.corruptedHits, "Receipt corruptedHits");
  const completedAtMs = nonNegativeInt(receipt.clientCompletedAtEpochMillis, "Receipt clientCompletedAtEpochMillis");
  const submittedAtMs = timestampMillis(receipt.submittedAt, "Receipt submittedAt");
  if (completedAtMs < startMs || completedAtMs >= endMs) throw new Error("Receipt completion time is outside the Cup window.");
  if (submittedAtMs < startMs || submittedAtMs > endMs) throw new Error("Receipt submission time is outside the Cup window.");

  return {
    receiptId: cleanReceiptId,
    weekKey: cleanWeek,
    ownerUid,
    walletAddress,
    score,
    maxCombo,
    perfectHits,
    radiantHits,
    corruptedHits,
    clientCompletedAtEpochMillis: completedAtMs,
    submittedAtEpochMillis: submittedAtMs,
  };
}

export function buildVerifiedRunDecision({ cup, weekKey, receiptId, receipt, expected, evidenceRef }) {
  const normalized = assertUnverifiedReceipt({ cup, weekKey, receiptId, receipt });
  const evidence = requiredString(evidenceRef, "evidenceRef", 240);
  const requiredExpected = {
    walletAddress: requiredString(expected?.walletAddress, "expected walletAddress", 64),
    score: nonNegativeInt(expected?.score, "expected score"),
    maxCombo: nonNegativeInt(expected?.maxCombo, "expected maxCombo"),
    perfectHits: nonNegativeInt(expected?.perfectHits, "expected perfectHits"),
    radiantHits: nonNegativeInt(expected?.radiantHits, "expected radiantHits"),
    corruptedHits: nonNegativeInt(expected?.corruptedHits, "expected corruptedHits"),
    clientCompletedAtEpochMillis: nonNegativeInt(
      expected?.clientCompletedAtEpochMillis,
      "expected clientCompletedAtEpochMillis",
    ),
  };

  for (const [key, value] of Object.entries(requiredExpected)) {
    if (normalized[key] !== value) {
      throw new Error(`Independent evidence mismatch for ${key}: expected ${value}, receipt has ${normalized[key]}.`);
    }
  }

  return {
    normalized,
    decision: "VERIFIED",
    update: {
      verificationStatus: "VERIFIED",
      trustedPlacementEligible: true,
      trustedVerificationSchemaVersion: RUN_VERIFICATION_SCHEMA_VERSION,
      trustedVerificationAuthority: RUN_VERIFICATION_AUTHORITY,
      trustedVerificationMethod: RUN_VERIFICATION_METHOD,
      trustedEvidenceRef: evidence,
      trustedVerificationRef: `competitionRunVerifications/${normalized.receiptId}`,
      payoutEligible: false,
      payoutStatus: "NOT_ELIGIBLE",
    },
  };
}

export function buildRejectedRunDecision({ cup, weekKey, receiptId, receipt, evidenceRef, reason }) {
  const normalized = assertUnverifiedReceipt({ cup, weekKey, receiptId, receipt });
  const evidence = requiredString(evidenceRef, "evidenceRef", 240);
  const rejectionReason = requiredString(reason, "rejection reason", 240);
  return {
    normalized,
    decision: "REJECTED",
    update: {
      verificationStatus: "REJECTED",
      trustedPlacementEligible: false,
      trustedVerificationSchemaVersion: RUN_VERIFICATION_SCHEMA_VERSION,
      trustedVerificationAuthority: RUN_VERIFICATION_AUTHORITY,
      trustedVerificationMethod: RUN_VERIFICATION_METHOD,
      trustedEvidenceRef: evidence,
      trustedVerificationRef: `competitionRunVerifications/${normalized.receiptId}`,
      trustedRejectionReason: rejectionReason,
      payoutEligible: false,
      payoutStatus: "NOT_ELIGIBLE",
    },
  };
}
