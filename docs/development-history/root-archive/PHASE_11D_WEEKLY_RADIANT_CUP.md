# Phase 11D — Weekly Radiant Cup + sponsor-ready architecture

## Goal

Turn the existing weekly Ranked Radiant Run board into a clear weekly competition without pretending that Solana Mobile, the hackathon, or any sponsor will automatically pay winners.

## Player experience

- `Ranks > Weekly Cup` is the weekly competition home.
- Shows the ISO UTC season key and a live countdown to Monday 00:00 UTC.
- Shows Top 3 podium, remaining standings, participant count, the connected wallet's rank and weekly PB.
- Shows a projected weekly cosmetic accolade:
  - #1: Radiant Champion Crest
  - #2–#3: Radiant Podium Crest
  - #4–#10: Radiant Top 10 Ribbon
  - other ranked players: Cup Finisher Mark
- When the previous week's Top 100 data is available, the app derives and displays the connected wallet's prior-season accolade.
- Sponsor area is visible but defaults to `No sponsored prize this week`.

## Sponsor-ready metadata

Optional server/admin-authored metadata may be published at:

`weeklyCupConfigs/{ISO_WEEK}`

Supported display fields:

- `status`: only `announced` activates sponsor presentation
- `sponsorName`
- `prizeLabel`
- `note`

Firestore allows public reads and denies all Android client writes. A future trusted backend/Admin SDK may publish metadata.

## Security boundary

- Existing weekly score rows remain client-reported prototype rankings.
- `WeeklyRadiantCupRules.sponsorState()` hard-codes `payoutEnabled = false`.
- No Android field, Firestore sponsor document, SKR balance, stake balance, or placement can enable token payout.
- No treasury/private key is added to the APK.
- No player entry fee or wagering is added.
- Real Sponsored SKR Cup payout belongs to Phase 12 after trusted score verification, funded prize custody, winner finalization, and server-side payout authority exist.

## Documentation/version decision

- CHANGELOG: updated.
- ARCHITECTURE: updated with Cup and sponsor metadata boundaries.
- AGENTS: unchanged; contributor workflow is unchanged.
- TESTING_AND_RELEASE: updated with Cup test matrix and Firebase rule deployment.
- SOLANA_SECURITY_AND_DATA_RULES: updated to state that sponsor metadata is display-only and never payout authority.
- MOBILE_UI_UX_STANDARDS: updated for responsive Cup/podium/countdown layouts.
- VERSION: bumped to `versionCode = 22`, `versionName = "1.1.7-phase11d"`.
