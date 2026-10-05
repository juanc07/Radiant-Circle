# Phase 1 Native Android App

## Goal

Ship the first native Android UI shell for Radiant Rush. The app must feel mobile-first before Firebase and Solana integrations are added.

## What this phase delivers

- Kotlin Android app.
- Jetpack Compose UI.
- One-hand bottom navigation.
- Welcome screen.
- Home / Today screen.
- Quest board screen.
- Badge grid screen.
- Leaderboard preview screen.
- Profile readiness screen.
- Clear placeholder states for Firebase, Mobile Wallet Adapter, Solana proof, and SKR.

## What this phase must not do

- Must not use WebView as the product surface.
- Must not fake successful wallet connection.
- Must not fake successful Solana transactions.
- Must not include private keys, seed phrases, mint authority, or reward authority.
- Must not claim live Firebase or Solana functionality before it exists.

## Acceptance checks

- App opens on a real Android device.
- User can preview all primary screens.
- UI works on small portrait screens.
- Buttons are large enough for touch.
- Text wraps or adapts without major clipping.
- Wallet/Firebase/Solana placeholders are labeled honestly.
- APK can be built once local Android SDK/Gradle are available.

## Phase 2 handoff

The Phase 2 developer should keep the UI shell and replace static preview content with Firebase-backed state. Do not redesign the app before the persistence loop works.
