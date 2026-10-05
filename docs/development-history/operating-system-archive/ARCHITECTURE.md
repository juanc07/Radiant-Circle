## Phase 12C — trusted SKR funding-verification boundary

`verify-weekly-cup-funding.mjs` is a developer/admin-side trust boundary. It reads `weeklyCupConfigs/{weekKey}`, queries Solana Mainnet for liquid accounts of the official SKR mint, sums exact raw token amounts, excludes frozen accounts, and writes a funding snapshot only through Firebase Admin after an explicit reviewed `--apply`. Android never performs the trusted promotion.

Applied checks also create immutable admin-only audit receipts at `weeklyCupFundingChecks/{weekKey}/checks/{checkId}`. Client Firestore rules deny direct access to that audit collection. `weeklyCupConfigs/*` remains public-readable/client-write-denied so the app can present only the resulting product state.

Android accepts `VERIFIED` only when the complete Phase 12C evidence shape matches the configured prize: mainnet-beta, official SKR mint, finalized commitment, Phase 12C authority/version, exact required amount, sufficient observed amount, positive RPC slot, and trusted timestamps. A bare `VERIFIED` string fails closed to pending. `payoutEnabled` remains false.

Phase 12C is a balance snapshot, not escrow. It moves no SKR and does not guarantee the funding wallet cannot later change. Real transfer/escrow remains a later trusted phase.

## Phase 12A — trusted competition-verification boundary

`RadiantRunResult` now carries a stable client UUID receipt identity. `FirebaseRadiantRepository` keeps the existing Phase 11 gameplay/progression transaction unchanged as the prototype source for Weekly, All-Time, XP, rewards, and profile state. After a successful Ranked run, it separately attempts to create `competitionRunSubmissions/{receiptId}`. Receipt persistence is intentionally non-blocking so trusted-Cup infrastructure cannot make the game unusable.

`Phase12CompetitionVerificationRules` defines explicit `UNVERIFIED / VERIFIED / REJECTED` vocabulary and a fixed Android initial state of `UNVERIFIED`, not placement eligible, not payout eligible, and `NOT_ELIGIBLE`. Firestore rules enforce an exact client-create schema and deny client updates/deletes. A future trusted Firebase Admin/Cloud Functions/Cloud Run service may verify receipts and add trusted score/decision fields; ordinary Android writes never cross that boundary. Existing `runWeekly`, `runAllTime`, and `runWalletDaily` remain client-reported prototype data and must never be treated as payout proof.

## Phase 11F.4 — phone-first responsive presentation boundary

`rememberResponsiveUiSpec()` is the shared presentation breakpoint authority. Normal portrait phones are intentionally classified as compact so cards stack before dynamic identity, wallet, streak, tier, leaderboard, or reward text is forced into clipping or micro-font scaling. Required text is allowed to increase component height; horizontal density is reserved for wider layouts.

The public-profile avatar picker is presentation-only: it reads the bundled `PublicProfileRules` vocabulary and returns an avatar ID to the existing profile save path. It does not introduce image upload, location, wallet authority, or a new backend collection.

## Product naming boundary — Radiant Circle / Radiant Rush

`Radiant Circle` is the user-facing Android product. `Radiant Rush` is the skill game inside the product. The existing Android namespace/applicationId `com.thinkblox.radiantrush`, `RadiantRushApp`/`RadiantRunScreen` class names, Firestore field names, MWA identity URI, and memo protocol identifiers remain stable legacy technical identifiers so the rebrand does not break installed-app upgrades, Firebase registration, wallet continuity, or persisted data. New user-facing copy must use Radiant Circle for the app and Radiant Rush for the game.

## Phase 11F retention and public identity

`RetentionRules` is Android/Firebase-free and converts persisted daily/weekly activity counters plus current competition state into player-facing goals and milestones. Firebase remains the persistence layer; Home only renders derived `RetentionPreview` state. `PublicProfileRules` owns the bounded display-name and bundled animal-avatar vocabulary. Firebase anonymous UID remains private ownership/session state; wallet identity remains the public competition identity.

## Phase 11E — Daily chest presentation boundary

