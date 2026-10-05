# Phase 11F.4 — Responsive UI hardening

## Goal

Re-audit the Compose UI after the Phase 11F retention/public-profile work and the Radiant Circle rebrand. Required player-facing text must remain readable on Seeker/Samsung-class phones, narrow widths, and larger Android font scales without clipping, bleeding, forced ellipsis, or awkwardly compressed horizontal rows.

## Changes

- Broadened the shared compact-layout breakpoint so phone layouts stack earlier instead of shrinking text to fit desktop-like rows.
- Removed brittle dynamic text shrinking from metric cards; compact phones now use full-width cards with normal typography and wrapping.
- Hardened hero, sync-status, quest/status, section-title, badge, Passport, leaderboard, retention-goal, and profile-info layouts so required text can grow vertically.
- Compact leaderboard rows now stack identity, score, tier, and performance detail instead of forcing all content into one row.
- Compact bottom navigation no longer forces every label to remain visible at once; selected/large layouts retain accessible labels without six-way text crowding.
- Badge cards use a single-column grid on compact/large-text layouts.
- Collectible mini-cards are wider/taller and no longer cap discovered titles to two lines.

## Profile icon picker

The profile identity editor now stacks on phone layouts. Tapping the current avatar opens a near-full-screen dialog with:

- horizontally scrollable categories,
- a vertically scrollable adaptive icon grid,
- icon-only cells so labels cannot wrap/collide inside tiny tiles,
- a separate full-width selected-icon label,
- automatic opening on the current icon's category,
- additional Faces, Nature, and Food categories on top of Animals, Cosmic, Mystic, Spooky, Zodiac, and Weird.

## Authority / data boundaries

This phase is presentation-only plus an expanded bundled avatar vocabulary. It does not change Firebase rules, wallet/Solana signing, SKR/staking calculations, competition scoring, Ranked attempts, reward economics, sponsor metadata authority, or payout eligibility.

## Documentation decision

- CHANGELOG: updated.
- ARCHITECTURE: updated with the phone-first responsive boundary.
- MOBILE_UI_UX_STANDARDS: updated with text-growth and picker rules.
- TESTING_AND_RELEASE: updated with the full responsive regression matrix.
- SOLANA_SECURITY_AND_DATA_RULES: unchanged; no wallet/SKR/security authority changed.
- AGENTS: unchanged.
- VERSION: bumped to `versionCode = 26`, `versionName = "1.2.1-phase11f4"`.
