# Phase 14B.2 — Persistent Discovery Visibility

## Product rule

`Appear in Shake Discovery = ON` means the signed-in adult user remains discoverable until they explicitly turn the setting OFF. Shaking is used to **find** someone; it is no longer a visibility timer.

## Privacy model

- 18+ confirmation remains required.
- Only approximate/coarse matching is used.
- No raw latitude/longitude is written to `circleDiscovery`.
- No background location tracking is introduced.
- The last approximate area is refreshed when the user actively uses discovery.
- OFF immediately deletes `circleDiscovery/{uid}`.
- Private chat still requires mutual Spark acceptance.
- Block / report / remove controls are unchanged.

## Backend migration

New presence documents use `schemaVersion = 2` and `discoveryEnabled = true`, with persistent opaque area keys and no expiry fields. Firestore rules temporarily continue accepting the old short-lived schema so testers on the previous APK fail safely during transition.

## Required deployment

Unlike Phase 14B.1, this patch changes `firebase/firestore.rules`. Deploy the updated rules before testing the new APK:

```bash
firebase deploy --only firestore:rules --project radiant-rush-10a9c
```

## Device proof

1. Enable discovery and grant approximate location.
2. Confirm a `circleDiscovery/{uid}` v2 document exists.
3. Wait longer than two minutes; a second account can still discover the first account.
4. Shake from the enabled account to refresh its last approximate area and search for another member.
5. Turn discovery OFF.
6. Confirm `circleDiscovery/{uid}` is deleted immediately and the account can no longer be discovered.
