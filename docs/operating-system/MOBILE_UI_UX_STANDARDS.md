## Phase 11E — reward reveal UX rules

- Reward actions must respond immediately; do not add a fixed sleep before starting the real save/network work.
- A short minimum reveal pose is allowed only to make feedback legible. If the backend is slower, reveal as soon as the actual result arrives with no additional presentation delay.
- Animate chest art internally; do not shake/scale the surrounding card or move neighboring Home content.
- Required reward information (rarity, title, XP, tickets, SKR bonus) must wrap and remain readable on compact phones and large font scale. Do not use required-text ellipsis.
- Keep the established soft/pastel Radiant identity. Higher rarity should increase light/particle density, not replace the app with saturated arcade colors.
- Keep reward summary centered against the full visible card width.

## Phase 11D — Weekly Cup mobile layout rules

- Weekly Cup headline, countdown, personal rank/PB, reward preview, sponsor copy, and podium content must measure against full card width and remain centered.
- Top 3 uses stacked cards on phone layouts rather than narrow fixed three-column desktop composition.
- Required sponsor/reward text wraps; do not ellipsize prize labels, sponsor names, rank, or countdown.
- The standings list remains vertically scrollable and reachable on small phones and large Android font scale.
- Sponsor state must use concise player-facing copy; security/payout architecture explanations stay in technical documentation.


## Phase 11C.3 no-cut/no-bleed text rule

- Treat normal Android phone widths up to 480dp as compact unless a component has stronger local evidence that a multi-column layout is safe.
- Tier names and other single-token values must never break across lines (for example `Explore` + `r`) and must never be clipped. Prefer stacking first; if a single-line value still overflows, reduce only that value's display size until it fits.
- Multi-word required values may wrap to additional lines; do not use ellipsis to hide required state.
- Test compact phone width, Seeker/Samsung-class widths, large font scale, and a larger tablet/foldable width before accepting new cards or buttons.
- Animated cards must not change layout geometry frame-to-frame in ways that look like jitter. Prefer drawing glow/particles inside a stable box and use short damped transforms for intentional shake.

## Phase 11C.3 Radiant Run visual language

- Preserve the established pastel game palette. Do not hard-code saturated green/gold replacements for the main Radiant target merely to explain hit rules.
- Only the actual target may use target-like circular affordances. FEVER/background decoration must use a clearly non-target shape or placement.
- Keep target instructions concise: Radiant = hit, center = PERFECT, red Corruption = avoid.

## Phase 11C Passport and ticket clarity

- Label the SKR source as **Mainnet Liquid SKR**; do not combine it with an unverified staked amount.
- If staking data is unavailable, display `Not counted` / `Not verified` rather than zero-as-fact or a fabricated estimate.
- Show standard and `SKR casual` ticket balances separately anywhere entry type matters. Copy must state that SKR bonus tickets cannot fund ranked attempts.
- Before a run, the Competition row remains the authority for `RANKED` versus `CASUAL`; having SKR must never visually imply ranked advantage.
- Holder frame/aura/badge treatments are cosmetic status only and must not imply an NFT, token transfer, guaranteed prize, or on-chain ownership.
- Passport refresh remains available after today’s quest is completed, is a read-only in-app action, and must not unexpectedly open a wallet.

## Phase 11B.1 competition-state clarity

Radiant Run must never leave the user guessing why a score did or did not enter the ranked boards. Before play, the Competition row must explicitly show one of: `RANKED`, `CASUAL • connect wallet`, or `CASUAL • ranked attempts used`. Empty Weekly/All-Time states must likewise explain when wallet connection is required. Ranked-attempt copy should state that the allowance is shared by the connected wallet across devices.

## Phase 10 gameplay UI rules

- Radiant Run must remain playable on small Android screens with large touch targets; gameplay cannot depend on tiny text or precision taps.
- Radiant and Corruption targets must differ by both shape/marking and semantic copy, not color alone.
- Score, combo, and timer stay visible throughout the run.
- Haptics are feedback, not the only signal.
- The game must allow exiting without opening a wallet or losing previously earned XP. A ticket is consumed only when a completed run result is successfully persisted.
- Locked collectibles use clear silhouettes/question marks; never truncate names into meaningless ellipses on small screens.
- Native Compose-drawn visuals are acceptable production UI for this phase; external art assets are optional polish, not a dependency.

# RadiantSolanaHackatonAndroid — Mobile UI/UX Standards

## Mobile-first goal

The app must work clearly on real Android phones, not only on desktop previews or large emulator screens. Required text must not bleed, clip, collide, hide buttons, or depend on ellipsis to communicate critical meaning.

## Core layout rules

