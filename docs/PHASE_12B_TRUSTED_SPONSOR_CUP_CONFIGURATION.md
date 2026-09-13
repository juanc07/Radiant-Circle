# Phase 12B — Trusted Sponsor / Weekly Cup Configuration

## Goal

Move Weekly Radiant Cup sponsor/prize configuration out of ad-hoc display metadata and into a constrained trusted Admin-written schema without adding funding verification or payout authority to Android.

## Trusted Firestore document

Path:

```text
weeklyCupConfigs/{weekKey}
```

Schema v2 fields:

```text
schemaVersion: 2
weekKey: YYYY-Www
status: DRAFT | ANNOUNCED | OPEN | CLOSED | CANCELLED
sponsorName
sponsorNote
prizeAssetSymbol: SKR
prizeMint: official SKR mint
prizeDecimals: 6
prizeAmountAtomic: exact decimal-free string
placementAllocationsBps: map whose values total 10000
startsAt / endsAt: UTC ISO-week timestamps
fundingWalletAddress: optional public Solana address
fundingVerificationStatus: NOT_CONFIGURED | NOT_VERIFIED
trustedResultsRequired: true
payoutEnabled: false
configurationAuthority: trusted-admin-phase12b
createdAt / updatedAt
```

Phase 12C may later promote funding state after independent on-chain verification. Phase 12B does not.

## Android trust behavior

`Phase12WeeklyCupConfigRules` validates the current week, schema, authority marker, official SKR identity, positive exact atomic amount, valid UTC window, and a complete 100% placement split. Unexpected config fails closed and is not labeled trusted.

The Weekly Cup UI may show sponsor/prize configuration, but client-facing copy must stay product-facing and concise. Internal implementation language such as `trusted config`, `client scores`, `payout authority`, `Android payout`, schema/authority markers, or verification architecture belongs in logs/admin tools/docs, not in normal player UI.

Public Cup copy should use consumer labels such as `Weekly prize`, `Presented by …`, `Funding pending`, `Funding verified`, and `Prize unavailable`. A DRAFT Cup is not publicly presented as a sponsor/prize card. `sponsorNote` is treated as admin/internal metadata in Phase 12B and is not rendered to players. Android still keeps payout disabled internally and client-reported competition receipts remain unverified.

## Admin workflow

From `scripts/firebase-admin` after `npm install` and credentials are configured outside the repository:

```bash
node manage-weekly-cup.mjs \
  --project radiant-rush-10a9c \
  --week 2026-W37 \
  --status ANNOUNCED \
  --sponsor "ThinkBloxPH" \
  --prize-skr 1000 \
  --note "Sponsored results require trusted verification after Cup close."
```

This is dry-run only. Optional:

```bash
  --funding-wallet "PUBLIC_SOLANA_ADDRESS" \
  --placements "1:50,2:30,3:20"
```

After review, apply with the same arguments plus:

```bash
  --apply \
  --confirm-project radiant-rush-10a9c
```

When Phase 12C funding evidence exists, the tool preserves that evidence for safe status/sponsor presentation updates, but refuses prize-amount/mint or funding-wallet changes that would invalidate the verification. It still refuses payout-enabled or newer-schema state.

## Explicit non-goals

Phase 12B does not:

- verify the sponsor funding wallet balance,
- verify wallet ownership,
- verify competition receipts,
- select trusted winners,
- enable payouts,
- transfer SKR,
- hold a treasury/private key in Android or source control,
- add player wagering or entry fees.

Those trust transitions belong to Phase 12C and later.
