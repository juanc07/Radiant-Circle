## Phase 12A trusted competition receipt boundary

- A Radiant Rush Android score is still client-reported and is **not** payout-grade proof.
- Ranked runs may create `competitionRunSubmissions/{receiptId}` only as `UNVERIFIED`.
- Client Firestore rules require `trustedPlacementEligible=false`, `payoutEligible=false`, and `payoutStatus=NOT_ELIGIBLE`, and deny all client updates/deletes on the receipt.
- The exact client schema is allow-listed so Android cannot smuggle trusted fields such as `trustedScore`, `verifiedAt`, winner state, funding state, or a confirmed payout into the initial write.
- `VERIFIED` / `REJECTED`, trusted placement, sponsor funding, winner snapshots, and payout lifecycle are trusted Admin/server responsibilities only.
- Receipt score/time/week/wallet fields are still client-visible inputs; a trusted verifier must independently validate them rather than accepting them because Firestore stored them.
- Receipt failure must not block the existing prototype leaderboard/reward path. A missing receipt means there is nothing trusted to verify; it never implies eligibility.
- No treasury/private key, entry fee, wagering, automatic SKR transfer, fake funding, or fake confirmation is added. Mainnet SKR remains read-only.

## Rebrand compatibility boundary

The product name is `Radiant Circle` and the in-app game is `Radiant Rush`. Rebranding must not silently change security/storage identifiers. `com.thinkblox.radiantrush`, the existing MWA identity URI, Firestore paths/fields, and the `radiant-rush:daily-memo-proof` protocol string remain stable unless a separately planned migration provides backward compatibility. Wallet display identity may show `Radiant Circle`.

## Phase 11F public identity / future social boundary

Public profile data is intentionally limited to a short display name and a bundled animal-avatar id. Do not store private keys, Seed Vault secrets, precise location, device fingerprints, or hidden contact data. Future chat/discovery must be opt-in and add block/report/rate-limit controls. Future SKR transfers must remain non-custodial and require explicit wallet approval; no treasury/user private key belongs in the APK.

## Phase 11E — reward presentation is not economic authority

Daily Chest VFX, rarity colors, audio, haptics, timing, and reward-summary fields are display only. The UI must wait for the repository result and must never invent a reward, SKR amount, token transfer, or payout confirmation. Phase 11E adds no Solana transaction, treasury key, sponsor payout, staking action, or Firestore rule change.

## Phase 11D — Weekly Cup sponsor metadata is not payout authority

- `weeklyCupConfigs/{ISO_WEEK}` is public-readable presentation metadata and is not proof of funding, winner validity, or payment.
- Android clients cannot create or update sponsor config under Firestore rules.
- `WeeklyRadiantCupRules.sponsorState()` never enables payout; `payoutEnabled` is always false.
- Existing Radiant Rush weekly scores are client-reported prototype ranking data and must not authorize SOL/SKR transfer.
- No sponsor, organizer, Solana Mobile, or hackathon payout is assumed.
- No player entry fee or wagering exists.
- No treasury/private key may be embedded in the APK.
- Real Sponsored SKR Cup payout is deferred to Phase 12 and requires trusted score verification, final winner authority, funded prize custody, and server-side transfer execution.

## Phase 11C SKR Passport v2 security boundary

Passport v2 may use the existing read-only Mainnet liquid SKR balance observation to unlock **non-economic, off-chain app perks only**: casual-only tickets, fixed chest XP/casual-ticket additions, and cosmetic profile status. The scan must not open the wallet, request a signature, transfer SKR/SOL, or claim transaction confirmation.

Liquid balance is not proof of staking. Phase 11C has no trusted staking integration, so staked SKR is explicitly marked not verified/not counted. Never infer staking state from liquid balance or invent a staked amount.

SKR wealth must never change Radiant Rush ranked raw score, ranking tie-breaks, or the three-ranked-attempt UTC-daily limit. Holder bonus tickets live in `skrCasualRushTickets` and cannot fund Ranked mode. This split is a product-fairness boundary, not economic security: owner-writable Firestore/client state is still insufficient for real-value payout authorization.

The legacy `skrXpMultiplier` profile field may remain for backward-compatible Phase 5 data, but Phase 11C does not apply it to ranked score, ranked attempts, or the controlled gameplay-XP formula. No treasury/private key, payout signer, staking transaction, or token-spend path is added.

## Phase 11B.1 competition identity boundary

The shared `runWalletDaily` counter uses a public Solana wallet address as prototype competition identity so one wallet does not gain extra ranked attempts by using multiple Firebase Anonymous UIDs/devices. Firestore rules require the authenticated user's post-write profile wallet to match the wallet document path and enforce a maximum of three attempts.

This is an anti-abuse/prototype fairness measure only. The Android client and Firebase profile can still report client-controlled game state, so neither `runWalletDaily`, `runWeekly`, nor `runAllTime` is trusted evidence for a real SKR payout. No treasury key, payout signer, staking authority, or automatic token transfer is introduced.