- Design for small Android phones first, then scale up to tablets/foldables.
- Respect safe areas, navigation bars, status bars, keyboard insets, display cutouts, and gesture navigation.
- Keep primary actions reachable by thumb.
- Avoid placing critical actions only at the very top of the screen.
- Keep loading and error states inside the same layout ownership area as the content they replace.
- Avoid separate phone/tablet implementations unless the content truly needs different information architecture.
- For cards or popups that are visually designed as centered content, the inner content container must occupy the available card width before applying `Alignment.CenterHorizontally`; centered text should also use the available width so wrapping remains optically centered.

## Text rules

- Required information must fit without clipping.
- Do not rely on ellipsis for wallet addresses, transaction status, warning copy, seed/security warnings, amounts, or primary actions.
- Use short player/user-facing copy.
- Put long explanations in expandable detail areas or scrollable sections.
- Test with Android large font/accessibility text settings.
- Use address shortening deliberately, for example `ABCD...WXYZ`, with copy/view-full affordance when needed.

## Buttons and actions

- Every button needs a clear owned rectangle and touch target.
- Disable actions only when the reason is visible or obvious.
- Avoid duplicate rapid submits by blocking while an action is in progress.
- Destructive or irreversible actions require confirmation.
- Wallet signing buttons must not be hidden behind vague labels like `Continue` when the next step asks for signing.

## Solana-specific UI states

Every wallet/transaction feature must show accurate states:

- Wallet disconnected.
- Connecting.
- Connected.
- Wallet rejected/cancelled.
- Building transaction.
- Waiting for wallet approval.
- Signed.
- Submitted.
- Pending confirmation.
- Confirmed.
- Failed.
- RPC unavailable/offline.

Do not collapse these into one generic loading spinner.

## Screen standards

### Home / landing

- Explain the core value in one sentence.
- Show network/demo mode clearly.
- Provide one primary action.
- Do not overload the first screen with technical jargon.

### Wallet connect

- Explain why connection is needed.
- Show selected network.
- Handle no-wallet-installed gracefully.
- Provide retry and cancel.

### Asset/account screen

- Show last refreshed time when data can become stale.
- Pull-to-refresh or visible refresh action should be available.
- Empty state should explain what the user can do next.

### Transaction confirmation screen

- Use a structured summary before wallet approval.
- Show amount, destination, network, and risk information when relevant.
- After signing, show submitted/pending/confirmed/failure accurately.

### Settings/debug screen

- Keep dev/demo flags separate from normal user settings.
- Debug controls must show environment and build variant.
- Production builds must hide or disable unsafe debug controls.

## Accessibility rules

- Support screen reader labels for icon-only controls.
- Keep color from being the only source of meaning.
- Maintain contrast for text and buttons.
- Support large font settings.
- Avoid tiny clickable links or addresses.
- Keep important status changes announced or visibly persistent.

## Visual polish rules

- Prefer clean spacing over dense dashboard layouts.
- Use consistent cards, radius, spacing, and icon size.
- Avoid heavy effects that slow weak phones.
- Images/icons should preserve aspect ratio.
- Missing optional art should fall back to a deliberate placeholder, not a random asset.

## Performance UI rules

- Avoid long startup blockers.
- Use skeletons or lightweight loading states for slow network reads.
- Do not fetch every chain/resource item at startup if the first screen does not need it.
- Use pagination or lazy lists for long account/token/history views.
- Avoid excessive recomposition from rapidly polling RPC status.

## Acceptance checklist

Before a UI change is accepted:

1. Small phone portrait checked.
2. Small phone landscape checked when supported.
3. Large font/accessibility text checked.
4. Keyboard open/close checked if the screen has inputs.
5. Slow network/loading state checked.
6. Error/offline state checked.
7. Final row/action in scrollable areas is reachable.
8. No required text clips, bleeds, collides, or hides behind ellipsis.
9. Wallet/transaction stages are named accurately.
10. No UI-only state pretends to be authoritative chain state.

## Responsive text and button rules

- Primary buttons must use adaptive labels with full, compact, and tiny variants when text can exceed the available width.
- Small screens and large Android font scale must prefer shorter labels such as `Sign`, `Memo`, `Connect`, `Refresh`, or `Done` instead of forcing long labels into one line.
- Primary action text may wrap to two lines inside a taller button; it must not shrink into unreadable single letters.
- Fixed button heights are not allowed for reusable action components. Use minimum heights that grow with content.
- Bottom navigation labels must use short names on compact screens and may hide unselected labels on very small screens.
- Proof/status chips must use compact names such as `Cloud`, `Wallet`, `Sign`, `Memo`, and `SKR` on tiny screens.
- Large-font testing is required after every screen that adds or modifies buttons, chips, bottom navigation, or primary action cards.

## Phase 9 reward-loop UX rule

Reward screens must clearly say when no wallet popup is expected. The Daily Radiant Chest must use no-loss wording: no XP bet, no XP loss, no SOL/SKR transfer. Chest button text must be short enough for small phones: `Open Chest`, `Opening…`, `Claimed`.


