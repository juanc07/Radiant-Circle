import test from "node:test";
import assert from "node:assert/strict";
import {
  BATCH_STATUS_PAYMENT_PENDING,
  BATCH_STATUS_PAID,
  ITEM_STATUS_PAID,
  TOKEN_PROGRAM_ID,
  TRANSFER_EXECUTION_AUTHORITY,
  TRANSFER_STATUS_FINALIZED,
  atomicToUiString,
  assertRecipientSystemWallet,
  assertSkrMintProgram,
  buildCompletionPlan,
  buildTransferItemPlan,
  computePayoutManifestDigest,
  parseSignatureStatus,
  parseSplTokenTransferSignature,
  parseTransferableTokenAccounts,
  selectSourceTokenAccount,
  verifyFinalizedTransferTransaction,
} from "./weekly-cup-skr-transfer.mjs";
import {
  PAYOUT_AUTHORITY,
  PAYOUT_ITEM_STATUS_APPROVED,
  PAYOUT_STATUS_APPROVED,
  TRANSFER_STATUS_NOT_STARTED,
} from "./weekly-cup-payout-lifecycle.mjs";
import { OFFICIAL_SKR_MINT, SKR_DECIMALS } from "./weekly-cup-config.mjs";
import {
  COMPETITION_WALLET_LOCK_AUTHORITY,
  COMPETITION_WALLET_LOCK_SCHEMA_VERSION,
  competitionWalletLockRefPath,
} from "./competition-wallet-lock.mjs";

const funding = "HbyQrE2N1V8TPs5HJ9wGDq3M85Zm1i21RmgbLFk39xkS";
const wallets = [
  "J86vtTs7twTUuS4xfo8H8zaUeFyMNHWPPeEXyL5DscPg",
  "7YttLkHDoWckAKFvxDZksLB4Z4xDcTzzM9H6nTqziTzY",
  "3KP1vQ68G5eWYHEtubsgtKMg5ysHJZ7p3YBfPzWD4kNz",
];
const signatures = [
  "5".repeat(88),
  "6".repeat(88),
  "7".repeat(88),
];

function fixture() {
  const items = wallets.map((walletAddress, index) => ({
    schemaVersion: 1,
    lifecycleVersion: 1,
    weekKey: "2026-W38",
    placement: index + 1,
    walletAddress,
    receiptId: `receipt-${index + 1}`,
    amountAtomic: ["500000000", "300000000", "200000000"][index],
    prizeAssetSymbol: "SKR",
    status: PAYOUT_ITEM_STATUS_APPROVED,
    transferStatus: TRANSFER_STATUS_NOT_STARTED,
    payoutEnabled: false,
    transferEnabled: false,
    authority: PAYOUT_AUTHORITY,
    sourceWinnerRef: `weeklyCupResults/2026-W38/winners/${index + 1}`,
  }));
  const batch = {
    schemaVersion: 1,
    lifecycleVersion: 1,
    weekKey: "2026-W38",
    status: PAYOUT_STATUS_APPROVED,
    authority: PAYOUT_AUTHORITY,
    sourceRankingDigestSha256: "a".repeat(64),
    fundingWalletAddress: funding,
    fundingVerificationStatusAtClose: "VERIFIED",
    prizeAssetSymbol: "SKR",
    prizeMint: OFFICIAL_SKR_MINT,
    prizeDecimals: SKR_DECIMALS,
    totalAmountAtomic: "1000000000",
    winnerCount: 3,
    payoutEnabled: false,
    transferEnabled: false,
    transferStatus: TRANSFER_STATUS_NOT_STARTED,
  };
  batch.payoutManifestDigestSha256 = computePayoutManifestDigest(batch, items, batch.weekKey);
  return { batch, items };
}

test("Phase 12F transfer plan requires exact approved manifest and pays placement order", () => {
  const { batch, items } = fixture();
  const plan = buildTransferItemPlan({ batch, itemDocuments: items, weekKey: "2026-W38", placement: 1 });
  assert.equal(plan.amountAtomic, "500000000");
  assert.equal(plan.amountUi, "500");
  assert.equal(plan.recipientWalletAddress, wallets[0]);
  assert.equal(plan.remainingAmountAtomic, "1000000000");
  assert.throws(() => buildTransferItemPlan({ batch, itemDocuments: items, weekKey: "2026-W38", placement: 2 }), /placement order/);
});

test("Phase 12G payout manifest freezes account-to-wallet lock evidence", () => {
  const { batch, items } = fixture();
  batch.competitionWalletLockRequired = true;
  batch.competitionWalletLockSchemaVersion = COMPETITION_WALLET_LOCK_SCHEMA_VERSION;
  batch.competitionWalletLockAuthority = COMPETITION_WALLET_LOCK_AUTHORITY;
  items.forEach((item, index) => {
    item.ownerUid = `uid-${index + 1}`;
    item.competitionWalletLockRef = competitionWalletLockRefPath(batch.weekKey, item.ownerUid);
    item.competitionWalletLockAuthority = COMPETITION_WALLET_LOCK_AUTHORITY;
  });
  batch.payoutManifestDigestSha256 = computePayoutManifestDigest(batch, items, batch.weekKey);
  assert.doesNotThrow(() => buildTransferItemPlan({ batch, itemDocuments: items, weekKey: batch.weekKey, placement: 1 }));
  items[0].ownerUid = "tampered-owner";
  assert.throws(() => buildTransferItemPlan({ batch, itemDocuments: items, weekKey: batch.weekKey, placement: 1 }), /digest mismatch/);
});

