# Phase 13D.2 — Chat composer + first-message fix

## Fixed

- Removed whole-screen `imePadding()` from the chat route. Samsung/Android already resizes the app window for the IME in this build, so applying IME padding to the entire chat screen double-counted the keyboard height and pushed the composer far above the keyboard.
- The composer now stays at the bottom of the resized chat window, directly above the keyboard, with only navigation-bar protection.
- Reduced composer and send-button height to a compact Messenger-like proportion.
- Fixed first-message Firestore authorization: `circleChats/{edgeId}` reads now derive authorization from the accepted `circleEdges/{edgeId}` relationship. This lets the first-message transaction read a non-existent chat document before atomically creating the chat and its first message.
- Removed/blocked Circle relationships still cannot read or send messages because `isAcceptedCircleEdge(edgeId)` must remain true.

## Required after patch

Deploy Firestore rules before testing the first message:

```bash
npx firebase-tools deploy --only firestore:rules --project radiant-rush-10a9c
```

Then test A → B first message, B → A reply, keyboard composer positioning, block/remove behavior, and unread state.
