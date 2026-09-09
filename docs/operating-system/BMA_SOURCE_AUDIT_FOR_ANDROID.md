# BMA Source Audit for Android/Solana Reuse

## Audit scope

Audited source package: `BloxMonsterAdventure-08312026-920AM.zip` extracted into the BMA audit folder.

Observed source shape:

- Large maintained Roblox source tree.
- Hundreds of Luau files split across server, shared, and client script folders.
- Maintained documentation set with README, agent contract, architecture ledger, UI standards, testing/release rules, roadmap, balance rules, story rules, and art handoffs.
- Clear source-authority language: newest accepted full source wins over old patches, screenshots, and chat memory.

This audit is for transferable engineering rules only. BMA gameplay code is not the Android app.

## Most valuable transferable lessons

### 1. Source authority must come first

BMA repeatedly protects against editing from memory. The Android project needs the same rule:

- Find the latest source.
- Check real files.
- Preserve uncommitted work.
- Do not invent paths, APIs, IDs, or test results.
- Make focused patches.

### 2. Keep one owner for dangerous authority

BMA centralizes critical authority such as rewards, purchases, persistence, and server validation. Android/Solana equivalent:

- Wallet owns signing.
- Domain/use-case layer owns business decisions.
- Repository owns RPC/persistence.
- Backend authority, if added, must be explicit.
- UI must never pretend to be final authority for chain state.

### 3. Client presentation is not truth

BMA separates server-authoritative decisions from client presentation. Android equivalent:

- UI can show pending/cached state.
- UI cannot claim confirmed on-chain success until confirmation criteria are met.
- Demo UI must be labeled as demo.
- Caches must be labeled as cached/stale when needed.

### 4. Mobile-first means real small-device acceptance

BMA has strong rules against text clipping, text bleeding, unreachable scroll rows, unsafe scaling, and relying on ellipsis. Android equivalent:

- Test real phone constraints.
- Support large text/accessibility settings.
- Respect insets and keyboard.
- Keep important wallet/transaction copy visible.
- Avoid tiny controls and unclear icon-only buttons.

### 5. Persistence needs migration and failure protection

BMA treats save schema changes as dangerous. Android equivalent:

- Local database/preference schema changes need defaulting and migration.
- Do not store secrets.
- Cache failures must not corrupt wallet/session state.
- Old installs must not crash after update.

### 6. Analytics must never block core behavior

BMA treats analytics as observational. Android equivalent:

- Analytics failure must not block wallet, RPC, screen loading, or transaction result display.
- Analytics must not contain secrets.
- Event names should represent actual state, not hoped-for success.

### 7. Honest testing language prevents false confidence

BMA explicitly distinguishes static checks from Studio/device tests. Android equivalent:

- Build passed is not emulator tested.
- Emulator tested is not physical device tested.
- Signed is not submitted.
- Submitted is not confirmed.

### 8. Documentation should be cumulative but capped

BMA has a maintained documentation map and a cap. Android equivalent:

- Keep docs small and authoritative.
- Update existing docs instead of creating duplicate handoffs.
- Keep `README.md` as the continuation entry point.

## What not to reuse directly

Do not directly port:

- Luau code.
- Roblox services.
- Roblox Studio hierarchy assumptions.
- ServerScriptService/StarterPlayerScripts/ReplicatedStorage folder meanings.
- RemoteEvent/RemoteFunction patterns.
- MarketplaceService receipt handling.
- Roblox DataStore implementation.
- Pet/gacha/gameplay economy systems.
- Roblox-specific UI scale APIs.
- Studio plugin workflows.

## Android translation table

| BMA pattern | Android/Solana translation |
|---|---|
| Newest full ZIP is source of truth | Latest accepted repo/commit/source package is source of truth |
| Server owns rewards and persistence | Domain/data/backend/wallet boundaries own truth; UI does not |
| Client displays state and submits requests | Compose UI displays state and sends events to ViewModel |
| Validate RemoteEvents on server | Validate all user input before wallet/RPC actions |
| One ProcessReceipt owner | One transaction flow owner per action; no duplicate submit paths |
| DataStore migration/defaulting | Local database/preferences migration/defaulting |
| Analytics non-blocking | Analytics/logging never blocks app behavior |
| Mobile UI no clipping/ellipsis | Android small-phone/large-font acceptance |
| Studio/device acceptance | Gradle/emulator/physical-device/wallet/RPC evidence |
| Debug helpers disabled in published servers | Mock/demo/debug disabled in production builds |

## Recommended first Android implementation path

1. Audit the actual Android source.
2. Identify current package/module structure.
3. Create a minimal home screen with clear demo/network state.
4. Add wallet connection with clear cancel/reject/no-wallet states.
5. Add one read-only Solana data flow.
6. Add one transaction flow only after read/connect states are stable.
7. Add mobile acceptance tests/checklist.
8. Prepare a demo script and fallback.

## Biggest risk if BMA lessons are ignored

The biggest risk is building many impressive screens that do not have reliable authority behind them. For a Solana app, that becomes dangerous fast: fake balances, unclear signing, false confirmation, broken wallet rejection handling, or app crashes on slow RPC. The BMA lesson is to protect truth, testing, and mobile usability before adding more surface area.
