#!/usr/bin/env node

import fs from 'node:fs/promises';
import path from 'node:path';
import process from 'node:process';
import { applicationDefault, initializeApp } from 'firebase-admin/app';
import { getAuth } from 'firebase-admin/auth';
import { getFirestore } from 'firebase-admin/firestore';
import {
  classifyUidDocument,
  classifyWalletDocument,
  compactMetrics,
  walletFromData,
} from './cleanup-classification.mjs';

const DEFAULT_PROJECT = 'radiant-rush-10a9c';
const PROTECTED_ROOT_COLLECTIONS = new Set([
  'competitionRunSubmissions',
  'competitionRunVerifications',
  'weeklyCupConfigs',
  'weeklyCupFundingChecks',
  'weeklyCupResults',
  'weeklyCupPayouts',
]);

function parseArgs(argv) {
  const options = {
    project: DEFAULT_PROJECT,
    apply: false,
    confirmProject: null,
    keepUids: new Set(),
    canonicalUids: new Set(),
    keepWallets: new Set(),
    deleteSameWalletStale: false,
    deleteAuthUsers: false,
    allowLarge: false,
    maxDeletes: 500,
  };
  for (let i = 0; i < argv.length; i += 1) {
    const arg = argv[i];
    const next = () => {
      if (i + 1 >= argv.length) throw new Error(`Missing value after ${arg}`);
      i += 1;
      return argv[i];
    };
    switch (arg) {
      case '--project': options.project = next(); break;
      case '--apply': options.apply = true; break;
      case '--confirm-project': options.confirmProject = next(); break;
      case '--keep-uid': options.keepUids.add(next()); break;
      case '--canonical-uid': options.canonicalUids.add(next()); break;
      case '--keep-wallet': options.keepWallets.add(next()); break;
      case '--delete-same-wallet-stale': options.deleteSameWalletStale = true; break;
      case '--delete-auth-users': options.deleteAuthUsers = true; break;
      case '--allow-large': options.allowLarge = true; break;
      case '--max-deletes': {
        const value = Number.parseInt(next(), 10);
        if (!Number.isFinite(value) || value < 1) throw new Error('--max-deletes must be a positive integer');
        options.maxDeletes = value;
        break;
      }
      case '--help':
      case '-h': options.help = true; break;
      default: throw new Error(`Unknown argument: ${arg}`);
    }
  }
  return options;
}

function helpText() {
  return `Radiant Circle stale development-data cleanup\n\n` +
    `Conservative dry-run (keeps every Firebase Auth UID):\n` +
    `  node cleanup-stale-dev-data.mjs --project ${DEFAULT_PROJECT}\n\n` +
    `Canonical dev cleanup dry-run:\n` +
    `  node cleanup-stale-dev-data.mjs --project ${DEFAULT_PROJECT} \\\n` +
    `    --canonical-uid UID_ONE --canonical-uid UID_TWO \\\n` +
    `    --delete-same-wallet-stale --delete-auth-users\n\n` +
    `Apply reviewed cleanup:\n` +
    `  add --apply --confirm-project ${DEFAULT_PROJECT}\n\n` +
    `Safety options:\n` +
    `  --canonical-uid UID              in canonical mode, preserve only these Auth UIDs (repeatable)\n` +
    `  --keep-uid UID                   preserve an extra UID in addition to canonical UIDs\n` +
    `  --keep-wallet WALLET             preserve an extra public wallet\n` +
    `  --delete-same-wallet-stale       delete stale UID rows that reference a preserved wallet\n` +
    `  --delete-auth-users              plan/delete non-canonical Firebase Auth users\n` +
    `  --max-deletes N                  Firestore safety cap (default 500)\n` +
    `  --allow-large                    allow a Firestore plan above the safety cap\n`;
}

async function listAllAuthUsers(auth) {
  const users = [];
  let pageToken;
  do {
    const page = await auth.listUsers(1000, pageToken);
    users.push(...page.users);
    pageToken = page.pageToken;
  } while (pageToken);
  return users;
}

async function validateCanonicalUids(authUsers, canonicalUids) {
  const known = new Set(authUsers.map((u) => u.uid));
  const missing = [...canonicalUids].filter((uid) => !known.has(uid));
  if (missing.length) throw new Error(`Canonical UID(s) not present in Firebase Authentication: ${missing.join(', ')}`);
}

async function collectCurrentWallets(db, activeUids, extraWallets) {
  const wallets = new Set(extraWallets);
  for (const collectionName of ['users', 'leaderboard', 'runAllTime']) {
    for (const uid of activeUids) {
      const snap = await db.collection(collectionName).doc(uid).get();
      if (!snap.exists) continue;
      const wallet = walletFromData(snap.data());
      if (wallet) wallets.add(wallet);
    }
  }
  return wallets;
}

function addUnique(plan, item) {
  if (!plan.some((candidate) => candidate.path === item.path)) plan.push(item);
}

