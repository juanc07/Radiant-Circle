# Phase 11C.1 — Verified Staked SKR + Guardian Stake Boost

## Decision

Radiant Rush now treats verified active SKR stake as first-class SKR Passport v2 participation rather than ignoring users who have moved most of their SKR into staking.

The Android app performs **read-only Mainnet queries** against the official Solana Mobile SKR staking program. The scan uses only the already-connected public wallet address. It does not request a staking transaction, open a wallet, sign a message, move tokens, or store secrets.

Official staking program:

`SKRskrmtL83pcL4YqLWt6iPefDqwXQWHSw9S9vz94BZ`

Official SKR mint:

`SKRbvo6Gf7GondiT3BbTfuRDPqLWei4j2Qy2NPGZhW3`

The account layout and share-price calculation follow the official Solana Mobile `react-native-samples/skr-staking` Anchor IDL/sample. SKR uses 6 decimals and active stake is derived from `shares * sharePrice / 1e9`.

## Passport eligibility

Passport tier balance is now:

`verified liquid SKR + verified active staked SKR`

SKR already moved into an **unstaking/cooldown** state is displayed separately and is not included in Stake Boost eligibility. The official staking flow stops staking rewards as soon as unstaking begins, so Radiant Rush mirrors that distinction.

If the staking RPC read cannot be verified, Radiant Rush does not guess a stake amount. The liquid-SKR scan can still succeed, and staking is shown as unavailable/unverified for that refresh.

## Guardian Stake Boost

Any verified active stake above 0 SKR activates a small non-ranked incentive on top of the normal Passport tier perks:

- +1 casual-only Rush Ticket/day.
- +25 Daily Radiant Chest XP.
- Guardian Glow cosmetic/aura status.

The existing tier chest-ticket configuration remains unchanged.

### Competitive fairness

Stake Boost **does not**:

- multiply ranked raw score;
- add ranked attempts;
- alter combo/PERFECT tie-breakers;
- authorize an SKR payout;
- require a player to wager SKR;
- move, lock, stake, or unstake tokens from Radiant Rush.

The three ranked attempts per wallet per UTC day remain identical for every wallet.

## Staking discovery

Radiant Rush queries the staking program for `UserStake` accounts whose `user` field matches the connected public key. This supports more than one Guardian delegation instead of assuming a permanently fixed Guardian. Active shares are summed before conversion with the global `StakeConfig.share_price`.

## Mobile UI

New staking information is rendered only inside existing responsive `LazyColumn` / `fillMaxWidth` cards. No fixed screen width or single-device pixel assumptions were introduced. Long staking status/helper text is allowed to wrap/expand vertically instead of using ellipsis, and buttons keep the existing responsive minimum heights.

Manual QA must include a compact Android phone and the Solana Seeker when both are available.

## Version

- `versionCode = 18`
- `versionName = 1.1.3-phase11c1`

## Documentation decision

Updated: `CHANGELOG`, `ARCHITECTURE`, `TESTING_AND_RELEASE`, `SOLANA_SECURITY_AND_DATA_RULES`, `MOBILE_UI_UX_STANDARDS`.

`AGENTS.md`: no change; contributor/agent workflow rules did not change.

Version decision: Android version is bumped to `1.1.3-phase11c1`; there is no separate VERSION file in the current operating-system docs.
