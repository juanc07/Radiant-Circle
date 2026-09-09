# Phase 3 — Mobile Wallet Adapter Wallet Connect

## Goal

Add real Solana Mobile Wallet Adapter authorization to Radiant Rush without pretending that message signing, transaction submission, or SKR verification exists yet.

Phase 3 answers one judging requirement clearly:

> This is a native Android app that uses Solana Mobile Stack / Mobile Wallet Adapter to connect to a real Solana wallet.

## What Phase 3 includes

- Add Solana Mobile Wallet Adapter client dependency.
- Add Solana Kotlin helper dependencies for later phases.
- Add `MobileWalletRepository` as the wallet boundary.
- Add `Connect Wallet` action to Welcome, Today, Quest, and Profile flows.
- Save the public wallet address to Firestore.
- Update Wallet Ready quest/badge when wallet is connected.
- Add disconnect flow that clears stored public wallet identity.
- Fix Phase 2 profile creation so bootstrap does not overwrite saved XP/streak/wallet fields.

## What Phase 3 does not include

- No seed phrase entry.
- No private key storage.
- No message signing quest.
- No transaction signing.
- No memo transaction.
- No SKR token balance detection.
- No real reward distribution.

## User flow

```text
Open Radiant Rush
Firebase Anonymous Auth loads profile
Tap Connect Wallet
MWA-compatible wallet opens
User approves connection
App receives authorized public key
App saves public key in Firestore
Profile/quest/badge update
```

## Firestore fields added/used

```text
users/{uid}
  walletAddress
  walletAddressShort
  walletAccountLabel
  walletStatus
  phase
  updatedAt

users/{uid}/completedQuests/wallet-connect
  questId
  questTitle
  date
  proofType
  walletAddress
  walletAddressShort
  xpEarned
  createdAt
  updatedAt

leaderboard/{uid}
  walletAddressShort
  updatedAt
```

## Security rules

The existing Phase 2 Firestore rules are still acceptable for this MVP because users can only write their own profile, completed quest proofs, and leaderboard row. These rules are not reward-authority rules and must not be used for any valuable prize/reward distribution.

## Testing checklist

Required device checks:

```text
App opens after update
Firebase profile still loads
Existing XP/streak does not reset after app restart
Tap Connect Wallet with no wallet installed -> clear no-wallet message
Tap Connect Wallet with MWA-compatible wallet installed -> wallet approval opens
Reject/cancel wallet approval -> app stays usable
Approve wallet -> public address appears in Profile
Approve wallet -> Firestore users/{uid}.walletAddress is populated
Wallet Ready badge unlocks
Disconnect Wallet clears profile wallet state
Daily Firebase check-in still works after wallet connect
```

## Known limitations

- Auth token persistence is intentionally not implemented yet. A wallet may ask approval again after app restart.
- The dApp identity URI uses a placeholder ThinkBloxPH domain. Replace it with the final public project site before release.
- Phase 3 stores public wallet address only. It does not prove wallet ownership beyond the MWA authorization result.
- Phase 4 must add signed daily proof and memo transaction proof.
