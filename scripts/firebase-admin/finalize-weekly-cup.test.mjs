import test from "node:test";
import assert from "node:assert/strict";
import {
  RESULT_FINALIZATION_AUTHORITY,
  WINNER_PAYOUT_STATUS,
  bestEligibleResultPerWallet,
  buildFinalizationPlan,
  exactPrizeAllocations,
} from "./weekly-cup-finalization.mjs";
import { CONFIGURATION_AUTHORITY, OFFICIAL_SKR_MINT } from "./weekly-cup-config.mjs";
import { RUN_VERIFICATION_AUTHORITY, RUN_VERIFICATION_METHOD, RUN_VERIFICATION_SCHEMA_VERSION } from "./competition-run-verification.mjs";
import {
  COMPETITION_WALLET_LOCK_AUTHORITY,
  COMPETITION_WALLET_LOCK_SCHEMA_VERSION,
  competitionWalletLockRefPath,
} from "./competition-wallet-lock.mjs";

const startMs = Date.parse("2026-09-07T00:00:00.000Z");
const endMs = Date.parse("2026-09-14T00:00:00.000Z");
const afterEnd = endMs + 10_000;
const walletA = "J86vtTs7twTUuS4xfo8H8zaUeFyMNHWPPeEXyL5DscPg";
const walletB = "HbyQrE2N1V8TPs5HJ9wGDq3M85Zm1i21RmgbLFk39xkS";
const walletC = "9xQeWvG816bUx9EPjHmaT23yvVMdqN5WwQ1Z6J8nPABC";
const walletD = "7YWHMfk9JZeLMg1D8Y87xYXxKj5iM2T6zKQHFrABCD9";
function ts(ms) { return { toMillis: () => ms }; }
function cup(overrides = {}) {
  return {
    schemaVersion: 2,
    weekKey: "2026-W37",
    status: "OPEN",
    sponsorName: "ThinkBloxPH",
    prizeAssetSymbol: "SKR",
    prizeMint: OFFICIAL_SKR_MINT,
    prizeDecimals: 6,
    prizeAmountAtomic: "1000000000",
    placementAllocationsBps: { "1": 5000, "2": 3000, "3": 2000 },
    startsAt: ts(startMs),
    endsAt: ts(endMs),
    fundingWalletAddress: walletB,
    fundingVerificationStatus: "NOT_VERIFIED",
    trustedResultsRequired: true,
    payoutEnabled: false,
    configurationAuthority: CONFIGURATION_AUTHORITY,
    ...overrides,
  };
}
function verifiedReceipt(id, wallet, score, overrides = {}) {
  const completed = startMs + 86_400_000;
  const ownerUid = overrides.ownerUid ?? `uid-${id}`;
  const lockRef = competitionWalletLockRefPath("2026-W37", ownerUid);
  return {
    id,
    data: {
      schemaVersion: 1,
      receiptId: id,
      ownerUid,
      walletAddress: wallet,
      utcWeekKey: "2026-W37",
      mode: "Ranked",
      score,
      maxCombo: 10,
      perfectHits: 4,
      radiantHits: 20,
      corruptedHits: 1,
      clientCompletedAtEpochMillis: completed,
      scoreAuthority: "client-reported-prototype-not-payout-authority",
      submittedAt: ts(completed + 1000),
      verificationStatus: "VERIFIED",
      trustedPlacementEligible: true,
      trustedVerificationSchemaVersion: RUN_VERIFICATION_SCHEMA_VERSION,
      trustedVerificationAuthority: RUN_VERIFICATION_AUTHORITY,
      trustedVerificationMethod: RUN_VERIFICATION_METHOD,
      trustedEvidenceRef: `evidence-${id}`,
      trustedVerificationRef: `competitionRunVerifications/${id}`,
      trustedCompetitionWalletLockRef: lockRef,
      trustedCompetitionWalletAddress: wallet,
      trustedCompetitionWalletLockAuthority: COMPETITION_WALLET_LOCK_AUTHORITY,
      trustedVerifiedAt: ts(endMs + 1000),
      payoutEligible: false,
      payoutStatus: "NOT_ELIGIBLE",
      ...overrides,
    },
  };
}
function competitionWalletLockFor(receipt, overrides = {}) {
  const data = receipt.data;
  return {
    id: data.ownerUid,
    data: {
      schemaVersion: COMPETITION_WALLET_LOCK_SCHEMA_VERSION,
      weekKey: data.utcWeekKey,
      ownerUid: data.ownerUid,
      walletAddress: data.walletAddress,
      firstReceiptId: receipt.id,
      lockAuthority: COMPETITION_WALLET_LOCK_AUTHORITY,
      ...overrides,
    },
  };
}
function verificationFor(receipt, overrides = {}) {
  const data = receipt.data;
  return {
    id: receipt.id,
    data: {
      schemaVersion: RUN_VERIFICATION_SCHEMA_VERSION,
      receiptId: receipt.id,
      weekKey: data.utcWeekKey,
      decision: "VERIFIED",
      walletAddress: data.walletAddress,
      ownerUid: data.ownerUid,
      score: data.score,
      maxCombo: data.maxCombo,
      perfectHits: data.perfectHits,
      radiantHits: data.radiantHits,
      corruptedHits: data.corruptedHits,
      clientCompletedAtEpochMillis: data.clientCompletedAtEpochMillis,
      evidenceRef: data.trustedEvidenceRef,
      verificationAuthority: RUN_VERIFICATION_AUTHORITY,
      verificationMethod: RUN_VERIFICATION_METHOD,
      competitionWalletLockRef: data.trustedCompetitionWalletLockRef,
      competitionWalletAddress: data.trustedCompetitionWalletAddress,
      competitionWalletLockAuthority: data.trustedCompetitionWalletLockAuthority,
      payoutEnabled: false,
      decidedAt: ts(endMs + 1000),
      ...overrides,
    },
  };
}
function trustedInputs(receipts) {
  const lockByOwner = new Map();
  for (const receipt of receipts) {
    if (!lockByOwner.has(receipt.data.ownerUid)) {
      lockByOwner.set(receipt.data.ownerUid, competitionWalletLockFor(receipt));
    }
  }
  return {
    receiptDocuments: receipts,
    verificationDocuments: receipts
      .filter((receipt) => receipt.data.verificationStatus === "VERIFIED")
      .map((receipt) => verificationFor(receipt)),
    competitionWalletLockDocuments: [...lockByOwner.values()],
  };
}
function planFor(receipts, overrides = {}) {
  return buildFinalizationPlan({
    cup: cup(),
    weekKey: "2026-W37",
    ...trustedInputs(receipts),
    nowEpochMillis: afterEnd,
    ...overrides,
  });
}

