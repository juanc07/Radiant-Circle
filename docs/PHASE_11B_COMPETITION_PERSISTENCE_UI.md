# Phase 11B — Competition Persistence + Visible UI

## Scope

Phase 11B connects the Phase 11A pure rules to Firebase and the Compose experience. It adds separate Radiant Run Weekly, All-Time, and Personal Stats views while retaining the existing long-term XP leaderboard.

## Data model

- `runWeekly/{UTC_WEEK}/entries/{firebaseUid}` — one owner-writable ranked PB row for the current UTC week.
- `runAllTime/{firebaseUid}` — one owner-writable ranked all-time PB row.
- `users/{uid}` — private daily attempt/XP counters and personal-stat mirrors used for responsive UI/fallback.
- Public rendering collapses duplicate Firebase UIDs by connected wallet identity.

## Fairness

Every connected wallet receives three ranked attempts per UTC day. Additional ticket-backed plays are Casual. Casual play still consumes a ticket, opens the normal non-token capsule/collectible reward, and can earn capped gameplay XP, but it cannot change Weekly/All-Time ranked PBs. SKR balance does not multiply ranked score or attempts.

## XP

Run performance XP uses `Phase11CompetitionRules.performanceXpForRun()` and is capped at 300 gameplay XP per UTC day. Raw score remains the competition metric.

## Security

All current run scores are client-reported prototype data. Competition documents include `scoreAuthority=client-reported-prototype-not-payout-authority` and `payoutEligible=false`. No code in this phase transfers SKR or contains a treasury signer. A trusted future payout authority is required before real prizes can be authorized from results.

## Deployment note

Deploy `firebase/firestore.rules` before expecting public Run ranks to load/write in the target Firebase project. The app degrades to empty run-rank lists with an explanatory message if the new collections are not yet allowed.

## Documentation decisions

- CHANGELOG: updated.
- ARCHITECTURE: updated.
- AGENTS: unchanged; workflow contract unchanged.
- TESTING_AND_RELEASE: updated.
- SOLANA_SECURITY_AND_DATA_RULES: updated.
- MOBILE_UI_UX_STANDARDS: updated because visible competition UI was added.
- VERSION: bumped to `versionCode 15` / `1.1.0-phase11b` because this is a visible milestone app patch.
