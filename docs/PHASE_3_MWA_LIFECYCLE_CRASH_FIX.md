# Phase 3 MWA Lifecycle Crash Fix

## Problem

The app built successfully, but crashed immediately at startup on a physical Android phone after Phase 3.

The Logcat crash was:

```text
java.lang.IllegalStateException: LifecycleOwner com.thinkblox.radiantrush.MainActivity is attempting to register while current state is RESUMED. LifecycleOwners must call register before they are STARTED.
```

The stack trace pointed to:

```text
com.solana.mobilewalletadapter.clientlib.ActivityResultSender.<init>
com.thinkblox.radiantrush.solana.MobileWalletRepository.<init>
com.thinkblox.radiantrush.ui.RadiantRushAppKt.RadiantRushApp
```

## Cause

`MobileWalletRepository` creates `ActivityResultSender`, and `ActivityResultSender` registers an Activity Result launcher internally.

The repository was previously constructed from inside the Compose tree using `remember(activity)`. On the tested device, that composition happened after the Activity was already `RESUMED`, which is too late for Activity Result registration.

## Fix

`MobileWalletRepository` is now created in `MainActivity.onCreate()` before `setContent { ... }`.

The Compose app now receives the already-created repository:

```kotlin
RadiantRushApp(walletRepository = walletRepository)
```

This preserves the Phase 3 architecture boundary:

- `MainActivity` owns Android lifecycle-sensitive objects.
- `MobileWalletRepository` owns Mobile Wallet Adapter actions.
- Compose owns UI state and user actions.
- Firebase repository still owns profile/quest persistence.

## Files changed

```text
app/src/main/java/com/thinkblox/radiantrush/MainActivity.kt
app/src/main/java/com/thinkblox/radiantrush/ui/RadiantRushApp.kt
docs/PHASE_3_MWA_LIFECYCLE_CRASH_FIX.md
docs/operating-system/ARCHITECTURE.md
docs/operating-system/CHANGELOG.md
```

## Test checklist

After applying this patch:

```bash
./gradlew :app:assembleDebug
```

Then run on the phone and verify:

```text
App no longer crashes immediately on startup
Firebase status/profile still loads
Daily Firebase check-in still works
Connect Wallet opens wallet flow or shows no-wallet message
No private key or seed phrase is requested
```