`RadiantChestPresentationRules` is Android/Firebase-free presentation logic for minimum reveal pacing and rarity-driven visual intensity. `RadiantChestCard` consumes the already-resolved `RadiantChestPreview`; it does not roll rewards or authorize persistence.

The Firestore claim begins immediately. UI may hold a very fast success until the 520 ms opening pose has registered, but the UI never fabricates a reward while waiting. `RewardLoopRules` and `FirebaseRadiantRepository` remain the reward/economy authority. Phase 11E changes no Ranked state, wallet/Solana authority, sponsor payout state, or Firestore security rules.

## Phase 11D — Weekly Radiant Cup boundary

`WeeklyRadiantCupRules` is Android/Firebase-free domain logic for UTC season timing, placement accolades, and sponsor metadata normalization. `LeaderboardScreen` presents the Cup using the existing wallet-deduped Ranked Radiant Rush board.

`FirebaseRadiantRepository` reads optional server/admin-authored sponsor metadata from `weeklyCupConfigs/{ISO_WEEK}` and reads the previous week's board for a display-only prior-season accolade. Android clients cannot write sponsor config. Sponsor metadata cannot enable payout: the domain model always returns `payoutEnabled = false`.

The Cup does not create a new score authority. Existing `runWeekly` rows remain client-reported prototype competition data. Phase 11D awards only cosmetic in-app placement labels/crests; no SOL/SKR transfer, treasury key, player entry fee, wagering, or economic claim path exists. A future Phase 12 Sponsored SKR Cup requires trusted score verification and server-side payout authority.

## Phase 11C.5 — presentation-only identity layer

`RadiantIdentityComponents.kt` owns scalable Compose-drawn badge and SKR Passport identity marks. It receives already-derived UI state only and has no Firebase, Mobile Wallet Adapter, Solana RPC, reward, ranking, or payout authority. `BadgeMedallion` delegates its visual mark to this component, while Profile uses `PassportCrestCard` as a responsive summary above the existing detailed Passport rows.

## Phase 11C.4.1 — transient Radiant Rush route

`showRadiantRun` is intentionally ordinary Compose state, not saveable navigation state. The run's timer, target, combo, score, and audio state are transient in-memory presentation state; restoring only the route after Activity recreation could reopen an inconsistent run. The stable shell may restore, while a run always starts from a fresh launcher state.

## Phase 11C — SKR Passport v2 perk boundary

`SkrBalanceRepository` remains a read-only Mainnet RPC boundary. It observes liquid SKR for the official mint and maps that observation through the Android/Firebase-free `SkrPassportRules` domain rules. It does not sign, transfer, stake, unstake, or spend tokens.

Passport holder playtime is represented by a separate `skrCasualRushTickets` ledger. Standard `rushTickets` are ranked-eligible; SKR bonus tickets are casual-only. `Phase11CompetitionRules.rankedAttemptDecision` therefore requires both a connected wallet and a standard entry ticket before classifying a run as Ranked. `FirebaseRadiantRepository.completeRadiantRun` always consumes a standard ticket for Ranked and prefers an SKR casual ticket for Casual. This prevents a holder perk from becoming an indirect fourth ranked attempt.

Daily Passport grant state is stored in the existing owner profile (`skrPerkTicketGrantDate`, `skrPerkTicketsGrantedToday`) so repeat scans are idempotent. The same-day completed SKR quest snapshot is used to derive the chest perk; the base chest reward seed does not include SKR tier, so wealth does not improve base rarity.

Frame/aura/badge fields are presentation-only off-chain status. `skrStakedVerified=false` and an explicit unavailable message are persisted because no staking authority is integrated. Phase 11C adds no new Firestore collection and does not change payout authority.

## Phase 11B.1 — wallet-scoped ranked-attempt authority

Phase 11B originally mirrored ranked-attempt usage only on `users/{uid}`. Because Firebase Anonymous Auth creates different UIDs on different installations, that accidentally made the 3-attempt allowance device/UID scoped rather than wallet scoped.

