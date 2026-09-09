# Radiant Rush Android

Radiant Rush is a native Android app for the Solana Mobile hackathon.

Product direction:

> Daily quests for Solana Mobile users: cloud-saved streaks, badges, leaderboard progress, then Mobile Wallet Adapter, on-chain proof, and SKR-powered status.

## Current phase

```text
Phase 2 — Firebase Foundation
```

Phase 2 adds Firebase Auth + Firestore foundations while keeping wallet and Solana features honestly locked until the next phases.

## What works now

- Native Android app, not a WebView wrapper.
- Kotlin + Jetpack Compose mobile-first shell.
- Welcome, Today, Quests, Badges, Leaderboard, and Profile screens.
- Firebase dependencies are present.
- App still builds before Firebase is configured.
- Anonymous Auth and Firestore sync work after adding `app/google-services.json`.
- Daily Firebase check-in saves progress, XP, streak, badges, and leaderboard state.

## What does not exist yet

- Mobile Wallet Adapter connection — Phase 3.
- Solana signed messages — Phase 3/4.
- Solana memo transaction proof — Phase 4.
- SKR token balance detection — Phase 5.
- Real reward distribution — later backend/on-chain verified phase.

## Firebase setup

1. Create a Firebase project.
2. Add Android app package:

```text
com.thinkblox.radiantrush
```

3. Download Firebase `google-services.json`.
4. Place it at:

```text
app/google-services.json
```

5. Enable Firebase Authentication > Anonymous.
6. Create Cloud Firestore.
7. Paste rules from:

```text
firebase/firestore.rules
```

8. Sync Gradle and build.

The app intentionally does not commit `app/google-services.json`. Use `app/google-services.json.example` only as a shape reference.

## Build

```bash
./gradlew :app:assembleDebug
```

Debug APK path:

```text
app/build/outputs/apk/debug/app-debug.apk
```

## Git safety

Do not commit generated Gradle/Android Studio cache folders:

```text
.gradle/
build/
app/build/
caches/
daemon/
kotlin-profile/
native/
wrapper/
android/
```

The required Gradle wrapper is:

```text
gradle/wrapper/
```

## Docs

Main operating-system docs live in:

```text
docs/operating-system/
```

Phase-specific docs:

```text
docs/PHASE_1_NATIVE_ANDROID_APP.md
docs/PHASE_2_FIREBASE_FOUNDATION.md
```
