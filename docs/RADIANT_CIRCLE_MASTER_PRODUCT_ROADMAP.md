# Radiant Circle — Master Product Vision & Phase Roadmap
**Date:** 2026-09-13
**Status:** Living product guide / source of truth for product direction
**Primary product:** Radiant Circle
**First embedded game:** Radiant Rush

---

## 1. Product Positioning

### One-line positioning
**Radiant Circle is the social layer for Solana Mobile — discover fellow Seeker users, build a Solana-native identity, make friends, chat, and play community games together.**

### What Radiant Circle is
Radiant Circle is a **mobile-first social community app for Solana Mobile / Seeker users**. The social product is the core. Games, daily rituals, leaderboards, and sponsored competitions are engagement layers inside the social network.

Radiant Circle should feel like a **Solana Mobile social hub**, not a standalone game and not a dating app.

### What Radiant Circle is not
- Not primarily a dating app.
- Not primarily a chat app.
- Not only a game launcher.
- Not a wallet replacement.
- Not a crypto casino or wagering product.
- Not a system where players pay an entry fee to win prizes.
- Not a product that trusts Android/client scores for payouts.

### Internal inspiration
The long-term product can borrow the idea of an all-in-one mobile social ecosystem from products such as WeChat, but Radiant Circle must have its own identity:
- Solana Mobile-native identity
- Seeker-to-Seeker discovery
- wallet-aware but privacy-conscious profiles
- games and community competition
- daily positive ritual
- friend graph and messaging

Do not pitch it as "WeChat for Solana" as the main description. That is useful internally, but the public pitch should be clearer and more differentiated:

> **Radiant Circle — the social layer for Solana Mobile.**

---

## 2. Product Pillars

### A. Identity
A profile that combines social identity and optional Solana Mobile identity.

Core elements:
- avatar
- display name
- short bio / motto
- selected interest sparks
- optional country
- Radiant badges
- Radiant Rush stats
- SKR Passport status
- friend count
- streaks / participation badges

Wallet addresses should never be treated as the user's entire identity.

### B. Daily
Reasons to open Radiant Circle every day.

Core elements:
- Daily Radiance / affirmation card
- streak
- daily chest/reward
- daily Radiant Rush attempts
- weekly Cup progress
- social notifications

### C. Social
Mobile-first ways to discover and connect with other Seeker users.

Core elements:
- Shake to Radiate
- mystery discovery card
- interest overlap / Shared Sparks
- friend requests
- accepted friends
- profiles
- chat
- block/report/privacy controls

### D. Play & Compete
Games live inside Radiant Circle.

First game:
**Radiant Rush**

Radiant Rush includes:
- Casual and Ranked modes
- leaderboards
- daily attempts
- Weekly Radiant Cup
- trusted competition receipts
- future sponsor-funded SKR prizes
- badges / profile achievements

Future games can be added later without changing Radiant Circle's core identity.

---

## 3. Clear Judge / Public Story

The product story should remain simple:

> **Radiant Circle helps Solana Mobile users discover each other, build a social identity, form a Circle, chat, and return daily for community games and experiences. Radiant Rush is the first game inside the Circle.**

This prevents judges from asking:
- "Is this a game?"
- "Is this a dating app?"
- "Is this just chat?"
- "Is this only a wallet utility?"

The answer is:

> **It is a social app. Games, chat, wallet identity, and daily rituals are features inside the social network.**

Radiant Rush is important, but it is not the entire product.

---

## 4. Navigation Direction

Recommended mobile-first primary navigation:

1. **Home**
   - Daily Radiance
   - Daily Chest
   - streak
   - Cup status
   - friend activity
   - quick launch into Radiant Rush

2. **Circle**
   - Shake to Radiate
   - friend requests
   - friends
   - discovery history / recent encounters

3. **Games**
   - Radiant Rush
   - future games
   - leaderboards
   - Weekly Cup

