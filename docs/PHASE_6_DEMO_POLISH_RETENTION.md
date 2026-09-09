# Phase 6 — Demo Polish + Retention

## Goal

Make the working Radiant Rush flow easier to judge, record, and explain without adding risky wallet code.

Phase 6 does not change the trust model:

- No seed phrase collection.
- No private key storage.
- No fake token balance.
- No token transfer.
- No production reward authority in the APK.

## Added

- New `Demo` bottom navigation tab.
- New `DemoScreen` with a 3-minute judge walkthrough.
- Demo readiness card showing completed quest count, XP, streak, wallet state, signature state, memo state, and SKR state.
- Clear talking points for native Android, MWA, devnet memo proof, mainnet SKR scan, Firebase stickiness, and no-secret safety.
- Updated Welcome, Home, Quests, and Profile wording to point users/judges toward the demo flow.

## Expected demo behavior

- `Connect Wallet` opens Phantom or another MWA-compatible wallet.
- `Sign Daily Proof` opens Phantom for message signing.
- `Send Memo Proof` opens Phantom for a devnet Memo transaction.
- `Scan SKR Passport` stays inside Radiant Rush because it is read-only mainnet RPC.
- A zero-SKR wallet should show `Explorer` / `0 SKR`; this is honest and expected.

## Acceptance checks

- App builds with `:app:assembleDebug`.
- Bottom navigation shows the new Demo tab.
- Demo tab text does not clip on the test phone.
- Existing Phase 4/5 proof quests still work.
- No `.git` metadata or `google-services.json` is included in the patch.
