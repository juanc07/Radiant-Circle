# RadiantSolanaHackatonAndroid — Phased Delivery Plan

## Product direction

Build **Radiant Rush**: a native Android quest and streak app for Solana Mobile users.

The app lets users connect a Solana wallet through Mobile Wallet Adapter, complete daily Solana actions, prove activity on-chain, build streaks, unlock badges, appear on leaderboards, and receive SKR-powered status or XP boosts.

## Hackathon target

End-of-month deliverables:

- Working Android APK.
- Demo video, maximum 3 minutes.
- GitHub repo that clones and runs.
- Pitch deck or short presentation.

Core constraints:

- Android only.
- Native mobile app, not a website in a wrapper.
- Uses Solana Mobile Stack and Mobile Wallet Adapter.
- Meaningfully interacts with Solana.
- Mobile-first UX.
- Firebase used as the lightweight backend.
- No private keys, seed phrases, mint authority, or reward authority inside the APK.

## Scoring strategy

The app should be built to score well across four equal categories:

1. **Stickiness and PMF** — daily quests, streaks, progression, leaderboards.
2. **User Experience** — fast native Android flow, one-hand mobile UI, clear wallet states.
3. **Innovation / X-Factor** — mobile-native on-chain activity reputation for Solana Mobile.
4. **Presentation and Demo** — simple wallet connection, quest proof, SKR boost, badge, leaderboard.

SKR prize strategy:

- SKR must affect the experience, not just appear as text.
- SKR holders should receive visible status, XP multiplier, badge, and leaderboard treatment.
- SKR checks must be honest: real on-chain balance read or clearly labeled demo data.

---

# Phase 0 — Repository and operating-system foundation

## Goal

Prepare the project so future coding is controlled, auditable, and easy to continue.

## Build

- Create or verify Android Studio project.
- Use Kotlin and Jetpack Compose.
- Connect Firebase project configuration.
- Add BMA-derived operating-system docs:
  - `README.md`
  - `AGENTS.md`
  - `ARCHITECTURE.md`
  - `SOLANA_SECURITY_AND_DATA_RULES.md`
  - `MOBILE_UI_UX_STANDARDS.md`
  - `TESTING_AND_RELEASE.md`
  - `BACKLOG_AND_ROADMAP.md`
  - `PHASED_DELIVERY_PLAN.md`
  - `CHANGELOG.md`
- Add `.gitignore` for Android, Gradle, local secrets, and build outputs.
- Add basic build instructions.

## Firebase setup

- Create Firebase project.
- Register Android app package.
- Add `google-services.json` only when appropriate for the repo policy.
- Enable Firebase Authentication.
- Enable Cloud Firestore.
- Do not add Cloud Functions yet.

## Acceptance checks

- Fresh clone can open in Android Studio.
- Debug APK builds.
- App launches to a blank or starter native screen.
- No WebView shell.
- No hardcoded private keys or wallet secrets.
- Documentation is committed with the source.

## Demo value

Low, but it proves the project is real and buildable.

---

# Phase 1 — Native mobile-first shell

## Goal

Make the app feel good on a phone before adding blockchain complexity.

## Screens

1. **Welcome**
   - App name and one-line value proposition.
   - Primary `Connect Wallet` button.
   - Secondary `Preview Demo` option if demo mode is enabled.

2. **Home / Today**
   - Current streak.
   - Today’s quests.
   - XP and level.
   - SKR tier card placeholder.

3. **Quests**
   - Daily quest list.
   - Quest state: locked, ready, in progress, completed.

4. **Badges**
   - Earned badges.
   - Locked badges.

5. **Leaderboard**
   - Top XP users.
   - Top streak users.
   - Current user rank card.

6. **Profile**
   - Wallet status.
   - SKR tier.
   - App settings.
   - Network indicator.

## UX rules

- Portrait-first layout.
- Bottom navigation.
- Large tap targets.
- Safe-area aware top and bottom spacing.
- No desktop-style tables.
- No text bleeding on small screens.
- Clear loading, empty, error, and success states.
- Fast perceived performance.

## Acceptance checks

- Every screen is reachable.
- App works on small and medium Android screens.
- Text does not clip or bleed.
- Buttons remain tappable with large font settings.
- Back navigation is predictable.
- App does not depend on Solana or Firebase to render the base shell.

## Demo value

Medium. Judges can already see product clarity and mobile-first intent.

