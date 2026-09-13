# Phase 11F.5 — Badge Gallery Polish

## Why

The responsive Phase 11F.4 pass used adaptive minimum badge-card widths.
On compact phones this could collapse the Badges screen to one badge per row,
making the collection feel like a settings list rather than a badge gallery.

## Change

- Phones now use a two-column badge gallery.
- Medium/tablet widths use three columns.
- Wide layouts use four columns.
- Only extremely narrow layouts below 300dp fall back to one column.
- Badge tiles fill their grid cell consistently.
- Scaffold top/bottom padding is respected so the gallery remains clear of app bars.
- No badge data, unlock logic, Firebase behavior, wallet behavior, rewards, or competition rules change.

## QA

Check Badges on:
- Seeker at normal font size.
- Samsung test device at normal font size.
- At least one increased Android font-size setting.
- Confirm two badges per row on normal phone portrait width and no text/card overlap.
