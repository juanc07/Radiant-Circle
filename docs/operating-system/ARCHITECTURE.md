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