Phase 11B.1 adds a shared prototype fairness document at `runWalletDaily/{utcDay}/wallets/{walletAddress}`. `FirebaseRadiantRepository.completeRadiantRun` reads that document in the same Firestore transaction used to save a run and increments it only when the run is ranked. Firestore transaction retries serialize concurrent attempts from two phones using the same wallet. The existing per-user fields remain as a local/profile mirror and migration fallback.

This shared counter is not economic authority. Wallet ownership is still represented by client-established MWA public-address state plus Firebase rules; it is sufficient for prototype competition limits but not for real SKR payout authorization.

## Phase 10 — Native game layer

Radiant Circle now has a lightweight game layer above the existing Firebase/Solana proof stack. `RadiantRunScreen` owns transient 20-second gameplay state. `RadiantGameRules` owns pure ticket/capsule/collectible calculations. `FirebaseRadiantRepository.completeRadiantRun` performs the owner-profile transaction that consumes one free Rush Ticket and persists XP, best score, run count, collection counts, shards, and latest reward.

The game layer does not call Mobile Wallet Adapter and does not move SOL/SKR. The stable MWA/SKR repositories remain separate boundaries. Public leaderboard deduplication remains wallet-based while Firestore document ownership stays Firebase-UID-based.

The Phase 10 arcade visuals are produced with native Compose Canvas/Brush/Material primitives and haptics, not a WebView, game engine, or external graphic asset dependency.

# RadiantSolanaHackatonAndroid — Architecture

## Architecture goal

Build a small, reliable Android app that can survive hackathon speed without becoming unmaintainable. The app should be modular enough to protect wallet/security logic, but not over-engineered for features that do not exist yet.

## Preferred shape

Use a clean Android architecture with clear layers:

```text
app/
  ui/                 Screens, Compose components, navigation shell
  presentation/       ViewModels, UI state, events/effects
  domain/             Use cases, business rules, app models
  data/               Repositories, RPC clients, persistence, mappers
  wallet/             Wallet connection/session/signing adapters
  solana/             Public Solana constants, transaction builders/parsers
  core/               Shared result types, logging, time, dispatchers
```

The exact package names may differ if the source already has an established structure. Existing project structure wins unless it is broken.

## Ownership boundaries

### UI layer

Owns:

- Layout and interaction.
- Display-only formatting.
- Empty/loading/error/success states.
- User intent events.

Does not own:

- Transaction construction.
- Balance truth.
- Wallet identity truth.
- Security decisions.
- Persistence migrations.

### Presentation layer

Owns:

- Screen state.
- Input orchestration.
- Loading/error state transitions.
- Calling use cases.
- Preventing double-tap duplicate actions.

Does not own:

- RPC implementation details.
- Wallet internals.
- Raw database access.
- Secret handling.

### Domain layer

Owns:

- Business decisions.
- Validation rules.
- Use-case sequencing.
- Transaction intent models.
- Feature-specific invariants.

Does not own:

- Android UI APIs.
- Concrete RPC clients.
- Concrete database APIs.

### Data layer

Owns:

- Repository implementations.
- RPC/network calls.
- Cache policy.
- Local storage.
- DTO/model mapping.
- Retry/timeout boundaries.

Does not own:

- UI copy.
- Navigation.
- Wallet secret material.

### Wallet layer

Owns:

- Wallet connection/session state.
- User approval flow integration.
- Signing request orchestration.
- Handling wallet unavailable/rejected/cancelled states.

Does not own:

- Private keys or seed phrases.
- Silent signing.
- Fake approvals.
- Long-lived unbounded wallet sessions.

### Solana layer

Owns:

- Public chain constants.
- Transaction/message building helpers.
- Signature/result parsing.
- Network selection rules.

Does not own:

- User secrets.
- UI state.
- Demo claims.

## Data flow standard

```text
Screen -> ViewModel -> UseCase -> Repository -> RPC/Wallet/Storage
Screen <- ViewModel <- UseCase <- Repository <- RPC/Wallet/Storage
```

Rules:

- UI never talks directly to RPC or wallet SDKs unless this is a deliberate small-prototype exception documented in the changelog.
- Repositories return typed results, not raw exceptions thrown into UI.
- Use cases return states the UI can explain clearly.
- Wallet rejection/cancel/error are normal states, not crashes.

