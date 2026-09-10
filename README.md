# Radiant Rush — Phase 10.1 Game Juice / Procedural Audio + VFX

Radiant Rush is a native Android / Jetpack Compose Solana Mobile quest game.

Phase 10 proved the playable loop. Phase 10.1 makes each run feel more like an arcade game without adding external audio/image packs:

```text
Solana/Firebase quest
        ↓
Earn free Rush Tickets
        ↓
20-second Radiant Run
        ↓
Procedural BGM + tap SFX + impact particles
        ↓
PERFECT / combo / FEVER feedback
        ↓
Capsule + collectible reward
```

## No imported game media required

Radiant Run still uses no PNG/JPG sprites and Phase 10.1 adds no MP3/WAV files. The field and hit effects are drawn with Jetpack Compose Canvas/animation APIs. Music and SFX are synthesized at runtime into PCM samples and mixed through Android `AudioTrack`.

## Phase 10.1 additions

- Procedural synth background music during countdown/gameplay.
- Runtime-generated SFX for countdown, GO, normal hit, PERFECT hit, corruption hit, miss, FEVER, final-five-second urgency, run complete, capsule open, and rarity reveal.
- In-game mute/unmute button.
- Pulsing targets and moving native Canvas grid.
- Hit particle bursts for Radiant/PERFECT/Corruption impacts.
- Corruption red impact flash and PERFECT white impact flash.
- FEVER background rings and faster procedural music intensity.
- Last-five-seconds music/tick intensity.
- New center-hit PERFECT zone with a small +50 skill bonus.
- AutoMirrored back icon cleanup on the game screen.
- Restores the project README after the earlier test-only patch READMEs.

## Safety / economy boundary

Audio/VFX are presentation-only. Rush Tickets, collectibles, Radiant Shards, and XP remain app-only progression. Phase 10.1 does not add SOL/SKR spending, token transfers, paid random rewards, or wallet calls from the mini-game.

## Test

```bash
./gradlew --stop
./gradlew :app:testDebugUnitTest
./gradlew :app:assembleDebug
./gradlew :app:connectedDebugAndroidTest
```

Then install on the connected phone:

```bash
./gradlew :app:installDebug
```

Physical QA should confirm sound is audible, mute works, no crackling/crash occurs, impact particles are visible, FEVER escalates correctly, the last-five-second urgency is noticeable, and game progression still persists.

## Version

`versionCode = 13`

`versionName = 1.0.1-phase10.1`
