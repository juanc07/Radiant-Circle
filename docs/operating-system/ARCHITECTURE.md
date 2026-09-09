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