## App state categories

Use distinct state types. Do not blur them.

- `LocalUiState`: screen-only temporary state.
- `SessionState`: wallet connection, selected network, authenticated session.
- `CachedChainState`: last known balance/token/account data; must show age/staleness if user decisions depend on it.
- `ConfirmedChainState`: data read after confirmation/finality rules chosen by the app.
- `DemoState`: mock-only data; must be visibly labeled in demo/dev builds.

## Error handling

Every async Solana action must have these outcomes:

- Wallet unavailable.
- Wallet not connected.
- User rejected/cancelled.
- RPC timeout.
- RPC returned error.
- Transaction submitted but not yet confirmed.
- Transaction failed.
- Transaction confirmed.
- Unknown error with safe recovery.

Never trap the user in a spinner after wallet/RPC failure.

## Offline and slow-network behavior

- Show cached data only as cached.
- Disable send/submit actions when the app cannot safely build or submit a transaction.
- Allow read-only browsing when possible.
- Use bounded timeouts and visible retry actions.
- Avoid app startup being blocked by non-critical network calls.

## Configuration ownership

Environment configuration should be explicit:

```text
network = devnet | testnet | mainnet-beta | custom
rpcUrl = configured per build/environment
programIds = documented public constants
analyticsEnabled = build/environment controlled
mockMode = dev/demo only
```

Production builds must fail closed if required configuration is missing. Demo/dev builds may show a clear setup screen.

## Feature ownership template

For every feature, document:

```text
Feature:
User value:
UI owner:
ViewModel owner:
Use case owner:
Repository owner:
Wallet/Solana owner:
Persistence owner:
Analytics events:
Security concerns:
Test evidence required:
```


## Architecture documentation update rule

Update this file whenever a change affects app structure or ownership. Examples include new Gradle modules, packages, navigation ownership, ViewModels, repositories, use cases, Firebase collection ownership, Mobile Wallet Adapter flow, Solana RPC/transaction flow, or cache/persistence authority.

Do not update this file for small visual copy changes or dependency compatibility fixes unless they change architecture. For every patch, the handoff must explicitly say whether `ARCHITECTURE.md` was updated and why.

## Current Phase 3 implementation map

The current app is still one Android module:

```text
app/
  src/main/java/com/thinkblox/radiantrush/
    data/                 Shared UI/domain models for early phases
    firebase/             Firebase Auth + Firestore repository
    solana/               Mobile Wallet Adapter repository boundary
    ui/                   Compose app shell, components, and screens
```

Phase 2 introduced `FirebaseRadiantRepository` as the only Firebase owner. Phase 3 adds `MobileWalletRepository` as the only Mobile Wallet Adapter owner. Compose screens display state and send user intents, but do not call Firebase or MWA APIs directly.

Current Firestore ownership:

```text
users/{uid}                              FirebaseRadiantRepository
users/{uid}/completedQuests/{questId}    FirebaseRadiantRepository
leaderboard/{uid}                        FirebaseRadiantRepository
quests/{questId}                         Reserved read-only config path for later
```

Current Phase 3 data flow:

```text
Screen -> RadiantRushApp event -> FirebaseRadiantRepository -> Firebase Auth / Firestore
Screen -> RadiantRushApp event -> MobileWalletRepository -> Mobile Wallet Adapter
Screen <- RadiantRushApp state <- FirebaseRadiantRepository <- Firebase Auth / Firestore
Screen <- RadiantRushApp state <- MobileWalletRepository <- Mobile Wallet Adapter
```

This is an accepted short-term prototype exception to the preferred full clean architecture shape. A separate ViewModel/use-case layer should be added if Firebase, wallet, Solana RPC, and quest rules become complex enough to require independent testing.

Phase 3 owns public wallet identity after MWA authorization. It does not own Solana transaction proof or SKR balance truth. Those remain locked for Phase 4+.


## Do not port from BMA

Do not port these Roblox concepts directly:

