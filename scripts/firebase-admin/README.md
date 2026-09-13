# Radiant Circle Firebase Admin Tools

Developer-only Firebase Admin utilities for safe development-data cleanup and trusted Phase 12 Weekly Radiant Cup configuration. Android never receives Admin credentials.

## Protected collections

The tool never scans or deletes:

- `competitionRunSubmissions/*`
- `weeklyCupConfigs/*`

## Install and test

```bash
cd scripts/firebase-admin
npm install
npm test
```

## Credentials on Windows + Git Bash

Keep the service-account JSON outside the repository and convert the Git Bash path to a Windows path for Node:

```bash
CRED="/c/2026/firebase-secrets/your-service-account.json"
export GOOGLE_APPLICATION_CREDENTIALS="$(cygpath -w "$CRED")"
```

## Conservative dry run

Keeps every UID that still exists in Firebase Authentication:

```bash
node cleanup-stale-dev-data.mjs \
  --project radiant-rush-10a9c
```

## Canonical-development cleanup

Use this when development has accumulated many Firebase Anonymous Auth UIDs but only a small explicit set should remain.

Dry-run first:

```bash
node cleanup-stale-dev-data.mjs \
  --project radiant-rush-10a9c \
  --canonical-uid "UID_ONE" \
  --canonical-uid "UID_TWO" \
  --delete-same-wallet-stale \
  --delete-auth-users
```

This mode:

- preserves only the explicitly supplied canonical UIDs (plus any extra `--keep-uid` values),
- derives preserved public wallets from those canonical UIDs,
- plans recursive deletion of stale `users`, `leaderboard`, `runAllTime`, and `runWeekly/*/entries` rows,
- preserves `runWalletDaily` rows for canonical wallets,
- plans deletion of non-canonical Firebase Auth users only when `--delete-auth-users` is explicitly present,
- remains a dry run unless `--apply` and exact project confirmation are both supplied.

After reviewing the generated `.cleanup/cleanup-plan-*.json`, apply with the exact same options plus:

```bash
  --apply \
  --confirm-project radiant-rush-10a9c
```

Firestore data is deleted before Firebase Auth users. This ordering makes interruption safer.

## Safety notes

- Never store the service-account key in this repository.
- Never use this tool against production data without a separately reviewed retention/migration plan.
- `--delete-auth-users` refuses to run unless at least one `--canonical-uid` is provided.
- `--apply` refuses to run unless `--confirm-project` exactly matches `--project`.
- Review the dry-run plan before applying.


## Phase 12B trusted Weekly Cup configuration

`manage-weekly-cup.mjs` creates or replaces `weeklyCupConfigs/{weekKey}` using a constrained schema. The Android app has read-only access to this collection. The tool never verifies funding and always writes `payoutEnabled=false`.

Dry-run first:

```bash
node manage-weekly-cup.mjs \
  --project radiant-rush-10a9c \
  --week 2026-W37 \
  --status ANNOUNCED \
  --sponsor "ThinkBloxPH" \
  --prize-skr 1000 \
  --note "Sponsored results require trusted verification after Cup close."
```

Optional trusted configuration fields:

```bash
  --funding-wallet "PUBLIC_SOLANA_ADDRESS" \
  --placements "1:50,2:30,3:20"
```

A configured funding wallet starts as `NOT_VERIFIED`; no wallet starts as `NOT_CONFIGURED`. Phase 12B does not perform on-chain funding verification.

After reviewing the dry run, apply with the same arguments plus:

```bash
  --apply \
  --confirm-project radiant-rush-10a9c
```

Safety behavior:

- the exact official SKR mint and 6 decimals are hard-coded by the admin tool,
- prize amounts are stored as exact atomic-unit strings, not floating-point values,
- placement allocations must total 100%,
- `trustedResultsRequired=true` and `payoutEnabled=false` are forced,
- the tool refuses to overwrite a config whose funding is already `VERIFIED`, whose payout is already enabled, or whose schema is newer than Phase 12B,
- `weeklyCupConfigs/*` remains `allow write: if false` for Android clients.
