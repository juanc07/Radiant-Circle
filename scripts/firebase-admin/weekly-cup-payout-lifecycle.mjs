import { createHash } from "node:crypto";
import {
  CONFIGURATION_AUTHORITY,
  OFFICIAL_SKR_MINT,
  SCHEMA_VERSION as CUP_SCHEMA_VERSION,
  SKR_DECIMALS,
} from "./weekly-cup-config.mjs";
import {
  RESULT_FINALIZATION_AUTHORITY,
  RESULT_SCHEMA_VERSION,
  RESULT_STATUS_FINALIZED,
  RESULT_VERSION,
  WINNER_PAYOUT_STATUS,
  trustedFundingStatusAtClose,
} from "./weekly-cup-finalization.mjs";
import {
  COMPETITION_WALLET_LOCK_AUTHORITY,
  COMPETITION_WALLET_LOCK_SCHEMA_VERSION,
  assertCompetitionWalletLock,
} from "./competition-wallet-lock.mjs";

export const PAYOUT_SCHEMA_VERSION = 1;
export const PAYOUT_LIFECYCLE_VERSION = 1;
export const PAYOUT_AUTHORITY = "trusted-admin-phase12e";
export const PAYOUT_STATUS_READY_FOR_REVIEW = "READY_FOR_REVIEW";
export const PAYOUT_STATUS_APPROVED = "APPROVED";
export const PAYOUT_ITEM_STATUS_READY_FOR_REVIEW = "READY_FOR_REVIEW";
export const PAYOUT_ITEM_STATUS_APPROVED = "APPROVED";
export const TRANSFER_STATUS_NOT_STARTED = "NOT_STARTED";
export const PAYOUT_EVENT_PREPARED = "PAYOUT_PREPARED";
export const PAYOUT_EVENT_APPROVED = "PAYOUT_APPROVED";

const SOLANA_ADDRESS = /^[1-9A-HJ-NP-Za-km-z]{32,44}$/;
const ATOMIC = /^[0-9]{1,30}$/;
const SHA256 = /^[0-9a-f]{64}$/;

function requiredString(value, label, max = 240) {
  const clean = String(value ?? "").trim();
  if (!clean || clean.length > max) throw new Error(`${label} is required and must be at most ${max} characters.`);
  return clean;
}