## Phase 10 non-economic game progression

Rush Tickets, Radiant Shards, capsule results, run scores, and collectibles are application progression only. They do not represent SOL, SKR, SPL tokens, NFTs, redeemable prizes, or a claim on value. Starting or completing Radiant Rush must never invoke Mobile Wallet Adapter. XP is not wagered.

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


## Phase 11A competition / future SKR payout boundary

Phase 11A formalizes a non-economic competition data model before any prize-pool implementation:

- Android-generated Radiant Rush scores are marked `ClientReportedPrototype`.
- Client-produced run score may drive prototype ranking UI and capped app XP, but `payoutEligible` is always false.
- No client score, Firebase owner write, cached SKR balance, or UI placement may directly authorize a real SKR payout.
- A future real SKR Cup payout requires a trusted authority outside the APK (for example a verified backend and/or on-chain program) that independently validates eligibility/results.
- No treasury private key, seed phrase, signing secret, or automatic prize-transfer authority may exist in the Android APK.
- Players do not wager SKR to enter Radiant Rush competition.
- SKR balance/tier must not multiply ranked score or increase the fixed ranked-attempt allowance.
- Phase 11A does not modify MWA, Devnet Memo proof, Mainnet SKR read-only scanning, or wallet-secret handling.

## Phase 11B competition security boundary

Radiant Rush score is still produced by the Android client. Firestore rules provide ownership/isolation and force the prototype `payoutEligible` field to remain false, but they do not make the score cryptographically trustworthy. Weekly/All-Time ranks are suitable for hackathon prototype competition and non-cash progression only. Real SKR distribution must be authorized by a future trusted server/program/validator that independently validates eligible results. No treasury private key, payout signer, fake SKR balance, or fake transaction confirmation is added in Phase 11B.


## Phase 11C.1 — staked SKR read-only rule

- Staked SKR may be displayed/count toward Passport perks only when read from the official Mainnet SKR staking program/account layout.
- The scan uses a public wallet address only. It must not request signing, staking, unstaking, transferring, or any secret material.
- If staking account/config RPC data cannot be verified, show the staking read as unavailable/unverified; never infer a value from liquid balance, device type, prior screenshots, or cached assumptions.
- Active staked SKR may count toward Passport tier. `unstaking_amount` must be shown separately and must not receive Guardian Stake Boost because it is no longer active stake.
- Staking incentives are limited to casual/off-chain progression and cosmetic presentation. They cannot modify ranked raw score, ranked attempts, or client-side payout eligibility.
- Current Android staking reads are not treasury/payout authority and do not justify real SKR prize distribution.


## Phase 11C.2 presentation-only clarification

The chest charge/open/reveal sequence, procedural chest audio, haptics, particles, and 1.8-second presentation delay are non-authoritative UI. They cannot mint, transfer, stake, claim SKR, authorize payout, or create progression independently. The repository transaction remains the only Daily Chest reward/progression write path.

Radiant Rush target-color changes are presentation only and do not change ranked scoring rules, ranked-attempt limits, or SKR perk fairness.

## Phase 11C.4 wallet/day competition state

`runWalletDaily/{utcDay}/wallets/{walletAddress}` is prototype fairness state keyed by the connected public Solana wallet address. It now stores both ranked-attempt usage and capped gameplay XP so reinstalling or using a second phone does not create another daily allowance. Firebase rules require monotonic values and cap gameplay XP at 300. This does not prove wallet ownership to payout-grade standards and cannot authorize SKR transfers; `payoutEligible` remains false.

Weekly/All-Time personal stats may be read across multiple anonymous-auth UIDs by matching the same connected public wallet address. This is a presentation/prototype competition identity rule, not a trusted reward authority.



## Phase 12B trusted sponsor configuration boundary

- Android has public read-only access to `weeklyCupConfigs/{weekKey}` and no client create/update/delete authority.
- A Cup config is presentation metadata, not proof of funding, score validity, winner status, or payout authority.
- The config must identify the official SKR mint (`SKRbvo6Gf7GondiT3BbTfuRDPqLWei4j2Qy2NPGZhW3`) and 6 decimals. Prize amounts are stored as exact atomic-unit strings to avoid floating-point ambiguity.
- A configured funding wallet is a public address only. Phase 12B records it as `NOT_VERIFIED`; it does not prove ownership or balance. Phase 12C must independently verify funding on-chain from trusted infrastructure.
- `trustedResultsRequired` remains true and Android hard-codes `payoutEnabled=false` in presentation regardless of remote fields. Client-reported Ranked scores remain UNVERIFIED receipts until a trusted verifier says otherwise.
- No service-account JSON, treasury key, private key, seed phrase, payout signer, or automatic SKR transfer may be shipped in the APK or committed to the repository.
- No player wagering or paid entry is introduced.
