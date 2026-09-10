## Phase 10 non-economic game progression

Rush Tickets, Radiant Shards, capsule results, run scores, and collectibles are application progression only. They do not represent SOL, SKR, SPL tokens, NFTs, redeemable prizes, or a claim on value. Starting or completing Radiant Run must never invoke Mobile Wallet Adapter. XP is not wagered.

The Phase 10 score is client-generated and therefore not suitable as an authority for valuable rewards. A future competitive/rewarded mode must add trusted verification before assigning anything economically valuable. Existing Solana boundaries remain unchanged: devnet proof transaction/signing and mainnet read-only SKR lookup.

# RadiantSolanaHackatonAndroid — Solana Security and Data Rules

## Security goal

The app must be safe by default. Hackathon speed is not an excuse to store secrets, fake balances, hide transaction risk, or make irreversible actions unclear.

## Non-negotiable wallet rules

- Never ask the user for a seed phrase.
- Never store private keys.
- Never log private keys, seed phrases, signing payloads that expose secrets, auth tokens, or sensitive wallet metadata.
- Never silently sign transactions.
- Every signing request must be user-initiated or clearly tied to an active user action.
- The wallet remains the signing authority.
- A rejected wallet request is a normal user decision, not an error to fight.

## Network and environment rules

Supported environments must be explicit:

- `devnet` for early hackathon testing.
- `staging/demo` for judged demos.
- `mainnet-beta` only after approval and clear risk review.
- `custom` only if the user/developer deliberately configures it.

UI must clearly show when the app is using devnet, mock/demo data, or a non-production backend.

## Balance and token rules

- Do not show cached balances as current truth.
- Display loading, stale, error, and confirmed states separately.
- If showing a balance from local cache, label it as last known data when relevant.
- Do not use UI-only balance math as final authority for spend/send decisions.
- Before a send/swap/mint/claim action, refresh or validate the source data required for the transaction.

## Transaction rules

Every transaction flow should have these stages:

1. User intent.
2. Validate required inputs.
3. Build transaction or message.
4. Present a human-readable summary.
5. Request wallet approval/signing.
6. Submit or hand off submission according to wallet/app design.
7. Track signature/status.
8. Confirm or show pending/failure.
9. Refresh affected account data.
10. Record analytics/logging without blocking the user.

Human-readable summary should include, when applicable:

- Network.
- Wallet/account used.
- Asset/token name and amount.
- Recipient/destination.
- Estimated fees when available.
- App/program being called.
- Risk warning for irreversible action.

## Confirmation rules

- Do not say `success` only because the user signed.
- Do not say `confirmed` only because submission returned a signature.
- Distinguish `signed`, `submitted`, `pending`, `confirmed`, and `failed`.
- If confirmation cannot be checked, say so and provide a retry/check status action.

## RPC and backend rules

- RPC failure must not crash the app.
- Use timeouts and retry limits.
- Avoid infinite polling loops.
- Rate-limit repeated balance/status checks.
- Backend endpoints, if added, must be documented in `ARCHITECTURE.md`.
- Backend should never receive wallet secrets.
- Backend trust assumptions must be explicit.

## Firebase Phase 2 data rules

Phase 2 Firebase progress is app-progress persistence only. It is not chain truth and not reward authority.

Rules:

- `app/google-services.json` must not be treated as a private key, but it should not be casually committed to the public repo unless the project is intentionally prepared as a public demo Firebase project.
- Firebase Anonymous Auth is acceptable for Phase 2 because it only owns app profile/progress.
- Client-side XP, level, streak, and leaderboard writes are acceptable only for MVP/demo progress with no real reward value.
- Do not use client-side Firestore writes as authority for SKR rewards, prize claims, token transfers, minting, or leaderboard prizes.
- When wallet-linked data arrives in Phase 3+, Firestore rules must ensure users can only write their own user documents.
- Any valuable reward flow must be verified by a backend/serverless function, a trusted indexer, or an on-chain program before reward issuance.


## Mobile Wallet Adapter Phase 3 rules

Phase 3 uses Mobile Wallet Adapter for wallet authorization only.

Rules:

- Store only the public wallet address, short display address, optional account label, and non-secret wallet status.
- Do not ask for seed phrases or private keys.
- Do not persist MWA auth tokens until an explicit token-storage design is reviewed.
- Do not claim wallet connection equals transaction success.
- Do not claim SKR holder status from wallet connection alone.
- Treat no-wallet, reject/cancel, and wallet failure as normal recoverable states.
- Message signing and memo transactions must remain Phase 4 work and must have separate user action, user-readable explanation, and result handling.

## Persistence rules

Allowed local persistence:

- Non-secret settings.
- Last selected network.
- Wallet public address/session metadata allowed by the wallet integration.
- Cached read-only account data with timestamp.
- Feature preferences.

Not allowed:

- Seed phrases.
- Private keys.
- Raw secrets.
- Production bypass flags.
- Fake transaction status.

Any schema change must include:

- Default value behavior.
- Migration behavior.
- Old-data protection.
- Clear rollback impact.

## Logging rules

Logs may include:

- Feature name.
- Error category.
- Network name.
- Short public address preview if needed.
- Transaction signature preview if needed.

Logs must not include:

