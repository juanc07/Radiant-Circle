# Phase 11F — Retention and User Value Loop

Phase 11F makes the existing game, quests, chest, collection, and Weekly Radiant Cup easier to understand as one repeatable loop.

## Player-facing loop

- Home shows one clear **What’s next** action.
- Three UTC-daily Radiant Run goals rotate deterministically across devices.
- Three weekly goals track total runs, ranked runs, and Daily Radiant Chests.
- Streak and collection milestones show the next cosmetic recognition target.
- Public profile adds a short display name plus a bundled animal avatar.
- Weekly/All-Time/XP leaderboard rows mark the connected player with **YOU** and show the chosen animal avatar.

## Persistence

The user profile mirrors lightweight retention counters:

- `dailyActivityKey`, `dailyRunsToday`, `dailyPerfectHitsToday`, `dailyBestScoreToday`, `dailyBestComboToday`
- `weeklyActivityKey`, `weeklyRunsCompleted`, `weeklyChestsOpened`
- `avatarId`

Daily and weekly values are reset logically by their UTC key; stale counters are never treated as current progress.

## Fairness and security

- Phase 11F adds no SKR payout and no player wagering.
- Retention milestones are guidance/cosmetic recognition only; they do not modify Ranked score or Ranked attempt limits.
- Public identity is display name + bundled animal avatar only. No photo upload, location, device fingerprint, private key, or Seed Vault secret is stored.
- Chat, friend discovery, SKR transfers, and a services marketplace are deliberately outside Phase 11F and require separate abuse, privacy, payment, and moderation architecture.

## Version

- `versionCode = 24`
- `versionName = 1.1.9-phase11f`

## Documentation decision

- CHANGELOG: updated.
- ARCHITECTURE: updated for retention and public identity flow.
- AGENTS: no workflow change required.
- TESTING_AND_RELEASE: updated with Phase 11F checks.
- SOLANA_SECURITY_AND_DATA_RULES: updated with public-profile/social boundaries.
- MOBILE_UI_UX_STANDARDS: updated for goals, avatar rows, and YOU marker.
- VERSION: Android build version advanced in `app/build.gradle.kts`; no separate VERSION file exists.