4. **Inbox**
   - chats
   - unread badges
   - friend request messages

5. **Profile**
   - avatar
   - SKR Passport
   - badges
   - game stats
   - interest sparks
   - privacy controls

If five tabs feel too crowded on smaller phones, Inbox can live inside Circle and the primary nav can remain four tabs:
**Home / Circle / Games / Profile**.

---

## 5. Seeker Profile Design

### Goal
Give another user a real reason to send a friend request while preserving some mystery.

### Recommended public profile fields
Keep profile data light and conversation-oriented.

**Good fields**
- Display name
- Avatar
- Motto / short life line
- Country — optional, broad only
- Favorite food
- Music / favorite genres
- Games
- Hobbies
- Books / reading interests
- Pets / favorite animals
- "Currently into..."
- "I can talk for hours about..."
- "My weekend vibe..."
- "Looking to connect about..."
- Radiant Rush stats / favorite game
- selected badges

### Country
Country is useful because it gives users social context and can spark conversation.

Rules:
- optional
- country only
- never exact address
- never automatic precise location disclosure
- do not show distance by default

Example:
`🇵🇭 Philippines`

### Gender
**Do not make gender a primary discovery field for the hackathon version.**

Reason:
- it pushes the product toward dating
- it can dominate matching behavior
- it creates moderation/safety complexity
- it is not needed for friend discovery

If added later:
- optional
- hidden by default
- user-controlled visibility
- do not use it as a mandatory matching filter

### Age
Do not display exact age in discovery.

For a future production social/chat launch, age eligibility/safety should be handled separately from the public profile. Public mystery should never depend on hiding safety-critical eligibility information.

### Motto
Yes. A motto is a very good profile field because it creates personality without turning the app into dating.

Examples:
- "Build something worth remembering."
- "Good energy finds good energy."
- "Always curious."

---

## 6. Shared Sparks — Why Would I Add This Person?

The strongest friend-discovery mechanic should be **Shared Sparks**, not romance matching.

Each user selects several interest tags.

Examples:
- Anime
- Roblox
- RPGs
- Cats
- Dogs
- Cooking
- Basketball
- K-pop
- Rock
- EDM
- Sci-fi
- Books
- Coding
- AI
- Solana
- Photography
- Travel
- Coffee
- Fitness

When Shake discovers another person, Radiant Circle can show:

> **3 Shared Sparks**
>
> 🎮 RPGs
> 🎵 EDM
> 🐶 Dogs

This creates a concrete reason to send a friend request.

Use language such as:
- Shared Sparks
- Circle Compatibility
- Common Vibes
- Things You Both Like

Avoid:
- Love Match
- Dating Match
- Romantic Compatibility

The app may naturally lead to friendships or relationships, but the product itself should remain **friend-first**.

---

## 7. Mystery Discovery Experience

### Signature feature
**Shake to Radiate**

Both users explicitly enter Shake mode.

Flow:

1. User opens Circle.
2. Taps **Shake to Radiate**.
3. App activates a short discovery window.
4. User physically shakes the Seeker.
5. Android accelerometer confirms a shake gesture.
6. Backend places user in an ephemeral matching pool.
7. Another opted-in user who shook in roughly the same period is selected.
8. A mystery profile card is shown.
9. User decides:
   - Add to Circle
   - Maybe Later / Skip
10. The other person receives the friend request.
11. Chat becomes available only after acceptance.

### Discovery card — before friendship
Reveal enough to spark curiosity, but not the entire profile.

Recommended visible information:
- avatar or stylized avatar
- display name or chosen alias
- optional country
- motto
- 3 Shared Sparks
- 1–3 public badges
- SKR Holder / Seeker indicator if appropriate
- Radiant Rush badge/stat summary
- streak / Circle activity indicator

Do not expose:
- exact location
- phone number
- email
- private wallet information
- sensitive profile fields

### After friend acceptance
Unlock:
- full public profile
- chat
- more selected interests
- game invites later
- shared activity later

