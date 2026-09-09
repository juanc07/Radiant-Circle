# Radiant Rush — Phase 1 Android App

Radiant Rush is a native Android Solana Mobile quest/streak app. Phase 1 delivers the mobile-first Kotlin + Jetpack Compose shell that later phases will connect to Firebase, Mobile Wallet Adapter, Solana RPC, and SKR tier logic.

## Phase 1 scope

This package includes:

- Native Android project structure.
- Kotlin + Jetpack Compose UI.
- Welcome screen.
- Bottom navigation.
- Today/Home screen.
- Quest board screen.
- Badge grid screen.
- Leaderboard preview screen.
- Profile readiness screen.
- Operating-system docs copied into `docs/operating-system/`.

This package intentionally does **not** fake these flows:

- No Firebase persistence yet.
- No Mobile Wallet Adapter wallet connection yet.
- No Solana transaction signing yet.
- No live SKR balance check yet.
- No live leaderboard yet.

Those start in Phase 2 and Phase 3.

## Build requirements

Recommended current stack used by this project:

- Android Studio with Android SDK 36 installed.
- JDK 17+.
- Gradle 9.x or Android Studio Gradle sync.
- Kotlin 2.4.20.
- Android Gradle Plugin 9.0.1.
- Jetpack Compose BOM 2026.04.01.
- AndroidX Activity Compose 1.12.4.
- AndroidX Core KTX 1.17.0.

The ZIP does not include a Gradle wrapper JAR because it was generated in a sandbox without Gradle installed. Open the project in Android Studio and let it sync, or generate a wrapper locally:

```bash
gradle wrapper --gradle-version 9.0.0
./gradlew :app:assembleDebug
```

If you already use Android Studio, you can usually open the folder and run the app from the IDE.

## Project layout

```text
RadiantRushPhase1Android/
  app/
    src/main/
      java/com/thinkblox/radiantrush/
        MainActivity.kt
        data/PhaseOneModels.kt
        ui/RadiantRushApp.kt
        ui/components/RushComponents.kt
        ui/screens/
        ui/theme/
      res/
  docs/operating-system/
  gradle/libs.versions.toml
  settings.gradle.kts
  build.gradle.kts
```

## Next phase

Phase 2 should add Firebase Authentication and Firestore while preserving the current UI contract:

1. Anonymous Firebase sign-in.
2. `users/{uid}` document.
3. Quest progress collection.
4. Firestore-backed leaderboard.
5. Loading/error/offline states.

Phase 3 should add Mobile Wallet Adapter. Do not store private keys in the APK.

## Android Studio Gradle sync note

This package has been updated for Android Gradle Plugin 9 built-in Kotlin support. The old `org.jetbrains.kotlin.android` plugin was removed because AGP 9 now provides Kotlin support directly. Keep `org.jetbrains.kotlin.plugin.compose` because this app uses Jetpack Compose. See `docs/GRADLE_SYNC_FIX_AGP9.md`.

## Android Studio AAR metadata note

This package now avoids Compose 1.12.x and AndroidX Core 1.19.x because those dependencies require API 37 and newer Android Gradle Plugin versions than the Phase 1 baseline. The dependency fix is documented in `docs/GRADLE_AAR_METADATA_FIX_COMPOSE_202608.md`.

