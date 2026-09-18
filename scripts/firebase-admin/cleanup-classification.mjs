export function walletFromData(data) {
  const value = data?.walletAddress;
  return typeof value === 'string' && value.trim() ? value.trim() : null;
}

export function ownerUidFromData(data) {
  const value = data?.ownerUid;
  return typeof value === 'string' && value.trim() ? value.trim() : null;
}

export function compactMetrics(data) {
  const fields = [
    ['score', data?.score],
    ['bestRunScore', data?.bestRunScore],
    ['totalXp', data?.totalXp],
    ['runsPlayed', data?.runsPlayed],
  ].filter(([, value]) => value !== undefined && value !== null);
  return Object.fromEntries(fields);
}

export function classifyUidDocument({ docId, data, activeUids, currentWallets, deleteSameWalletStale }) {
  const ownerUid = ownerUidFromData(data);
  const wallet = walletFromData(data);
  const uidIsCurrent = activeUids.has(docId) || (ownerUid && activeUids.has(ownerUid));

  if (uidIsCurrent) {
    return { action: 'KEEP', reason: 'current Firebase Auth UID', wallet };
  }

  if (wallet && currentWallets.has(wallet) && !deleteSameWalletStale) {
    return {
      action: 'REVIEW',
      reason: 'stale UID references a wallet used by a current Auth user; preserve until reviewed',
      wallet,
    };
  }

  return { action: 'DELETE', reason: 'UID is not present in Firebase Authentication', wallet };
}

export function classifyWalletDocument({ walletId, currentWallets }) {
  if (currentWallets.has(walletId)) {
    return { action: 'KEEP', reason: 'wallet belongs to a current Auth user' };
  }
  return { action: 'DELETE', reason: 'wallet is not associated with a current Auth user' };
}
