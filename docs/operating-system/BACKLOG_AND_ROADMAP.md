# RadiantSolanaHackatonAndroid — Backlog and Roadmap

## Current phase

Radiant Rush is moving through Phase 3: Mobile Wallet Adapter wallet connection. The app now has a native Compose shell, Firebase profile/progress persistence, and a wallet boundary for real MWA authorization. The immediate owner task is to test Connect Wallet on a real Android device with an MWA-compatible Solana wallet, confirm the public wallet address saves to Firestore, and verify XP/streak still persist after restart.

## Owner-ordered near-term work

1. Create or audit the Android project skeleton. — done for Phase 1
2. Add native mobile-first Compose shell for Radiant Rush. — done for Phase 1
3. Add Firebase Auth and Firestore profile/quest persistence. — done for Phase 2
4. Add safe environment configuration: dev/demo/production separation. — partial, continue after wallet flow
5. Add Mobile Wallet Adapter connection flow. — Phase 3 patch delivered, needs real-device wallet testing
6. Add signed daily proof quest. — next phase
7. Add memo transaction proof quest.
8. Add SKR balance/tier detection and XP multiplier.
9. Add streaks, badges, and leaderboard.
10. Polish mobile UI for small phones and Seeker device demo.
11. Prepare APK, demo video, GitHub repo, and pitch deck.

## Foundation backlog

### A. Architecture

- Confirm package/module structure.
- Add feature ownership map.
- Add dependency injection if not present.
- Define result/error types for wallet/RPC flows.
- Prevent UI from calling RPC directly unless documented as a prototype exception.

### B. Solana/security

- Add network selector or locked environment config.
- Add wallet connection/session state.
- Add transaction status model: signed/submitted/pending/confirmed/failed.
- Add RPC timeout/retry boundaries.
- Confirm no secrets are stored or logged.
- Add production guard for mock/demo mode.

### C. Mobile UI

- Build small-phone-first home flow.
- Add clear network/demo indicator.
- Add no-wallet, rejected, offline, and stale-data screens.
- Add transaction review screen before signing.
- Test large font and keyboard insets.

### D. Testing/release

- Add Gradle build/test/lint commands to project docs.
- Add unit tests for use cases and state transitions.
- Add emulator smoke test checklist.
- Add release ZIP/export process.
- Add changelog discipline.

### E. Demo/pitch

- Define one wow moment.
- Keep the live demo flow under 2 minutes.
- Prepare fallback path if RPC/wallet/network fails during judging.
- Prepare screenshots/video backup if allowed by hackathon rules.

## Deferred until MVP flow works

- Token launch/minting logic.
- NFT/SFT flows.
- Marketplace/trading flows.
- Custom on-chain program integration.
- Cloud Functions or paid backend services.
- Push notifications beyond optional local reminder.
- Advanced social/referral features.
- Analytics beyond basic event tracking.
- Production mainnet launch.
- Real reward distribution requiring server authority.

## Execution rule

Do not start broad features before the primary demo flow works. A small complete flow is better than many half-working screens. Follow `PHASED_DELIVERY_PLAN.md` for phase order and acceptance checks.
