# Phase 11A — SKR Arena Competition Foundation

Date: 2026-09-10

## Goal

Create the pure, testable competition rules required before Phase 11B touches Firebase or Compose UI. Phase 10.1.1 gameplay and the working MWA/Phantom/Mainnet-SKR flows are intentionally unchanged.

## Added

- `Phase11CompetitionRules`
- deterministic UTC day keys and ISO UTC week keys
- `RunScoreRecord` with explicit `ClientReportedPrototype` authority
- `WeeklyRunStats`
- `RunPersonalBest`
- `GameplayXpAward`
- `RankedAttemptDecision`
- `RunLeaderboardCandidate`
- wallet-deduplicated run-board ordering
- 3 equal ranked attempts per connected wallet per UTC day
- casual fallback after ranked attempts are exhausted or when no wallet is connected
- performance-based gameplay XP with a 300 XP/day cap

## Competition rules

The competitive metric is raw Radiant Run score. XP is progression only and is never substituted for score.

Ranked ordering is:

1. higher raw score
2. higher best combo when raw score ties
3. more PERFECT hits when score and combo tie
4. earlier achievement timestamp when all skill metrics tie

Weekly ranked state uses an ISO week key such as `2026-W37`, computed in UTC. All-Time state is personal-best based rather than cumulative score.

## Ranked attempts

A connected wallet receives 3 ranked attempts per UTC day. This limit is equal for every player. Additional runs that still have a Rush Ticket are casual and do not update Weekly/All-Time ranked stats. SKR wealth is not an input to the ranked-attempt rule.

## Gameplay XP

Run-performance XP has a base finish award plus bounded score/combo/PERFECT contributions. It is deliberately not score 1:1 and is capped at 300 gameplay XP per UTC day.

This is a domain rule only in Phase 11A. Phase 11B will wire it into Firebase persistence and the app UI.

## Security

Android currently produces Radiant Run score. Therefore every Phase 11A `RunScoreRecord` is explicitly `ClientReportedPrototype` and exposes `payoutEligible = false`.

Phase 11A does not implement:

- real SKR payouts
- SKR wagering or entry fees
- a treasury signer/private key
- staking state
- transaction confirmation
- Firebase competition persistence
- competition UI

A later real SKR Cup must use a trusted validation/payout authority outside the APK. Client score alone cannot authorize token transfer.

## Files

New:

- `app/src/main/java/com/thinkblox/radiantrush/logic/Phase11CompetitionRules.kt`
- `app/src/test/java/com/thinkblox/radiantrush/logic/Phase11CompetitionRulesTest.kt`
- `docs/PHASE_11A_COMPETITION_FOUNDATION.md`

Updated:

- `docs/operating-system/CHANGELOG.md`
- `docs/operating-system/ARCHITECTURE.md`
- `docs/operating-system/TESTING_AND_RELEASE.md`
- `docs/operating-system/SOLANA_SECURITY_AND_DATA_RULES.md`

## Documentation decision

- CHANGELOG.md: updated — Phase 11A is meaningful new domain behavior.
- ARCHITECTURE.md: updated — added the competition-domain authority boundary and Phase 11B handoff.
- AGENTS.md: not updated — source authority, patch workflow, and agent rules did not change.
- TESTING_AND_RELEASE.md: updated — added the Phase 11A unit/acceptance gate.
- SOLANA_SECURITY_AND_DATA_RULES.md: updated — formalized client-score/non-payout authority and no-treasury-key rules.
- MOBILE_UI_UX_STANDARDS.md: not updated — Phase 11A has no UI changes; Phase 11B will add the run leaderboard UI.
- VERSION: not updated — this is a foundation source patch, not an APK/release handoff.
