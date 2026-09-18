import test from "node:test";
import assert from "node:assert/strict";
import {
  CONFIGURATION_AUTHORITY,
  OFFICIAL_SKR_MINT,
  assertSafeExistingConfig,
  buildWeeklyCupConfig,
  isoWeekBounds,
  parsePlacements,
  parseSkrAmountAtomic,
} from "./weekly-cup-config.mjs";

test("SKR decimal amount converts to exact atomic string", () => {
  assert.equal(parseSkrAmountAtomic("1000"), "1000000000");
  assert.equal(parseSkrAmountAtomic("1.234567"), "1234567");
});

test("default placement split totals 100 percent", () => {
  assert.deepEqual(parsePlacements(), { "1": 5000, "2": 3000, "3": 2000 });
});

test("invalid placement total is rejected", () => {
  assert.throws(() => parsePlacements("1:50,2:30"), /total 100/);
});

test("ISO week bounds cover exactly seven days", () => {
  const bounds = isoWeekBounds("2026-W37");
  assert.equal(new Date(bounds.startMs).toISOString(), "2026-09-07T00:00:00.000Z");
  assert.equal(new Date(bounds.endMs).toISOString(), "2026-09-14T00:00:00.000Z");
});

test("trusted config hard-codes official SKR identity and payout false", () => {
  const config = buildWeeklyCupConfig({
    weekKey: "2026-W37",
    status: "OPEN",
    sponsorName: "ThinkBloxPH",
    prizeSkr: "1000",
  });

  assert.equal(config.prizeMint, OFFICIAL_SKR_MINT);
  assert.equal(config.configurationAuthority, CONFIGURATION_AUTHORITY);
  assert.equal(config.fundingVerificationStatus, "NOT_CONFIGURED");
  assert.equal(config.trustedResultsRequired, true);
  assert.equal(config.payoutEnabled, false);
});

test("funding wallet is configuration only and starts NOT_VERIFIED", () => {
  const config = buildWeeklyCupConfig({
    weekKey: "2026-W37",
    status: "ANNOUNCED",
    sponsorName: "ThinkBloxPH",
    prizeSkr: "1000",
    fundingWalletAddress: "J86vtTs7twTUuS4xfo8H8zaUeFyMNHWPPeEXyL5DscPg",
  });

  assert.equal(config.fundingVerificationStatus, "NOT_VERIFIED");
  assert.equal(config.payoutEnabled, false);
});

test("Phase 12B refuses ambiguous VERIFIED state without Phase 12C evidence", () => {
  assert.throws(
    () => assertSafeExistingConfig({ schemaVersion: 2, fundingVerificationStatus: "VERIFIED" }, {
      prizeMint: OFFICIAL_SKR_MINT,
      prizeDecimals: 6,
      prizeAmountAtomic: "1000000000",
      fundingWalletAddress: null,
    }),
    /VERIFIED without Phase 12C evidence/,
  );
});

test("Phase 12B may update status while preserving matching Phase 12C evidence", () => {
  const result = assertSafeExistingConfig({
    schemaVersion: 2,
    prizeMint: OFFICIAL_SKR_MINT,
    prizeDecimals: 6,
    prizeAmountAtomic: "1000000000",
    fundingWalletAddress: "J86vtTs7twTUuS4xfo8H8zaUeFyMNHWPPeEXyL5DscPg",
    fundingVerificationStatus: "VERIFIED",
    fundingVerificationAuthority: "trusted-admin-phase12c",
    fundingObservedAmountAtomic: "1200000000",
  }, {
    prizeMint: OFFICIAL_SKR_MINT,
    prizeDecimals: 6,
    prizeAmountAtomic: "1000000000",
    fundingWalletAddress: null,
  });
  assert.equal(result.preservePhase12cEvidence, true);
});

test("Phase 12B refuses prize changes after Phase 12C evidence exists", () => {
  assert.throws(
    () => assertSafeExistingConfig({
      schemaVersion: 2,
      prizeMint: OFFICIAL_SKR_MINT,
      prizeDecimals: 6,
      prizeAmountAtomic: "1000000000",
      fundingWalletAddress: "J86vtTs7twTUuS4xfo8H8zaUeFyMNHWPPeEXyL5DscPg",
      fundingVerificationStatus: "NOT_VERIFIED",
      fundingVerificationAuthority: "trusted-admin-phase12c",
      fundingObservedAmountAtomic: "500000000",
    }, {
      prizeMint: OFFICIAL_SKR_MINT,
      prizeDecimals: 6,
      prizeAmountAtomic: "2000000000",
      fundingWalletAddress: null,
    }),
    /tied to the existing prize/,
  );
});


test("Phase 12B cannot reopen or mutate a Phase 12D finalized Cup", () => {
  assert.throws(
    () => assertSafeExistingConfig({
      schemaVersion: 2,
      finalizationStatus: "FINALIZED",
      trustedResultAuthority: "trusted-admin-phase12d",
      trustedResultRef: "weeklyCupResults/2026-W37",
      payoutEnabled: false,
    }, {
      prizeMint: OFFICIAL_SKR_MINT,
      prizeDecimals: 6,
      prizeAmountAtomic: "1000000000",
      fundingWalletAddress: null,
    }),
    /finalized results are immutable/,
  );
});
