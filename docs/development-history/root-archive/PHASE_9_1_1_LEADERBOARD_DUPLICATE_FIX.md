# Phase 9.1.1 — Leaderboard duplicate compatibility fix

## Why another duplicate could remain

Older app builds cleared a leaderboard wallet by writing `walletAddressShort = "No wallet"` but could leave the previous full `walletAddress` field in the same Firestore document. Phase 9.1 correctly deduplicated ordinary repeated anonymous UIDs, but it treated that stale full address as an active wallet and could therefore keep an old test identity visible.

## Fix

- `No wallet`, `Wallet not connected`, and `Disconnected` short-address markers are now authoritative disconnect tombstones.
- A tombstoned row is excluded even if a stale full wallet address remains in Firestore.
- Legacy shortened wallet strings stored in either the full or short field are normalized to the same first-4/last-4 fingerprint.
- Shortened legacy rows collapse into a unique full wallet row when the fingerprint maps unambiguously.
- The Ranks screen now shows each row's shortened wallet identity so two genuinely different Phantom/Solana accounts can be distinguished from an anonymous-UID duplicate.

## Important boundary

If two remaining rows show **different shortened wallet addresses**, they are genuinely different public wallet identities. Firebase Anonymous Auth alone cannot safely prove that two different wallets belong to one human, so the app must not merge different full wallets merely because their display names or device are the same.

## Documentation decision

- `CHANGELOG.md`: updated.
- `ARCHITECTURE.md`: no structural architecture change; Phase 9.1 wallet-as-public-identity design remains.
- `AGENTS.md`: no change.
- `TESTING_AND_RELEASE.md`: updated with the stale-disconnect regression case.
- `SOLANA_SECURITY_AND_DATA_RULES.md`: no security-boundary change; wallet remains a public identifier only.
- `MOBILE_UI_UX_STANDARDS.md`: updated because Ranks now displays a shortened wallet label for identity transparency.
- `VERSION`: bumped to `0.9.2-phase9.1.1` / versionCode 11.
