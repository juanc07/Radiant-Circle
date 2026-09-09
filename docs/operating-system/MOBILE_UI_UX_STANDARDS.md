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
