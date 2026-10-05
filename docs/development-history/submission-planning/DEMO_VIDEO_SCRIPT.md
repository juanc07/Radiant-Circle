# Radiant Rush — Phase 10 3-Minute Demo Script

Target: 2:35–2:55. The key story is **real Solana proof → free game ticket → native skill run → collectible**.

## 0:00–0:15 — Hook

Say:

> Radiant Rush turns Solana Mobile participation into a daily game. Real wallet proofs earn free Rush Tickets, and those tickets unlock a native 20-second skill run with collectible rewards.

Show Welcome, then Today with the Radiant Run card/Vault.

## 0:15–0:40 — Native + wallet proof

Say:

> This is native Android with Jetpack Compose and Mobile Wallet Adapter. We never import private keys.

Show Connect Wallet and return from Phantom/MWA. Keep public wallet info only.

## 0:40–1:15 — Proof actions

Say:

> The daily loop uses a signed message plus a lightweight devnet Memo transaction as proof. The app also scans official SKR on mainnet by public address only.

Show Sign Daily Proof, Memo Proof, then SKR Passport. If the demo wallet has 0 SKR, explicitly say Explorer is the honest result.

## 1:15–1:35 — Chest + ticket bridge

Say:

> Completing proofs gives XP and free Rush Tickets. The Daily Radiant Chest adds bonus XP and two more tickets. Nothing here spends SOL, SKR, or earned XP.

Show the ticket count and chest state/reveal.

## 1:35–2:15 — Radiant Run wow moment

Tap **Play • 1 Rush Ticket**.

Say:

> Now the proof app becomes a game. This entire arcade field is drawn with Compose Canvas — no WebView, no game engine, and no external graphics are required.

Show:

- 3-2-1 countdown.
- Tap bright Radiant targets.
- Build at least a 5-hit combo so FEVER appears.
- Ignore red Corruption if possible; intentionally tap one only if you want to show the penalty.
- Let the 20-second timer finish.

## 2:15–2:35 — Capsule + collection

Say:

> The run opens a score-based capsule. New items fill the Radiant Vault; duplicates convert to Radiant Shards. Best score, collection, XP, and ticket balance persist in Firebase.

Show the capsule result, then return to Today and show Vault progress.

## 2:35–2:50 — Profile / ranking integrity

Say:

> Profile keeps public proof metadata and game progression. Public ranks collapse reinstall-created anonymous Firebase accounts by connected Solana wallet, so one wallet appears once.

Show Profile and Ranks briefly.

## 2:50–2:58 — Close

Say:

> Radiant Rush makes Solana Mobile activity something you can prove, play, collect, and come back to tomorrow.

End on Today / Radiant Vault.

## Recording notes

- Pre-fund the demo wallet with devnet SOL if the Memo transaction needs fees.
- Do not show recovery phrases/private keys.
- Practice the run once so you can trigger FEVER during the recording.
- Keep the phone in the orientation used by the submission APK.
- Do not claim a Memo is confirmed unless the explorer actually shows confirmation; the app saves the submitted transaction proof/signature.
