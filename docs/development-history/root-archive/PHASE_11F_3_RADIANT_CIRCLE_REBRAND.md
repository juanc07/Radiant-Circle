# Phase 11F.3 — Radiant Circle product rebrand

## Naming

- App/product: **Radiant Circle**
- In-app skill game: **Radiant Rush**
- Weekly competition: **Weekly Radiant Cup**

## Compatibility decision

The user-facing brand changes without changing the Android application identity. `namespace` and `applicationId` remain `com.thinkblox.radiantrush`; internal Kotlin symbols and persisted Firestore identifiers are left intact. This prevents a cosmetic rebrand from creating a second installed app or breaking Firebase, wallet authorization continuity, saved profiles, score history, or existing memo-proof parsing.

The wallet-facing `identityName` becomes `Radiant Circle`, while the existing identity URI and memo protocol string remain stable legacy identifiers.

## Scope

This is branding/packaging cleanup only. It does not change gameplay scoring, Ranked attempts, Weekly Cup behavior, XP, chest odds/rewards, SKR Passport rules, staking verification, Firestore security rules, or payout authority.
