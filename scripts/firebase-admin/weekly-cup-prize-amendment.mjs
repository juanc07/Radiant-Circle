import { createHash } from "node:crypto";
import {
  CONFIGURATION_AUTHORITY,
  OFFICIAL_SKR_MINT,
  SCHEMA_VERSION,
  SKR_DECIMALS,
  parseSkrAmountAtomic,
} from "./weekly-cup-config.mjs";
import { assertSolanaPublicAddress } from "./skr-funding-verification.mjs";

export const PRIZE_AMENDMENT_AUTHORITY = "trusted-admin-phase12f-prize-amendment";
export const PRIZE_AMENDMENT_SCHEMA_VERSION = 1;

const AMENDABLE_STATUSES = new Set(["DRAFT", "ANNOUNCED", "OPEN"]);
const SHA256 = /^[0-9a-f]{64}$/;

export const FUNDING_EVIDENCE_FIELDS_TO_RESET = Object.freeze([
  "fundingRequiredAmountAtomic",
  "fundingObservedAmountAtomic",
  "fundingTokenAccountCount",
  "fundingFrozenTokenAccountCount",
  "fundingVerificationSlot",
  "fundingVerificationNetwork",
  "fundingVerificationMint",
  "fundingVerificationCommitment",
  "fundingVerificationAuthority",
  "fundingVerificationSchemaVersion",
  "fundingCheckedAt",
  "fundingVerifiedAt",
]);

function atomicString(value, label) {
  const raw = String(value ?? "").trim();
  if (!/^\d+$/.test(raw)) throw new Error(`${label} must be a non-negative integer string.`);
  return raw;
}

export function assertPrizeAmendableCup(cup, weekKey) {
  if (!cup) throw new Error("Weekly Cup config does not exist.");
  if (Number(cup.schemaVersion) !== SCHEMA_VERSION) {
    throw new Error(`Cup schemaVersion must be ${SCHEMA_VERSION}.`);
  }
  if (String(cup.configurationAuthority ?? "") !== CONFIGURATION_AUTHORITY) {
    throw new Error("Cup configurationAuthority is not the trusted Phase 12B authority.");
  }
  if (String(cup.weekKey ?? "") !== String(weekKey ?? "")) {
    throw new Error("Cup weekKey does not match the requested week.");
  }
  const status = String(cup.status ?? "").toUpperCase();
  if (!AMENDABLE_STATUSES.has(status)) {
    throw new Error("Prize amendment is allowed only while the Cup is DRAFT, ANNOUNCED, or OPEN.");
  }
  if (String(cup.finalizationStatus ?? "").toUpperCase() === "FINALIZED" ||
      String(cup.trustedResultAuthority ?? "").trim() ||
      String(cup.trustedResultRef ?? "").trim()) {
    throw new Error("Cup already has trusted finalization evidence. Prize is immutable.");
  }
  if (cup.payoutEnabled === true || cup.payoutReady === true) {
    throw new Error("Cup payout state is enabled/ready. Prize amendment is forbidden.");
  }
  if (cup.trustedResultsRequired !== true) {
    throw new Error("Cup must require trusted results.");
  }
  if (String(cup.prizeAssetSymbol ?? "").toUpperCase() !== "SKR" ||
      String(cup.prizeMint ?? "") !== OFFICIAL_SKR_MINT ||
      Number(cup.prizeDecimals) !== SKR_DECIMALS) {
    throw new Error("Cup prize identity does not match the trusted official SKR configuration.");
  }
  const currentPrizeAmountAtomic = atomicString(cup.prizeAmountAtomic, "Cup prizeAmountAtomic");
  if (BigInt(currentPrizeAmountAtomic) <= 0n) throw new Error("Cup prizeAmountAtomic must be greater than zero.");
  return { status, currentPrizeAmountAtomic };
}

export function buildPrizeAmendmentPlan({
  cup,
  weekKey,
  newPrizeSkr,
  fundingWalletAddress,
  reason,
}) {
  const trusted = assertPrizeAmendableCup(cup, weekKey);
  const newPrizeAmountAtomic = parseSkrAmountAtomic(newPrizeSkr);
  if (newPrizeAmountAtomic === trusted.currentPrizeAmountAtomic) {
    throw new Error("New prize amount is identical to the current prize amount.");
  }

  const cleanReason = String(reason ?? "").trim();
  if (cleanReason.length < 8 || cleanReason.length > 240) {
    throw new Error("--reason is required and must be 8-240 characters.");
  }

  const previousFundingWalletAddress = String(cup.fundingWalletAddress ?? "").trim() || null;
  const nextFundingWalletAddress = fundingWalletAddress == null || String(fundingWalletAddress).trim() === ""
    ? previousFundingWalletAddress
    : assertSolanaPublicAddress(fundingWalletAddress, "funding wallet");
  if (!nextFundingWalletAddress) {
    throw new Error("A funding wallet is required for this sponsored Cup amendment.");
  }

  const digestPayload = [
    "radiant-circle-phase12f-prize-amendment-v1",
    String(weekKey),
    trusted.status,
    trusted.currentPrizeAmountAtomic,
    newPrizeAmountAtomic,
    previousFundingWalletAddress ?? "",
    nextFundingWalletAddress,
    cleanReason,
  ].join("\n");
  const amendmentDigest = createHash("sha256").update(digestPayload, "utf8").digest("hex");

  return {
    schemaVersion: PRIZE_AMENDMENT_SCHEMA_VERSION,
    authority: PRIZE_AMENDMENT_AUTHORITY,
    weekKey: String(weekKey),
    cupStatus: trusted.status,
    previousPrizeAmountAtomic: trusted.currentPrizeAmountAtomic,
    newPrizeAmountAtomic,
    previousFundingWalletAddress,
    newFundingWalletAddress: nextFundingWalletAddress,
    previousFundingVerificationStatus: String(cup.fundingVerificationStatus ?? "NOT_VERIFIED").toUpperCase(),
    reason: cleanReason,
    amendmentDigest,
    fundingEvidenceResetRequired: true,
    payoutEnabled: false,
  };
}

export function assertAmendmentDigest(value) {
  const digest = String(value ?? "").trim().toLowerCase();
  if (!SHA256.test(digest)) throw new Error("Confirmation digest must be a 64-character lowercase SHA-256 value.");
  return digest;
}
