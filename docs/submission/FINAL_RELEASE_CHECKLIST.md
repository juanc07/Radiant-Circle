# Radiant Rush — Final Release Checklist

## Branch state

Before submission:

```bash
git checkout main
git pull origin main
git status
```

Expected:

```text
On branch main
Your branch is up to date with 'origin/main'.
nothing to commit, working tree clean
```

## Required tags

Recommended working tags:

- `phase-3-mwa-wallet-connect-working`
- `phase-4-solana-proof-quests-working`
- `phase-5-skr-balance-tier-working`
- `phase-6-demo-polish-retention-working`
- `phase-7-final-qa-release-working`

Optional final tag:

```bash
git tag hackathon-submission-v1
git push origin hackathon-submission-v1
```

## Automated tests

Run:

```bash
bash scripts/run_phase7_tests.sh
```

Or direct:

```bash
./gradlew --stop
./gradlew :app:testDebugUnitTest
./gradlew :app:assembleDebug
```

Expected:

```text
BUILD SUCCESSFUL
Phase 7 local automated checks passed.
```

## Manual phone QA

Perform on a real Android phone:

- App fresh install opens.
- Firebase anonymous sign-in works.
- Wallet connect opens wallet and returns to app.
- Sign Daily Proof opens wallet and completes.
- Send Memo Proof opens wallet and completes.
- SKR Passport scan completes inside app with no wallet popup.
- Profile shows wallet/proof/SKR info.
- Demo tab opens and reads correctly.
- No obvious text clipping on small screen.

## Build APK

Debug APK:

```bash
./gradlew :app:assembleDebug
bash scripts/copy_debug_apk_to_submission.sh
```

Output:

```text
release/submission/RadiantRush-debug.apk
```

## Do not commit secrets

Before final push:

```bash
git status
```

Do not commit:

```text
app/google-services.json
release/submission/*.apk
*.jks
*.keystore
local.properties
```

## GitHub repository checklist

- README explains app purpose.
- README includes build steps.
- README explains manual wallet QA.
- Docs include submission materials.
- No `.git` metadata inside any ZIP.
- No Firebase private config committed.
- Main branch builds.

## Hackathon form checklist

Prepare:

- Project name: Radiant Rush.
- Short description.
- GitHub repository URL.
- APK or release download link.
- Demo video link.
- Pitch summary.
- Screenshots.
- Team/member info.
- Known limitation note.

## Final known limitation note

Memo proof depends on external wallet authorization handoff. On some fresh Phantom/MWA sessions, it may need one retry. This does not affect SKR scan because SKR scan is read-only RPC and does not open the wallet.
