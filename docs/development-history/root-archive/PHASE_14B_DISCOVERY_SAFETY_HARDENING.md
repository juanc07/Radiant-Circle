# Phase 14B — Shake to Discover Safety Hardening

## Scope

Safety/compliance polish only. No new social/game mode and no change to wallet or Radiant Cup logic.

## Changes

- Shake to Discover is now explicitly adult-only (18+) in the app UI.
- Discovery visibility defaults OFF on a fresh install until the user reviews the disclosure and confirms 18+.
- Adds **Appear in Shake Discovery** opt-in control.
- Enabling the control does not create background presence; presence is still created only after a physical Shake discovery action.
- Turning the control OFF deletes the user's current `circleDiscovery/{uid}` presence immediately.
- Keeps approximate foreground location only; exact coordinates are never written to discovery documents or shown to another member.
- Adds an in-app Privacy & Safety explanation and basic community-safety standards.
- Reinforces that private chat requires a mutual accepted Circle connection.
- Updates Home and Guide copy to describe the safer flow accurately.

## Deliberately not added

- No date-of-birth collection.
- No background location.
- No exact location storage.
- No automatic stranger chat.
- No Firebase rule expansion; existing owner-only discovery delete permission is sufficient.

## Device QA

1. Fresh install / cleared app data → open Circle → discovery switch is OFF.
2. Shake while OFF → disclosure appears; location permission is not requested first.
3. Tap **Not now** → discovery remains OFF.
4. Enable → confirm 18+ → discovery turns ON.
5. Shake → coarse location prompt/lookup → normal discovery still works.
6. Confirm `circleDiscovery/{uid}` is short-lived and contains no raw latitude/longitude.
7. Turn **Appear in Shake Discovery** OFF → current presence is deleted and discovered-member card clears.
8. Re-enable → no presence is created until the next shake.
9. Verify Send Spark → Accept Spark → chat still requires mutual connection.
10. Verify Block and Report still work from profile/chat.

## Build checks

Run from repository root:

```bash
./gradlew :app:testDebugUnitTest
./gradlew :app:assembleDebug
./gradlew :app:connectedDebugAndroidTest
```
