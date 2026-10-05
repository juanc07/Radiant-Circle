# Phase 11C.5 — Visual Identity + Badge Polish

## Goal

Give Radiant Rush a consistent player-facing visual identity without changing wallet, SKR, competition, reward, or payout authority.

## Changes

- Replaced the generic check/bolt badge medallions with scalable Compose-drawn Radiant Rush crests.
- Added distinct badge glyphs for launch, daily check-in, wallet, signed proof, on-chain proof, SKR Passport, chest, first run, combo, collection, and streak achievements.
- Added an SKR Passport crest card that surfaces tier, eligible SKR, stake boost, frame, and aura in one responsive visual identity block.
- Kept all badge/Passport marks vector-like and density-independent; no bitmap asset is required for these identity marks.
- Tightened badge descriptions so achievement cards read like player rewards rather than implementation notes.
- Kept compact/large-font layouts stacked so tier/balance/badge text can wrap without clipping, bleeding, or ellipsis.

## Fairness / security

This patch is presentation-only. It does not change SKR balance/staking reads, ranked attempts, score ordering, gameplay-XP caps, chest reward authority, wallet signing, Firestore authority, or payout eligibility.

## Documentation decision

- CHANGELOG: updated.
- ARCHITECTURE: updated with the presentation-only identity component boundary.
- AGENTS: unchanged; contributor workflow did not change.
- TESTING_AND_RELEASE: updated with visual identity QA.
- SOLANA_SECURITY_AND_DATA_RULES: unchanged; no Solana/security authority changed.
- MOBILE_UI_UX_STANDARDS: updated with badge/crest responsiveness rules.
- VERSION: bumped to `versionCode = 21`, `versionName = "1.1.6-phase11c5"` because this is a visible UI milestone.
## Alignment hotfix

- Badge cards now size their inner content to the full card width before applying centered alignment, so wrapped titles/descriptions are centered against the actual card rather than a wrap-content child width.
- Radiant Run completion and reward cards now use full-width centered content blocks for the run-complete headline, score summary, save message, rarity/reward copy, and run summary.
- Quest detail dialogs now center title, body copy, and the acknowledgement action.
- No gameplay, wallet, SKR, staking, Firestore, ranking, reward, or payout authority changed.

