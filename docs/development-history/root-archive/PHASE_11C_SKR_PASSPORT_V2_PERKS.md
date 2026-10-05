# Phase 11C — SKR Passport v2 + meaningful SKR perks

## Goal

Make the existing real Mainnet read-only SKR scan materially useful inside Radiant Rush without turning SKR wealth into ranked power and without introducing token custody or payout authority into Android.

## Source of truth

Passport v2 uses only the liquid SKR balance returned by the existing read-only Mainnet `getTokenAccountsByOwner` scan for the official SKR mint. The app does not sign, transfer, stake, unstake, or spend SKR when refreshing the Passport.

Staked SKR is **not counted** in Phase 11C. No trustworthy staking source is integrated yet, so the UI explicitly says that staking state is not verified instead of inventing it.

## Tier perks

| Passport tier | Daily SKR casual tickets | Daily chest enhancement | Cosmetic status |
| --- | ---: | --- | --- |
| Explorer | 0 | Standard chest | Explorer frame |
| Radiant Scout | +1 | +25 XP | Solar Spark frame / Spark aura / Scout Sigil badge |
| Radiant Holder | +2 | +50 XP +1 casual ticket | Neon Halo frame / aura / Holder Sigil badge |
| Radiant Elite | +3 | +75 XP +1 casual ticket | Aurora Circuit frame / aura / Elite Sigil badge |
| Radiant Legend | +4 | +100 XP +2 casual tickets | Radiant Crown frame / Crownflare aura / Legend Sigil badge |

The badge/frame/aura are off-chain app cosmetics/status, not NFTs or on-chain assets.

## Ranked fairness invariant

SKR perks never change raw Radiant Run score, tie-break rules, or the three ranked attempts per UTC day. Holder bonus tickets are stored separately as `skrCasualRushTickets` and **cannot fund a ranked attempt**. If standard tickets are zero but an SKR casual ticket remains, the run is Casual even when ranked attempts remain.

A Casual run prefers an SKR casual ticket before consuming a standard ticket. Ranked runs always consume a standard ticket.

## Daily grant behavior

The daily Passport ticket entitlement is idempotent. The SKR quest remains refreshable after completion because the scan is read-only; re-scanning the same tier on the same app day grants no duplicate tickets. If the observed liquid balance moves into a higher tier later that day, only the missing difference is granted.

The ordinary daily SKR scan quest reward remains separate from the holder perk. Existing quest progression therefore continues to work for an Explorer wallet with zero SKR.

## Chest behavior

The Daily Radiant Chest keeps its existing deterministic base reward roll. SKR does **not** alter the base rarity seed. A same-day Passport scan may add the tier's fixed XP and casual-ticket enhancement after the base reward is selected.

Chest bonus tickets also go only into `skrCasualRushTickets`.

## Security boundary

All Passport v2 perks are client-observed/off-chain hackathon product perks. Firestore ownership rules and the Mainnet RPC read do not make the Android client an economic authority. These perks cannot authorize an SKR payout, prove staking, mint anything, or change token balances.

No treasury private key, token-transfer code, staking transaction, payout signer, or fake transaction confirmation is introduced by Phase 11C.

## Documentation decision

- `CHANGELOG.md`: updated.
- `ARCHITECTURE.md`: updated for the Passport rules boundary and split ticket ledger.
- `AGENTS.md`: unchanged; source/patch/security workflow is unchanged.
- `TESTING_AND_RELEASE.md`: updated with Phase 11C unit/device QA.
- `SOLANA_SECURITY_AND_DATA_RULES.md`: updated with liquid-only, off-chain-perk and no-staking-authority rules.
- `MOBILE_UI_UX_STANDARDS.md`: updated for Passport status and standard-vs-casual ticket clarity.
- VERSION: milestone APK version bumped to `versionCode 17`, `versionName "1.1.2-phase11c"`.
- `firebase/firestore.rules`: unchanged in Phase 11C; all new Passport fields live under existing owner-writable user/completed-quest documents and no new public collection is introduced.
