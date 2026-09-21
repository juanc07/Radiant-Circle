import test from "node:test";
import assert from "node:assert/strict";
import { buildSummary, deriveNextAction, formatAtomic } from "./radiant-cup-operator.mjs";

test("formatAtomic formats SKR atomic values", () => {
  assert.equal(formatAtomic("300000000", 6), "300");
  assert.equal(formatAtomic("150500000", 6), "150.5");
});

test("next action starts with Cup setup", () => {
  const summary = buildSummary({ cup: null });
  assert.equal(summary.nextAction.code, "SETUP_CUP");
});

test("funded Cup with unverified receipts asks for review", () => {
  const summary = buildSummary({
    cup: { weekKey: "2026-W39", status: "OPEN", fundingVerificationStatus: "VERIFIED", endsAt: "2099-09-30T00:00:00Z" },
    receipts: [{ ownerUid: "u1", verificationStatus: "UNVERIFIED" }],
    locks: [{ id: "u1" }],
  });
  assert.equal(summary.nextAction.code, "REVIEW_RUNS");
});

test("approved payout selects first unpaid placement", () => {
  const action = deriveNextAction({
    cupExists: true,
    fundingStatus: "VERIFIED",
    resultExists: true,
    payoutExists: true,
    payoutStatus: "APPROVED",
    unverifiedReceipts: 0,
    cupEndsAtMillis: 0,
    payoutItems: [
      { placement: 1, status: "PAID", transferStatus: "FINALIZED" },
      { placement: 2, status: "APPROVED", transferStatus: "NOT_STARTED" },
    ],
  });
  assert.equal(action.code, "EXECUTE_TRANSFER");
  assert.equal(action.placement, 2);
});

test("submitted transfer demands reconciliation, never automatic resend", () => {
  const action = deriveNextAction({
    cupExists: true,
    fundingStatus: "VERIFIED",
    resultExists: true,
    payoutExists: true,
    payoutStatus: "PAYMENT_PENDING",
    unverifiedReceipts: 0,
    cupEndsAtMillis: 0,
    payoutItems: [{ placement: 1, status: "PAYMENT_PENDING", transferStatus: "SUBMITTED" }],
  });
  assert.equal(action.code, "RECONCILE_TRANSFER");
});