---

# Phase 2 — Firebase foundation

## Goal

Persist users, quest progress, badges, streaks, and leaderboard state using Firebase.

## Firebase services

Use first:

- Firebase Authentication with anonymous sign-in.
- Cloud Firestore.

Avoid until needed:

- Cloud Functions.
- Paid extensions.
- Secret server keys.
- Complex backend jobs.

## Firestore collections

```text
users/{uid}
  walletAddress: string?
  displayName: string?
  skrTier: string
  xp: number
  level: number
  currentStreak: number
  longestStreak: number
  lastQuestDate: string?
  createdAt: timestamp
  updatedAt: timestamp

users/{uid}/completedQuests/{questCompletionId}
  questId: string
  date: string
  walletAddress: string?
  txSignature: string?
  proofType: string
  xpEarned: number
  completedAt: timestamp

quests/{questId}
  title: string
  description: string
  xpReward: number
  type: string
  active: boolean
  requiresWallet: boolean
  requiresTransaction: boolean
  requiresSkr: boolean
  sortOrder: number

badges/{badgeId}
  title: string
  description: string
  requirement: string
  iconName: string
  active: boolean

leaderboard/{uid}
  displayName: string
  walletAddressShort: string?
  xp: number
  level: number
  currentStreak: number
  longestStreak: number
  skrTier: string
  updatedAt: timestamp
```

## App behavior

- Anonymous Firebase user is created on first launch.
- Wallet address can be linked later after MWA connection.
- Local UI should work if Firestore is slow.
- Firestore writes should be bounded and safe.
- Failed writes should show retry states.

## Firestore security direction

- Users can read their own private profile.
- Users cannot write another user’s private data.
- Quest definitions are read-only from the client.
- Leaderboard writes must be conservative until server verification exists.
- If client writes leaderboard directly during MVP, document it as a hackathon limitation.

## Acceptance checks

- User profile survives app restart.
- Quest completion survives app restart.
- XP updates after quest completion.
- Leaderboard loads.
- Offline and slow-network states do not crash the app.
- Firestore rules are documented and reviewed.

## Demo value

Medium-high. The app starts to feel alive and persistent.

---

# Phase 3 — Mobile Wallet Adapter connection

## Goal

Make the app a real Solana Mobile Android app by connecting to a wallet through Mobile Wallet Adapter.

## Build

- Add Mobile Wallet Adapter dependency.
- Add wallet connection use case.
- Add wallet session state.
- Add wallet reconnect and disconnect.
- Save public wallet address to Firebase profile.
- Show shortened address in Home/Profile.

## User flow

```text
Open app
→ Tap Connect Wallet
→ Wallet app opens
→ User approves connection
→ App receives public key
→ App saves wallet address
→ Quest features unlock
```

## Required states

- Wallet not connected.
- Wallet connecting.
- Wallet connected.
- Wallet unavailable.
- User cancelled.
- User rejected.
- Session expired.
- Disconnect complete.

## Acceptance checks

- Works on a physical Android phone.
- Handles no wallet installed.
- Handles user rejection.
- Handles wallet cancel.
- Handles reconnect after app restart.
- Does not ask for seed phrase.
- Does not store private keys.

## Demo value

High. This proves Solana Mobile integration is real.

---

# Phase 4 — First meaningful Solana quest

## Goal

Complete one real Solana action and use it as quest proof.

## Quest 1: Daily signed proof

The user signs a human-readable message:

```text
Radiant Rush Daily Check-In
Wallet: <wallet-address>
Date: <yyyy-mm-dd>
Quest: daily_check_in
Network: <network>
```

This proves the wallet owner intentionally checked in.

## Quest 2: On-chain memo proof

The user sends a small memo transaction containing a quest proof string:

```text
radiant_rush:daily_check_in:<yyyy-mm-dd>:<wallet-address-short>
```

## Transaction stages

The UI must show accurate stages:

1. Ready.
2. Building transaction.
3. Awaiting wallet approval.
4. Signed.
5. Submitted.
6. Pending confirmation.
7. Confirmed.
8. Failed or cancelled.

## Firebase save

After successful proof:

```text
users/{uid}/completedQuests/{daily_check_in_yyyy_mm_dd}
  questId: daily_check_in
  date: yyyy-mm-dd
  walletAddress: <wallet>
  txSignature: <signature>
  proofType: memo_transaction
  xpEarned: <xp>
  completedAt: <timestamp>
```