test("wrong or missing Cup schema/status/finalization fails closed", () => {
  const receipts = [verifiedReceipt("a", walletA, 3000), verifiedReceipt("b", walletB, 2000), verifiedReceipt("c", walletC, 1000)];
  const trusted = trustedInputs(receipts);
  assert.throws(() => buildFinalizationPlan({ cup: cup({ schemaVersion: 99 }), weekKey: "2026-W37", ...trusted, nowEpochMillis: afterEnd }), /schemaVersion/);
  assert.throws(() => buildFinalizationPlan({ cup: cup({ status: "DRAFT" }), weekKey: "2026-W37", ...trusted, nowEpochMillis: afterEnd }), /must be OPEN/);
  assert.throws(() => buildFinalizationPlan({ cup: cup({ finalizationStatus: "FINALIZED" }), weekKey: "2026-W37", ...trusted, nowEpochMillis: afterEnd }), /already finalized/);
});

test("configured placement ranks must be contiguous from first place", () => {
  const receipts = [verifiedReceipt("a", walletA, 3000), verifiedReceipt("b", walletB, 2000), verifiedReceipt("c", walletC, 1000)];
  const trusted = trustedInputs(receipts);
  assert.throws(() => buildFinalizationPlan({
    cup: cup({ placementAllocationsBps: { "1": 5000, "3": 3000, "4": 2000 } }),
    weekKey: "2026-W37",
    ...trusted,
    nowEpochMillis: afterEnd,
  }), /contiguous/);
});

test("Cup cannot close before configured end", () => {
  const receipts = [verifiedReceipt("a", walletA, 3000), verifiedReceipt("b", walletB, 2000), verifiedReceipt("c", walletC, 1000)];
  assert.throws(() => planFor(receipts, { nowEpochMillis: endMs - 1 }), /cannot be finalized before/);
});

test("UNVERIFIED and REJECTED receipts are excluded", () => {
  const receipts = [
    verifiedReceipt("a", walletA, 3000),
    verifiedReceipt("b", walletB, 2000),
    verifiedReceipt("c", walletC, 1000),
    verifiedReceipt("u", walletD, 9999, { verificationStatus: "UNVERIFIED", trustedPlacementEligible: false }),
    verifiedReceipt("r", walletD, 8888, { verificationStatus: "REJECTED", trustedPlacementEligible: false }),
  ];
  const plan = planFor(receipts);
  assert.equal(plan.sourceReceiptCount, 5);
  assert.equal(plan.eligibleReceiptCount, 3);
  assert.equal(plan.winners[0].receiptId, "a");
});

