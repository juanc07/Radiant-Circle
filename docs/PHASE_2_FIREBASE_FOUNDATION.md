# Phase 2 — Firebase Foundation

## Goal

Phase 2 adds real cloud persistence without pretending that wallet or Solana flows exist yet.

The app now supports:

- Firebase dependency foundation.
- Safe build behavior before Firebase config is added.
- Anonymous Firebase Auth when `app/google-services.json` is present.
- Firestore user profile creation.
- Firestore daily check-in proof for app progress only.
- XP, level, streak, badge state, and leaderboard rows in Firestore.

Wallet authorization starts in Phase 3. On-chain Solana proof starts in Phase 4. SKR balance detection starts in Phase 5.

## Firebase setup required for real sync

1. Open Firebase Console.
2. Create a Firebase project.
3. Add an Android app using package name:

```text
com.thinkblox.radiantrush
```

4. Download `google-services.json`.
5. Place it here:

```text
app/google-services.json
```

6. Enable Authentication > Sign-in method > Anonymous.
7. Create a Cloud Firestore database.
8. Paste the rules from:

```text
firebase/firestore.rules
```

9. Sync Gradle and build again.

## Build behavior

The Google services Gradle plugin is loaded in the root build file but applied in the app module only when `app/google-services.json` exists. This keeps the project buildable before Firebase is configured.

```kotlin
if (file("google-services.json").exists()) {
    apply(plugin = "com.google.gms.google-services")
}
```

## Firestore structure

```text
users/{uid}
  displayName
  walletAddress
  walletStatus
  skrTier
  xp
  level
  currentStreak
  longestStreak
  lastQuestDate
  phase
  createdAt
  updatedAt

users/{uid}/completedQuests/{questId_date}
  questId
  questTitle
  date
  proofType
  xpEarned
  createdAt
  updatedAt

leaderboard/{uid}
  displayName
  walletAddressShort
  xp
  level
  currentStreak
  longestStreak
  skrTier
  updatedAt
```

## Security note

This is a client-side hackathon MVP foundation. It is acceptable for Phase 2 because no real token reward is issued here.

Do not use this client-side XP write path for real-money rewards, minting, prize claims, or valuable SKR rewards. Those must be verified server-side or by an on-chain program in a later phase.

## Acceptance checks

- App still builds without `google-services.json`.
- App shows a clear Firebase setup-needed state when config is missing.
- After config is added, app signs in anonymously.
- A Firestore `users/{uid}` document is created.
- Tapping `Save Firebase Check-In` creates a completed quest document.
- XP, level, streak, badges, and leaderboard update after save.
- Completing the same daily check-in twice does not add more XP.
- Wallet and Solana actions remain locked and honest.
