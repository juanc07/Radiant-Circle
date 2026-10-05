# Phase 10 — Radiant Game Layer

## Goal

Make Radiant Rush feel like a product/game rather than a sequence of wallet/Firebase demonstrations, while preserving the stable Solana proof code from Phases 3–9.

## Vertical slice

Phase 10 delivers one end-to-end loop:

1. Daily proof actions continue to award XP.
2. Newly completed proof actions also award free Rush Tickets.
3. Existing users without a Phase 10 ticket field receive the compatibility default of 3 starter tickets.
4. One Rush Ticket starts a 20-second Radiant Run.
5. The player taps Radiant targets and avoids Corruption targets.
6. Five consecutive Radiant hits activates FEVER scoring.
7. The final score determines the capsule band.
8. The capsule reveals one of six collectibles.
9. A new collectible is added to the Vault; a duplicate produces Radiant Shards.
10. Run XP, best score, ticket balance, total runs, latest capsule, shards, and collection counts are persisted to the owner's Firebase profile.

## No art-asset dependency

The Phase 10 game intentionally does not require external raster/vector art assets. Compose Canvas draws the gameplay field and targets. Material/Compose primitives provide gradients, cards, glows, rings, silhouettes, typography, and the reward reveal. Haptics provide tactile feedback.

Future art can replace or embellish these primitives without changing the game rules or persistence model.

## Ticket sources

- Compatibility/new-profile starter balance: 3.
- First wallet reward: +1 once for the existing wallet proof document.
- New daily check-in: +1.
- New daily signed proof: +1.
- New daily memo proof: +1.
- First SKR scan of the day: +1.
- Daily Radiant Chest: +2.
- One completed Radiant Run consumes 1 ticket when its result is saved.

Tickets are not purchasable with SOL/SKR in Phase 10.

## Capsule / collectible set

- Spark Bit — Common
- Neon Circuit — Common
- Solar Shard — Uncommon
- Nova Prism — Rare
- Phantom Halo — Epic
- Radiant Crown — Legendary

Better run scores widen access to higher rarity outcomes. The app does not sell random outcomes and does not represent these items as tokens or items with monetary value.

## Persistence fields

Owner profile fields added/used:

- `rushTickets`
- `bestRunScore`
- `totalRuns`
- `lastRunScore`
- `lastRunMaxCombo`
- `lastRunRadiantHits`
- `lastRunCorruptedHits`
- `lastRunCapsuleTier`
- `lastRunRewardId`
- `lastRunRewardTitle`
- `lastRunRewardRarity`
- `lastRunRewardXp`
- `lastRunRewardShards`
- `radiantShards`
- `radiantCollection`
- `collectionOwned`

The leaderboard remains Firebase-UID-owned for current Firestore rule compatibility, while the public ranking UI continues collapsing duplicate anonymous UIDs by Solana wallet identity.

## Required QA

Automated:

```bash
./gradlew --stop
./gradlew :app:testDebugUnitTest
./gradlew :app:assembleDebug
./gradlew :app:connectedDebugAndroidTest
```

Physical Android device:

1. Open Today and confirm the Radiant Run card is visible without text clipping.
2. Confirm an upgraded legacy profile shows 3 starter tickets when no `rushTickets` field existed.
3. Start a run and confirm the 3-2-1 countdown appears.
4. Tap bright Radiant targets and confirm score/combo increases with haptic feedback.
5. Tap a red Corruption target and confirm score penalty + combo reset.
6. Reach 5 combo and confirm FEVER copy/visual state appears.
7. Let the 20-second timer finish.
8. Confirm the score is saved and exactly 1 ticket is consumed.
9. Confirm the capsule reveal appears and XP increases.
10. Confirm Home Vault discovers the item; repeat a deterministic/duplicate case over additional runs and verify shards can increase.
11. Restart the app and confirm tickets, best score, run count, last reward, and Vault progress persist.
12. Re-check Ranks: the same Solana wallet must still appear once only.
13. Re-check Connect / Sign / Memo / SKR; Phase 10 must not regress wallet behavior.

## Known limitation / security boundary

Radiant Run scoring is client-side in this hackathon vertical slice. Firestore owner rules prevent another anonymous UID from writing this profile, but they do not make game scores cheat-proof. The game items have no token/cash redemption and no economic authority. A production competitive season should move score validation/reward issuance to a trusted backend before offering valuable rewards.

## Documentation decision

- `CHANGELOG.md`: updated.
- `ARCHITECTURE.md`: updated for native game/persistence layer.
- `BACKLOG_AND_ROADMAP.md`: updated to mark Phase 10 vertical slice and follow-up polish.
- `TESTING_AND_RELEASE.md`: updated with game QA.
- `SOLANA_SECURITY_AND_DATA_RULES.md`: updated with non-economic client-game boundary.
- `MOBILE_UI_UX_STANDARDS.md`: updated with gameplay/mobile rules.
- `AGENTS.md`: no change; contributor operating rules are unchanged.
- Version: bumped to `1.0.0-phase10` / code 12.
