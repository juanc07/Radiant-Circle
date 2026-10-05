# Phase 13D.4.3 — Circle Interaction Polish

Changes in this patch:

- Clears stale `Opening <name>'s profile...` Circle banners after profile navigation succeeds or closes.
- Adds a distinct audible Shake-to-Discover cue.
- Raises foreground chat send/receive cue loudness and uses the media stream so the sounds are easier to hear on real devices while still respecting device media volume.
- Keeps the previous optimistic chat, typing, read state, avatar grouping, keyboard-safe composer, and centered send-button behavior unchanged.

Manual checks:

1. Circle -> tap a member -> return to Circle. No stale `Opening ... profile...` message should remain.
2. Shake the phone on Circle. A short discovery shimmer should play once when the shake is accepted.
3. Open chat on two phones. Send and receive cues should be clearly audible at a normal media volume.
4. Verify the composer remains directly above the keyboard and the send button remains vertically centered.

No Firestore rule change is required for this patch.
