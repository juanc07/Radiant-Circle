# Phase 8 — Submission Package

Phase 8 freezes app features and prepares Radiant Rush for hackathon submission.

## Status

Radiant Rush now has a working end-to-end demo path:

1. Firebase anonymous account creation.
2. Mobile wallet connection through Solana Mobile Wallet Adapter.
3. Daily message signature proof.
4. Devnet memo transaction proof.
5. Mainnet SKR read-only balance scan.
6. Profile proof display.
7. Demo tab for judge walkthrough.
8. Automated tests and local APK build checks.

## Scope

Phase 8 adds submission assets only:

- Judge-friendly README.
- 3-minute demo script.
- Pitch talking points.
- Screenshot checklist.
- Final release checklist.
- Known limitations.
- APK copy helper scripts.

## Non-goals

Do not add new risky features in Phase 8:

- No wallet transaction flow changes.
- No Firebase rules rewrite.
- No dependency upgrades.
- No SKR transfer/spending feature.
- No private key handling.
- No fake SKR balances.

## Submission APK

Use a locally built debug APK for testing/demo unless the hackathon explicitly requires a signed release build.

Local debug APK source:

```text
app/build/outputs/apk/debug/app-debug.apk
```

Suggested local copy:

```text
release/submission/RadiantRush-debug.apk
```

Do not commit APK binaries by default. Upload the APK directly to the hackathon form or to GitHub Releases if required.

## Manual QA requirement

Wallet consent flows cannot be fully automated. Before final submission, perform one real-device pass:

1. Open fresh install.
2. Connect wallet.
3. Sign daily proof.
4. Send memo proof.
5. Scan SKR Passport.
6. Check Profile proof details.
7. Open Demo tab.

## Documentation decision

- `docs/submission/*`: added because Phase 8 is a submission package.
- `docs/PHASE_8_SUBMISSION_PACKAGE.md`: added to document final package scope.
- `README.md`: not changed by this patch to reduce merge risk.
- `CHANGELOG.md`: not changed by this patch because no app code changed.
- `ARCHITECTURE.md`: not changed because no runtime architecture changed.
- `TESTING_AND_RELEASE.md`: not changed because Phase 7 already added testing/release workflow.
- `SOLANA_SECURITY_AND_DATA_RULES.md`: not changed because no Solana behavior changed.
- `MOBILE_UI_UX_STANDARDS.md`: not changed because no UI rules changed.
- `VERSION`: not changed because Phase 8 adds docs/scripts only.