---

## 8. Friendship Model

Friendship is mutual.

States:
- NONE
- REQUEST_SENT
- REQUEST_RECEIVED
- FRIENDS
- BLOCKED

Flow:
`Shake discovery → View mystery card → Add to Circle → request → accepted → Friends → Chat`

Required controls:
- decline request
- cancel outgoing request
- remove friend
- block
- report

A blocked user should not be rematched through Shake.

---

## 9. Chat Direction

Chat is a consequence of friendship, not open anonymous messaging.

Phase-one chat:
- text messages
- timestamps
- unread count
- delivery/read state only if simple
- report
- block
- remove friend

Later:
- game invites
- stickers / reactions
- share Daily Radiance
- share achievements
- invite to Radiant Rush

Do not prioritize:
- voice/video
- public group chats
- anonymous DMs
- file uploads

Those add too much moderation and implementation risk for the hackathon.

---

## 10. Daily Radiance / Affirmation

### Purpose
Create a lightweight emotional reason to return every day even when the user does not want to play.

Home card:
> **TODAY'S RADIANCE**
> Tap to reveal your message ✨

Reveal:
> "Your next opportunity may begin with one small action today."

Possible metadata:
- opened today
- consecutive-day streak
- category: courage / focus / kindness / luck / creativity

Future social actions:
- share with a friend
- compare today's Radiance
- send encouragement

This is not a financial reward and should remain lightweight.

---

## 11. Games Inside Radiant Circle

### Game hub model
Radiant Circle owns the identity, friend graph, daily experience, and wallet/SKR context.

Games plug into that social shell.

### Game #1 — Radiant Rush
Radiant Rush is a short skill game designed for mobile.

Key systems:
- Casual
- Ranked
- Daily attempts
- Weekly / All-Time leaderboards
- rewards
- Weekly Radiant Cup
- trusted run receipts
- future trusted SKR sponsorship

Profile integration:
- best score
- Cup placement
- badges
- number of Ranked runs
- wins / achievements later

### Future game model
Do not promise many games in the hackathon submission.

Say:
> "Radiant Rush is the first game inside Radiant Circle, and the architecture is designed so more community games can be added later."

---

## 12. Trusted Competition Architecture

### Phase 12A — COMPLETE
**Trusted Competition Verification Foundation**

Implemented:
- immutable per-run `competitionRunSubmissions/{receiptId}`
- stable receipt identity
- `UNVERIFIED / VERIFIED / REJECTED`
- Android cannot promote a receipt to VERIFIED
- Android cannot enable payout eligibility
- trusted verification boundary
- client aggregate leaderboards remain prototype/community presentation
- Firestore rules deployed
- real Ranked receipt confirmed on Seeker
- cleanup tooling created
- stale development Auth/Firestore records cleaned
- Phase 12A tagged

Trust state created by Android:
- `verificationStatus = UNVERIFIED`
- `trustedPlacementEligible = false`
- `payoutEligible = false`
- `payoutStatus = NOT_ELIGIBLE`

### Why `competitionRunSubmissions` stores multiple documents
This collection is intentionally **one immutable document per Ranked run**, not one document per player.

Every distinct Ranked completion gets a new `receiptId`.

Purpose:
- audit trail
- anti-tampering foundation
- future trusted verification
- season close reconstruction
- dispute/debug evidence
- prevents later client overwriting of the original submission

Multiple documents for one user are expected.

Aggregate collections such as `runWeekly` and `runAllTime` answer:
> "What is this user's current/best score?"

`competitionRunSubmissions` answers:
> "What exactly did the client submit for each individual Ranked run?"

Do not collapse this into one document per UID.

---

## 13. Current Phase Roadmap

### Phase 12B — COMPLETE
## Trusted Sponsor / Cup Configuration

Goal:
Create a trusted, admin-controlled Weekly Radiant Cup configuration.

