# Phase 12C — Trusted SKR Funding Verification

## Goal

Phase 12C verifies a Weekly Radiant Cup's configured SKR prize against a real Solana Mainnet funding wallet from trusted Firebase Admin tooling.

This phase does **not** transfer SKR and does **not** enable payout.

## Trusted flow

1. Read `weeklyCupConfigs/{weekKey}` with Firebase Admin.
2. Require the Phase 12B trusted Cup shape, official SKR mint, 6 decimals, positive atomic prize amount, trusted results, and `payoutEnabled=false`.
3. Query Solana Mainnet `getTokenAccountsByOwner` for the public funding wallet filtered by the official SKR mint.
4. Sum exact raw liquid SKR amounts using integer arithmetic.
5. Exclude frozen token accounts from transferable funding.
6. Compare observed liquid SKR against the configured required prize amount.
7. Dry-run by default.
8. On explicit `--apply --confirm-project`, transactionally update the Cup funding snapshot and create an immutable admin-only audit receipt under `weeklyCupFundingChecks/{weekKey}/checks/{checkId}`.

## Verification states

- `NOT_CONFIGURED` — no funding wallet is configured.
- `NOT_VERIFIED` — a wallet is configured but the trusted Mainnet snapshot does not currently cover the required liquid SKR amount.
- `VERIFIED` — the trusted Mainnet snapshot observed transferable liquid SKR greater than or equal to the configured prize requirement.
- `REJECTED` — reserved for trusted administrative rejection; RPC/network failures do not silently write this state.

A failed RPC call writes nothing.

## Current trusted evidence fields

`weeklyCupConfigs/{weekKey}` may receive:

- `fundingWalletAddress`
- `fundingVerificationStatus`
- `fundingRequiredAmountAtomic`
- `fundingObservedAmountAtomic`
- `fundingTokenAccountCount`
- `fundingFrozenTokenAccountCount`
- `fundingVerificationSlot`
- `fundingVerificationNetwork = "mainnet-beta"`
- `fundingVerificationMint = <official SKR mint>`
- `fundingVerificationCommitment = "finalized"`
- `fundingVerificationAuthority = "trusted-admin-phase12c"`
- `fundingVerificationSchemaVersion = 1`
- `fundingCheckedAt`
- `fundingVerifiedAt` only when status is `VERIFIED`

`payoutEnabled` remains false.

## Immutable funding check receipts

Each applied check creates:

`weeklyCupFundingChecks/{weekKey}/checks/{checkId}`

The receipt records the wallet, required and observed raw amounts, token account counts, RPC slot, network, commitment, status, authority, and server timestamp.

Firestore client rules deny reads/writes to this audit collection. Firebase Admin tooling bypasses client rules.

## Android trust rule

Android remains read-only. It does not trust a bare remote string `fundingVerificationStatus = VERIFIED`.

The app displays verified funding only when the Phase 12C evidence is internally consistent:

- official SKR mint
- mainnet-beta
- finalized commitment
- expected Phase 12C authority and evidence version
- required amount equals the configured prize amount
- observed amount is greater than or equal to required amount
- positive RPC slot
- trusted check and verified timestamps exist

If any required evidence is missing or inconsistent, the player-facing state falls back to pending rather than showing verified funding.

## Player-facing copy

Normal UI stays product-focused:

- `Weekly prize`
- `Presented by ...`
- `Funding pending`
- `Funding verified`
- `Prize unavailable`

Do not expose RPC slots, authority names, schema versions, payout-boundary explanations, or internal verification diagnostics to ordinary players.

## Funding is a snapshot, not escrow

Phase 12C proves what the configured wallet held at a specific trusted Mainnet check. It does not lock funds and does not guarantee that the wallet balance cannot later change.

Before any real payout flow, trusted infrastructure must re-check funding and Phase 12F must handle the actual transfer/escrow model safely.

## Security boundaries

- No service-account JSON in Git.
- No treasury/private key in Android.
- No seed phrase/private key in admin source.
- No client write authority for Cup config or funding evidence.
- No player wagering or entry fee.
- Staked SKR is not counted as prize funding because it is not immediately transferable liquid SKR.
- No fake balance, fake funding confirmation, or fake payout.
