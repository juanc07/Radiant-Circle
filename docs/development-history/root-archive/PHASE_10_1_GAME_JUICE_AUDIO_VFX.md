# Phase 10.1 — Radiant Run Game Juice: Procedural Audio + Native VFX

Date: 2026-09-10

## Goal

Improve the Phase 10 vertical slice without introducing an external game engine or a media-asset pipeline. Every gameplay sound is synthesized at runtime; gameplay visuals remain native Jetpack Compose.

## Runtime audio

`audio/ProceduralGameAudioEngine.kt` owns the Phase 10.1 audio boundary. It uses one background audio thread and one streaming Android `AudioTrack` to mix:

- procedural arpeggio/bass/kick background music;
- countdown and GO cues;
- normal Radiant-hit ping;
- brighter PERFECT-hit chord;
- low corruption buzz;
- miss cue;
- FEVER sweep;
- last-five-second urgency ticks;
- run-complete cue;
- capsule-open sweep;
- rarity-sensitive reward reveal.

No MP3, WAV, OGG, or remote audio download is required. The audio engine is enhancement-only: driver/init/write failures are caught and must not crash or block the game.

## Native Canvas VFX

`RadiantRunScreen.kt` adds:

- moving grid motion;
- pulsing targets;
- visible center ring for PERFECT timing/accuracy;
- deterministic radial particles on successful/corrupted impacts;
- expanding impact ring;
- corruption screen flash;
- PERFECT screen flash;
- FEVER rings;
- impact-scale feedback on the score/status chip.

## Gameplay adjustment

A tap inside 50% of the target radius is a `PERFECT` hit and adds +50 points on top of the existing hit/combo/FEVER score. This is a skill bonus only; it does not spend currency or alter wallet/Solana behavior.

## Audio lifecycle

- The procedural engine exists only while `RadiantRunScreen` is composed.
- BGM is active only during Countdown/Playing.
- FEVER and final-five-second intensity are state-driven.
- The screen owns a mute/unmute toggle.
- The engine releases its AudioTrack/thread from `DisposableEffect` when the screen leaves composition.

## Manual acceptance

1. Open Radiant Run and start a run.
2. Confirm countdown tones and procedural BGM are audible.
3. Hit a normal Radiant target; confirm bright SFX + particles.
4. Hit the center ring; confirm `PERFECT`, stronger SFX, larger particles, and +50 skill bonus.
5. Hit a red target; confirm low corruption sound, red particles/flash, score penalty, and combo reset.
6. Reach combo x5; confirm FEVER sound and stronger background presentation.
7. Reach five seconds remaining; confirm urgency ticks/faster music feel.
8. Finish; confirm run-complete/capsule/reward sounds.
9. Mute during a run; confirm game continues normally with no procedural audio.
10. Restart the app; confirm Phase 10 tickets/score/collection persistence is unaffected.

## Documentation decision

- CHANGELOG: updated — records Phase 10.1 production behavior.
- ARCHITECTURE: updated — documents the presentation-only procedural audio boundary.
- AGENTS: not updated — no contributor/agent workflow changed.
- TESTING_AND_RELEASE: updated — adds audio/VFX physical-device QA.
- SOLANA_SECURITY_AND_DATA_RULES: not updated — Solana/wallet/security behavior is unchanged.
- MOBILE_UI_UX_STANDARDS: updated — documents gameplay feedback/audio accessibility rules.
- VERSION: updated via `app/build.gradle.kts` to `13 / 1.0.1-phase10.1`.