function timestampMillis(value, label) {
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

function atomic(value, label) {
  const raw = String(value ?? "").trim();
  if (!ATOMIC.test(raw)) throw new Error(`${label} must be a decimal-free atomic amount string.`);
  return BigInt(raw);
}

function canonicalWallet(value, label) {
  const clean = String(value ?? "").trim();
  if (!SOLANA_ADDRESS.test(clean)) throw new Error(`${label} must be a Solana public address.`);
  return clean;
}

function digestLines(lines) {
  return createHash("sha256").update(lines.join("\n"), "utf8").digest("hex");
}

function placementMap(raw) {
  const entries = Object.entries(raw ?? {}).map(([rankRaw, bpsRaw]) => {
    const rank = Number(rankRaw);
    const bps = Number(bpsRaw);
    if (!Number.isInteger(rank) || rank < 1 || rank > 10) throw new Error("Placement ranks must be integers from 1 to 10.");
    if (!Number.isInteger(bps) || bps <= 0 || bps > 10_000) throw new Error(`Placement ${rank} bps is invalid.`);
    return [rank, bps];
  }).sort((a, b) => a[0] - b[0]);

  if (entries.length === 0) throw new Error("placementAllocationsBps is empty.");
  if (entries.some(([rank], index) => rank !== index + 1)) {
    throw new Error("Placement ranks must be contiguous starting at #1.");
  }
  if (entries.reduce((sum, [, bps]) => sum + bps, 0) !== 10_000) {
    throw new Error("Placement allocations must total exactly 10000 bps.");
  }
  return new Map(entries);
}

function winnerByPlacement(winnerDocuments) {
  const map = new Map();
  for (const doc of winnerDocuments ?? []) {
    const data = doc?.data ?? doc;
    const placement = Number(data?.placement);
    if (!Number.isInteger(placement) || placement < 1 || placement > 10) {
      throw new Error("Winner placement must be an integer from 1 to 10.");
    }
    if (map.has(placement)) throw new Error(`Duplicate winner placement #${placement}.`);
    map.set(placement, { id: String(doc?.id ?? placement), data });
  }
  return map;
}

function assertTrustedCup(cup, result, weekKey) {
  if (!cup) throw new Error("Weekly Cup config does not exist.");
  if (Number(cup.schemaVersion) !== CUP_SCHEMA_VERSION) throw new Error(`Cup schemaVersion must be ${CUP_SCHEMA_VERSION}.`);
  if (String(cup.configurationAuthority ?? "") !== CONFIGURATION_AUTHORITY) {
    throw new Error("Cup configurationAuthority is not trusted Phase 12B.");
  }
  if (String(cup.weekKey ?? "").trim() !== weekKey) throw new Error("Cup weekKey does not match requested week.");
  if (String(cup.status ?? "").toUpperCase() !== "CLOSED") throw new Error("Payout lifecycle requires a CLOSED Cup.");
  if (String(cup.finalizationStatus ?? "").toUpperCase() !== RESULT_STATUS_FINALIZED) {
    throw new Error("Cup finalizationStatus is not FINALIZED.");
  }
  if (String(cup.trustedResultAuthority ?? "") !== RESULT_FINALIZATION_AUTHORITY) {
    throw new Error("Cup trustedResultAuthority is not Phase 12D.");
  }
  if (String(cup.trustedResultRef ?? "") !== `weeklyCupResults/${weekKey}`) {
    throw new Error("Cup trustedResultRef does not match the finalized result.");
  }
  if (cup.payoutEnabled === true) throw new Error("Cup payoutEnabled must remain false in Phase 12E.");
  if (String(cup.prizeMint ?? "") !== OFFICIAL_SKR_MINT || Number(cup.prizeDecimals) !== SKR_DECIMALS) {
    throw new Error("Cup SKR identity is invalid.");
  }
  if (trustedFundingStatusAtClose(cup) !== "VERIFIED") {
    throw new Error("Payout lifecycle requires trusted Phase 12C funding VERIFIED before Cup close.");
  }
  canonicalWallet(cup.fundingWalletAddress, "Cup fundingWalletAddress");

  if (!result) throw new Error("Trusted Weekly Cup result does not exist.");
  if (Number(result.schemaVersion) !== RESULT_SCHEMA_VERSION || Number(result.resultVersion) !== RESULT_VERSION) {
    throw new Error("Trusted result schema/version is invalid.");
  }
  if (String(result.weekKey ?? "").trim() !== weekKey) throw new Error("Result weekKey does not match requested week.");
  if (String(result.finalizationStatus ?? "").toUpperCase() !== RESULT_STATUS_FINALIZED) {
    throw new Error("Result finalizationStatus is not FINALIZED.");
  }
  if (String(result.finalizationAuthority ?? "") !== RESULT_FINALIZATION_AUTHORITY) {
    throw new Error("Result finalizationAuthority is not Phase 12D.");
  }
  timestampMillis(result.finalizedAt, "Result finalizedAt");
  if (String(result.fundingVerificationStatusAtClose ?? "").toUpperCase() !== "VERIFIED") {
    throw new Error("Trusted result was not finalized with VERIFIED funding.");
  }
  if (result.payoutEnabled === true || result.payoutReady === true) {
    throw new Error("Phase 12D result unexpectedly enabled payout readiness.");
  }
  if (String(result.prizeAssetSymbol ?? "").toUpperCase() !== "SKR" ||
      String(result.prizeMint ?? "") !== OFFICIAL_SKR_MINT ||
      Number(result.prizeDecimals) !== SKR_DECIMALS) {
    throw new Error("Result SKR identity is invalid.");
  }
  if (String(result.prizeAmountAtomic ?? "") !== String(cup.prizeAmountAtomic ?? "")) {
    throw new Error("Cup/result prizeAmountAtomic mismatch.");
  }
  if (String(result.rankingDigestSha256 ?? "").match(SHA256) == null) {
    throw new Error("Result rankingDigestSha256 is invalid.");
  }
  if (result.competitionWalletLockRequired !== true ||
      Number(result.competitionWalletLockSchemaVersion) !== COMPETITION_WALLET_LOCK_SCHEMA_VERSION ||
      String(result.competitionWalletLockAuthority ?? "") !== COMPETITION_WALLET_LOCK_AUTHORITY) {
    throw new Error("Trusted result is missing required Phase 12G competition-wallet lock evidence.");
  }
}

export function buildPayoutPreparationPlan({ cup, result, winnerDocuments, competitionWalletLockDocuments, weekKey }) {
  const cleanWeek = requiredString(weekKey, "weekKey", 16);
  assertTrustedCup(cup, result, cleanWeek);

  const placements = placementMap(result.placementAllocationsBps);
  const winners = winnerByPlacement(winnerDocuments);
  const lockByOwner = new Map((competitionWalletLockDocuments ?? []).map((doc) => [
    String(doc?.id ?? doc?.data?.ownerUid ?? ""),
    doc?.data ?? doc,
  ]));
  const resultWinnerCount = Number(result.winnerCount);
  if (!Number.isInteger(resultWinnerCount) || resultWinnerCount !== placements.size || winners.size !== placements.size) {
    throw new Error("Trusted result winner count does not match configured placements.");
  }

  const prizeAtomic = atomic(result.prizeAmountAtomic, "Result prizeAmountAtomic");
  const seenWallets = new Set();
  let total = 0n;
  const items = [];

  for (const [placement, bps] of placements.entries()) {
    const winnerDoc = winners.get(placement);
    if (!winnerDoc) throw new Error(`Missing trusted winner #${placement}.`);
    const winner = winnerDoc.data;
    if (Number(winner.schemaVersion) !== RESULT_SCHEMA_VERSION || Number(winner.resultVersion) !== RESULT_VERSION) {
      throw new Error(`Winner #${placement} schema/version is invalid.`);
    }
    if (String(winner.weekKey ?? "").trim() !== cleanWeek) throw new Error(`Winner #${placement} weekKey mismatch.`);
    if (String(winner.resultAuthority ?? "") !== RESULT_FINALIZATION_AUTHORITY) {
      throw new Error(`Winner #${placement} resultAuthority is invalid.`);
    }
    if (String(winner.fundingVerificationStatusAtClose ?? "").toUpperCase() !== "VERIFIED") {
      throw new Error(`Winner #${placement} was not finalized with VERIFIED funding.`);
    }
    if (String(winner.payoutStatus ?? "").toUpperCase() !== WINNER_PAYOUT_STATUS ||
        winner.payoutEnabled === true || winner.payoutReady === true) {
      throw new Error(`Winner #${placement} has unexpected payout authority.`);
    }

    const walletAddress = canonicalWallet(winner.walletAddress, `Winner #${placement} walletAddress`);
    if (seenWallets.has(walletAddress)) throw new Error("Trusted winners must use distinct full wallet addresses.");
    seenWallets.add(walletAddress);

    const ownerUid = requiredString(winner.ownerUid, `Winner #${placement} ownerUid`, 128);
    const walletLock = assertCompetitionWalletLock({
      weekKey: cleanWeek,
      ownerUid,
      walletAddress,
      lockDocId: ownerUid,
      lock: lockByOwner.get(ownerUid),
    });
    if (String(winner.competitionWalletLockRef ?? "") !== walletLock.ref ||
        String(winner.competitionWalletLockAuthority ?? "") !== COMPETITION_WALLET_LOCK_AUTHORITY ||
        String(winner.competitionWalletAddress ?? "") !== walletAddress) {
      throw new Error(`Winner #${placement} competition-wallet lock snapshot is invalid.`);
    }

    const receiptId = requiredString(winner.receiptId, `Winner #${placement} receiptId`, 100);
    const amountAtomic = atomic(winner.prizeAmountAtomic, `Winner #${placement} prizeAmountAtomic`);
    const expectedNumerator = prizeAtomic * BigInt(bps);
    if (expectedNumerator % 10_000n !== 0n || amountAtomic !== expectedNumerator / 10_000n) {
      throw new Error(`Winner #${placement} amount does not match the exact configured allocation.`);
    }
    if (String(winner.prizeAssetSymbol ?? "").toUpperCase() !== "SKR") {
      throw new Error(`Winner #${placement} prize asset is not SKR.`);
    }

    total += amountAtomic;
    items.push({
      schemaVersion: PAYOUT_SCHEMA_VERSION,
      lifecycleVersion: PAYOUT_LIFECYCLE_VERSION,
      weekKey: cleanWeek,
      placement,
      ownerUid,
      walletAddress,
      receiptId,
      competitionWalletLockRef: walletLock.ref,
      competitionWalletLockAuthority: COMPETITION_WALLET_LOCK_AUTHORITY,
      amountAtomic: amountAtomic.toString(),
      prizeAssetSymbol: "SKR",
      status: PAYOUT_ITEM_STATUS_READY_FOR_REVIEW,
      transferStatus: TRANSFER_STATUS_NOT_STARTED,
      payoutEnabled: false,
      transferEnabled: false,
      authority: PAYOUT_AUTHORITY,
      sourceWinnerRef: `weeklyCupResults/${cleanWeek}/winners/${placement}`,
    });
  }

  if (total !== prizeAtomic) throw new Error("Winner payout amounts do not sum to the configured prize.");

  const fundingWalletAddress = canonicalWallet(cup.fundingWalletAddress, "Cup fundingWalletAddress");
  const manifestDigestSha256 = digestLines([
    `week=${cleanWeek}`,
    `mint=${OFFICIAL_SKR_MINT}`,
    `decimals=${SKR_DECIMALS}`,
    `total=${prizeAtomic}`,
    `fundingWallet=${fundingWalletAddress}`,
    `rankingDigest=${String(result.rankingDigestSha256).toLowerCase()}`,
    ...items.map((item) => [
      item.placement,
      item.ownerUid,
      item.walletAddress,
      item.receiptId,
      item.amountAtomic,
      item.competitionWalletLockRef,
      item.competitionWalletLockAuthority,
    ].join("|")),
  ]);

  return {
    manifestDigestSha256,
    items,
    batch: {
      schemaVersion: PAYOUT_SCHEMA_VERSION,
      lifecycleVersion: PAYOUT_LIFECYCLE_VERSION,
      weekKey: cleanWeek,
      status: PAYOUT_STATUS_READY_FOR_REVIEW,
      authority: PAYOUT_AUTHORITY,
      competitionWalletLockRequired: true,
      competitionWalletLockSchemaVersion: COMPETITION_WALLET_LOCK_SCHEMA_VERSION,
      competitionWalletLockAuthority: COMPETITION_WALLET_LOCK_AUTHORITY,
      sourceResultRef: `weeklyCupResults/${cleanWeek}`,
      sourceResultVersion: RESULT_VERSION,
      sourceResultAuthority: RESULT_FINALIZATION_AUTHORITY,
      sourceRankingDigestSha256: String(result.rankingDigestSha256).toLowerCase(),
      payoutManifestDigestSha256: manifestDigestSha256,
      fundingWalletAddress,
      fundingVerificationStatusAtClose: "VERIFIED",
      prizeAssetSymbol: "SKR",
      prizeMint: OFFICIAL_SKR_MINT,
      prizeDecimals: SKR_DECIMALS,
      totalAmountAtomic: prizeAtomic.toString(),
      winnerCount: items.length,
      payoutEnabled: false,
      transferEnabled: false,
      transferStatus: TRANSFER_STATUS_NOT_STARTED,
    },
  };
}

export function buildPayoutApprovalPlan({ batch, itemDocuments, weekKey, reviewRef }) {
  const cleanWeek = requiredString(weekKey, "weekKey", 16);
  const cleanReviewRef = requiredString(reviewRef, "reviewRef", 180);
  if (!batch) throw new Error("Payout batch does not exist.");
  if (Number(batch.schemaVersion) !== PAYOUT_SCHEMA_VERSION || Number(batch.lifecycleVersion) !== PAYOUT_LIFECYCLE_VERSION) {
    throw new Error("Payout batch schema/version is invalid.");
  }
  if (String(batch.weekKey ?? "").trim() !== cleanWeek) throw new Error("Payout batch weekKey mismatch.");
  if (String(batch.authority ?? "") !== PAYOUT_AUTHORITY) throw new Error("Payout batch authority is invalid.");
  if (String(batch.status ?? "").toUpperCase() !== PAYOUT_STATUS_READY_FOR_REVIEW) {
    throw new Error("Only READY_FOR_REVIEW payout batches can be approved in Phase 12E.");
  }
  if (batch.payoutEnabled === true || batch.transferEnabled === true) {
    throw new Error("Phase 12E payout/transfer authority must remain disabled.");
  }
  if (String(batch.transferStatus ?? "").toUpperCase() !== TRANSFER_STATUS_NOT_STARTED) {
    throw new Error("Phase 12E requires transferStatus=NOT_STARTED.");
  }
  const lockRequired = batch.competitionWalletLockRequired === true;
  if (lockRequired && (
      Number(batch.competitionWalletLockSchemaVersion) !== COMPETITION_WALLET_LOCK_SCHEMA_VERSION ||
      String(batch.competitionWalletLockAuthority ?? "") !== COMPETITION_WALLET_LOCK_AUTHORITY)) {
    throw new Error("Payout batch competition-wallet lock contract is invalid.");
  }
  if (String(batch.sourceResultRef ?? "") !== `weeklyCupResults/${cleanWeek}` ||
      Number(batch.sourceResultVersion) !== RESULT_VERSION ||
      String(batch.sourceResultAuthority ?? "") !== RESULT_FINALIZATION_AUTHORITY) {
    throw new Error("Payout batch source result contract is invalid.");
  }
  const sourceRankingDigest = String(batch.sourceRankingDigestSha256 ?? "").trim().toLowerCase();
  if (!SHA256.test(sourceRankingDigest)) throw new Error("Payout batch source ranking digest is invalid.");
  if (String(batch.fundingVerificationStatusAtClose ?? "").toUpperCase() !== "VERIFIED") {
    throw new Error("Payout approval requires VERIFIED funding at Cup close.");
  }
  if (String(batch.prizeAssetSymbol ?? "").toUpperCase() !== "SKR" ||
      String(batch.prizeMint ?? "") !== OFFICIAL_SKR_MINT ||
      Number(batch.prizeDecimals) !== SKR_DECIMALS) {
    throw new Error("Payout batch SKR identity is invalid.");
  }
  const totalAmountAtomic = atomic(batch.totalAmountAtomic, "Payout batch totalAmountAtomic");
  const expectedDigest = String(batch.payoutManifestDigestSha256 ?? "").trim().toLowerCase();
  if (!SHA256.test(expectedDigest)) throw new Error("Payout batch manifest digest is invalid.");

  const items = [...(itemDocuments ?? [])]
    .map((doc) => doc?.data ? doc.data : doc)
    .sort((a, b) => Number(a?.placement) - Number(b?.placement));
  if (items.length !== Number(batch.winnerCount)) throw new Error("Payout item count does not match batch winnerCount.");

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
    if (String(item.status ?? "").toUpperCase() !== PAYOUT_ITEM_STATUS_READY_FOR_REVIEW ||
        String(item.transferStatus ?? "").toUpperCase() !== TRANSFER_STATUS_NOT_STARTED ||
        item.payoutEnabled === true || item.transferEnabled === true) {
      throw new Error(`Payout item #${placement} is not safely reviewable.`);
    }
    const wallet = canonicalWallet(item.walletAddress, `Payout item #${placement} walletAddress`);
    if (seenWallets.has(wallet)) throw new Error("Payout items must use distinct full wallet addresses.");
    seenWallets.add(wallet);
    requiredString(item.receiptId, `Payout item #${placement} receiptId`, 100);
    if (lockRequired) {
      requiredString(item.ownerUid, `Payout item #${placement} ownerUid`, 128);
      requiredString(item.competitionWalletLockRef, `Payout item #${placement} competitionWalletLockRef`, 300);
      if (String(item.competitionWalletLockAuthority ?? "") !== COMPETITION_WALLET_LOCK_AUTHORITY) {
        throw new Error(`Payout item #${placement} competition-wallet lock authority is invalid.`);
      }
    }
    if (String(item.prizeAssetSymbol ?? "").toUpperCase() !== "SKR" ||
        String(item.sourceWinnerRef ?? "") !== `weeklyCupResults/${cleanWeek}/winners/${placement}`) {
      throw new Error(`Payout item #${placement} source/prize contract is invalid.`);
    }
    total += atomic(item.amountAtomic, `Payout item #${placement} amountAtomic`);
  }
  if (total !== totalAmountAtomic) throw new Error("Payout item amounts do not sum to the batch total.");

  const recomputedDigest = digestLines([
    `week=${cleanWeek}`,
    `mint=${OFFICIAL_SKR_MINT}`,
    `decimals=${SKR_DECIMALS}`,
    `total=${totalAmountAtomic}`,
    `fundingWallet=${canonicalWallet(batch.fundingWalletAddress, "Payout batch fundingWalletAddress")}`,
    `rankingDigest=${sourceRankingDigest}`,
    ...items.map((item) => (lockRequired ? [
      Number(item.placement),
      String(item.ownerUid ?? "").trim(),
      String(item.walletAddress).trim(),
      String(item.receiptId).trim(),
      String(item.amountAtomic).trim(),
      String(item.competitionWalletLockRef ?? "").trim(),
      String(item.competitionWalletLockAuthority ?? "").trim(),
    ] : [
      Number(item.placement),
      String(item.walletAddress).trim(),
      String(item.receiptId).trim(),
      String(item.amountAtomic).trim(),
    ]).join("|")),
  ]);
  if (recomputedDigest !== expectedDigest) throw new Error("Payout manifest digest mismatch. Refusing approval.");

  return {
    manifestDigestSha256: expectedDigest,
    reviewRef: cleanReviewRef,
    items,
    batchPatch: {
      status: PAYOUT_STATUS_APPROVED,
      approvalAuthority: PAYOUT_AUTHORITY,
      approvalReviewRef: cleanReviewRef,
      payoutEnabled: false,
      transferEnabled: false,
      transferStatus: TRANSFER_STATUS_NOT_STARTED,
    },
    itemPatch: {
      status: PAYOUT_ITEM_STATUS_APPROVED,
      approvalAuthority: PAYOUT_AUTHORITY,
      approvalReviewRef: cleanReviewRef,
      payoutEnabled: false,
      transferEnabled: false,
      transferStatus: TRANSFER_STATUS_NOT_STARTED,
    },
  };
}
