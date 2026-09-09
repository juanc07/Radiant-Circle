# RadiantSolanaHackatonAndroid — Backlog and Roadmap

## Current phase

Radiant Rush is moving through **Phase 5: SKR balance tier**. The app now has a native Compose shell, Firebase profile/progress persistence, Mobile Wallet Adapter wallet connection, MWA daily message signing, devnet Memo transaction proof, and read-only mainnet SKR Passport scanning.

Immediate owner task: test Phase 5 on a real Android phone, then confirm Firestore receives `skr-holder_<date>` with either a real SKR balance or honest `0 SKR` Explorer state.

## Owner-ordered near-term work

1. Create or audit the Android project skeleton. — done for Phase 1
2. Add native mobile-first Compose shell for Radiant Rush. — done for Phase 1
3. Add Firebase Auth and Firestore profile/quest persistence. — done for Phase 2
4. Add Mobile Wallet Adapter connection flow. — done for Phase 3
5. Add signed daily proof quest. — delivered in Phase 4 patch
6. Add devnet memo transaction proof quest. — delivered in Phase 4 patch, needs device/RPC test
7. Add transaction confirmation polling and status refresh.
8. Add SKR balance/tier detection and XP multiplier. — delivered in Phase 5 patch
9. Add streaks, badges, and leaderboard polish.
10. Polish mobile UI for small phones and Seeker device demo.
11. Prepare APK, demo video, GitHub repo, and pitch deck.

## Phase 5 delivered scope

- Read SKR token balance on mainnet-beta using the connected wallet public address.
- Keep SKR read-only; no paid transaction, staking, or transfer is required.
- Add SKR tiers and XP multipliers.
- Add SKR badge state and Profile/Home display.
- Clearly distinguish devnet quest proofs from mainnet SKR balance reads.

## Phase 6 candidate scope

- Add transaction confirmation polling and explorer status refresh.
- Add richer SKR Passport visuals/profile frame.
- Add Seeker-specific polish and demo flow.
- Prepare APK, demo video, GitHub README, and pitch deck.

## Deferred until core proof flow is stable

- Token launch/minting logic.
- NFT/SFT reward minting.
- Custom on-chain program integration.
- Cloud Functions or paid backend services.
- Push notifications beyond optional local reminder.
- Production mainnet write transactions.
- Real reward distribution requiring server authority.

## Execution rule

Do not start broad features before the primary demo flow works. A small complete flow is better than many half-working screens. Follow `PHASED_DELIVERY_PLAN.md` for phase order and acceptance checks.
