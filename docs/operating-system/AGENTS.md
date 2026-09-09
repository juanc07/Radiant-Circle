# RadiantSolanaHackatonAndroid — Agent Contract & Development Workflow

## Project contract

This is an Android app project for a Solana hackathon. Do not rebuild it as a new app unless explicitly requested. Audit the current source and make focused, safe changes.

The Blox Monster Adventure source is a reference for engineering rules only. Roblox-specific services, UI hierarchy, gameplay systems, DataStore, MarketplaceService, Remotes, Studio plugins, and Luau code are not Android implementation authorities.

## Mandatory source gate

Before editing:

1. Identify the newest accepted source package, repo branch, or commit.
2. Record the source filename or Git commit hash when available.
3. Inspect `git status --short` and preserve meaningful uncommitted work.
4. Confirm whether the input is a full source, partial patch, or documentation-only package.
5. Verify real file paths, package names, Gradle modules, build variants, API endpoints, environment files, and ownership paths before coding.
6. Read the relevant maintained guide before editing:
   - UI work: `MOBILE_UI_UX_STANDARDS.md`
   - Solana/wallet/transaction work: `SOLANA_SECURITY_AND_DATA_RULES.md`
   - Release/testing work: `TESTING_AND_RELEASE.md`
   - Architecture/module work: `ARCHITECTURE.md`
7. Do not create patches from memory, screenshots, chat history, or old ZIPs when a newer source exists.

## Standard task loop

`Audit -> trace authority -> define one task -> smallest patch -> static checks -> device/emulator test -> output evidence -> update docs -> deliver changed files or clean ZIP`

After two failed fixes in the same area, stop stacking local tweaks. Re-audit the complete ownership path, data flow, and UI state before editing again.

## Patch strategy

- One issue per patch unless changes are inseparable.
- Do not casually mix UI, wallet, transaction signing, backend, persistence, analytics, and release work.
- Change only files necessary for the requested task.
- Avoid unrelated refactors, formatting sweeps, dependency churn, or package renames.
- Reuse existing app patterns before introducing new abstractions.
- Keep comments focused on authority, invariants, security, and non-obvious behavior.
- Never claim runtime behavior from static checks alone.

## Source control rules

- Keep a backup before large edits.
- Prefer changed-files-only patches for focused tasks.
- Export a clean full source ZIP after a milestone or after file deletions/renames.
- The newest fully accepted source wins over old handoffs, old patches, screenshots, and remembered implementation details.
- Record assumptions and unresolved gaps instead of silently inventing answers.


## ZIP and Git metadata safety rules

Generated source packages and patch ZIPs must never include the repository's `.git/` directory or internal Git metadata files. Including Git metadata can corrupt, overwrite, or confuse the user's existing repository history and remotes.

Rules:

- Do not include `.git/`, `.git/config`, `.git/index`, `.git/objects`, `.git/refs`, `.git/logs`, or any other internal Git metadata in delivered ZIPs.
- Do not generate a new repository history inside a user's existing project unless explicitly requested.
- Do not overwrite the user's Git history, branches, remotes, tags, hooks, or local uncommitted work.
- Patch ZIPs must contain only project-relative changed, modified, or new working-tree files.
- Full source ZIPs must contain clean working-tree source files only, never Git metadata.
- `.gitignore` is allowed only when it is intentionally changed for the task and listed as a changed file.
- Before packaging, run a no-Git-metadata check against the output folder or ZIP.

## Authority rules

The app must have clear ownership boundaries:

- UI displays state and sends user intent.
- ViewModels own presentation state and input orchestration.
- Domain/use-case layer owns business rules.
- Data layer owns repositories, persistence, network/RPC calls, and cache policy.
- Wallet/signing authority must remain with the user wallet. The app must not custody private keys.
- Backend authority, if added, must be explicit and documented.

Hard invariants:

- Do not store private keys, seed phrases, or wallet secrets.
- Do not fake wallet balances, confirmed signatures, token ownership, or on-chain success.
- Do not treat a client-side cache as authoritative for money, tokens, rewards, or identity.
- Analytics must be observational and non-blocking.
- Network, RPC, image, analytics, or optional backend failure must not crash the app.
- Debug shortcuts, fake wallets, mock RPC, and demo-only bypasses must never be enabled in production builds.

## Android/Kotlin rules

- Keep Kotlin strict and readable; avoid global mutable state.
- Prefer dependency injection over hidden singletons.
- Keep coroutine scopes lifecycle-aware.
- Treat cancellation, timeout, retry, and offline states deliberately.
- Keep UI state immutable where practical.
- Keep persistence migrations explicit.
- Do not hardcode secrets, RPC keys, API keys, package credentials, wallet addresses, token mints, or program IDs unless they are clearly public constants and documented.


## Documentation and versioning gate

Every meaningful code, dependency, build-system, Firebase, Solana, wallet, UI, or release change must include a documentation decision. Read `DOCUMENTATION_AND_VERSIONING_RULES.md` before delivering the patch.

Required handoff block:

```text
Documentation decision:
- CHANGELOG.md: updated / not updated — reason
- ARCHITECTURE.md: updated / not updated — reason
- AGENTS.md: updated / not updated — reason
- TESTING_AND_RELEASE.md: updated / not updated — reason
- SOLANA_SECURITY_AND_DATA_RULES.md: updated / not updated — reason
- MOBILE_UI_UX_STANDARDS.md: updated / not updated — reason
- VERSION: updated / not updated — reason
```

Default rules:

- `CHANGELOG.md` updates for all meaningful changes.
- `ARCHITECTURE.md` updates when modules, packages, ownership, data flow, Firebase structure, repositories, ViewModels, wallet/Solana flow, or navigation change.
- `AGENTS.md` updates only when workflow rules change.
- `TESTING_AND_RELEASE.md` updates when checks, release gates, device testing, APK process, or evidence language change.
- App `versionCode` and `versionName` update only for APK handoffs, milestone builds, demo builds, release candidates, or final hackathon builds.

## Handoff format

Every completed change should include:

- What changed.
- Files changed.
- Why the change is safe.
- Checks actually run.
- Checks not run.
- Known risks or follow-up work.

Use honest language:

- `Static check passed` means compile/lint/build checked only.
- `Tested on emulator` means actually run on emulator.
- `Tested on device` means actually run on physical Android hardware.
- `Not tested` is acceptable when true.

### Git hygiene rule

Before committing, inspect `git status`. Do not commit Gradle/Android Studio cache folders, generated lock files, machine-local build state, or root-level Gradle user-home folders such as `/caches/`, `/daemon/`, `/kotlin-profile/`, `/native/`, `/wrapper/`, `/android/`, `/jdks/`, or `/notifications/`.

If `git add .` fails on a cache lock file, stop. Update `.gitignore`, move or delete the accidental cache folders, then stage only real project files. Never force-add cache lock files.
