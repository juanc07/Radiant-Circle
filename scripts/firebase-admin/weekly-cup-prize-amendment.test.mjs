import test from "node:test";
import assert from "node:assert/strict";
import {
  FUNDING_EVIDENCE_FIELDS_TO_RESET,
  PRIZE_AMENDMENT_AUTHORITY,
  buildPrizeAmendmentPlan,
} from "./weekly-cup-prize-amendment.mjs";
import {
  CONFIGURATION_AUTHORITY,
  OFFICIAL_SKR_MINT,
  SCHEMA_VERSION,
  SKR_DECIMALS,
} from "./weekly-cup-config.mjs";

const OLD_WALLET = "HbyQrE2N1V8TPs5HJ9wGDq3M85Zm1i21RmgbLFk39xkS";
const NEW_WALLET = "EqLUDQpfZrCJcQ5obVWzjuBn1v4sHH8PxVfcCg4rppzj";

function cup(overrides = {}) {
  return {
    schemaVersion: SCHEMA_VERSION,
    weekKey: "2026-W38",
    status: "OPEN",
    configurationAuthority: CONFIGURATION_AUTHORITY,
    prizeAssetSymbol: "SKR",
    prizeMint: OFFICIAL_SKR_MINT,
    prizeDecimals: SKR_DECIMALS,
    prizeAmountAtomic: "1000000000",
    fundingWalletAddress: OLD_WALLET,
    fundingVerificationStatus: "NOT_VERIFIED",
    fundingVerificationAuthority: "trusted-admin-phase12c",
    fundingRequiredAmountAtomic: "1000000000",
    fundingObservedAmountAtomic: "0",
    trustedResultsRequired: true,
    payoutEnabled: false,
    ...overrides,
  };
}

const input = {
  weekKey: "2026-W38",
  newPrizeSkr: "300",
  fundingWalletAddress: NEW_WALLET,
  reason: "Operator-authorized reduced-value live payout test",
};

test("OPEN trusted Cup may be explicitly amended from 1000 SKR to 300 SKR", () => {
  const plan = buildPrizeAmendmentPlan({ cup: cup(), ...input });
  assert.equal(plan.authority, PRIZE_AMENDMENT_AUTHORITY);
  assert.equal(plan.previousPrizeAmountAtomic, "1000000000");
  assert.equal(plan.newPrizeAmountAtomic, "300000000");
  assert.equal(plan.previousFundingWalletAddress, OLD_WALLET);
  assert.equal(plan.newFundingWalletAddress, NEW_WALLET);
  assert.equal(plan.fundingEvidenceResetRequired, true);
  assert.equal(plan.payoutEnabled, false);
  assert.match(plan.amendmentDigest, /^[0-9a-f]{64}$/);
});

test("prize amendment refuses CLOSED or finalized Cups", () => {
  assert.throws(
    () => buildPrizeAmendmentPlan({ cup: cup({ status: "CLOSED" }), ...input }),
    /allowed only while the Cup is DRAFT, ANNOUNCED, or OPEN/,
  );
  assert.throws(
    () => buildPrizeAmendmentPlan({ cup: cup({ finalizationStatus: "FINALIZED" }), ...input }),
    /finalization evidence/,
  );
});

test("prize amendment refuses payout-enabled state and no-op amount", () => {
  assert.throws(
    () => buildPrizeAmendmentPlan({ cup: cup({ payoutEnabled: true }), ...input }),
    /payout state is enabled\/ready/,
  );
  assert.throws(
    () => buildPrizeAmendmentPlan({ cup: cup(), ...input, newPrizeSkr: "1000" }),
    /identical to the current prize amount/,
  );
});

test("amendment digest is deterministic and binds wallet/reason", () => {
  const a = buildPrizeAmendmentPlan({ cup: cup(), ...input });
  const b = buildPrizeAmendmentPlan({ cup: cup(), ...input });
  const c = buildPrizeAmendmentPlan({ cup: cup(), ...input, reason: "Different operator-authorized live payout test" });
  assert.equal(a.amendmentDigest, b.amendmentDigest);
  assert.notEqual(a.amendmentDigest, c.amendmentDigest);
});

test("funding reset list removes every Phase 12C evidence field tied to the old target", () => {
  for (const required of [
    "fundingRequiredAmountAtomic",
    "fundingObservedAmountAtomic",
    "fundingVerificationAuthority",
    "fundingCheckedAt",
    "fundingVerifiedAt",
  ]) {
    assert.ok(FUNDING_EVIDENCE_FIELDS_TO_RESET.includes(required), `${required} must be reset`);
  }
});
