# Phase 13D — Mutual Friend Chat

## Scope

Phase 13D completes the core Radiant Circle social loop:

`Shake → Discover → Shared Sparks → Send Spark → Accept → Your Circle → Profile → Message`

The implementation is intentionally small for hackathon stability:

- private 1-to-1 text chat only;
- only ACCEPTED Circle relationships can read or send messages;
- 500-character message cap;
- real-time message updates while a thread is open;
- unread indicator/last-message preview when Circle is refreshed;
- remove connection;
- block connection;
- safety report reasons: Spam, Harassment, Unsafe behavior, Other;
- server-enforced minimum one-second send interval per thread;
- no images, files, voice, video, groups, stickers, or public chat.

## Firestore collections

- `circleEdges/{edgeId}` — existing deterministic relationship record.
- `circleChats/{edgeId}` — private thread summary/read state.
- `circleChats/{edgeId}/messages/{messageId}` — immutable messages.
- `circleReports/{reportId}` — write-only client safety reports for trusted/admin review.

## Trust and privacy rules

- Firebase UID remains the social identity; wallet addresses are not part of chat.
- Chat reads/writes require the deterministic Circle edge to remain `ACCEPTED`.
- Removing/blocking changes the relationship out of `ACCEPTED`, which stops message access/writes.
- Messages are immutable after creation.
- Reports intentionally contain no message body, wallet address, exact location, email, or private profile data.
- Firestore server time and rules enforce the message send floor; UI throttling is not the security boundary.

## Manual test

Use two devices/accounts that already have an accepted Spark:

1. Device A → Circle → accepted member → Message.
2. Send `Hello from A`.
3. Device B opens the same member/thread and sees the message without reinstalling or refreshing the app.
4. Device B replies; Device A sees the reply in the open thread.
5. Leave the thread on A, send from B, then reopen/refresh Circle on A and confirm the unread marker appears.
6. Open the thread on A and confirm the unread marker clears.
7. Attempt two sends less than one second apart; the second must fail closed/friendly.
8. Test Report and confirm success feedback.
9. Test Remove on a disposable test connection; messaging must stop.
10. Test Block on a disposable test connection; messaging must stop and the connection must disappear from Your Circle.

## Release gate

Do not commit/tag Phase 13D complete until:

- `git diff --check` passes;
- Android unit tests pass;
- APK assembles/installs;
- connected Android smoke tests pass;
- Firestore rules compile/deploy;
- two-device mutual-chat proof passes;
- remove/block/report behavior is manually verified.