## Acceptance checks

- Wallet signature or transaction is required before completion.
- Failed, rejected, or cancelled signing does not complete the quest.
- Duplicate same-day completion is blocked.
- Transaction signature is displayed.
- Quest state survives app restart.
- App distinguishes signed, submitted, pending, confirmed, and failed.

## Demo value

Very high. This is the first real on-chain wow moment.

---

# Phase 5 — SKR integration

## Goal

Make SKR central enough to compete for the SKR prize.

## Build

- Fetch wallet token accounts or balance data needed to detect SKR.
- Store SKR tier in user profile.
- Apply SKR XP multiplier.
- Unlock SKR badge.
- Show SKR tier on Home and Profile.
- Add SKR-specific quests.

## Tier model

```text
Visitor
  Requirement: no SKR detected
  XP multiplier: 1.00x
  Perk: base quests

Radiant
  Requirement: holds SKR
  XP multiplier: 1.25x
  Perk: SKR holder badge

Core Radiant
  Requirement: higher SKR balance threshold
  XP multiplier: 1.50x
  Perk: profile glow

Legend
  Requirement: highest SKR balance threshold
  XP multiplier: 2.00x
  Perk: leaderboard frame
```

Final thresholds should be stored in Firestore config so they can change without an APK rebuild.

## SKR quests

- `prove_skr_holder`
- `radiant_daily_check_in`
- `skr_streak_day`
- `mobile_ecosystem_supporter`

## Important rule

If SKR detection is not finished before the deadline, the app may include a clearly labeled demo-mode SKR tier. The final presentation must not claim demo data is real on-chain state.

## Acceptance checks

- Wallet without SKR still has a complete app experience.
- Wallet with SKR gets visible status and multiplier.
- XP calculation clearly shows multiplier.
- SKR badge is not awarded from fake production data.
- Errors in SKR lookup do not block core quests.

## Demo value

Very high. This directly targets the SKR prize.

---

# Phase 6 — Seeker and mobile-native polish

## Goal

Make the app feel built for Solana Mobile and real Android devices.

## Build

- Add strong haptic feedback for quest completion.
- Add subtle animation for streak increase.
- Add badge unlock animation.
- Add transaction proof card.
- Add explorer/open proof action.
- Add shareable quest completion card.
- Add Seeker-specific profile badge if reliable detection is available.
- Add `.skr` display name only if there is a reliable resolution path.

## Mobile-first details

- Home screen should be usable with one thumb.
- Primary action should be above the bottom navigation but still comfortable.
- Wallet approval state should not hide behind navigation.
- Loading should feel intentional, not broken.
- Errors should tell the user what to do next.

## Acceptance checks

- Works on non-Seeker Android devices.
- Works on Seeker device if available.
- Haptics do not fire repeatedly or annoyingly.
- Animations do not block core actions.
- App remains smooth on weaker Android phones.

## Demo value

High. This improves UX and presentation score.

---

# Phase 7 — Stickiness loop

## Goal

Make users want to return daily.

## Build

- Daily quest reset.
- Streak logic.
- Longest streak.
- Level progression.
- Badge unlocks.
- Quest history.
- Tomorrow preview.
- Optional local notification reminder.

## Daily quest examples

```text
Daily Check-in
  Action: sign daily message
  Reward: 50 XP

On-chain Proof
  Action: send memo transaction
  Reward: 100 XP

Radiant Holder
  Action: verify SKR balance
  Reward: 75 XP

Explorer
  Action: open/check proof transaction
  Reward: 25 XP

Streak Keeper
  Action: complete any quest today
  Reward: streak maintained
```

## Streak rules

- Completing at least one eligible daily quest maintains the streak.
- A user cannot gain multiple streak days on the same date.
- Missed day resets the current streak unless a future streak-freeze feature is added.
- Date logic must be explicit and stable.

## Acceptance checks

- Same-day quest farming is blocked.
- Streak updates once per eligible day.
- App clearly shows completed state.
- User knows what to do next after finishing today’s quests.
- Local time/date behavior is documented.

## Demo value

High. This supports stickiness and PMF.

---

# Phase 8 — Leaderboard and social proof

## Goal

Make progression public, competitive, and easy to understand.

## Build