- Full secrets.
- Seed phrases.
- Private keys.
- Sensitive auth tokens.
- Full personal data.
- Unredacted payloads unless explicitly safe.

## Analytics rules

Analytics is observational and non-blocking.

Do track:

- Wallet connect started/completed/failed/cancelled.
- Transaction flow stage changes.
- RPC failure category.
- Screen engagement.
- Demo completion events.

Do not track:

- Sensitive wallet secrets.
- Full user identifiers when a hashed or shortened form is enough.
- Misleading conversion events before confirmation.

## Demo-mode rules

Mock/demo mode is allowed for hackathon speed, but must be honest:

- Label demo-only data clearly.
- Do not mix mock balances with real wallet actions in the same UI without clear separation.
- Keep mock signing/approval unavailable in production builds.
- Make demo flows easy to disable before production.

## Security review checklist

Before release/demo:

- No private key or seed phrase collection exists.
- No secrets are hardcoded in the repository.
- Production build disables mock wallet and fake success flows.
- Transaction statuses use signed/submitted/pending/confirmed/failed accurately.
- Wallet rejection and disconnect flows are handled.
- RPC timeout and offline states are handled.
- Local persistence contains no secrets.
- UI clearly identifies network and demo mode.

## Phase 4 signed proof and memo rules

Phase 4 adds real MWA signing and devnet memo transactions.

Rules:

- `Sign Daily Proof` is an off-chain wallet signature. It proves wallet control, but it is not an on-chain transaction.
- `On-Chain Memo Proof` is a devnet transaction submitted by the wallet through `signAndSendTransactions`.
- Mainnet/live mode is not required for Phase 4 and should not be forced while testing.
- The memo transaction may require devnet SOL for fees.
- A signed message is not the same as a submitted transaction.
- A submitted transaction signature is not the same as confirmed/finalized chain state.
- Phase 4 may save submitted transaction signatures to Firestore, but must not claim final confirmation until confirmation polling is added.
- Firestore proof documents are app records, not reward authority.
- No seed phrase, private key, mint authority, reward authority, or wallet auth token should be stored.


## Phase 4 MWA authorization recovery rule

If a wallet returns `authorization request failed` during Phase 4 proof actions, do not fake completion and do not retry silently in a loop.

Rules:

- Use a fresh MWA authorization session for each signed proof or memo transaction request when wallet reauthorization is unstable.
- Keep Devnet explicit for Phase 4 proof work.
- Only mark `Sign Daily Proof` complete after a message signature is returned and saved.
- Only mark `On-Chain Memo Proof` complete after a transaction signature is returned and saved.
- The user must approve each wallet signing/transaction request.
- The app may show a recoverable error telling the user to reopen Phantom/confirm Devnet, but it must not auto-complete or spam repeated wallet prompts.

## Phase 4 Memo Proof Network Rule

The memo proof must fetch devnet chain context before wallet handoff. This prevents the app from opening Phantom when the phone cannot reach devnet RPC. The wallet may only be asked to sign/send after the app has a fresh blockhash and context slot.

For Phantom compatibility, memo submission should pass `minContextSlot` to `signAndSendTransactions` whenever available.


## Phase 5 SKR read-only rules

SKR balance scanning is read-only. The Android app may read the connected wallet public address and query Solana mainnet RPC for token accounts matching the official SKR mint.

Allowed SKR fields to store:

- wallet public address
- official SKR mint
- network label
- public token balance
- token account count
- derived app tier
- derived XP multiplier label
- RPC slot and check timestamp

Never store or request:

- seed phrase
- private key
- wallet auth token
- staking authority
- mint authority
- reward authority
- transaction approval for a read-only balance scan

If the wallet has zero SKR, the app must show `0 SKR` / `Explorer`; it must not invent a token balance for demos.

## Phase 6 demo tab rules

The Demo tab is presentation-only. It may summarize current app state for judges, but it must not invent proof.

Rules:

- Read current `RushUiState` only.
- Do not trigger wallet signing from the Demo tab.
- Do not complete quests from the Demo tab.
- Do not generate fake SKR, fake signatures, or fake transaction hashes.
- Keep network explanations explicit: Devnet for memo proof, mainnet-beta for SKR balance.


## Phase 7 testing safety rule

Automated tests must never fake successful wallet signatures, fake SKR holdings, or store private wallet material. Unit tests may verify pure tier/interaction rules, and UI smoke tests may navigate the app shell, but final wallet approval remains a real manual MWA/Phantom consent step.

## Phase 9 Daily Radiant Chest rule

The Daily Radiant Chest is an in-app Firebase reward loop. It must never spend XP as a wager, move SOL, move SKR, request a wallet signature, or request a wallet transaction. It may only grant bonus XP after all daily proof quests are complete.

Do not implement paid chance mechanics, token staking, token burning, or gambling-style loss mechanics in the Android app without a separate legal/product/security review.


## Phase 9.1 public leaderboard identity

The public Solana wallet address may be used as a leaderboard identity/deduplication key because it is public account data. Firebase Anonymous Auth UID remains the write-ownership boundary. Client code must not treat possession of a wallet address string as proof of ownership. This patch therefore does not move client-writable leaderboard documents to `leaderboard/{walletAddress}` and does not grant broader Firestore write permissions.
