# Radiant Circle — Solana Mobile quests, competition, identity, and community foundation

**Radiant Circle** is the product/app. **Radiant Rush** is the native 20-second skill game inside the app.

Current product loop:

```text
Open Radiant Circle
        ↓
Daily quests + streak goals
        ↓
Earn Rush Tickets
        ↓
Play Radiant Rush
        ↓
Weekly Radiant Cup + personal stats
        ↓
Daily Radiant Chest + collection
        ↓
SKR Passport + public profile
```

## Naming contract

- User-facing app name: `Radiant Circle`
- In-app game name: `Radiant Rush`
- Weekly competition: `Weekly Radiant Cup`
- SKR identity/status: `SKR Passport`
- Android namespace/applicationId remains `com.thinkblox.radiantrush` intentionally for upgrade, Firebase, wallet, and persisted-data compatibility. Internal class/file names such as `RadiantRushApp`, `RadiantRunScreen`, and existing Firestore field names are legacy technical identifiers and are not user-facing branding.
- The existing MWA identity URI and `radiant-rush:daily-memo-proof` protocol string remain unchanged for compatibility; wallet display identity is now `Radiant Circle`.

## Security boundary

Radiant Rush scores are client-reported prototype competition data and cannot authorize real SKR payouts. Wallets remain the signing authority. No treasury/private key is embedded in the APK. Sponsored SKR payouts remain deferred until trusted verification and server-side payout authority exist.

## Standard verification

```bash
./gradlew :app:testDebugUnitTest
./gradlew :app:assembleDebug
./gradlew :app:connectedDebugAndroidTest
./gradlew :app:installDebug
```
