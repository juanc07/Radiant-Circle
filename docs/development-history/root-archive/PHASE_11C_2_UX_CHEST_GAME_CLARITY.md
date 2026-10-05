# Phase 11C.2 — Product Copy, Juicy Chest, and Radiant Run Clarity

## Goal

Remove developer-facing implementation commentary from normal product UI, harden compact-screen text behavior, make the Daily Radiant Chest feel like a real reward moment, and remove ambiguous light-blue circular game visuals that looked tappable.

## Product-copy pass

Normal user screens now focus on actions, rewards, progress, and gameplay. Internal implementation terms such as phase numbers, Compose/Canvas/media-asset commentary, Firebase internals, judge instructions, prototype payout language, and architecture explanations are not shown as routine product copy.

The former Demo tab is presented as a concise user Guide.

## Mobile-first text behavior

- Compact / large-font profile cards stack icon/title above values and helper text.
- Single-token labels such as `Explorer` are kept on one line instead of producing an orphan final character on the next line.
- Buttons continue to use width-aware compact/tiny labels.
- Required copy wraps normally; no required information relies on ellipsis.

## Daily Radiant Chest

The Home chest now renders an actual chest with Compose Canvas geometry and has a visible state sequence:

1. ready aura / breathing glow;
2. shake + charge;
3. light beam;
4. lock/lid burst;
5. particles + shockwaves;
6. haptic impact;
7. procedural charge/open/reveal SFX and temporary music;
8. rarity-colored reward reveal.

The claim operation is deliberately delayed for 1.8 seconds after entering the Opening state so the presentation cannot be replaced immediately by the persisted Claimed state. The delay is presentation-only; Firebase remains reward authority.

## Radiant Run clarity

- Valid targets remain green during normal play and FEVER.
- Red targets remain Corruption and should be avoided.
- FEVER no longer draws cyan circular rings that can be mistaken for tappable targets; it uses a rectangular gold screen-state glow instead.
- The tappable radius is slightly more forgiving.
- The header no longer describes the rendering/audio implementation.

## Security / economy

No Solana, wallet, SKR, staking, ranked-attempt, score, chest-reward, or payout authority changed in this patch. UI animation, audio, haptics, and the 1.8-second reveal delay are presentation only.

## Version

`versionCode = 19`

`versionName = "1.1.4-phase11c2"`


## Final responsive/juice refinements

- Long single-token metric values such as `Explorer` use a smaller compact typography size instead of splitting the final character onto another line.
- Compact/large-font Radiant Run legends stack vertically so required hit instructions do not collide or clip.
- Claimed chest rewards now lift/fade/scale into view after the burst, in addition to the lid, beam, particles, shockwaves, haptics, and procedural reveal cue.
- Player-facing proof labels avoid implementation names such as Firebase/MWA/Devnet; internal source identifiers remain unchanged for compatibility.