Android:
- read-only Cup config
- present sponsor
- present prize asset/amount
- present allocation
- present start/end
- present funding state honestly

Trusted config concepts:
- weekKey
- status
- sponsorName
- sponsorNote
- prizeAssetSymbol
- prizeMint
- prizeDecimals
- prizeAmountAtomic
- placementAllocationsBps
- startsAt
- endsAt
- fundingWalletAddress
- fundingVerificationStatus
- trustedResultsRequired
- payoutEnabled
- configurationAuthority

Android must not be able to:
- set sponsor
- set prize amount
- claim funding verification
- close season
- set winners
- enable payout

### Phase 12C — COMPLETE
## SKR Funding Verification

Goal:
Verify that the configured sponsor/funding wallet actually holds the expected SKR amount or that the prize pool is otherwise verifiably funded.

Requirements:
- trusted/server/admin-side verification
- official SKR mint
- no fake balance
- record verification timestamp
- record observed on-chain amount
- record verification status
- record an immutable admin-only funding-check receipt
- count only transferable liquid SKR; staked/frozen SKR is not prize funding
- Android only displays `VERIFIED` when the complete trusted evidence shape is valid
- RPC failures write nothing and never fabricate verification

Possible states:
- NOT_CONFIGURED
- NOT_VERIFIED
- VERIFIED
- REJECTED

### Phase 12D — IMPLEMENTED, LIVE PROOF PENDING
## Trusted Season Close + Winners

Goal:
Close the Weekly Cup using trusted logic.

Implemented contract:
- Phase 12C audit confirmed no existing trusted run-promotion mechanism, so raw client `UNVERIFIED` receipts are never treated as trusted winners
- explicit Admin-only manual independent-evidence attestation prerequisite for each receipt
- immutable `competitionRunVerifications/{receiptId}` decision required in addition to promoted receipt fields
- Cup closes only from `OPEN` and only after configured `endsAt`
- only exact Phase 12D `VERIFIED` + `trustedPlacementEligible=true` receipts with matching audit evidence can rank
- exact full-wallet dedupe using the best eligible run
- deterministic existing tiebreak order: score, max combo, PERFECT hits, earlier completion, receipt id
- exact integer placement allocation
- immutable/frozen eligible snapshot and winner records under `weeklyCupResults/{weekKey}`
- duplicate finalization refused; Phase 12B cannot reopen/mutate a finalized Cup
- transactional input re-read + snapshot digest comparison before write
- Android reads validated winner records only and cannot close/select/write winners
- Phase 12C funding state is snapshotted independently; `NOT_VERIFIED` may freeze competition results but never becomes payout-ready
- `payoutEnabled=false`, `payoutReady=false`, no SKR transfer

Live Firestore/admin/device proof is still required before commit/tag.

### STOP AND ASSESS — decision recorded 2026-09-14
After the Phase 12D checkpoint, the project deliberately chose to finish the trusted payout lifecycle foundation before starting Daily Radiance. Real transfer still remains a separate safety gate.

Current order:
- complete the live W38 Phase 12D winner-close proof
- Phase 12E trusted payout lifecycle
- reassess Phase 12F real sponsor-funded SKR transfer
- then resume Phase 13 social features

---

## 14. Social Feature Roadmap

### Phase 13A
## Daily Radiance

Build:
- daily affirmation / inspiration card
- deterministic or trusted daily message
- reveal animation
- daily open state
- streak
- mobile-first presentation

Goal:
Improve daily-return behavior with low implementation risk.

### Phase 13B
## Shake to Radiate — Seeker Discovery

Build:
- accelerometer shake detection
- explicit opt-in discovery mode
- ephemeral matching window
- no precise location required
- pair users who are actively shaking
- avoid rematching blocked users
- discovery timeout
- mystery profile card
- Shared Sparks

Goal:
Create the signature mobile-native social feature.

### Phase 13C
## Social Profiles + Seeker Friends

