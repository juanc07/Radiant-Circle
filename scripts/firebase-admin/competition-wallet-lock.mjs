export const COMPETITION_WALLET_LOCK_SCHEMA_VERSION = 1;
export const COMPETITION_WALLET_LOCK_AUTHORITY = "firestore-first-ranked-entry-phase12g";

const SOLANA_ADDRESS = /^[1-9A-HJ-NP-Za-km-z]{32,44}$/;

function requiredString(value, label, max = 240) {
  const clean = String(value ?? "").trim();
  if (!clean || clean.length > max) throw new Error(`${label} is required and must be at most ${max} characters.`);
  return clean;
}

export function canonicalCompetitionWallet(value, label = "competition wallet") {
  const clean = requiredString(value, label, 64);
  if (!SOLANA_ADDRESS.test(clean)) throw new Error(`${label} must be a Solana public address.`);
  return clean;
}

export function competitionWalletLockRefPath(weekKey, ownerUid) {
  const cleanWeek = requiredString(weekKey, "weekKey", 16);
  const cleanUid = requiredString(ownerUid, "ownerUid", 128);
  return `weeklyCupCompetitionWalletLocks/${cleanWeek}/accounts/${cleanUid}`;
}

export function assertCompetitionWalletLock({
  weekKey,
  ownerUid,
  walletAddress,
  lockDocId,
  lock,
}) {
  const cleanWeek = requiredString(weekKey, "weekKey", 16);
  const cleanUid = requiredString(ownerUid, "ownerUid", 128);
  const cleanWallet = canonicalCompetitionWallet(walletAddress);
  if (!lock) throw new Error(`Missing Phase 12G competition-wallet lock for account ${cleanUid} in ${cleanWeek}.`);
  if (String(lockDocId ?? cleanUid) !== cleanUid) throw new Error("Competition-wallet lock document id does not match ownerUid.");
  if (Number(lock.schemaVersion) !== COMPETITION_WALLET_LOCK_SCHEMA_VERSION) {
    throw new Error(`Competition-wallet lock schemaVersion must be ${COMPETITION_WALLET_LOCK_SCHEMA_VERSION}.`);
  }
  if (String(lock.lockAuthority ?? "") !== COMPETITION_WALLET_LOCK_AUTHORITY) {
    throw new Error("Competition-wallet lock authority is not trusted Phase 12G policy.");
  }
  if (String(lock.weekKey ?? "").trim() !== cleanWeek) throw new Error("Competition-wallet lock weekKey mismatch.");
  if (String(lock.ownerUid ?? "").trim() !== cleanUid) throw new Error("Competition-wallet lock ownerUid mismatch.");
  const lockedWallet = canonicalCompetitionWallet(lock.walletAddress, "locked competition wallet");
  if (lockedWallet !== cleanWallet) {
    throw new Error("This Radiant Circle account is already locked to a different competition wallet for this Cup.");
  }
  const firstReceiptId = requiredString(lock.firstReceiptId, "competition-wallet lock firstReceiptId", 100);
  return {
    schemaVersion: COMPETITION_WALLET_LOCK_SCHEMA_VERSION,
    authority: COMPETITION_WALLET_LOCK_AUTHORITY,
    weekKey: cleanWeek,
    ownerUid: cleanUid,
    walletAddress: lockedWallet,
    firstReceiptId,
    ref: competitionWalletLockRefPath(cleanWeek, cleanUid),
  };
}
