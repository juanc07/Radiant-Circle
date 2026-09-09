# RadiantSolanaHackatonAndroid — Documentation and Versioning Rules

## Purpose

Every meaningful code or build-system change must include a documentation decision. This prevents silent architecture drift, unclear release state, and repeated fixes that are not recorded.

These rules apply to Android code, Firebase work, Solana/Mobile Wallet Adapter work, UI changes, build configuration, docs, and release packaging.

## Required documentation decision block

Every delivered patch must include this block in the handoff message:

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

A file may be marked `not updated`, but the reason must be explicit.

## Always update `CHANGELOG.md` for meaningful changes

Update `CHANGELOG.md` when the patch adds, changes, fixes, removes, or documents anything meaningful.

Examples:

- New screen, feature, repository, wallet flow, Firebase collection, Solana transaction path, or UI state.
- Dependency or Gradle change.
- Security rule change.
- Release process change.
- Bug fix.
- New known limitation.

Small typo-only fixes may skip the changelog only when the patch clearly says why.

## When to update `ARCHITECTURE.md`

Update architecture docs when the structure or ownership of the app changes.

Update it for:

- New Gradle module or package structure.
- New UI navigation pattern.
- New ViewModel, repository, use case, wallet adapter, RPC client, or Firebase data owner.
- New persistent data model or Firestore collection shape.
- Any change to wallet/Solana/Firebase authority boundaries.
- Any dependency that changes app architecture, such as Firebase, MWA, networking, database, DI, or navigation.

Do not update it for purely visual copy, small layout tweaks, or dependency version compatibility fixes unless those changes alter ownership or architecture.

## When to update `AGENTS.md`

Update the agent contract only when the development workflow changes.

Update it for:

- Patch packaging policy changes.
- Source authority changes.
- Required audit steps.
- Required test or evidence language.
- Rules about AI/developer handoff behavior.
- Rules about changed-files-only delivery versus full source delivery.

Do not update it for normal feature work unless the feature introduces a new workflow requirement.

## When to update `TESTING_AND_RELEASE.md`

Update testing/release rules when the way we prove the app works changes.

Update it for:

- New build commands.
- New test commands.
- New required manual test flows.
- New release gates.
- APK packaging changes.
- Firebase, wallet, RPC, transaction, or device-test acceptance changes.
- New demo/video acceptance requirements.

## When to update `SOLANA_SECURITY_AND_DATA_RULES.md`

Update this file for any rule that affects wallet, chain, token, transaction, RPC, or security behavior.

Update it for:

- Mobile Wallet Adapter connection/session rules.
- Transaction signing or submission behavior.
- Message signing behavior.
- SKR token detection logic.
- RPC provider/network/commitment changes.
- Firestore security rules that affect wallet-linked data.
- Any rule about secrets, private keys, seed phrases, or production mock blocking.

## When to update `MOBILE_UI_UX_STANDARDS.md`

Update this file when a UI rule should become reusable across the whole app.

Update it for:

- New standard component behavior.
- Navigation rules.
- Mobile layout rules.
- Accessibility, text clipping, loading, error, or empty-state standards.
- Wallet/signing confirmation UX standards.

Do not update it for one-off copy or one screen's internal layout unless the pattern will be reused.

## Version system

The Android app version is controlled by:

```kotlin
versionCode = <integer>
versionName = "<semantic-version>-<phase-or-build-label>"
```

Current Phase 1 style:

```kotlin
versionCode = 1
versionName = "0.1.0-phase1"
```

### Version update rules

Update `versionCode` and `versionName` for:

- APK handoff builds.
- Demo builds.
- Release candidates.
- Major milestone completion.
- Store/distribution candidates.
- Any build sent outside the development machine for testing.

Do not bump app version for:

- Documentation-only patches.
- Local-only experimental changes not delivered as APK.
- Failed fixes.
- Dependency compatibility patches that do not produce a new APK, unless the patched project is handed off as a build candidate.

### Suggested version labels

```text
0.1.0-phase1        Native UI shell
0.2.0-phase2        Firebase profile/quest persistence
0.3.0-phase3        Mobile Wallet Adapter connection
0.4.0-phase4        Daily proof/sign/send transaction flow
0.5.0-phase5        SKR detection and tier behavior
0.8.0-demo          End-to-end demo candidate
1.0.0-hackathon     Final hackathon APK candidate
```

`versionCode` must only increase. Never reuse an older `versionCode` for a newer APK.

## Changed-files-only delivery rule

For stable projects, deliver only changed, modified, and new files unless a full source package is explicitly requested or safer because of structural/build-system risk.

Every changed-files-only patch must preserve the real project-relative path, for example:

```text
gradle/libs.versions.toml
docs/operating-system/CHANGELOG.md
app/src/main/java/com/thinkblox/radiantrush/MainActivity.kt
```

If a full source ZIP is delivered, it must be clearly labeled as full source, not a patch.


## Git metadata delivery rule

Never include the repository's `.git/` directory or internal Git metadata files in any delivered patch or full source ZIP. Git metadata belongs to the user's local repository, not to an AI-generated handoff package.

Forbidden in delivered ZIPs:

```text
.git/
.git/config
.git/index
.git/objects/
.git/refs/
.git/logs/
.git/hooks/
```

Packaging requirements:

- Changed-files-only patches must contain only the project-relative files needed for the patch.
- Full source ZIPs must contain source/project files only, with `.git/` excluded.
- Do not create or overwrite repository history, remotes, branches, tags, hooks, or Git configuration.
- `.gitignore` may be included only when it is intentionally modified and clearly listed in the changed files.
- Handoff notes must state whether the ZIP is a patch or full source.

Required packaging check:

```bash
zipinfo -1 <handoff>.zip | grep -E '(^|/)\.git(/|$)' && echo "ERROR: Git metadata found" || echo "OK: no .git metadata"
```

If Git metadata is found, delete the ZIP and rebuild it before delivery.

## Handoff evidence rule

Every patch handoff must include:

```text
Changed files:
- ...

Why this is safe:
- ...

Checks run:
- ...

Checks not run:
- ...

Known risks:
- ...

Documentation decision:
- ...
```

Never claim a build, APK, emulator run, device run, wallet connection, RPC call, or on-chain transaction was tested unless it was actually tested.

## Git hygiene documentation rule

When a change fixes repository hygiene, generated cache folders, `.gitignore`, Git push failures, or accidental local files, update `CHANGELOG.md`. Update `AGENTS.md` only when the workflow rule changes. Do not bump Android `versionCode` or `versionName` for Git-only or documentation-only fixes.

Every source ZIP or patch ZIP must exclude `.git/` metadata and generated cache folders. `.gitignore` may be included only when intentionally changed and listed in the patch notes.