## Rank identity transparency

- Public rank rows should show a compact shortened wallet label (`AAAA…BBBB`) beneath the display name.
- Never expose private keys, auth tokens, or secret material. The public wallet label is for identity transparency and duplicate-rank diagnosis only.
- Keep the wallet label single-line and non-truncating on small screens; the shortened form is already bounded.


## Phase 10.1 gameplay feedback and audio rules

- Every high-frequency gameplay action should have immediate visual feedback; do not depend on audio alone.
- Positive, PERFECT, corruption, and miss outcomes must remain distinguishable by shape/text/motion in addition to color.
- Procedural SFX/BGM are enhancement-only and must have a visible mute control while the game screen is active.
- Impact flashes must stay brief/subtle enough to avoid obscuring the next target; avoid full-screen strobe patterns.
- Keep particle counts bounded and deterministic so weak phones are not punished by unbounded allocations/effects.
- FEVER/final-seconds escalation may increase motion/audio intensity, but core tap targets and timer must remain readable.

## Phase 10.1.1 gameplay feedback additions
- Fast transient gameplay messages must use readable high-contrast semantic accents; hit, perfect, miss, and corruption feedback may use distinct restrained pastel colors rather than low-contrast default text.
- Screen shake is reserved for high-value milestones (x10+ combo / major reward) and must decay quickly so it does not interfere with target acquisition.
- Reward celebration intensity should scale with rarity instead of giving common and high-rarity drops identical visual weight.

## Phase 11B competition UI decision

The Ranks destination now uses horizontally scrollable tabs (`Run Weekly`, `Run All-Time`, `My Stats`, `XP`) so small Android screens do not compress four competing labels into clipped fixed-width tabs. Run rows keep score visually dominant while combo/PERFECT/run-count metadata may wrap to two lines. The Radiant Run briefing explicitly shows Ranked vs Casual status and daily gameplay-XP progress before the player spends a ticket. Required security/fairness copy uses wrapping text rather than ellipsis.


## Phase 11C.1 — staking/Passport responsiveness

- SKR liquid, active-stake, unstaking, and Stake Boost status must use the existing responsive screen/card primitives (`LazyColumn`, `fillMaxWidth`, responsive padding/minimum button height).
- Do not introduce fixed screen widths/heights for staking cards or Passport actions.
- Long wallet/staking status copy must wrap and expand vertically; required information must not be replaced with `...` on compact phones.
- Staking state is secondary information: keep primary values concise and move explanation into wrapping helper text.
- Verify new/changed Passport UI on at least a compact Android viewport and Seeker-sized hardware when available; also sanity-check a larger phone/tablet/emulator width.


## Phase 11C.2 — product copy and orphan-text rule

- Normal user screens must speak to the player, not to the developer or judge. Do not expose phase numbers, framework/rendering implementation, storage internals, "no media assets required" commentary, prototype/payout-authority explanations, or demo-script directions in routine UI. Put those details in docs/submission material instead.
- Single-token values such as `Explorer` must not split into an orphan final character on a new line. Give the value more width (stack compact layouts) and use one-line/no-soft-wrap treatment for single-token pills/metrics.
- On compact screens or large font scale, prefer vertical stacking over squeezing icon + title + long value + action into one row.
- Required product copy may wrap, but must not be truncated with ellipsis.
- Reward moments may temporarily prioritize visual space, but controls must remain full-width and at least the responsive minimum touch height.
- Manual QA for every visible UI patch must include one compact Android viewport, Seeker-sized hardware, and a larger phone with increased font scale.

- Single-token tier/status values (for example `Explorer`) must stay intact; on narrow/large-font layouts reduce base typography or stack the containing layout rather than orphaning one character on a second line.
- Multi-item game legends must stack on compact/large-font layouts instead of relying on one horizontal row.

## My Stats responsive metrics

Competition metric rows must not force a label and value onto one horizontal line on compact displays or large font scales. Stack them vertically in those conditions, allow normal wrapping, and never clip required values such as PB score, PERFECT hits, ranked-run counts, or gameplay-XP progress.

## Phase 11C.5 badge and crest identity rules

- Achievement badges must use scalable vector/Compose-drawn marks rather than generic success/check icons for every achievement.
- Badge glyphs must remain recognizable at compact phone sizes and must not force the title or description outside the card.
- Tier and Passport identity blocks must stack on compact screens or large font scale; do not squeeze icon + tier + balance + perk status into one fixed-height row.
- Required badge/Passport text must wrap and expand vertically. Do not use ellipsis for titles, tier labels, eligible SKR values, or perk status.
- Visual identity may be richer than surrounding cards, but it must preserve the established pastel Radiant Rush palette and Material theme contrast.