async function buildPlan(db, activeUids, currentWallets, options) {
  const plan = [];
  const reviews = [];
  const keeps = [];

  for (const collectionName of ['users', 'leaderboard', 'runAllTime']) {
    const snapshot = await db.collection(collectionName).get();
    for (const doc of snapshot.docs) {
      const data = doc.data();
      const c = classifyUidDocument({
        docId: doc.id,
        data,
        activeUids,
        currentWallets,
        deleteSameWalletStale: options.deleteSameWalletStale,
      });
      const item = {
        path: doc.ref.path,
        collection: collectionName,
        reason: c.reason,
        walletAddress: c.wallet ?? null,
        metrics: compactMetrics(data),
      };
      if (c.action === 'DELETE') addUnique(plan, item);
      else if (c.action === 'REVIEW') reviews.push(item);
      else keeps.push(item);
    }
  }

  const weeklyEntries = await db.collectionGroup('entries').get();
  for (const doc of weeklyEntries.docs) {
    if (!/^runWeekly\/[^/]+\/entries\/[^/]+$/.test(doc.ref.path)) continue;
    const data = doc.data();
    const c = classifyUidDocument({
      docId: doc.id,
      data,
      activeUids,
      currentWallets,
      deleteSameWalletStale: options.deleteSameWalletStale,
    });
    const item = {
      path: doc.ref.path,
      collection: 'runWeekly/*/entries',
      reason: c.reason,
      walletAddress: c.wallet ?? null,
      metrics: compactMetrics(data),
    };
    if (c.action === 'DELETE') addUnique(plan, item);
    else if (c.action === 'REVIEW') reviews.push(item);
    else keeps.push(item);
  }

  const walletDocs = await db.collectionGroup('wallets').get();
  for (const doc of walletDocs.docs) {
    if (!/^runWalletDaily\/[^/]+\/wallets\/[^/]+$/.test(doc.ref.path)) continue;
    const c = classifyWalletDocument({ walletId: doc.id, currentWallets });
    const item = {
      path: doc.ref.path,
      collection: 'runWalletDaily/*/wallets',
      reason: c.reason,
      walletAddress: doc.id,
      metrics: {},
    };
    if (c.action === 'DELETE') addUnique(plan, item);
    else keeps.push(item);
  }

  return { plan, reviews, keeps };
}

function buildAuthDeletePlan(authUsers, activeUids, deleteAuthUsers) {
  if (!deleteAuthUsers) return [];
  return authUsers
    .filter((user) => !activeUids.has(user.uid))
    .map((user) => ({
      uid: user.uid,
      createdAt: user.metadata.creationTime ?? null,
      lastSignInAt: user.metadata.lastSignInTime ?? null,
      anonymous: user.providerData.length === 0,
    }))
    .sort((a, b) => a.uid.localeCompare(b.uid));
}

async function writePlanFile(project, authUsers, activeUids, currentWallets, authDeletePlan, result, options) {
  const directory = path.resolve(process.cwd(), '.cleanup');
  await fs.mkdir(directory, { recursive: true });
  const stamp = new Date().toISOString().replaceAll(':', '-').replaceAll('.', '-');
  const output = path.join(directory, `cleanup-plan-${stamp}.json`);
  await fs.writeFile(output, JSON.stringify({
    generatedAt: new Date().toISOString(),
    project,
    dryRun: !options.apply,
    canonicalMode: options.canonicalUids.size > 0,
    protectedRootCollections: [...PROTECTED_ROOT_COLLECTIONS],
    authUserCount: authUsers.length,
    preservedAuthUids: [...activeUids].sort(),
    preservedWallets: [...currentWallets].sort(),
    deleteSameWalletStale: options.deleteSameWalletStale,
    deleteAuthUsers: options.deleteAuthUsers,
    firestoreDeleteCount: result.plan.length,
    authDeleteCount: authDeletePlan.length,
    reviewCount: result.reviews.length,
    firestoreDelete: result.plan,
    authDelete: authDeletePlan,
    review: result.reviews,
  }, null, 2) + '\n', 'utf8');
  return output;
}

function printSection(title, items, prefix) {
  console.log(`\n${title} (${items.length})`);
  if (!items.length) return console.log('  (none)');
  for (const item of items) {
    const wallet = item.walletAddress ? ` wallet=${item.walletAddress}` : '';
    const metrics = Object.keys(item.metrics ?? {}).length ? ` metrics=${JSON.stringify(item.metrics)}` : '';
    console.log(`  ${prefix} ${item.path} — ${item.reason}${wallet}${metrics}`);
  }
}

function printAuthPlan(items) {
  console.log(`\nAUTH DELETE plan (${items.length})`);
  if (!items.length) return console.log('  (none)');
  for (const item of items) {
    console.log(`  DELETE AUTH ${item.uid} — created=${item.createdAt ?? '?'} lastSignIn=${item.lastSignInAt ?? '?'} anonymous=${item.anonymous}`);
  }
}

async function applyFirestorePlan(db, plan) {
  let completed = 0;
  for (const item of plan) {
    console.log(`[Firestore ${completed + 1}/${plan.length}] recursive delete ${item.path}`);
    await db.recursiveDelete(db.doc(item.path));
    completed += 1;
  }
  return completed;
}