test("VERIFIED receipt without matching immutable verification audit is excluded", () => {
  const receipts = [verifiedReceipt("a", walletA, 3000), verifiedReceipt("b", walletB, 2000), verifiedReceipt("c", walletC, 1000), verifiedReceipt("no-audit", walletD, 9999)];
  const verificationDocuments = receipts.slice(0, 3).map((receipt) => verificationFor(receipt));
  const competitionWalletLockDocuments = receipts.map((receipt) => competitionWalletLockFor(receipt));
  const plan = buildFinalizationPlan({ cup: cup(), weekKey: "2026-W37", receiptDocuments: receipts, verificationDocuments, competitionWalletLockDocuments, nowEpochMillis: afterEnd });
  assert.equal(plan.eligibleReceiptCount, 3);
  assert.equal(plan.winners[0].receiptId, "a");
});

test("mismatched verification audit cannot authorize placement", () => {
  const receipts = [verifiedReceipt("a", walletA, 3000), verifiedReceipt("b", walletB, 2000), verifiedReceipt("c", walletC, 1000), verifiedReceipt("mismatch", walletD, 9999)];
  const verificationDocuments = receipts.map((receipt) => verificationFor(receipt));
  verificationDocuments[3] = verificationFor(receipts[3], { score: 1111 });
  const competitionWalletLockDocuments = receipts.map((receipt) => competitionWalletLockFor(receipt));
  const plan = buildFinalizationPlan({ cup: cup(), weekKey: "2026-W37", receiptDocuments: receipts, verificationDocuments, competitionWalletLockDocuments, nowEpochMillis: afterEnd });
  assert.equal(plan.eligibleReceiptCount, 3);
});

test("VERIFIED string without trusted Phase 12D attestation is excluded", () => {
  const forged = verifiedReceipt("forged", walletD, 9999, { trustedVerificationAuthority: "client" });
  const receipts = [forged, verifiedReceipt("a", walletA, 3000), verifiedReceipt("b", walletB, 2000), verifiedReceipt("c", walletC, 1000)];
  const plan = planFor(receipts);
  assert.equal(plan.eligibleReceiptCount, 3);
});

test("same Firebase account cannot produce trusted eligible receipts from two Cup wallets", () => {
  const sameUid = "shared-firebase-account";
  const a = verifiedReceipt("a-lock", walletA, 5000, { ownerUid: sameUid });
  const b = verifiedReceipt("b-switch", walletB, 9999, { ownerUid: sameUid });
  const c = verifiedReceipt("c", walletC, 3000);
  const d = verifiedReceipt("d", walletD, 2000);
  const receipts = [a, b, c, d];
  const verificationDocuments = receipts.map((receipt) => verificationFor(receipt));
  const competitionWalletLockDocuments = [
    competitionWalletLockFor(a),
    competitionWalletLockFor(c),
    competitionWalletLockFor(d),
  ];
  const plan = buildFinalizationPlan({
    cup: cup(),
    weekKey: "2026-W37",
    receiptDocuments: receipts,
    verificationDocuments,
    competitionWalletLockDocuments,
    nowEpochMillis: afterEnd,
  });
  assert.equal(plan.eligibleReceiptCount, 3);
  assert.equal(plan.winners.some((winner) => winner.receiptId === "b-switch"), false);
});

test("one best eligible result per wallet uses existing deterministic tiebreak order", () => {
  const early = verifiedReceipt("early", walletA, 5000, { maxCombo: 20, perfectHits: 8, clientCompletedAtEpochMillis: startMs + 1000 }).data;
  const late = verifiedReceipt("late", walletA, 5000, { maxCombo: 20, perfectHits: 8, clientCompletedAtEpochMillis: startMs + 2000 }).data;
  const betterCombo = verifiedReceipt("combo", walletB, 5000, { maxCombo: 21, perfectHits: 1 }).data;
  const ranked = bestEligibleResultPerWallet([
    { ...early, receiptId: "early" },
    { ...late, receiptId: "late" },
    { ...betterCombo, receiptId: "combo" },
  ]);
  assert.equal(ranked[0].receiptId, "combo");
  assert.equal(ranked[1].receiptId, "early");
});

test("exact 50/30/20 SKR allocation is integer-safe and sums to prize", () => {
  const allocations = exactPrizeAllocations(1_000_000_000n, new Map([[1, 5000], [2, 3000], [3, 2000]]));
  assert.deepEqual([...allocations.values()].map(String), ["500000000", "300000000", "200000000"]);
  assert.throws(() => exactPrizeAllocations(1n, new Map([[1, 5000], [2, 5000]])), /cannot represent/);
});

