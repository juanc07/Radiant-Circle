# Phase 13D.6 — Typing Presence Reliability

## Problem
Typing presence could remain invisible even though the peer was typing. The receiver compared Firestore `updatedAt` (server time) against the phone wall clock with a very small freshness window. Device clock skew could therefore make every presence snapshot look stale. Presence write/listener errors were also intentionally silent, making diagnosis difficult.

## Fix
- Treat receipt of a peer `typing = true` Firestore snapshot as fresh presence.
- Expire typing locally after the existing stale timeout unless the peer refreshes it.
- Remove dependence on phone-vs-server wall-clock comparison.
- Signal typing immediately when the draft transitions blank → nonblank or back to blank.
- Keep the existing periodic typing refresh while text remains in the composer.
- Log presence read/write failures without breaking normal chat.

## Test
1. Deploy current Firestore rules if the Phase 13D.4 presence rules have not been deployed yet.
2. Open the same accepted Circle chat on two devices.
3. On Device A type without sending. Device B should show `A is typing…` within roughly a second.
4. Stop typing / clear the field. The indicator should disappear.
5. Type continuously for >10 seconds. The indicator should stay visible because presence refreshes.
6. Leave the chat. The peer indicator should clear.
