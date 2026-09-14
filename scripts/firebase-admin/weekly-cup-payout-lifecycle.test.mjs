import test from "node:test";
import assert from "node:assert/strict";
import {
  PAYOUT_AUTHORITY,
  PAYOUT_ITEM_STATUS_APPROVED,
  PAYOUT_STATUS_APPROVED,
  PAYOUT_STATUS_READY_FOR_REVIEW,
  TRANSFER_STATUS_NOT_STARTED,
  buildPayoutApprovalPlan,
  buildPayoutPreparationPlan,
} from "./weekly-cup-payout-lifecycle.mjs";
import { OFFICIAL_SKR_MINT } from "./weekly-cup-config.mjs";

const weekKey = "2026-W38";
const walletA = "J86vtTs7twTUuS4xfo8H8zaUeFyMNHWPPeEXyL5DscPg";
const walletB = "HbyQrE2N1V8TPs5HJ9wGDq3M85Zm1i21RmgbLFk39xkS";
const walletC = "9xQeWvG816bUx9EPjHmaT23yvVMdqN5WwQ1Z6J8nPABC";
function ts(ms = 1789948800000) { return { toMillis: () => ms }; }
function cup(overrides = {}) {
  return {
    schemaVersion: 2,
    weekKey,
    status: "CLOSED",
    configurationAuthority: "trusted-admin-phase12b",
    prizeAssetSymbol: "SKR",
    prizeMint: OFFICIAL_SKR_MINT,
    prizeDecimals: 6,
    prizeAmountAtomic: "1000000000",
    placementAllocationsBps: { "1": 5000, "2": 3000, "3": 2000 },
    fundingWalletAddress: walletB,
    fundingVerificationStatus: "VERIFIED",
    fundingRequiredAmountAtomic: "1000000000",
    fundingObservedAmountAtomic: "1000000000",
    fundingVerificationSlot: 123456,
    fundingVerificationNetwork: "mainnet-beta",
    fundingVerificationMint: OFFICIAL_SKR_MINT,
    fundingVerificationCommitment: "finalized",
    fundingVerificationAuthority: "trusted-admin-phase12c",
    fundingVerificationSchemaVersion: 1,
    fundingCheckedAt: ts(),
    fundingVerifiedAt: ts(),
    finalizationStatus: "FINALIZED",
    trustedResultAuthority: "trusted-admin-phase12d",
    trustedResultRef: `weeklyCupResults/${weekKey}`,
    payoutEnabled: false,
    ...overrides,
  };
}
function result(overrides = {}) {
  return {
    schemaVersion: 1,
    resultVersion: 1,
    weekKey,
    finalizationStatus: "FINALIZED",
    finalizationAuthority: "trusted-admin-phase12d",
    finalizedAt: ts(),
    prizeAssetSymbol: "SKR",
    prizeMint: OFFICIAL_SKR_MINT,
    prizeDecimals: 6,
    prizeAmountAtomic: "1000000000",
    placementAllocationsBps: { "1": 5000, "2": 3000, "3": 2000 },
    fundingVerificationStatusAtClose: "VERIFIED",
    winnerCount: 3,
    rankingDigestSha256: "a".repeat(64),
    payoutEnabled: false,
    payoutReady: false,
    ...overrides,
  };
}
function winner(placement, walletAddress, amountAtomic, overrides = {}) {
  return {
    id: String(placement),
    data: {
      schemaVersion: 1,
      resultVersion: 1,
      weekKey,
      placement,
      walletAddress,
      receiptId: `receipt-${placement}`,
      prizeAmountAtomic: amountAtomic,
      prizeAssetSymbol: "SKR",
      payoutStatus: "NOT_ENABLED",
      payoutEnabled: false,
      payoutReady: false,
      fundingVerificationStatusAtClose: "VERIFIED",
      resultAuthority: "trusted-admin-phase12d",
      ...overrides,
    },
  };
}
function winners() {
  return [
    winner(1, walletA, "500000000"),
    winner(2, walletB, "300000000"),
    winner(3, walletC, "200000000"),
  ];
}
function preparedPlan(overrides = {}) {
  return buildPayoutPreparationPlan({ cup: cup(), result: result(), winnerDocuments: winners(), weekKey, ...overrides });
}

