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

## Phase 3 wallet-connect acceptance checks

Before Phase 3 is called stable:

- Android app builds after MWA dependencies are added.
- App still works when Firebase is configured.
- Existing XP/streak/profile data does not reset after app restart.
- Connect Wallet opens an MWA-compatible wallet on a real Android device.
- No-wallet state shows a clear message and does not crash.
- Rejected/cancelled wallet authorization shows a recoverable message and does not mark the wallet quest complete.
- Successful wallet authorization saves only the public wallet address and optional account label.
- Firestore `users/{uid}.walletAddress` updates after successful authorization.
- Wallet Ready quest/badge updates after successful authorization.
- Disconnect Wallet clears the stored public wallet state.
- No message signing, transaction success, or SKR verification is claimed in Phase 3.

## Phase 4 Solana proof test matrix

Before accepting Phase 4 as stable, test these flows on a physical Android device with Phantom in test/devnet mode:

- App opens without crash after Phase 4 patch.
- Firebase Anonymous Auth still signs in.
- Wallet connect still works.
- `Sign Daily Proof` opens the wallet.
- User rejection of message signing does not complete the quest.
- Successful message signing saves `sign-daily-proof_<date>` under `users/{uid}/completedQuests`.
- `On-Chain Memo Proof` opens the wallet.
- User rejection of transaction signing does not complete the quest.
- Successful memo submission saves `on-chain-proof_<date>` under `users/{uid}/completedQuests`.
- Profile shows latest message signature and memo transaction signature.
- Duplicate same-day proof quests do not award duplicate XP.
- Wallet/RPC failure shows a recoverable message and does not crash.

Evidence labels:

- Use `Wallet tested` only after Phantom actually opened and returned a result.
- Use `RPC tested` only after the app fetched a devnet blockhash and attempted/submitted a memo transaction.
- Use `Submitted` for Phase 4 memo results, not `confirmed`, until confirmation polling exists.


## Phase 4 memo proof regression test

For every Phase 4 build after the memo proof fix, test this exact flow on a real Android phone:

1. Connect an MWA-compatible wallet in devnet/test mode.
2. Tap `Submit Memo Proof` once.
3. Approve the wallet request.
4. Return to Radiant Rush.
5. Confirm the on-chain memo quest enters a saving/syncing state and cannot be tapped repeatedly.
6. Confirm the quest changes to completed after Firebase saves the transaction proof.
7. Restart the app and confirm the quest remains completed for that date.
8. Check Firestore for `lastOnChainProofDate`, `lastOnChainTxSignature`, and the completed quest document.


## Phase 4 MWA fresh-session regression test

Run this after applying the MWA reauthorization/signature action fix:

1. Force stop Phantom and Radiant Rush, then reopen Phantom.
2. Confirm Phantom is on Devnet and has devnet SOL.
3. Open Radiant Rush and connect wallet.
4. Tap `Sign Daily Proof` once and approve.
5. Confirm the quest becomes completed and the Profile screen shows `Last Signed Proof`.
6. Tap `Copy Signature` and confirm Android shows a copied toast.
7. Tap `Submit Memo Proof` once and approve the new authorization/sign-and-send prompt.
8. Confirm Logcat includes `Memo proof using fresh MWA authorization session on solana:devnet`.
9. Confirm the memo quest only completes after a transaction signature is returned and saved to Firestore.
10. Confirm the Profile screen shows `Last Memo Transaction` and `Copy Explorer` after success.

Failure evidence to capture:

```text
RadiantRushWallet
authorization request failed
Memo transaction failed
FATAL EXCEPTION
```

Do not call Phase 4 stable until both signing and memo submission work on a physical Android device with Phantom Devnet.

## Phase 4 Memo Preflight Regression Test

After applying the memo preflight fix:

