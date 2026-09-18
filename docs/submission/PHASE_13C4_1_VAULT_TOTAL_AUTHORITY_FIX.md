# Phase 13C.4.1 — Radiant Vault total authority fix

## Problem
A device could still display `0/6` after the 12-collectible expansion. The 12-item catalog was present, but the runtime total was not explicitly anchored to the catalog version.

## Fix
- Adds `RadiantGameRules.COLLECTION_TOTAL = 12` as the catalog authority.
- Hydration now sets `RadiantRunPreview.collectionTotal` from that authority, never from an older persisted/profile total.
- Adds startup invariants for exactly 12 unique collectible IDs.
- Keeps all six legacy collectible IDs unchanged, preserving existing player progress.
- Adds tests proving old profiles still render all 12 Vault slots.

No reward odds, token logic, wallet logic, or payout logic are changed by this patch.
