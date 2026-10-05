# Phase 11E — Final reward / chest polish

## Goal

Make the Daily Radiant Chest feel immediate, premium, and readable on phone screens without changing reward economics, Ranked fairness, wallet authority, or SKR payout behavior.

## Changes

- Starts the Firestore claim immediately instead of sleeping before the request.
- Keeps only a short 520 ms minimum opening pose for very fast saves; slower saves reveal as soon as the real result arrives with no extra artificial delay.
- Adds testable presentation-only rarity rules for the actual chest ladder: Spark, Pulse, Flare, Aurora, Legendary.
- Scales particles, rays, shockwaves, reveal audio, and reward color by rarity.
- Keeps the chest/card geometry stable while using a short damped anticipation movement only on the chest art.
- Adds a persistent reward orb after the burst so the reveal does not visually disappear when particles finish.
- Replaces the single dense reward sentence with a centered reward tray: rarity, reward title, total XP, standard Rush Tickets, SKR casual tickets, and SKR XP boost when present.
- Keeps required reward text wrapping on compact phones and large Android font scales; no required chest copy relies on ellipsis.
- Improves procedural chest reveal audio timing/intensity while continuing to ship no external media asset requirement.

## Authority and fairness

Phase 11E is presentation/pacing only. It does not change:

- deterministic chest reward selection;
- base rarity probabilities;
- streak XP formula;
- SKR Passport bonus amounts;
- Ranked attempts or Ranked score;
- Firestore security rules;
- wallet signing or Solana transaction behavior;
- sponsor/Cup payout state;
- `payoutEnabled = false` for Phase 11.

## Version

- `versionCode = 23`
- `versionName = "1.1.8-phase11e"`

## Documentation decision

- CHANGELOG: updated.
- ARCHITECTURE: updated with presentation/authority boundary.
- TESTING_AND_RELEASE: updated with chest timing/device QA.
- MOBILE_UI_UX_STANDARDS: updated with reward reveal rules.
- SOLANA_SECURITY_AND_DATA_RULES: updated to state that reward presentation cannot authorize economic actions.
- AGENTS: unchanged; the existing workflow/authority contract already covers this patch.
- VERSION: bumped in `app/build.gradle.kts`.