Build:
- editable social profile
- interests / Shared Sparks
- optional country
- motto
- hobby/music/game/food/book/pet prompts
- friend requests
- accept/decline
- mutual friend graph
- remove/block/report
- profile privacy controls

Goal:
Turn a one-time Shake encounter into a persistent Circle relationship.

### Phase 13D
## Mutual Friend Chat

Build:
- one-to-one text chat
- only between accepted friends
- timestamps
- unread badge
- block/report
- conversation deletion/hiding rules
- mobile-first responsive UI

Goal:
Give friendships a reason to persist.

### Phase 13E — Optional if time permits
## Social Notifications / Presence / Game Invites

Possible:
- friend request notification
- accepted request
- new message
- "friend is playing Radiant Rush"
- challenge/invite to game

Do only if stable.

---

## 15. Optional Payout Roadmap

Payout work is intentionally after the trusted competition foundation and after reassessing judging value.

### Phase 12E — IMPLEMENTED, LIVE PROOF PENDING
## Trusted Payout Lifecycle

Implemented foundation:
- deterministic Admin-only payout manifest from immutable Phase 12D winners
- requires Cup `CLOSED`, trusted Phase 12D result, and Phase 12C funding `VERIFIED` at close
- exact winner wallet + exact atomic SKR amount preserved per placement
- manifest SHA-256 covers source ranking digest, funding wallet, winners, receipts, and amounts
- `READY_FOR_REVIEW -> APPROVED` lifecycle with explicit review reference
- apply-time digest confirmation and transactional re-read before approval
- immutable `prepared` / `approved` audit events
- duplicate preparation and duplicate approval refused
- Android denied access to internal payout lifecycle documents
- cleanup tooling protects payout collections
- `payoutEnabled=false`, `transferEnabled=false`, `transferStatus=NOT_STARTED` throughout
- no private key, transaction signing, or SKR transfer

Live proof waits for a successfully finalized and funded Cup. W38 must be funded/verified before close if it will be used for the Phase 12E proof.

### Phase 12F — Optional
## Real Sponsor-Funded SKR Transfer

Only if:
- Phase 12B–12D are stable
- enough hackathon time remains
- transfer can be implemented safely
- no private key is embedded in Android
- payout story materially improves demo/judging

Real transfer must be trusted/admin/server controlled.

---

## 16. Submission & Finalization Roadmap

After core product work:

### Final UX / Polish
- mobile text clipping audit
- small-screen audit
- empty/loading/error states
- animation polish
- accessibility
- first-run onboarding
- clear social/game hierarchy

### Fresh-Clone Build Audit
Judges must be able to understand and build the project.

Prepare:
- README
- JUDGE_BUILD.md
- required local configuration instructions
- no credentials in repository
- reproducible dependency setup
- clean branch/tag

### Seeker Acceptance Test
Test actual device flows:
- launch
- wallet
- SKR Passport
- Daily Radiance
- Shake
- friend request
- chat
- Radiant Rush
- leaderboard
- Weekly Cup

### Release APK
- final version
- clean build
- install on physical device
- smoke test
- archive APK/hash

### Three-Minute Demo
Recommended story:

1. Open Radiant Circle.
2. Show social identity / SKR Passport.
3. Reveal Daily Radiance.
4. Shake the Seeker.
5. Discover another Radiant.
6. Show Shared Sparks.
7. Send / accept friend request.
8. Show chat briefly.
9. Open Games.
10. Launch Radiant Rush.
11. Finish a Ranked run.
12. Show leaderboard / Weekly Cup.
13. Show trusted sponsored SKR Cup status.

The demo should make the product understandable even if the judge watches only once.

### Pitch Deck
Tell one clear story:
- Problem: Solana Mobile users have wallets and apps but limited persistent social identity/community connection.
- Solution: Radiant Circle.
- Signature interaction: Shake to Radiate.
- Daily retention: Daily Radiance + streaks.
- Social graph: Shared Sparks → Friends → Chat.
- Engagement: games inside the Circle.
- First game: Radiant Rush.
- Web3 utility: SKR Passport + trusted sponsored Cup.
- Security: client is never payout authority.
- Vision: a social layer for the Solana Mobile ecosystem.

