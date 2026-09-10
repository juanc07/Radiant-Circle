# Phase 10.1.1 — Juice Polish

This patch sharpens the feel of the existing native Compose Radiant Run without adding media assets or changing Solana/Firebase authority.

## Gameplay feedback

- Hit/miss feedback text now uses distinct pastel colors on high-contrast dark pills instead of relying on dark/default text.
- Successful-hit SFX now rise musically with combo using a semitone-based pitch curve, so the combo climb is clearly audible.
- x10+ successful combos add stronger particles, multi-ring shockwaves, and a short decaying screen shake.
- x10/x15/x20 milestones add a procedural combo-burst SFX while x5 still owns the FEVER transition.

## Reward reveal

- Reward explosions now scale with rarity: common rewards use a compact burst, while Epic+ rewards use more particles, farther travel, multiple shockwaves, and stronger procedural impact/glitter layers.
- Reward rarity/symbol presentation uses a pastel rarity accent.
- The lightweight procedural music bed remains active on the reward screen so the reveal does not feel sonically empty.

## Safety / scope

- No `.mp3`, `.wav`, `.ogg`, sprite, WebView, Unity, wallet, Solana transaction, SKR, leaderboard, or Firebase schema changes.
- Audio remains enhancement-only and failure-safe; gameplay must continue if the device audio path fails.

## QA

Run:

```bash
./gradlew :app:testDebugUnitTest
./gradlew :app:assembleDebug
./gradlew :app:connectedDebugAndroidTest
./gradlew :app:installDebug
```

On device, verify normal hit, PERFECT, MISS, corruption hit, x5 FEVER, x10/x15 combo milestones, and at least one normal reward plus one Epic-or-better reward when available.