- ServerScriptService / StarterPlayerScripts / ReplicatedStorage structure.
- RemoteEvent/RemoteFunction patterns.
- MarketplaceService receipt code.
- Roblox DataStore patterns.
- Studio-managed hierarchy rules.
- Roblox UI scale APIs.
- Gameplay loops, pets, gacha, NPCs, leaderboards, or monetization systems unless the Android product explicitly needs an equivalent.

Port the principle, not the platform code.


## Phase 3 wallet boundary

`MobileWalletRepository` owns direct calls to Solana Mobile Wallet Adapter. UI code may trigger wallet intent events, but must not parse wallet internals or store wallet secrets. `FirebaseRadiantRepository` owns persistence of the public wallet address after MWA authorization. Message signing and transaction submission are not part of Phase 3 and must remain locked until Phase 4.

## Android lifecycle-sensitive ownership rule

Objects that register Activity Result launchers, request Activity-owned launchers, or depend on `ComponentActivity.registerForActivityResult` must be created by `MainActivity` before `setContent { ... }` whenever the library requires registration before `STARTED`.

For Phase 3 Mobile Wallet Adapter, `MobileWalletRepository` is created in `MainActivity.onCreate()` and passed into Compose. Compose may call wallet actions through callbacks, but it must not construct `ActivityResultSender` or other registration-sensitive MWA objects during composition.


## Phase 4 implemented structure

Current package structure intentionally remains simple for hackathon speed:

```text
app/src/main/java/com/thinkblox/radiantrush/
  MainActivity.kt                         Activity lifecycle owner
  data/PhaseOneModels.kt                  UI/domain models for quests, badges, user state
  firebase/FirebaseRadiantRepository.kt   Firebase Auth + Firestore profile/proof persistence
  solana/MobileWalletRepository.kt        MWA wallet connect, message signing, memo transaction proof
  ui/                                     Compose app shell, screens, components, theme
```

### Phase 4 ownership boundary

`MobileWalletRepository` owns all Solana/MWA work:

- Wallet connection/disconnection.
- `signMessagesDetached` daily proof request.
- Devnet Memo transaction construction.
- `signAndSendTransactions` submission.
- Public signature/explorer URL result mapping.

`FirebaseRadiantRepository` owns app-progress persistence only:

- Anonymous Firebase profile.
- Completed quest proof documents.
- XP, level, streak, badge state inputs.
- Leaderboard rows for MVP/demo use.

The UI must not build Solana transactions directly. It may call the repository through app-level event handlers only.


## Phase 4 MWA proof-session note

The Solana boundary keeps `MobileWalletRepository` as the only owner of MWA calls.

After the Phantom devnet test failure on 2026-09-09, proof actions were adjusted so:

- Wallet connect/disconnect uses a connection-scoped `MobileWalletAdapter`.
- `Sign Daily Proof` creates a fresh devnet `MobileWalletAdapter` for that one proof request.
- `On-Chain Memo Proof` creates a fresh devnet `MobileWalletAdapter` for that one transaction request.
- The UI still treats wallet connection as public-address state only; it does not own or persist wallet auth tokens.

Reason: the log showed memo proof failing inside the MWA `reauthorize` path before the transaction proof could complete. Fresh proof sessions avoid stale/broken auth-token reuse while preserving explicit wallet approval for every sensitive action.


## Phase 5 SKR balance boundary

`SkrBalanceRepository` owns read-only Solana mainnet SKR balance scanning. It calls JSON-RPC by public wallet address and official SKR mint only. It does not use Mobile Wallet Adapter because a balance read does not require wallet approval or signing.

Network split:

- `MobileWalletRepository` keeps proof transactions on Solana Devnet.
- `SkrBalanceRepository` reads the official SKR SPL token on mainnet-beta.
- `FirebaseRadiantRepository` stores the public SKR snapshot and derived app UX tier.

The UI must display SKR tier and multiplier as app status, not as an on-chain authority decision.

## Phase 6 demo polish boundary

Phase 6 adds a UI-only `Demo` tab. This screen may summarize current app state for judges, but it must not become a second source of truth.

Rules:

