import test from "node:test";
import assert from "node:assert/strict";
import {
  FUNDING_NETWORK,
  FUNDING_VERIFICATION_AUTHORITY,
  assertCupFundingTarget,
  buildFundingEvidence,
  formatAtomicSkr,
  parseTokenAccountsByOwnerResponse,
} from "./skr-funding-verification.mjs";
import {
  CONFIGURATION_AUTHORITY,
  OFFICIAL_SKR_MINT,
} from "./weekly-cup-config.mjs";

function cup(overrides = {}) {
  return {
    schemaVersion: 2,
    weekKey: "2026-W37",
    status: "ANNOUNCED",
    sponsorName: "ThinkBloxPH",
    prizeAssetSymbol: "SKR",
    prizeMint: OFFICIAL_SKR_MINT,
    prizeDecimals: 6,
    prizeAmountAtomic: "1000000000",
    trustedResultsRequired: true,
    payoutEnabled: false,
    configurationAuthority: CONFIGURATION_AUTHORITY,
    fundingWalletAddress: null,
    ...overrides,
  };
}

const wallet = "J86vtTs7twTUuS4xfo8H8zaUeFyMNHWPPeEXyL5DscPg";

function rpcResponse(amounts, { slot = 123456789, frozenIndexes = [] } = {}) {
  return {
    jsonrpc: "2.0",
    result: {
      context: { slot },
      value: amounts.map((amount, index) => ({
        pubkey: `TokenAccount${index}1111111111111111111111111111`,
        account: {
          data: {
            parsed: {
              info: {
                mint: OFFICIAL_SKR_MINT,
                owner: wallet,
                state: frozenIndexes.includes(index) ? "frozen" : "initialized",
                tokenAmount: { amount: String(amount), decimals: 6, uiAmountString: "0" },
              },
            },
          },
        },
      })),
    },
  };
}

test("funding target requires trusted Phase 12B SKR config", () => {
  const result = assertCupFundingTarget(cup(), wallet);
  assert.equal(result.requiredAtomic, 1000000000n);
  assert.equal(result.wallet, wallet);
  assert.throws(() => assertCupFundingTarget(cup({ prizeMint: "WrongMint" }), wallet), /official SKR mint/);
  assert.throws(() => assertCupFundingTarget(cup({ payoutEnabled: true }), wallet), /payoutEnabled=true/);
});

test("different configured wallet requires explicit replacement", () => {
  const other = "HbyQrE2N1V8TPs5HJ9wGDq3M85Zm1i21RmgbLFk39xkS";
  assert.throws(() => assertCupFundingTarget(cup({ fundingWalletAddress: other }), wallet), /replace-funding-wallet/);
  assert.equal(assertCupFundingTarget(cup({ fundingWalletAddress: other }), wallet, { allowWalletReplace: true }).wallet, wallet);
});

test("RPC parser sums exact liquid SKR across token accounts", () => {
  const parsed = parseTokenAccountsByOwnerResponse(rpcResponse(["400000000", "650000000"]), wallet);
  assert.equal(parsed.observedAmountAtomic, "1050000000");
  assert.equal(parsed.tokenAccountCount, 2);
  assert.equal(parsed.frozenTokenAccountCount, 0);
  assert.equal(parsed.slot, 123456789);
});

test("frozen token accounts are excluded from transferable funding", () => {
  const parsed = parseTokenAccountsByOwnerResponse(rpcResponse(["900000000", "500000000"], { frozenIndexes: [1] }), wallet);
  assert.equal(parsed.observedAmountAtomic, "900000000");
  assert.equal(parsed.tokenAccountCount, 2);
  assert.equal(parsed.frozenTokenAccountCount, 1);
});

test("funding becomes VERIFIED only when observed liquid SKR covers required amount", () => {
  const verified = buildFundingEvidence({
    cup: cup(),
    walletAddress: wallet,
    snapshot: { observedAmountAtomic: "1000000000", tokenAccountCount: 1, frozenTokenAccountCount: 0, slot: 42 },
    checkedAtEpochMillis: 123456,
  });
  assert.equal(verified.fundingVerificationStatus, "VERIFIED");
  assert.equal(verified.fundingVerificationAuthority, FUNDING_VERIFICATION_AUTHORITY);
  assert.equal(verified.fundingVerificationNetwork, FUNDING_NETWORK);
  assert.equal(verified.payoutEnabled, false);

  const short = buildFundingEvidence({
    cup: cup(),
    walletAddress: wallet,
    snapshot: { observedAmountAtomic: "999999999", tokenAccountCount: 1, frozenTokenAccountCount: 0, slot: 43 },
    checkedAtEpochMillis: 123457,
  });
  assert.equal(short.fundingVerificationStatus, "NOT_VERIFIED");
  assert.equal(short.fundingVerifiedAtEpochMillis, null);
});

test("atomic SKR formatter never uses floating point", () => {
  assert.equal(formatAtomicSkr("1000000000"), "1000 SKR");
  assert.equal(formatAtomicSkr("1000500000"), "1000.5 SKR");
});
