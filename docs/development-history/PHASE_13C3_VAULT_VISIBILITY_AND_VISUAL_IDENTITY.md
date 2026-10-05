# Phase 13C.3 — Vault Visibility + Visual Identity

## Why this patch exists

The Radiant Rush reward state and `radiantCollection` persistence were still intact, but the social-first screen refactor left `RadiantVaultStrip` unused. Players could open a capsule and receive a collectible, yet had no obvious place in the current UI to browse the collection.

## Important terminology

Capsules are reward tiers, not collectibles:

- Spark Capsule
- Pulse Capsule
- Nova Capsule
- Radiant Capsule

A capsule reveals one collectible from the six-item Radiant Vault. `Nova Prism` is the Rare collectible whose name is closest to `Nova Capsule`.

## Changes

- Restores the Radiant Vault directly below the Radiant Rush launcher on Today.
- Hydrates the persisted `lastRunCapsuleTier` into `RadiantRunPreview`.
- Shows the relationship explicitly: `Nova Capsule → Nova Prism` (or the actual last capsule/reward).
- Corrects Profile "Last Capsule" copy to include both the capsule tier and revealed collectible.
- Gives the game/Vault surface a more recognizable Radiant Circle visual language: asymmetric signature corners, gradient energy field, capsule-to-relic reveal ledger, and compact relic tiles.

## Security / economy

No wallet, Solana, SKR, payout, competition authority, reward odds, or collectible persistence behavior changes. This is visibility/presentation plus hydration of an already-persisted non-economic field.
