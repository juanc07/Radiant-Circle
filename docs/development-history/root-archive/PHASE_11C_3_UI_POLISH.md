# Phase 11C.3 — UI polish regression fix

This patch is intentionally presentation-only on top of Phase 11C.2.

## Fixes

- Common phone widths now enter compact layouts earlier so metric cards do not squeeze tier/status text.
- Single-token metric values use measured shrink-to-fit and remain one intact line.
- Radiant Run restores the existing pastel primary/tertiary/error palette while FEVER decoration stays non-target-shaped.
- The chest keeps its glow/particles/audio/haptics but replaces charge-derived micro-oscillation with a short damped keyframed shake and stable geometry.
- Remaining player-facing implementation jargon is removed from normal copy.

## Unchanged boundaries

- No Firestore rule or schema change.
- No wallet/MWA/Solana proof-flow change.
- No SKR/staking calculation change.
- No ranked-attempt or payout-authority change.
- No reward-authority change.

## Documentation decision

- CHANGELOG: updated.
- ARCHITECTURE: unchanged; no architecture change.
- AGENTS: unchanged; workflow rules unchanged.
- TESTING_AND_RELEASE: updated with device/UI regression checks.
- SOLANA_SECURITY_AND_DATA_RULES: unchanged; no security/data-rule change.
- MOBILE_UI_UX_STANDARDS: updated with no-cut/no-bleed and animation-stability rules.
- VERSION: bumped to `1.1.5-phase11c3` / code 20.