- Demo cards read from existing `RushUiState`.
- Demo cards do not mark quests complete.
- Demo cards do not call wallet, Solana RPC, or Firebase writes.
- Demo copy must distinguish devnet memo proof from mainnet SKR balance scanning.
- Demo mode must not create fake balances, fake signatures, or fake transaction hashes.


## Phase 7 testing architecture

Phase 7 separates testable product rules from Android/wallet side effects:

- `logic/SkrTierRules.kt` is pure JVM logic for SKR mint metadata, tier thresholds, and balance formatting.
- `logic/QuestInteractionRules.kt` is pure JVM logic that documents which quests open an external wallet and which remain inside the app.
- `ui/testing/UiTestTags.kt` provides stable Compose semantic tags for smoke tests.
- `app/src/test` covers pure business rules without an emulator.
- `app/src/androidTest` covers app launch and Demo navigation on an emulator or Android device.

Wallet approvals are intentionally not bypassed by tests. Real MWA signing and transaction approval remain manual final QA.

## Phase 9 reward-loop boundary

Phase 9 adds `logic/RewardLoopRules.kt` as a pure, testable domain rule object for the Daily Radiant Chest. The UI may display the chest and call the Firebase repository, but reward eligibility and reward selection must remain deterministic and testable outside Compose.

Daily Radiant Chest is not wallet logic. It must not open Mobile Wallet Adapter, request a signature, send a transaction, transfer SKR, or spend SOL. It writes a Firebase proof/reward document only after the existing proof quests are complete.


## Phase 9.1 leaderboard identity boundary

- `users/{uid}` and `leaderboard/{uid}` remain Firebase-owned storage keyed by the anonymous Firebase UID.
- Public ranking identity is the connected Solana wallet address, not the anonymous UID.
- `LeaderboardRules` collapses duplicate UID-backed rows by wallet before presenting Top 20 ranks.
- Full wallet address is now persisted on leaderboard writes; legacy short-only rows are supported as a migration fallback.
- Changing leaderboard document IDs to wallet addresses is intentionally deferred until wallet ownership can be enforced by a stronger backend/rules model.


## Phase 10.1 procedural game-audio boundary

`ProceduralGameAudioEngine` is presentation infrastructure owned by the Radiant Rush screen. It synthesizes/mixes PCM on an audio thread and outputs through one Android `AudioTrack`. It does not read/write Firebase, call Mobile Wallet Adapter, call Solana RPC, award XP/tickets, or decide collectible outcomes. Audio failures are non-authoritative and fail silent.

`RadiantRunScreen` remains the owner of transient gameplay presentation (particles, hit flash, target pulse, FEVER visuals, audio cue requests). `RadiantGameRules` and Firebase remain the progression/reward authorities established by Phase 10.


## Phase 11A competition-domain boundary

Phase 11A introduces `logic/Phase11CompetitionRules.kt` as an Android/Firebase-free domain boundary for Radiant Rush competition. It owns deterministic UTC day/week keys, ranked-attempt classification, capped gameplay-XP decisions, weekly/personal-best aggregation, and public run-board wallet deduplication.

The authority split is intentionally strict:

- `RadiantRunScreen` remains transient client gameplay/presentation.
- Phase 11A rules describe competition state only; they do not write Firestore yet.
- Phase 11B will be responsible for mapping these pure models to Firebase persistence and Compose UI.
- Existing XP leaderboard behavior remains separate from the new Radiant Rush raw-score board.
- A `RunScoreRecord` produced by Android is `ClientReportedPrototype` data and has `payoutEligible == false`; no Android client record is an SKR payout authority.
- Ranked score ordering is raw score first. Best combo, PERFECT hits, and earliest achievement are tie-breakers only.
- A connected wallet receives the same three ranked attempts per UTC day regardless of SKR balance/tier. Additional ticket-backed runs are casual and cannot improve ranked weekly/all-time stats.

## Phase 11B implemented architecture — competition persistence and UI

Phase 11B now maps the Phase 11A rules into Firebase persistence and Compose UI. Private daily counters and personal mirrors stay under `users/{uid}`. Public prototype competition PB rows live under `runWeekly/{utcWeekKey}/entries/{uid}` and `runAllTime/{uid}`. Firebase UID remains the owner/write key; connected Solana wallet identity is used to collapse duplicate public rows caused by anonymous-auth reinstalls. Weekly and All-Time ordering is raw score first, then best combo, then PERFECT hits. Casual runs do not update ranked PB documents. Every competition row is explicitly marked client-reported prototype data and `payoutEligible=false`; no real SKR payout authority is implemented in Android.

