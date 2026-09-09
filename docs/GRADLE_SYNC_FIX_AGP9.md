# Gradle Sync Fix - AGP 9 Built-in Kotlin

## Problem fixed

Android Studio failed Gradle sync with this error:

```text
The 'org.jetbrains.kotlin.android' plugin is no longer required for Kotlin support since AGP 9.0.
Solution: Remove the 'org.jetbrains.kotlin.android' plugin from this project's build file: app/build.gradle.kts.
```

## Cause

Android Gradle Plugin 9 includes built-in Kotlin support. Applying `org.jetbrains.kotlin.android` in the app module now conflicts with AGP 9.

## Change made

Removed the old Kotlin Android plugin from:

- `app/build.gradle.kts`
- root `build.gradle.kts`
- `gradle/libs.versions.toml`

Kept the Compose compiler plugin:

```kotlin
alias(libs.plugins.kotlin.compose)
```

The Compose compiler plugin is still needed because this app uses Jetpack Compose.

## After pulling this fix

In Android Studio:

1. Click **File > Sync Project with Gradle Files**.
2. If Android Studio asks to install SDK/platform tools, allow it.
3. Build with:

```bash
./gradlew :app:assembleDebug
```

If there is no Gradle wrapper on your machine yet, use Android Studio's Gradle sync/build button first, or create the wrapper from Android Studio.
