# Phase 13D.1 — Circle Profile + Chat UI Fix

## Purpose
Polish the Phase 13D social experience after device QA found the Circle member profile too box-heavy and the active chat composer difficult to use with the keyboard open.

## Changes
- Removes the duplicate in-page back button; the app bar remains the single Back affordance.
- Reworks the Circle member profile into a compact identity card, one primary Message action, and small icon actions for Remove / Block / Report.
- Converts Shared Sparks into compact horizontally scrolling chips.
- Consolidates public About fields into one compact section with Show more / Show less.
- Simplifies active chat to a messenger-style layout: compact safety actions, message bubbles, and a composer pinned above the keyboard.
- Hides the main bottom navigation while an active chat is open.
- Uses the member name as the chat app-bar title.
- Makes first-thread creation preserve the accepted Circle edge's member list, improving compatibility with existing accepted connections.
- Loosens only the chat-thread member ordering check in Firestore rules; both members must still exactly belong to the accepted Circle edge.

## Security preserved
- Only ACCEPTED Circle edges can read or write chat.
- Messages remain immutable.
- 500-character limit remains.
- One-second server-side send floor remains.
- Remove / Block ends the accepted relationship and therefore chat access.
- Reports remain write-only and do not store wallet, location, or message body.

## Required deployment
After applying this patch, deploy Firestore rules before testing first-message creation:

```bash
npx firebase-tools deploy --only firestore:rules --project radiant-rush-10a9c
```

## Device QA
1. Open an accepted Circle member profile.
2. Verify Message is the primary action and Remove / Block / Report are compact icon actions.
3. Verify Shared Sparks and About are compact and readable on a small phone.
4. Open chat; bottom navigation must disappear.
5. Tap the composer and verify it stays directly above the keyboard.
6. Send the first message, reply from the second account/device, and confirm bubbles update in real time.
7. Close keyboard and verify composer returns to the bottom without leaving a large blank gap.
8. Verify Report and Block dialogs still work.