## Phase 11C.1 — verified SKR staking read path

`SkrBalanceRepository` remains a read-only Mainnet boundary, but now composes two public-data sources into one Passport snapshot: liquid SPL token accounts and the official Solana Mobile SKR staking program (`SKRskrmtL83pcL4YqLWt6iPefDqwXQWHSw9S9vz94BZ`). The staking read uses the connected public address only and never invokes Mobile Wallet Adapter.

Staking account decoding follows the official Anchor IDL. `StakeConfig.share_price` is used with summed `UserStake.shares` to derive active stake using the official `shares * sharePrice / 1e9` formula. `UserStake.unstaking_amount` is tracked separately. Staking data is used only when its on-chain account read/decoding is verified; an unavailable staking query cannot silently become zero/eligible stake.

`SkrPassportRules` owns the economic-neutral perk decision: eligible Passport balance = liquid + verified active stake. Verified active stake adds the Guardian Stake Boost (+1 casual ticket/day and +25 chest XP). `RewardLoopRules` still owns deterministic chest base rarity; the Firebase repository passes the exact Passport bonus persisted by today's SKR proof so staking never changes the base roll.

This remains prototype app-progression authority only. It is not payout authority and does not prove server-trusted competition score.


## Phase 11C.2 — presentation authority boundary

Daily Radiant Chest animation is presentation-only. `RadiantChestCard` owns transient charge, shake, lid-open, burst, particle, haptic, and audio state. `RadiantRushApp.claimDailyRadiantChest` holds the UI in `Opening` for 1.8 seconds, then calls `FirebaseRadiantRepository.claimDailyRadiantChest`; Firebase remains the reward/progression authority. Closing the animation or audio path must never create a reward by itself.

Radiant Rush keeps one visual contract for tappable objects: green circles are valid targets and red circles are Corruption. FEVER is a non-circular gold screen-state treatment so decoration cannot look like another target.

## Phase 11C.4 — wallet-scoped My Stats

`My Stats` is now a wallet-scoped read model rather than a Firebase-UID-only mirror. The repository queries the current week's `runWeekly` entries and `runAllTime` entries for the connected full wallet address, then aggregates the wallet's personal PB/run data across anonymous-auth UIDs. `users/{uid}` competition fields remain fallback/migration state.

The existing `runWalletDaily/{utcDay}/wallets/{walletAddress}` document also mirrors capped `gameplayXpEarnedToday`, keeping both ranked-attempt usage and the 300 XP/day gameplay cap consistent across phones/reinstalls. This shared record remains prototype fairness state only and has no economic or payout authority.



## Phase 12B trusted Weekly Cup configuration

`weeklyCupConfigs/{weekKey}` is now the trusted, Admin-written configuration source for the current Weekly Radiant Cup. The Android app may read the document but Firestore rules deny all client writes. Schema v2 contains sponsor presentation, exact SKR prize amount in atomic units, official SKR mint/decimals, ISO-week start/end, placement allocation, optional public funding wallet, funding-verification status, `trustedResultsRequired=true`, `payoutEnabled=false`, and an explicit trusted-admin authority marker.

`Phase12WeeklyCupConfigRules` is a fail-closed presentation parser: malformed schema, wrong week, wrong mint/decimals, invalid allocation, invalid time window, or wrong authority marker is not presented as a trusted sponsor config. `FirebaseRadiantRepository` retains a legacy Phase 11 announcement fallback but never labels it trusted. Android never promotes funding state, winner state, or payout eligibility.

`scripts/firebase-admin/manage-weekly-cup.mjs` is developer/admin tooling, not app code. It is dry-run by default and writes only after exact project confirmation. It refuses to overwrite future verified-funding, enabled-payout, or newer-schema state so Phase 12B tooling cannot accidentally roll back Phase 12C+ trust.
