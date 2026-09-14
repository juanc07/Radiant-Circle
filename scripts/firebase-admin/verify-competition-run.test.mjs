import test from "node:test";
import assert from "node:assert/strict";
import {
  RUN_VERIFICATION_AUTHORITY,
  RUN_VERIFICATION_METHOD,
  buildRejectedRunDecision,
  buildVerifiedRunDecision,
} from "./competition-run-verification.mjs";
import { CONFIGURATION_AUTHORITY } from "./weekly-cup-config.mjs";

const wallet = "J86vtTs7twTUuS4xfo8H8zaUeFyMNHWPPeEXyL5DscPg";
const startMs = Date.parse("2026-09-07T00:00:00.000Z");
const endMs = Date.parse("2026-09-14T00:00:00.000Z");
const completedAt = Date.parse("2026-09-10T12:00:00.000Z");

function ts(ms) { return { toMillis: () => ms }; }
function cup(overrides = {}) {
  return {
    schemaVersion: 2,
    weekKey: "2026-W37",
    status: "OPEN",
    trustedResultsRequired: true,
    payoutEnabled: false,
    configurationAuthority: CONFIGURATION_AUTHORITY,
    startsAt: ts(startMs),
    endsAt: ts(endMs),
    ...overrides,
  };
}
function receipt(overrides = {}) {
  return {
    schemaVersion: 1,
    receiptId: "11111111-2222-3333-4444-555555555555",
    ownerUid: "firebase-uid",
    walletAddress: wallet,
    utcDayKey: "2026-09-10",
    utcWeekKey: "2026-W37",
    mode: "Ranked",
    score: 4200,
    maxCombo: 18,
    perfectHits: 7,
    radiantHits: 25,
    corruptedHits: 2,
    clientCompletedAtEpochMillis: completedAt,
    scoreAuthority: "client-reported-prototype-not-payout-authority",
    verificationStatus: "UNVERIFIED",
    trustedPlacementEligible: false,
    payoutEligible: false,
    payoutStatus: "NOT_ELIGIBLE",
    submittedAt: ts(completedAt + 1000),
    ...overrides,
  };
}
function verifiedDecision(overrides = {}) {
  return buildVerifiedRunDecision({
    cup: cup(),
    weekKey: "2026-W37",
    receiptId: receipt().receiptId,
    receipt: receipt(),
    evidenceRef: "independent-capture-run-001",
    expected: {
      walletAddress: wallet,
      score: 4200,
      maxCombo: 18,
      perfectHits: 7,
      radiantHits: 25,
      corruptedHits: 2,
      clientCompletedAtEpochMillis: completedAt,
      ...overrides,
    },
  });
}

test("VERIFIED requires an OPEN trusted Cup and exact independent facts", () => {
  const decision = verifiedDecision();
  assert.equal(decision.update.verificationStatus, "VERIFIED");
  assert.equal(decision.update.trustedPlacementEligible, true);
  assert.equal(decision.update.trustedVerificationAuthority, RUN_VERIFICATION_AUTHORITY);
  assert.equal(decision.update.trustedVerificationMethod, RUN_VERIFICATION_METHOD);
  assert.equal(decision.update.trustedVerificationRef, `competitionRunVerifications/${receipt().receiptId}`);
  assert.equal(decision.update.payoutEligible, false);
});

test("independent evidence mismatch fails closed", () => {
  assert.throws(() => verifiedDecision({ score: 4201 }), /evidence mismatch for score/);
});

test("already-decided receipt cannot be promoted again", () => {
  assert.throws(() => buildVerifiedRunDecision({
    cup: cup(), weekKey: "2026-W37", receiptId: receipt().receiptId,
    receipt: receipt({ verificationStatus: "VERIFIED" }), evidenceRef: "evidence",
    expected: { walletAddress: wallet, score: 4200, maxCombo: 18, perfectHits: 7, radiantHits: 25, corruptedHits: 2, clientCompletedAtEpochMillis: completedAt },
  }), /already VERIFIED/);
});

test("receipt outside Cup window is rejected", () => {
  assert.throws(() => buildVerifiedRunDecision({
    cup: cup(), weekKey: "2026-W37", receiptId: receipt().receiptId,
    receipt: receipt({ clientCompletedAtEpochMillis: endMs }), evidenceRef: "evidence",
    expected: { walletAddress: wallet, score: 4200, maxCombo: 18, perfectHits: 7, radiantHits: 25, corruptedHits: 2, clientCompletedAtEpochMillis: endMs },
  }), /outside the Cup window/);
});

test("REJECTED stays placement-ineligible and payout-disabled", () => {
  const decision = buildRejectedRunDecision({
    cup: cup(), weekKey: "2026-W37", receiptId: receipt().receiptId,
    receipt: receipt(), evidenceRef: "manual-review-run-001", reason: "Evidence mismatch",
  });
  assert.equal(decision.update.verificationStatus, "REJECTED");
  assert.equal(decision.update.trustedPlacementEligible, false);
  assert.equal(decision.update.payoutEligible, false);
});