async function applyAuthPlan(auth, authDeletePlan) {
  if (!authDeletePlan.length) return { successCount: 0, failureCount: 0 };
  const result = await auth.deleteUsers(authDeletePlan.map((item) => item.uid));
  if (result.failureCount > 0) {
    for (const failure of result.errors) {
      console.error(`AUTH DELETE FAILED index=${failure.index}: ${failure.error?.message ?? failure.error}`);
    }
  }
  return result;
}

async function main() {
  let options;
  try {
    options = parseArgs(process.argv.slice(2));
  } catch (error) {
    console.error(`ERROR: ${error.message}\n\n${helpText()}`);
    process.exitCode = 2;
    return;
  }
  if (options.help) return console.log(helpText());
  if (options.apply && options.confirmProject !== options.project) {
    console.error(`REFUSING APPLY: --confirm-project must exactly match --project (${options.project}).`);
    process.exitCode = 2;
    return;
  }
  if (options.deleteAuthUsers && options.canonicalUids.size === 0) {
    throw new Error('--delete-auth-users requires at least one --canonical-uid. Refusing to plan broad Auth deletion without an explicit canonical keep set.');
  }

  console.log('Radiant Circle Firebase development-data cleanup');
  console.log(`Project: ${options.project}`);
  console.log(`Mode: ${options.apply ? 'APPLY' : 'DRY RUN'}`);
  console.log('[1/5] Connecting to Firebase Admin...');

  initializeApp({ credential: applicationDefault(), projectId: options.project });
  const auth = getAuth();
  const db = getFirestore();

  console.log('[2/5] Loading Firebase Authentication users...');
  const authUsers = await listAllAuthUsers(auth);
  if (authUsers.length === 0) throw new Error('Firebase Authentication returned zero users; refusing cleanup.');

  const allAuthUids = new Set(authUsers.map((user) => user.uid));
  let activeUids;
  if (options.canonicalUids.size > 0) {
    await validateCanonicalUids(authUsers, options.canonicalUids);
    activeUids = new Set([...options.canonicalUids, ...options.keepUids]);
  } else {
    activeUids = new Set([...allAuthUids, ...options.keepUids]);
  }

  console.log('[3/5] Resolving preserved wallets from canonical/current UIDs...');
  const currentWallets = await collectCurrentWallets(db, activeUids, options.keepWallets);
  console.log(`Firebase Auth users found: ${authUsers.length}`);
  console.log(`Preserved Firebase UIDs: ${activeUids.size}`);
  console.log(`Preserved wallets: ${currentWallets.size}`);
  console.log(`Canonical mode: ${options.canonicalUids.size > 0 ? 'YES' : 'NO'}`);
  console.log(`Protected root collections: ${[...PROTECTED_ROOT_COLLECTIONS].join(', ')}`);

  console.log('[4/5] Scanning Firestore prototype collections...');
  const result = await buildPlan(db, activeUids, currentWallets, options);
  const authDeletePlan = buildAuthDeletePlan(authUsers, activeUids, options.deleteAuthUsers);

  printSection('KEEP summary', result.keeps, 'KEEP');
  printSection('REVIEW — not deleted by default', result.reviews, 'REVIEW');
  printSection('FIRESTORE DELETE plan', result.plan, 'DELETE');
  printAuthPlan(authDeletePlan);

  console.log('[5/5] Writing reviewable cleanup plan...');
  const planFile = await writePlanFile(options.project, authUsers, activeUids, currentWallets, authDeletePlan, result, options);
  console.log(`\nPlan written to: ${planFile}`);

  if (result.plan.length > options.maxDeletes && !options.allowLarge) {
    throw new Error(`Firestore delete plan contains ${result.plan.length} paths, above safety cap ${options.maxDeletes}. Review it before using --allow-large.`);
  }

  if (!options.apply) {
    console.log('\nDRY RUN ONLY — 0 Firestore documents and 0 Auth users deleted.');
    if (result.reviews.length) console.log('REVIEW rows are preserved. Add --delete-same-wallet-stale only when those duplicate UID rows are intentionally disposable.');
    return;
  }

  console.log('\nAPPLY CONFIRMED. Deleting Firestore data first so a partial failure cannot strand data behind deleted Auth accounts...');
  const firestoreCompleted = await applyFirestorePlan(db, result.plan);

  let authResult = { successCount: 0, failureCount: 0 };
  if (options.deleteAuthUsers) {
    console.log(`\nDeleting ${authDeletePlan.length} non-canonical Firebase Auth users...`);
    authResult = await applyAuthPlan(auth, authDeletePlan);
  }

  console.log(`\nCleanup complete: ${firestoreCompleted} Firestore paths recursively deleted.`);
  console.log(`Firebase Auth deleted: ${authResult.successCount}; failed: ${authResult.failureCount}.`);
  console.log('Protected Phase 12 competition/config/funding/result collections were not touched.');
  if (authResult.failureCount > 0) process.exitCode = 1;
}

main().catch((error) => {
  console.error(`\nFATAL: ${error?.stack ?? error}`);
  process.exitCode = 1;
});
