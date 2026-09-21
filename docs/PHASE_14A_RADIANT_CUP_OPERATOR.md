# Phase 14A — Radiant Cup Operator

## Goal

Reduce Weekly Radiant Cup operations from many memorized commands to one safe operator entry point without weakening the trusted Phase 12A–G security model.

## Location

`scripts/firebase-admin/radiant-cup.mjs`

This tool wraps the existing proven scripts. It does not replace their verification/finalization/payout logic.

## Safety contract

- `status` and `report` are read-only.
- Wrapped commands stay dry-run unless `--apply` is explicitly supplied.
- Existing `--confirm-project`, manifest digest, funding wallet, recipient, and amount confirmations remain required where the underlying tool requires them.
- The operator never auto-marks a run `VERIFIED`.
- Ambiguous/submitted transactions are sent to reconciliation, never automatic retry.
- Signer material remains external through `RADIANT_PAYOUT_SIGNER_KEYPAIR` and is never stored in Git, Firestore, Android, or the operator config.

## First use

From `scripts/firebase-admin`:

```bash
npm test
node radiant-cup.mjs status --week 2026-W39
```

Create a non-secret Cup config:

```bash
cp cups/example.json cups/2026-W39.json
```

Edit the public funding wallet and prize details, then dry-run setup:

```bash
node radiant-cup.mjs setup --config cups/2026-W39.json
```

Only after reviewing the dry run:

```bash
node radiant-cup.mjs setup \
  --config cups/2026-W39.json \
  --apply \
  --confirm-project radiant-rush-10a9c
```

## Daily operator command

```bash
node radiant-cup.mjs status --week 2026-W39
```

It summarizes funding, players/locks, Ranked receipts, trusted verification decisions, finalization, payout state, transfers, and the next safe action.

## Evidence report

```bash
node radiant-cup.mjs report \
  --week 2026-W39 \
  --out ../../docs/evidence/RADIANT_CUP_2026_W39_REPORT.md
```

The generated report is a convenient operator/judge snapshot. Immutable Firestore records and Solana transaction history remain the authoritative evidence.
