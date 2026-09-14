# Phase 12E — Trusted Payout Lifecycle

**Project:** Radiant Circle
**Game:** Radiant Rush
**Scope:** trusted/admin payout preparation and approval only; no token transfer

## Goal

Phase 12E converts one immutable Phase 12D Weekly Radiant Cup result into an equally constrained payout manifest that can be reviewed and approved before any future transfer implementation exists.

It deliberately does **not** sign a transaction, hold a private key, enable Android payout authority, or transfer SKR.

## Required trust chain

A payout manifest can be prepared only when all of the following are true:

1. `weeklyCupConfigs/{weekKey}` is `CLOSED` and points to the Phase 12D trusted result.
2. The Cup still carries valid Phase 12C `VERIFIED` funding evidence.
3. `weeklyCupResults/{weekKey}` is a valid immutable Phase 12D `FINALIZED` result.
4. `fundingVerificationStatusAtClose == VERIFIED`.
5. Every configured placement has exactly one trusted Phase 12D winner.
6. Winner wallets are distinct full Solana addresses.
7. Winner amounts exactly match the configured basis-point split and sum to the Cup prize.
8. All Phase 12D payout/transfer flags remain disabled.

If any prerequisite is missing, Phase 12E fails closed.

## Firestore schema

Admin-only operational state:

```text
weeklyCupPayouts/{weekKey}
weeklyCupPayouts/{weekKey}/items/{placement}
weeklyCupPayouts/{weekKey}/events/prepared
weeklyCupPayouts/{weekKey}/events/approved
```

The root payout document stores the source result authority/digest, funding wallet, exact total atomic SKR amount, manifest digest, lifecycle status, and disabled transfer flags.

Each item preserves:

```text
placement
walletAddress
receiptId
amountAtomic
prizeAssetSymbol = SKR
status
transferStatus = NOT_STARTED
payoutEnabled = false
transferEnabled = false
sourceWinnerRef
```

Android cannot read or write this collection. Player-facing result presentation continues to use `weeklyCupResults` and its public read-only winners.

## Lifecycle implemented in 12E

```text
(no payout record)
      ↓ PREPARE
READY_FOR_REVIEW
      ↓ explicit reviewed digest + review reference
APPROVED
```

`APPROVED` means only that the exact immutable manifest has been reviewed for a **future trusted transfer phase**.

It does **not** mean:

- transaction signed,
- SKR sent,
- blockchain confirmation received,
- payout complete.

Every Phase 12E record forces:

```text
payoutEnabled = false
transferEnabled = false
transferStatus = NOT_STARTED
```

## Deterministic payout manifest

The manifest SHA-256 covers:

- week key,
- official SKR mint and decimals,
- total atomic amount,
- funding wallet,
- Phase 12D ranking digest,
- every placement,
- full winner wallet,
- source receipt id,
- exact atomic prize amount.

Approval re-computes the manifest and refuses any mismatch.

## Admin commands

Prepare — dry run first:

```bash
node prepare-weekly-cup-payout.mjs \
  --project radiant-rush-10a9c \
  --week 2026-W38
```

Apply only after reviewing the exact winners/amounts/digest:

```bash
node prepare-weekly-cup-payout.mjs \
  --project radiant-rush-10a9c \
  --week 2026-W38 \
  --apply \
  --confirm-project radiant-rush-10a9c
```

Approval — dry run:

```bash
node approve-weekly-cup-payout.mjs \
  --project radiant-rush-10a9c \
  --week 2026-W38 \
  --review-ref "operator-review-w38"
```

The dry run prints the exact `Payout manifest SHA-256`. Approval apply requires that digest to be copied explicitly:

```bash
node approve-weekly-cup-payout.mjs \
  --project radiant-rush-10a9c \
  --week 2026-W38 \
  --review-ref "operator-review-w38" \
  --confirm-digest "<EXACT_64_CHAR_SHA256_FROM_DRY_RUN>" \
  --apply \
  --confirm-project radiant-rush-10a9c
```

Duplicate preparation and duplicate approval are refused.

## Phase 12F boundary

A later Phase 12F may consume only an `APPROVED` Phase 12E manifest and must independently re-check live on-chain funding immediately before transfer.

Phase 12F must keep signing authority outside Android, Firebase client data, Git, and chat. It must add transaction-level idempotency and confirmation handling before any real SKR is sent.
