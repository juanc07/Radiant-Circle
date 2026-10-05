# Phase 13D.4 — Chat Polish

This patch is built on top of Phase 13D.3.

## Added

- Subtle local send and receive chat sounds.
- Optimistic outgoing bubble so sending feels immediate instead of waiting on Firestore round-trip.
- Composer clears immediately after send while the user can continue typing the next message.
- Latest outgoing status: `Sending…`, `Sent`, then `Read` once the other Circle member has read through that timestamp.
- Existing Circle-list unread badge remains the unread indicator outside the open conversation.
- Lightweight typing presence for accepted Circle members only.
- Typing presence is kept fresh every few seconds while text is being composed and is cleared on send/exit.
- Report reasons are now aligned as full-width rows instead of uneven button labels.

## Privacy / security

- Typing presence stores only: edge id, owner Firebase UID, boolean typing state, and server update timestamp.
- Presence is readable/writable only while the Circle edge is `ACCEPTED`.
- Users may write only their own presence row.
- Message body rules and 500-character limit remain unchanged.
- The existing one-second trusted Firestore anti-spam floor remains unchanged.
- `Read` is based on the peer's server-backed chat read timestamp. The UI intentionally does not claim a fake `Delivered` state because the app does not have device-delivery acknowledgements.

## Required deployment

Because Firestore rules add `circleChatPresence`, deploy rules before testing typing:

```bash
npx firebase-tools deploy --only firestore:rules --project radiant-rush-10a9c
```

## Manual acceptance test

1. Open the same accepted Circle chat on Device A and Device B.
2. On A, type but do not send. B should show `A is typing…` within a few seconds.
3. Send from A. The draft should clear and a `Sending…` bubble should appear immediately.
4. After Firestore accepts it, status becomes `Sent`.
5. B receives a subtle receive cue and sees the new message.
6. Once B has the chat open/read, A's latest outgoing status becomes `Read`.
7. Leave B outside the chat, send from A, refresh/open Circle on B and verify the existing unread/new-message indicator.
8. Open Report and verify Spam / Harassment / Unsafe behavior / Other are left-aligned and evenly spaced.
