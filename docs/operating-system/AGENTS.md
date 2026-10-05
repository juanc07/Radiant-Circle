# Radiant Circle Engineering Agent Rules

These rules apply when using AI-assisted engineering workflows on Radiant Circle.

## Source Is Authority

Before changing code:

1. inspect the current implementation
2. identify the actual source of truth
3. understand related data and trust boundaries
4. make the smallest safe change
5. validate the result

Do not invent files, APIs, schemas, states, or behavior that do not exist in the project.

## Protect Trust Boundaries

Never move sensitive authority into the Android client.

Keep:

- wallet private keys inside the user's wallet
- OpenAI credentials server-side
- Firebase Admin credentials outside Android
- payout authority in trusted operator/backend systems
- authorization enforced by Firestore/backend rules where appropriate

## Mobile First

All UI changes must consider:

- small screens
- readable text
- reachable controls
- scrolling
- loading/error states
- touch targets
- real-device behavior

## Validation

Use the strongest available evidence.

A feature is not considered proven merely because code was generated or compiled.

Prefer:

- tests
- debug/release builds
- physical-device validation
- two-device validation for social features
- backend logs
- verifiable Mainnet state where applicable

## Documentation

Update current architecture/evidence documents when behavior materially changes.

Do not clutter current judge-facing documentation with temporary patch notes or old implementation phases.

Historical notes belong under `docs/development-history/`.

## AI Assistance

AI may assist with:

- analysis
- implementation
- debugging
- testing strategy
- documentation
- code review

AI output must still be validated against the actual repository and working application.