test("bare VERIFIED funding string degrades to NOT_VERIFIED at close", () => {
  const receipts = [verifiedReceipt("a", walletA, 3000), verifiedReceipt("b", walletB, 2000), verifiedReceipt("c", walletC, 1000)];
  const trusted = trustedInputs(receipts);
  const plan = buildFinalizationPlan({
    cup: cup({ fundingVerificationStatus: "VERIFIED" }),
    weekKey: "2026-W37",
    ...trusted,
    nowEpochMillis: afterEnd,
  });
  assert.equal(plan.fundingVerificationStatusAtClose, "NOT_VERIFIED");
  assert.equal(plan.result.payoutReady, false);
});

test("funding NOT_VERIFIED still allows competitive freeze but never payout readiness", () => {
  const receipts = [verifiedReceipt("a", walletA, 3000), verifiedReceipt("b", walletB, 2000), verifiedReceipt("c", walletC, 1000)];
  const plan = planFor(receipts);
  assert.equal(plan.result.finalizationAuthority, RESULT_FINALIZATION_AUTHORITY);
  assert.equal(plan.fundingVerificationStatusAtClose, "NOT_VERIFIED");
  assert.equal(plan.result.payoutEnabled, false);
  assert.equal(plan.result.payoutReady, false);
  assert.equal(plan.winners[0].payoutStatus, WINNER_PAYOUT_STATUS);
});

test("finalization refuses to invent missing configured winners", () => {
  const receipts = [verifiedReceipt("a", walletA, 3000), verifiedReceipt("b", walletB, 2000)];
  assert.throws(() => planFor(receipts), /requires 3 configured placements/);
});

test("admin early close requires an explicit audit reason", () => {
  const earlyNow = startMs + 3 * 86_400_000;
  const receipts = [
    verifiedReceipt("a", walletA, 3000, { trustedVerifiedAt: ts(earlyNow - 1000) }),
    verifiedReceipt("b", walletB, 2000, { trustedVerifiedAt: ts(earlyNow - 1000) }),
    verifiedReceipt("c", walletC, 1000, { trustedVerifiedAt: ts(earlyNow - 1000) }),
  ];
  const verificationDocuments = receipts.map((receipt) => verificationFor(receipt, { decidedAt: ts(earlyNow - 500) }));
  assert.throws(() => buildFinalizationPlan({
    cup: cup(),
    weekKey: "2026-W37",
    receiptDocuments: receipts,
    verificationDocuments,
    competitionWalletLockDocuments: receipts.map((receipt) => competitionWalletLockFor(receipt)),
    nowEpochMillis: earlyNow,
    allowEarlyClose: true,
  }), /audit reason/);
});

test("admin early close freezes only trusted runs at or before the forced cutoff", () => {
  const earlyNow = startMs + 3 * 86_400_000;
  const common = {
    clientCompletedAtEpochMillis: earlyNow - 10_000,
    submittedAt: ts(earlyNow - 8_000),
    trustedVerifiedAt: ts(earlyNow - 1_000),
  };
  const receipts = [
    verifiedReceipt("a", walletA, 3000, common),
    verifiedReceipt("b", walletB, 2000, common),
    verifiedReceipt("c", walletC, 1000, common),
    verifiedReceipt("future", walletD, 9999, {
      clientCompletedAtEpochMillis: earlyNow + 1_000,
      submittedAt: ts(earlyNow + 2_000),
      trustedVerifiedAt: ts(earlyNow - 1_000),
    }),
  ];
  const verificationDocuments = receipts.map((receipt) => verificationFor(receipt, { decidedAt: ts(earlyNow - 500) }));
  const plan = buildFinalizationPlan({
    cup: cup(),
    weekKey: "2026-W37",
    receiptDocuments: receipts,
    verificationDocuments,
    competitionWalletLockDocuments: receipts.map((receipt) => competitionWalletLockFor(receipt)),
    nowEpochMillis: earlyNow,
    allowEarlyClose: true,
    earlyCloseReason: "Operator-authorized live payout test",
  });
  assert.equal(plan.closedEarly, true);
  assert.equal(plan.scheduledEndsAtEpochMillis, endMs);
  assert.equal(plan.effectiveEndsAtEpochMillis, earlyNow);
  assert.equal(plan.earlyCloseReason, "Operator-authorized live payout test");
  assert.equal(plan.sourceReceiptCount, 4);
  assert.equal(plan.eligibleReceiptCount, 3);
  assert.equal(plan.winners[0].receiptId, "a");
  assert.equal(plan.result.closedEarly, true);
  assert.equal(plan.result.effectiveEndsAtEpochMillis, earlyNow);
});
