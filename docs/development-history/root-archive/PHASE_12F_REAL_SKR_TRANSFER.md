# Phase 12F — Real Sponsor-Funded SKR Transfer

**Project:** Radiant Circle
**Game:** Radiant Rush
**Scope:** trusted Admin-side execution of one approved SKR winner transfer at a time

## Goal

Phase 12F consumes only a Phase 12E `APPROVED` payout manifest and turns each approved payout item into a real, finalized Solana Mainnet SKR transfer.

The Android app never receives signer authority. No seed phrase/private key is stored in Git, Firestore, Android, or documentation.

## Required trust chain

A transfer is eligible only after:

1. Phase 12C funding was `VERIFIED` before Cup close.
2. Phase 12D finalized the Cup and immutable winners.
3. Phase 12E prepared and explicitly approved the exact payout manifest.
4. The payout manifest digest still recomputes exactly.
5. The funding wallet still has enough liquid SKR for all remaining payouts.
6. The requested placement is the next unpaid placement.
7. Recipient is a standard System Program wallet.
8. The official SKR mint is still owned by the expected SPL Token Program.

Any mismatch fails closed.

## Two execution modes

### A. External / hardware / Seed Vault wallet — recommended when the funding wallet is not an exportable server key

1. Dry-run `prepare-weekly-cup-skr-transfer-intent.mjs`.
2. Apply the intent only after confirming manifest digest, funding wallet, recipient, and atomic amount.
3. The intent locks that payout item as `AWAITING_EXTERNAL_SIGNATURE`.
4. Use the configured sponsor wallet itself to send the exact SKR amount to the exact winner wallet.
5. Capture the real Solana transaction signature.
6. Run `reconcile-weekly-cup-skr-transfer.mjs --signature ...` dry-run.
7. Apply reconciliation only when the tool proves the finalized transaction debited the funding wallet and credited the winner by the exact atomic SKR amount.

This mode never exports a Seed Vault/private key.

### B. Trusted desktop/server CLI signer

Use only when the configured funding wallet is intentionally controlled by a local operational keypair outside the repository.

Set the signer path via an environment variable, never by pasting key contents:

```bash
export RADIANT_PAYOUT_SIGNER_KEYPAIR="C:\\secure\\sponsor-keypair.json"
```

`execute-weekly-cup-skr-transfer.mjs` verifies the public key derived by `solana-keygen` exactly matches the configured funding wallet before it can submit one `spl-token transfer`.

## One placement per execution

Transfers are intentionally sequential:

```text
#1 -> #2 -> #3
```

The tool refuses to skip an unpaid earlier placement.

Before the transaction is submitted it writes a Firestore transfer intent. If process state becomes ambiguous after submission, the item is locked for reconciliation instead of being automatically retried.

## Transaction verification

A payout item becomes `PAID` only after Solana Mainnet reports the transaction finalized and `getTransaction` proves:

- official SKR mint,
- exact funding wallet debit,
- exact winner wallet credit,
- exact atomic payout amount,
- expected transaction signature.

A finalized on-chain failure can be explicitly reset for a reviewed retry. An unknown/ambiguous transaction outcome cannot be automatically retried.

## Final lifecycle

```text
Phase 12E APPROVED
      ↓
PAYMENT_PENDING
      ↓ one finalized transaction per winner
all payout items PAID
      ↓
complete-weekly-cup-payout.mjs
      ↓
PAID
```

The public Phase 12D winner records remain immutable. Phase 12F operational state stays Admin-only under `weeklyCupPayouts/{weekKey}`.

## Prerequisites on the operator machine

For automatic CLI signing mode:

```bash
solana-keygen --version
spl-token --version
```

Install/use trusted Solana tooling appropriate for the operator environment. The transfer CLI uses the official SKR mint and `--fund-recipient` so the sender may fund a missing recipient associated token account.

The operator funding wallet must also hold enough SOL for transaction fees and any required associated-token-account creation.

## Commands

### External-wallet intent — dry run

```bash
node prepare-weekly-cup-skr-transfer-intent.mjs \
  --project radiant-rush-10a9c \
  --week 2026-W38 \
  --placement 1
```

Apply only after copying the exact values printed by the dry run:

```bash
node prepare-weekly-cup-skr-transfer-intent.mjs \
  --project radiant-rush-10a9c \
  --week 2026-W38 \
  --placement 1 \
  --confirm-digest "<MANIFEST_SHA256>" \
  --confirm-funding-wallet "<FUNDING_WALLET>" \
  --confirm-recipient "<WINNER_WALLET>" \
  --confirm-amount-atomic "<ATOMIC_AMOUNT>" \
  --apply \
  --confirm-project radiant-rush-10a9c
```

After the sponsor wallet signs/sends the exact transfer, reconcile:

```bash
node reconcile-weekly-cup-skr-transfer.mjs \
  --project radiant-rush-10a9c \
  --week 2026-W38 \
  --placement 1 \
  --signature "<REAL_SOLANA_SIGNATURE>"
```

Apply only after the dry run verifies the exact finalized transfer:

```bash
node reconcile-weekly-cup-skr-transfer.mjs \
  --project radiant-rush-10a9c \
  --week 2026-W38 \
  --placement 1 \
  --signature "<REAL_SOLANA_SIGNATURE>" \
  --apply \
  --confirm-project radiant-rush-10a9c
```

### Automatic CLI signer — dry run

```bash
node execute-weekly-cup-skr-transfer.mjs \
  --project radiant-rush-10a9c \
  --week 2026-W38 \
  --placement 1
```

The dry run opens no signer and sends no transaction. Apply requires every exact confirmation printed by the dry run plus `RADIANT_PAYOUT_SIGNER_KEYPAIR`.

### Complete the payout batch

After all configured winner items are `PAID`/`FINALIZED`:

```bash
node complete-weekly-cup-payout.mjs \
  --project radiant-rush-10a9c \
  --week 2026-W38
```

Then apply with the exact manifest digest:

```bash
node complete-weekly-cup-payout.mjs \
  --project radiant-rush-10a9c \
  --week 2026-W38 \
  --confirm-digest "<MANIFEST_SHA256>" \
  --apply \
  --confirm-project radiant-rush-10a9c
```

## Fail-closed recovery

- `SUBMITTED` but not finalized: wait and reconcile; never resend.
- `RECONCILIATION_REQUIRED`: inspect the signer wallet/activity and reconcile the original signature; never auto-retry.
- finalized on-chain failure: reconciliation can prove failure and, with explicit `--reset-failed`, reset that item for a fresh reviewed attempt.
- no known signature after an ambiguous submit: keep the item locked until the original transaction is found or manually investigated.

## Live proof boundary

Do not execute a real W38 transfer until W38 has:

- three legitimate distinct trusted winner wallets,
- Phase 12C funding `VERIFIED` before close,
- successful Phase 12D finalization,
- successful Phase 12E preparation and approval.

Until then, Phase 12F should be tested only through pure tests and fail-closed dry runs.