test("preparation requires a trusted CLOSED Phase 12D Cup/result", () => {
  assert.throws(() => preparedPlan({ cup: cup({ status: "OPEN" }) }), /CLOSED/);
  assert.throws(() => preparedPlan({ cup: cup({ trustedResultAuthority: "client" }) }), /trustedResultAuthority/);
  assert.throws(() => preparedPlan({ result: result({ finalizationAuthority: "client" }) }), /finalizationAuthority/);
});

test("preparation requires VERIFIED Phase 12C funding at close", () => {
  assert.throws(() => preparedPlan({ cup: cup({ fundingVerificationStatus: "NOT_VERIFIED" }) }), /funding VERIFIED/);
  assert.throws(() => preparedPlan({ result: result({ fundingVerificationStatusAtClose: "NOT_VERIFIED" }) }), /not finalized with VERIFIED funding/);
});

test("bare VERIFIED funding label without Phase 12C evidence fails closed", () => {
  assert.throws(() => preparedPlan({ cup: cup({ fundingVerificationAuthority: null, fundingVerificationSlot: null }) }), /funding VERIFIED/);
});

test("preparation rejects payout-enabled source state", () => {
  assert.throws(() => preparedPlan({ cup: cup({ payoutEnabled: true }) }), /payoutEnabled/);
  assert.throws(() => preparedPlan({ result: result({ payoutReady: true }) }), /payout readiness/);
});

test("preparation validates exact winner allocation and distinct wallets", () => {
  const wrongAmount = winners();
  wrongAmount[0].data.prizeAmountAtomic = "499999999";
  assert.throws(() => preparedPlan({ winnerDocuments: wrongAmount }), /exact configured allocation/);

  const duplicateWallet = winners();
  duplicateWallet[2].data.walletAddress = walletA;
  assert.throws(() => preparedPlan({ winnerDocuments: duplicateWallet }), /distinct full wallet/);
});

test("preparation emits deterministic review-only manifest with no transfer authority", () => {
  const first = preparedPlan();
  const second = preparedPlan({ winnerDocuments: [...winners()].reverse() });
  assert.equal(first.manifestDigestSha256, second.manifestDigestSha256);
  assert.equal(first.batch.status, PAYOUT_STATUS_READY_FOR_REVIEW);
  assert.equal(first.batch.authority, PAYOUT_AUTHORITY);
  assert.equal(first.batch.totalAmountAtomic, "1000000000");
  assert.equal(first.batch.payoutEnabled, false);
  assert.equal(first.batch.transferEnabled, false);
  assert.equal(first.batch.transferStatus, TRANSFER_STATUS_NOT_STARTED);
  assert.deepEqual(first.items.map((item) => item.amountAtomic), ["500000000", "300000000", "200000000"]);
});

test("approval requires READY_FOR_REVIEW and exact unchanged manifest", () => {
  const plan = preparedPlan();
  const approval = buildPayoutApprovalPlan({ batch: plan.batch, itemDocuments: plan.items, weekKey, reviewRef: "operator-review-w38" });
  assert.equal(approval.batchPatch.status, PAYOUT_STATUS_APPROVED);
  assert.equal(approval.itemPatch.status, PAYOUT_ITEM_STATUS_APPROVED);
  assert.equal(approval.batchPatch.transferEnabled, false);
  assert.equal(approval.itemPatch.payoutEnabled, false);

  assert.throws(() => buildPayoutApprovalPlan({
    batch: { ...plan.batch, status: "APPROVED" },
    itemDocuments: plan.items,
    weekKey,
    reviewRef: "review",
  }), /READY_FOR_REVIEW/);

  const changed = plan.items.map((item) => ({ ...item }));
  changed[0].amountAtomic = "499999999";
  assert.throws(() => buildPayoutApprovalPlan({ batch: plan.batch, itemDocuments: changed, weekKey, reviewRef: "review" }), /sum|digest/i);
});

test("approval rejects duplicate wallets and any transfer-enabled state", () => {
  const plan = preparedPlan();
  const duplicate = plan.items.map((item) => ({ ...item }));
  duplicate[2].walletAddress = duplicate[0].walletAddress;
  assert.throws(() => buildPayoutApprovalPlan({ batch: plan.batch, itemDocuments: duplicate, weekKey, reviewRef: "review" }), /distinct full wallet/);
  assert.throws(() => buildPayoutApprovalPlan({
    batch: { ...plan.batch, transferEnabled: true },
    itemDocuments: plan.items,
    weekKey,
    reviewRef: "review",
  }), /must remain disabled/);
});
