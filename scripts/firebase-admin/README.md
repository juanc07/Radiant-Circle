# Radiant Circle Firebase Admin Tools

Developer-only Firebase Admin utilities for safe development-data cleanup, trusted Weekly Radiant Cup configuration, Phase 12C SKR funding verification, and Phase 12D trusted run attestation / Cup finalization. Android never receives Admin credentials.

## Protected collections

The tool never scans or deletes:

- `competitionRunSubmissions/*`
- `competitionRunVerifications/*`
- `weeklyCupConfigs/*`
- `weeklyCupFundingChecks/*`
- `weeklyCupResults/*`

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
- after Phase 12C evidence exists, safe status/presentation updates preserve it; prize/funding-wallet changes that would invalidate it are refused,
- `weeklyCupConfigs/*` remains `allow write: if false` for Android clients.

## Phase 12C trusted SKR funding verification

`verify-weekly-cup-funding.mjs` reads the current Cup configuration, queries Solana Mainnet for **liquid** SKR held by the public funding wallet, and compares the exact raw amount against the configured prize requirement. It is dry-run by default.

Before running it, create temporary Firebase Admin credentials outside the repository and set `GOOGLE_APPLICATION_CREDENTIALS`. The funding wallet is a public Solana address; never provide or store its seed phrase/private key.

Dry-run first:

```bash
node verify-weekly-cup-funding.mjs \
  --project radiant-rush-10a9c \
  --week 2026-W37 \
  --funding-wallet "PUBLIC_SOLANA_ADDRESS"
```

By default the tool uses Solana's public Mainnet RPC. If a custom provider is needed, prefer an environment variable so API keys do not enter shell history or source:

```bash
export SOLANA_MAINNET_RPC_URL="https://your-provider.example/..."
```

After reviewing required SKR, observed transferable liquid SKR, token-account counts, slot, and result, apply with the exact same arguments plus:

```bash
  --apply \
  --confirm-project radiant-rush-10a9c
```

Applied verification:

- writes the current funding snapshot into `weeklyCupConfigs/{weekKey}`,
- creates an immutable Admin-only receipt at `weeklyCupFundingChecks/{weekKey}/checks/{checkId}`,
- writes `VERIFIED` only when observed non-frozen liquid SKR is greater than or equal to the configured prize,
- writes `NOT_VERIFIED` when the wallet is short,
- writes nothing when the RPC read itself fails,
- never enables payout and never transfers SKR.

The verifier refuses to silently switch an already configured funding wallet. To intentionally verify a replacement wallet, add `--replace-funding-wallet` and review the dry run carefully.

After Phase 12C evidence exists, `manage-weekly-cup.mjs` may still update safe presentation/status fields, but it preserves the evidence and refuses prize or funding-wallet changes that would invalidate the trusted check.


## Phase 12D trusted run attestation prerequisite

Phase 12C does **not** contain a trusted game-run verifier. Android receipts start `UNVERIFIED` and are not winner authority. Phase 12D therefore fails closed unless an operator has independently checked a run and attested the exact receipt facts through `verify-competition-run.mjs`.

Live W37 proof (2026-09-14): five receipts resolved to only two distinct wallets and none had a trusted Phase 12D verification. After Cup end, `finalize-weekly-cup.mjs` correctly refused to invent winners. That refusal is expected and must not be bypassed.

The verifier is dry-run by default and requires an evidence reference plus the independently checked wallet, score, combo, hit counts, and completion time. A VERIFIED apply updates the receipt to `trustedPlacementEligible=true` while keeping payout disabled, and creates the matching immutable Admin-only `competitionRunVerifications/{receiptId}` decision. Finalization requires **both** records to match.

Dry-run example:

```bash
node verify-competition-run.mjs \
  --project radiant-rush-10a9c \
  --week 2026-W37 \
  --receipt "RECEIPT_ID" \
  --decision VERIFIED \
  --evidence-ref "device-capture-2026-w37-run-001" \
  --wallet "PUBLIC_WALLET" \
  --score 1234 \
  --max-combo 20 \
  --perfect-hits 10 \
  --radiant-hits 30 \
  --corrupted-hits 2 \
  --completed-at-ms 1780000000000
```

Only after reviewing the exact dry-run result, repeat the same command with:

```bash
  --apply \
  --confirm-project radiant-rush-10a9c
```

A rejected receipt uses `--decision REJECTED --evidence-ref ... --reason ...`; it can never become placement eligible. Trusted decisions are immutable through this tool.

## Phase 12D trusted Weekly Cup finalization

`finalize-weekly-cup.mjs` closes one OPEN Cup after its configured end. It loads the Cup, all same-week run receipts, and matching immutable verification decisions; accepts only the exact Phase 12D VERIFIED attestation contract; keeps the best eligible result per full wallet; applies the existing deterministic competition tiebreak order (`score`, `maxCombo`, `perfectHits`, earlier completion, receipt id); calculates the exact configured SKR split; and freezes the result.

Dry-run:

```bash
node finalize-weekly-cup.mjs \
  --project radiant-rush-10a9c \
  --week 2026-W37
```

Apply only after the dry run shows the intended trusted eligible set and winners:

```bash
node finalize-weekly-cup.mjs \
  --project radiant-rush-10a9c \
  --week 2026-W37 \
  --apply \
  --confirm-project radiant-rush-10a9c
```

Applied finalization:

- creates `weeklyCupResults/{weekKey}`,
- freezes all trusted-eligible receipts under `weeklyCupResults/{weekKey}/eligibleReceipts/{receiptId}`,
- creates public read-only `weeklyCupResults/{weekKey}/winners/{placement}` records,
- marks the Cup `CLOSED` with the trusted result reference,
- refuses duplicate finalization,
- refuses to invent missing configured placements,
- re-reads authoritative inputs inside the Firestore transaction and aborts if snapshot digests changed,
- snapshots Phase 12C funding status honestly,
- always keeps `payoutEnabled=false` / `payoutReady=false`,
- never transfers SKR.

`NOT_VERIFIED` funding does not prevent competitive result freeze, but it never becomes payout-ready.

## Phase 12 operator runbook

For the complete end-to-end command sequence used during live Phase 12 verification—including Git Bash credential setup, Cup inspection, receipt listing, independent run attestation, finalization, duplicate-close proof, and cleanup—see:

```text
docs/PHASE_12_ADMIN_OPERATOR_RUNBOOK.md
```

The runbook also documents the `stdin is not a tty` heredoc issue seen on some Git Bash setups and provides `node --input-type=module -e` read-only alternatives.
