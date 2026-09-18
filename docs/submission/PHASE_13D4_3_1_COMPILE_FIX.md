# Phase 13D.4.3.1 — Compile Fix

Fixes the Phase 13D.4.3 Kotlin compile regression in `RadiantRushApp.kt`.

`CircleUiState.message` is a non-null `String`, so stale profile-loading messages are now cleared with an empty string rather than `null`. The Circle screen already renders the banner only when the message is non-blank.

No chat logic, sound assets, Firestore rules, or interaction behavior changed.
