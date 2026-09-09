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
