import test from 'node:test';
import assert from 'node:assert/strict';
import {
  classifyUidDocument,
  classifyWalletDocument,
} from './cleanup-classification.mjs';

const currentUids = new Set(['uid-current-a', 'uid-current-b']);
const currentWallets = new Set(['wallet-a', 'wallet-b']);

test('current Auth UID is kept', () => {
  assert.equal(classifyUidDocument({
    docId: 'uid-current-a', data: { walletAddress: 'wallet-a' }, activeUids: currentUids,
    currentWallets, deleteSameWalletStale: false,
  }).action, 'KEEP');
});

test('stale UID using a current wallet is review-only by default', () => {
  assert.equal(classifyUidDocument({
    docId: 'uid-stale', data: { walletAddress: 'wallet-a' }, activeUids: currentUids,
    currentWallets, deleteSameWalletStale: false,
  }).action, 'REVIEW');
});

test('stale UID using a current wallet can be explicitly deleted', () => {
  assert.equal(classifyUidDocument({
    docId: 'uid-stale', data: { walletAddress: 'wallet-a' }, activeUids: currentUids,
    currentWallets, deleteSameWalletStale: true,
  }).action, 'DELETE');
});

test('stale UID with obsolete wallet is delete candidate', () => {
  assert.equal(classifyUidDocument({
    docId: 'uid-stale', data: { walletAddress: 'wallet-old' }, activeUids: currentUids,
    currentWallets, deleteSameWalletStale: false,
  }).action, 'DELETE');
});

test('current wallet-day record is kept', () => {
  assert.equal(classifyWalletDocument({ walletId: 'wallet-b', currentWallets }).action, 'KEEP');
});

test('obsolete wallet-day record is deleted', () => {
  assert.equal(classifyWalletDocument({ walletId: 'wallet-old', currentWallets }).action, 'DELETE');
});

test('canonical keep set treats another historical Auth UID as stale', () => {
  const canonicalOnly = new Set(['uid-current-a', 'uid-current-b']);
  assert.equal(classifyUidDocument({
    docId: 'uid-historical-auth', data: { walletAddress: 'wallet-a' }, activeUids: canonicalOnly,
    currentWallets, deleteSameWalletStale: true,
  }).action, 'DELETE');
});
