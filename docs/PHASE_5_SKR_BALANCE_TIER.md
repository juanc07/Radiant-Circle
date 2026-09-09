# Phase 5 — SKR Balance Tier

Phase 5 adds the first SKR integration to Radiant Rush.

## Important network split

Radiant Rush now uses two Solana network contexts:

- **Devnet** for Phase 4 memo proof transactions.
- **Mainnet-beta** for Phase 5 read-only SKR balance scanning.

There is no need for a devnet SKR test token. SKR is treated as a live SPL token on Solana mainnet and is read by public wallet address only.

## Official SKR mint

```text
SKRbvo6Gf7GondiT3BbTfuRDPqLWei4j2Qy2NPGZhW3
```

## User flow

1. Connect wallet with Mobile Wallet Adapter.
2. Complete Sign Daily Proof if desired.
3. Complete Send Memo on Devnet if desired.
4. Tap **Check SKR Balance**.
5. The app reads token accounts for the official SKR mint on mainnet-beta.
6. The app saves a public SKR Passport snapshot to Firebase.
7. The UI shows SKR balance, tier, and XP multiplier.

## Zero-balance behavior

If the wallet has no SKR, the scan still succeeds and saves:

```text
Balance: 0 SKR
Tier: Explorer
Multiplier: 1.00x
```

This keeps development and demo testing possible without faking SKR ownership. The SKR Radiant badge only unlocks when the balance is greater than zero.

## Tier logic

```text
0 SKR        -> Explorer        -> 1.00x
>0 SKR       -> Radiant Scout   -> 1.05x
>=100 SKR    -> Radiant Holder  -> 1.10x
>=1,000 SKR  -> Radiant Elite   -> 1.20x
>=10,000 SKR -> Radiant Legend  -> 1.35x
```

These thresholds are app UX tiers only. They do not move, lock, stake, or spend SKR.

## Security rules

The app stores public snapshot data only:

- wallet public address
- SKR mint
- network
- balance
- token account count
- tier
- XP multiplier label
- RPC slot
- client check timestamp

The app does not store private keys, seed phrases, auth tokens, mint authority, reward authority, or staking authority.

## Files changed

- `README.md`
- `app/build.gradle.kts`
- `app/src/main/java/com/thinkblox/radiantrush/data/PhaseOneModels.kt`
- `app/src/main/java/com/thinkblox/radiantrush/firebase/FirebaseRadiantRepository.kt`
- `app/src/main/java/com/thinkblox/radiantrush/solana/SkrBalanceRepository.kt`
- `app/src/main/java/com/thinkblox/radiantrush/ui/RadiantRushApp.kt`
- `app/src/main/java/com/thinkblox/radiantrush/ui/components/RushComponents.kt`
- `app/src/main/java/com/thinkblox/radiantrush/ui/screens/HomeScreen.kt`
- `app/src/main/java/com/thinkblox/radiantrush/ui/screens/ProfileScreen.kt`
- `app/src/main/java/com/thinkblox/radiantrush/ui/screens/QuestsScreen.kt`
- `app/src/main/java/com/thinkblox/radiantrush/ui/screens/WelcomeScreen.kt`
- `docs/operating-system/ARCHITECTURE.md`
- `docs/operating-system/CHANGELOG.md`
- `docs/operating-system/SOLANA_SECURITY_AND_DATA_RULES.md`
- `docs/operating-system/TESTING_AND_RELEASE.md`
- `docs/operating-system/BACKLOG_AND_ROADMAP.md`
