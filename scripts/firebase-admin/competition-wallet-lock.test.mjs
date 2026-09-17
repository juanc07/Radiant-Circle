import test from "node:test";
import assert from "node:assert/strict";
import {
  COMPETITION_WALLET_LOCK_AUTHORITY,
  COMPETITION_WALLET_LOCK_SCHEMA_VERSION,
  assertCompetitionWalletLock,
  competitionWalletLockRefPath,
} from "./competition-wallet-lock.mjs";

const weekKey = "2026-W39";
const ownerUid = "firebase-user-a";
const walletA = "J86vtTs7twTUuS4xfo8H8zaUeFyMNHWPPeEXyL5DscPg";
const walletB = "HbyQrE2N1V8TPs5HJ9wGDq3M85Zm1i21RmgbLFk39xkS";

function lock(overrides = {}) {
  return {
    schemaVersion: COMPETITION_WALLET_LOCK_SCHEMA_VERSION,
    weekKey,
    ownerUid,
    walletAddress: walletA,
    firstReceiptId: "11111111-2222-3333-4444-555555555555",
    lockAuthority: COMPETITION_WALLET_LOCK_AUTHORITY,
    ...overrides,
  };
}

test("valid Phase 12G lock binds one Firebase account to one Cup wallet", () => {
  const normalized = assertCompetitionWalletLock({
    weekKey,
    ownerUid,
    walletAddress: walletA,
    lockDocId: ownerUid,
    lock: lock(),
  });
  assert.equal(normalized.walletAddress, walletA);
  assert.equal(normalized.ref, competitionWalletLockRefPath(weekKey, ownerUid));
});

test("same account switching to another wallet fails closed", () => {
  assert.throws(() => assertCompetitionWalletLock({
    weekKey,
    ownerUid,
    walletAddress: walletB,
    lockDocId: ownerUid,
    lock: lock(),
  }), /different competition wallet/);
});

test("missing, wrong-account, or forged-authority lock fails closed", () => {
  assert.throws(() => assertCompetitionWalletLock({ weekKey, ownerUid, walletAddress: walletA, lock: null }), /Missing/);
  assert.throws(() => assertCompetitionWalletLock({
    weekKey,
    ownerUid,
    walletAddress: walletA,
    lockDocId: "other-user",
    lock: lock(),
  }), /document id/);
  assert.throws(() => assertCompetitionWalletLock({
    weekKey,
    ownerUid,
    walletAddress: walletA,
    lockDocId: ownerUid,
    lock: lock({ lockAuthority: "client-forged" }),
  }), /authority/);
});