test("manifest mutation fails closed", () => {
  const { batch, items } = fixture();
  items[0] = { ...items[0], amountAtomic: "499999999" };
  assert.throws(() => buildTransferItemPlan({ batch, itemDocuments: items, weekKey: "2026-W38", placement: 1 }), /digest mismatch/);
});

test("atomic SKR formatting never uses floating point", () => {
  assert.equal(atomicToUiString("500000000"), "500");
  assert.equal(atomicToUiString("300000001"), "300.000001");
  assert.equal(atomicToUiString("1"), "0.000001");
});

test("source account selection is deterministic and refuses split-only funding", () => {
  const accounts = [
    { pubkey: wallets[1], amountAtomic: 600000000n },
    { pubkey: wallets[2], amountAtomic: 300000000n },
  ];
  const picked = selectSourceTokenAccount(accounts, "500000000");
  assert.equal(picked.sourceTokenAccount, wallets[1]);
  assert.throws(() => selectSourceTokenAccount([
    { pubkey: wallets[1], amountAtomic: 300000000n },
    { pubkey: wallets[2], amountAtomic: 300000000n },
  ], "500000000"), /consolidate/);
});

test("RPC token-account parser excludes frozen accounts", () => {
  const payload = {
    result: {
      value: [
        {
          pubkey: wallets[1],
          account: {
            data: {
              parsed: {
                info: {
                  owner: funding,
                  mint: OFFICIAL_SKR_MINT,
                  state: "initialized",
                  tokenAmount: { amount: "600000000", decimals: 6 },
                },
              },
            },
          },
        },
        {
          pubkey: wallets[2],
          account: {
            data: {
              parsed: {
                info: {
                  owner: funding,
                  mint: OFFICIAL_SKR_MINT,
                  state: "frozen",
                  tokenAmount: { amount: "900000000", decimals: 6 },
                },
              },
            },
          },
        },
      ],
    },
  };
  const parsed = parseTransferableTokenAccounts(payload, funding);
  assert.equal(parsed.length, 1);
  assert.equal(parsed[0].amountAtomic, 600000000n);
});

test("recipient must be a standard System Program wallet and mint must be Tokenkeg", () => {
  assert.equal(assertRecipientSystemWallet({ result: { value: { owner: "11111111111111111111111111111111", executable: false } } }, wallets[0]), wallets[0]);
  assert.throws(() => assertRecipientSystemWallet({ result: { value: { owner: TOKEN_PROGRAM_ID, executable: false } } }, wallets[0]), /not a standard/);
  assert.doesNotThrow(() => assertSkrMintProgram({ result: { value: { owner: TOKEN_PROGRAM_ID, executable: false } } }));
});

test("spl-token signature parser supports human output", () => {
  assert.equal(parseSplTokenTransferSignature(`Transfer 500 tokens\nSignature: ${signatures[0]}\n`), signatures[0]);
});

test("signature status distinguishes finalized, pending, and failed", () => {
  assert.deepEqual(parseSignatureStatus({ result: { value: [null] } }, signatures[0]), { found: false, finalized: false, error: null });
  assert.equal(parseSignatureStatus({ result: { value: [{ err: null, confirmationStatus: "finalized", slot: 42 }] } }, signatures[0]).finalized, true);
  assert.match(parseSignatureStatus({ result: { value: [{ err: { InstructionError: [0, "x"] }, confirmationStatus: "finalized", slot: 42 }] } }, signatures[0]).error, /InstructionError/);
});

test("finalized transaction must debit and credit exact SKR amount", () => {
  const plan = { amountAtomic: "500000000", fundingWalletAddress: funding, recipientWalletAddress: wallets[0] };
  const payload = {
    result: {
      slot: 123,
      blockTime: 1780000000,
      transaction: { signatures: [signatures[0]] },
      meta: {
        err: null,
        preTokenBalances: [
          { mint: OFFICIAL_SKR_MINT, owner: funding, uiTokenAmount: { amount: "1000000000" } },
          { mint: OFFICIAL_SKR_MINT, owner: wallets[0], uiTokenAmount: { amount: "0" } },
        ],
        postTokenBalances: [
          { mint: OFFICIAL_SKR_MINT, owner: funding, uiTokenAmount: { amount: "500000000" } },
          { mint: OFFICIAL_SKR_MINT, owner: wallets[0], uiTokenAmount: { amount: "500000000" } },
        ],
      },
    },
  };
  const proof = verifyFinalizedTransferTransaction(payload, plan, signatures[0]);
  assert.equal(proof.amountAtomic, "500000000");
  const bad = structuredClone(payload);
  bad.result.meta.postTokenBalances[1].uiTokenAmount.amount = "499999999";
  assert.throws(() => verifyFinalizedTransferTransaction(bad, plan, signatures[0]), /credit the exact expected/);
});

test("payout completion requires every item paid with unique finalized signature", () => {
  const { batch, items } = fixture();
  batch.status = BATCH_STATUS_PAYMENT_PENDING;
  batch.transferStatus = "IN_PROGRESS";
  items.forEach((item, index) => Object.assign(item, {
    status: ITEM_STATUS_PAID,
    transferStatus: TRANSFER_STATUS_FINALIZED,
    transferAuthority: TRANSFER_EXECUTION_AUTHORITY,
    transactionSignature: signatures[index],
  }));
  const plan = buildCompletionPlan({ batch, itemDocuments: items, weekKey: "2026-W38" });
  assert.equal(plan.batchPatch.status, BATCH_STATUS_PAID);
  assert.equal(plan.transactionSignatures.length, 3);
  items[2].transactionSignature = signatures[1];
  assert.throws(() => buildCompletionPlan({ batch, itemDocuments: items, weekKey: "2026-W38" }), /reuse/);
});
