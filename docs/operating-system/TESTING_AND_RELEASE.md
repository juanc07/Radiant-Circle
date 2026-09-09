# RadiantSolanaHackatonAndroid — Testing and Release

## Testing principle

Be honest about what was actually tested. Static analysis, builds, emulator runs, and physical-device runs are different evidence levels.

## Required status language

Use these exact meanings:

- `Not run` — the check was not executed.
- `Static check passed` — code was inspected or linted only.
- `Build passed` — Gradle build/assemble succeeded.
- `Unit tests passed` — automated unit tests ran and passed.
- `Emulator tested` — the flow was actually run on an Android emulator.
- `Device tested` — the flow was actually run on a physical Android phone.
- `Wallet tested` — the wallet flow was actually run with a real compatible wallet flow.
- `RPC tested` — the app actually connected to the configured RPC/network.
- `Demo-mode tested` — the mock/demo flow was run, not the real chain flow.

Never claim a device, wallet, RPC, or on-chain result from code inspection alone.

## Baseline local checks

Run what is available in the repo:

```bash
./gradlew clean
./gradlew assembleDebug
./gradlew testDebugUnitTest
./gradlew lintDebug
```

If commands differ, document the actual commands used.

## Critical manual test matrix

### Startup

- Fresh install.
- App update install.
- Offline startup.
- Slow network startup.
- Rotation if supported.
- Background/foreground resume.

### Firebase

- Build without `app/google-services.json` and confirm app shows setup-needed state, not a crash.
- Add valid `app/google-services.json`.
- Confirm Gradle applies the Google services plugin.
- Enable Anonymous Auth and confirm anonymous sign-in succeeds.
- Create Firestore and apply `firebase/firestore.rules`.
- Confirm `users/{uid}` is created.
- Complete daily Firebase check-in and confirm `completedQuests/{questId_date}` is created.
- Confirm duplicate same-day check-in does not grant extra XP.
- Confirm leaderboard row updates.
- Confirm offline or rules failure shows error state, not infinite loading.


### Wallet

- No wallet installed.
- Wallet installed but disconnected.
- Connect success.
- User cancels connection.
- Wallet disconnects/revokes session.
- Wrong network or unsupported network.

### Solana reads

- Balance/account load success.
- RPC timeout.
- RPC error.
- Empty account/no assets.
- Stale cache visible as stale.
- Pull-to-refresh or manual refresh.

### Transactions

- Invalid input blocked before signing.
- Confirmation summary is readable.
- User rejects signing.
- Signing succeeds.
- Submission fails.
- Submission succeeds but confirmation pending.
- Confirmation succeeds.
- Confirmation fails.
- App refreshes affected data after result.
- Double-tap does not submit twice.

### UI/mobile

- Small phone portrait.
- Small phone landscape if supported.
- Large font/accessibility text.
- Keyboard open/close for input screens.
- Scroll final item reachable.
- No required text clipped or hidden.

### Persistence

- Clear app data then launch.
- Upgrade from older local data if migration exists.
- Toggle settings and restart.
- Cached data does not appear as fresh truth.
- No secrets persisted.

## Release gates

A demo/release candidate cannot be accepted until:

- App builds successfully.
- No production-blocking lint errors remain.
- Startup works on emulator.
- Critical target flow works on emulator or device.
- Wallet rejection/cancel paths are handled.
- Firebase setup/error/offline path is handled when Firebase is included.
- RPC error/offline path is handled.
- Production build disables mock/fake success flows.
- `CHANGELOG.md` has an `[Unreleased]` entry or release entry describing the change.
- Documentation decision block is included in the handoff.
- App `versionCode`/`versionName` are bumped when the output is an APK handoff, demo build, release candidate, or final hackathon build.
- Known limitations are documented.

## Output evidence template

For every task result, include:

```text
Changed files:
- path/to/file.kt
- path/to/other.file

Checks run:
- ./gradlew assembleDebug — passed
- ./gradlew testDebugUnitTest — passed

Manual testing:
- Pixel emulator API XX — wallet connect cancelled path tested
- Physical device — not run

Known risks:
- ...

Documentation decision:
- CHANGELOG.md: updated / not updated — reason
- ARCHITECTURE.md: updated / not updated — reason
- AGENTS.md: updated / not updated — reason
- TESTING_AND_RELEASE.md: updated / not updated — reason
- SOLANA_SECURITY_AND_DATA_RULES.md: updated / not updated — reason
- MOBILE_UI_UX_STANDARDS.md: updated / not updated — reason
- VERSION: updated / not updated — reason
```

## Bug triage severity

### P0 — Must fix immediately

- App crash on normal launch.
- Secret/private key exposure.
- Fake success shown for failed transaction.
- Irreversible action can happen without user review.
- Production build enables mock transaction success.

### P1 — Fix before demo/release

- Wallet connect broken.
- Primary flow blocked.
- Critical mobile layout clipping.
- RPC failure leaves infinite spinner.
- Double submit possible for transaction action.

### P2 — Fix soon

- Non-critical layout issue.
- Missing empty state.
- Confusing but not dangerous copy.
- Optional analytics/event issue.

### P3 — Backlog

- Polish, animation, minor copy, non-blocking enhancements.

## Release package checklist

When creating a handoff ZIP:

- Include full source or clearly label changed-files-only patch.
- Include all maintained docs.
- Include `CHANGELOG.md` update.
- Exclude build outputs, local secrets, Gradle caches, `.idea` user files, and keystores unless explicitly required.
- State the source commit/hash or package name.