1. Build `./gradlew :app:assembleDebug`.
2. Run on a physical Android device with Phantom installed.
3. Set Phantom to Devnet and ensure the wallet has devnet SOL.
4. Connect wallet in Radiant Rush once.
5. Tap `Send Memo` once.
6. Confirm Phantom shows a transaction approval, not only a connect approval.
7. Approve the transaction and return to the app.
8. Confirm the memo quest becomes Done and Firestore stores the transaction signature.
9. Filter Logcat by `RadiantRushWallet` and confirm `Devnet chain context ready` appears before `Memo proof using active MWA session`.

If the app shows an RPC/DNS error before opening Phantom, test phone connectivity first. Do not keep tapping the memo button repeatedly.

## Phase 4 memo auth auto-retry test

When testing the memo proof, one first-attempt Phantom/MWA authorization failure may occur after earlier builds or stale wallet sessions. The app should now clear the cached auth token and retry once automatically. The test passes only when the final Logcat result includes `Memo proof MWA success. signatureReturned=true` and the memo quest becomes Done without requiring repeated user taps.


## Phase 5 SKR Passport regression test

1. Build `./gradlew :app:assembleDebug`.
2. Run on a physical Android device.
3. Connect an MWA-compatible wallet.
4. Keep Phantom Devnet for memo proof; Devnet SOL is only needed for Send Memo.
5. Tap `Check SKR Balance`.
6. Confirm the app does not ask Phantom to sign or send anything for the SKR scan.
7. Confirm the app calls mainnet SKR balance read and returns either a real SKR balance or `0 SKR`.
8. Confirm the SKR quest becomes Done after a successful read.
9. Confirm Profile shows SKR Balance, SKR Tier, and XP boost.
10. Confirm Firestore stores `users/{uid}.skrBalanceDisplay`, `skrTier`, `skrMint`, `skrNetwork`, and `completedQuests/skr-holder_<date>`.

Expected zero-balance behavior:

```text
SKR Passport scanned: 0 SKR on mainnet. Explorer tier saved without faking a balance.
```

Expected Logcat filter:

```text
RadiantRushSKR
```

### Phase 5 UX tap reliability checks

- Fresh install the app.
- Tap each ready quest once and verify the button changes to a disabled syncing state immediately.
- Confirm Connect Wallet, Sign Proof, and Send Memo are the only flows that open Phantom.
- Confirm Scan SKR Passport stays inside the app and completes by read-only mainnet RPC.
- Double-tap each action quickly and confirm only one request is accepted.
- Tap Refresh Firebase while a proof is running and confirm the app asks you to wait instead of interrupting the action.
- After success, restart the app and confirm completed quests stay Done for the current day.

## Phase 6 demo polish testing

Before committing Phase 6:

1. Build with `./gradlew :app:assembleDebug`.
2. Confirm the bottom navigation includes `Demo`.
3. Open `Demo` on a small Android phone and check for clipped text.
4. Complete the normal quest flow from Today, not from the Demo screen.
5. Confirm Demo readiness updates after quests are completed.
6. Confirm SKR Passport still scans inside the app without Phantom.
7. Confirm Sign Proof and Send Memo still use Phantom / MWA.
8. Confirm Profile still shows wallet address, signed proof, memo transaction/explorer link, and SKR tier.

Known acceptable issue: some wallets may still need more than one visible memo attempt because of wallet authorization handoff behavior. This does not block Phase 6 if the transaction proof eventually completes and is saved.


## Phase 7 automated testing

Phase 7 introduces repeatable local/CI checks so most regressions can be caught without repeatedly using a phone.

Fast automated checks:

```bash
./gradlew :app:testDebugUnitTest
./gradlew :app:assembleDebug
```

Convenience scripts:

```bash
./scripts/run_phase7_tests.sh
```

```powershell
.\scripts\run_phase7_tests.ps1
```

Emulator or physical-device Compose smoke test:

```bash
./gradlew :app:connectedDebugAndroidTest
```

Manual wallet approval is still required for Connect Wallet, Sign Daily Proof, and Send Memo Proof because MWA must preserve user consent in the wallet app. SKR Passport should remain read-only and should not open Phantom.