- Global XP leaderboard.
- Global streak leaderboard.
- SKR Radiants leaderboard.
- Current user rank card.
- Short wallet identity formatting.
- Share card for completed quest or rank.

## Leaderboard rules

- Do not display full wallet address by default.
- Do not expose private Firebase user data.
- Clearly label if leaderboard is client-trusted during MVP.
- Prefer server-verified leaderboard if Cloud Functions or an external worker is later approved.

## Acceptance checks

- Top leaderboard loads quickly.
- Current user rank is visible.
- Empty leaderboard state works.
- Permission rules prevent reading private user documents.
- Leaderboard does not crash if user has no wallet connected.

## Demo value

High. This gives judges an immediate sense of competition and community.

---

# Phase 9 — Final APK hardening

## Goal

Turn the MVP into a reliable submission build.

## Build

- Release build variant.
- App icon.
- Splash screen.
- Version code and version name.
- R8/ProGuard review.
- Firebase config review.
- Network indicator review.
- Error copy review.
- Remove or lock debug-only features.
- Add final README setup instructions.

## Test matrix

Test on:

- Small Android phone.
- Medium Android phone.
- Emulator.
- Seeker device if available.
- Slow network.
- Offline mode.
- No wallet installed.
- Wallet rejection.
- Wallet approval.
- App restart after completed quest.
- Fresh install.

## Acceptance checks

- Debug APK builds.
- Release APK builds.
- Fresh clone instructions work.
- App installs on a physical Android phone.
- Primary demo path works end to end.
- Known limitations are documented.
- No private keys or seed phrases exist in repo or app.

## Demo value

Very high. This converts the project from prototype to submission.

---

# Phase 10 — Demo video and pitch deck

## Goal

Show why the app deserves attention in under 3 minutes.

## Demo video structure

```text
0:00–0:20
Problem:
Solana Mobile users need daily reasons to open, use, and prove ecosystem activity.

0:20–0:45
Solution:
Radiant Rush turns Solana Mobile usage into quests, streaks, badges, and SKR-powered status.

0:45–1:30
Live demo:
Connect wallet with Mobile Wallet Adapter.
Complete daily quest.
Sign/send transaction.
Show transaction proof.
XP and streak increase.

1:30–2:10
SKR integration:
Show SKR tier, XP boost, badge, and leaderboard frame.

2:10–2:40
Mobile-native UX:
Show one-hand flow, haptics, clean UI, and Android-native screens.

2:40–3:00
Why it wins:
Sticky, native, Solana-connected, SKR-centered, and easy to grow.
```

## Pitch deck structure

1. Title — Radiant Rush.
2. Problem — Solana Mobile needs daily engagement loops.
3. Solution — on-chain quests, streaks, badges, SKR boosts.
4. Product flow — connect, complete, prove, progress.
5. Solana Mobile integration — MWA, wallet, transaction proof, RPC reads.
6. SKR integration — tier, multiplier, badge, leaderboard status.
7. UX — native Android, mobile-first, one-hand flow.
8. Technical architecture — Android + Firebase + Solana RPC + MWA.
9. Roadmap — partner quests, verified rewards, push reminders, dApp discovery.
10. Closing — daily engagement layer for Solana Mobile.

## Acceptance checks

- Video is under 3 minutes.
- Demo path is rehearsed.
- Backup recording/screenshots exist.
- Deck matches the actual app.
- Claims match what was actually built.
- Known limitations are honest.

## Demo value

Maximum. This directly affects the presentation score.

---

# Recommended build order

Build in this order:

1. Native UI shell.
2. Firebase user/profile/quests.
3. Mobile Wallet Adapter wallet connect.
4. Signed daily proof.
5. Memo transaction proof.
6. Save quest proof to Firebase.
7. SKR balance/tier detection.
8. Streaks and badges.
9. Leaderboard.
10. Polish, APK, demo video, pitch deck.

This order protects the project from the biggest risk: spending too much time on polish before the wallet and transaction proof work.

---

# Final MVP definition

The final hackathon MVP is complete when a judge can:

1. Install the APK on Android.
2. Open a native mobile-first app.
3. Connect a Solana wallet using Mobile Wallet Adapter.
4. Complete a daily quest.
5. Sign or submit a Solana proof transaction.
6. See the proof signature/status.
7. Gain XP and maintain a streak.
8. See SKR status affecting the experience.
9. View badges and leaderboard.
10. Understand the product in a 3-minute demo.