### Submission Package
Must include:
- functional Android APK
- judge-accessible GitHub repository
- demo video
- pitch deck / presentation
- setup/build documentation

---

## 17. Contest Strategy

The product should optimize for four equal judging dimensions:

### Stickiness / PMF
Supported by:
- Daily Radiance
- streak
- Daily Chest
- Shake discovery
- friend graph
- chat
- games
- Weekly Cup

### User Experience
Supported by:
- native Android
- mobile-first Compose
- physical Shake interaction
- concise navigation
- polished profile/discovery flow

### Innovation / X-Factor
Supported by:
- physical Seeker Shake social discovery
- Solana-native social profile
- Shared Sparks
- games embedded inside social identity
- trusted sponsored SKR competition

### Presentation / Demo
Supported by:
- visually understandable Shake moment
- one clear social story
- short Radiant Rush gameplay
- visible Weekly Cup
- security story that can be explained in one sentence

---

## 18. Client-Facing Copy Rule

Radiant Circle must never explain its internal security architecture to ordinary users unless that information is necessary for a user decision.

Do **not** put implementation/debug language in normal app UI, including phrases such as:
- `trusted configuration`
- `client scores are not payout authority`
- `Android payout remains disabled`
- schema/version/authority-marker explanations
- backend/admin/server trust-boundary explanations

Those details belong in:
- code comments
- logs
- admin tools
- engineering/security documentation
- judge/build documentation when technically relevant

Player-facing copy should communicate only the useful product state in natural language. Examples:
- `Weekly prize`
- `Presented by ThinkBloxPH`
- `Funding pending`
- `Funding verified`
- `Prize unavailable`
- `Cup open`
- `Cup closed`

A DRAFT Cup should not expose sponsor/prize presentation to normal users. Internal safety controls still remain enforced even when they are not narrated on screen.

---

## 19. Product Safety & Privacy Principles

These principles should guide all future phases:

- precise location is not required for Shake
- country is optional
- no exact location on discovery cards
- no open anonymous DMs
- chat requires mutual friendship
- block/report from first chat release
- do not expose sensitive wallet/profile data by default
- wallet ownership and social identity are separate concepts
- never expose private keys or seed phrases
- Android is not trusted payout authority
- do not fake blockchain state
- do not fake prize funding
- do not claim a payout happened without trusted evidence

---

## 20. Product North Star

Radiant Circle should feel like a place Seeker users want to open every day because something social, positive, or competitive may be waiting for them.

The intended loop:

`Open Radiant Circle`
→ `See today's Radiance`
→ `Check Circle activity`
→ `Shake / discover someone`
→ `Find Shared Sparks`
→ `Add to Circle`
→ `Chat`
→ `Play Radiant Rush`
→ `Check Weekly Cup`
→ `Earn identity/badges`
→ `Return tomorrow`

The goal is not to make every feature equally important.

The hierarchy is:

**Social app first.**
**Games second.**
**Solana/SKR makes the identity and competition more meaningful.**
**Daily rituals make the Circle habitual.**

---

## 21. Current Immediate Priority

At the time of this document:

1. Phase 12A — complete.
2. Phase 12B — complete: trusted Sponsor/Cup configuration.
3. Phase 12C — current: trusted SKR funding verification.
4. Phase 12D — trusted season close + winners.
5. Stop and assess.
6. Phase 13A — Daily Radiance.
7. Phase 13B — Shake to Radiate.
8. Phase 13C — Profiles + Friends.
9. Phase 13D — Mutual Chat.
10. Reassess 12E/12F payout value versus final submission polish.
11. Final UX / build / Seeker / APK / demo / deck / submission.

This document should remain the product guide until Radiant Circle is complete.
