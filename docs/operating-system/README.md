# Radiant Circle Engineering Workflow

This folder documents the engineering workflow used to design, build, validate, and maintain Radiant Circle.

It is separate from the product architecture.

For the application's technical design, see:

- [`../architecture/README.md`](../architecture/README.md)

## Purpose

The engineering workflow emphasizes:

- source-of-truth discipline
- small and reviewable changes
- mobile-first UX
- clear client/server authority
- secure handling of wallet and backend responsibilities
- persistence safety
- repeatable validation
- honest test evidence
- release gates
- documentation continuity
- AI-assisted development where useful

AI is used as an engineering tool, while implementation decisions remain grounded in the actual source, tests, device behavior, and trusted system boundaries.

## Core Principles

### Inspect before changing

Do not modify systems from memory or assumptions.

Check the current source, data model, dependencies, and surrounding behavior first.

### Prefer the smallest safe change

Avoid unnecessary rewrites.

Changes should be narrow enough to test, review, and reverse when necessary.

### Keep authority explicit

Security-sensitive decisions must live in the correct trusted layer.

Examples include:

- wallet signing stays with the user's wallet
- backend secrets stay server-side
- Firestore rules enforce access boundaries
- competition payouts are not authorized by Android client state alone

### Mobile-first UX

Radiant Circle is a mobile product.

Layouts, interaction targets, navigation, loading states, errors, and text must remain usable on real Android devices.

### Validate honestly

A successful compile is not the same as a successful feature.

Use the appropriate level of verification:

```text
static checks
    ↓
unit tests
    ↓
build
    ↓
device testing
    ↓
two-device testing where required
    ↓
real backend / wallet / Mainnet evidence where applicable
```

### Preserve evidence

Important releases and submission checkpoints should remain reproducible through:

- Git commits
- tags
- release artifacts
- SHA-256 hashes
- test logs
- evidence documentation

## Documentation Map

Current workflow documents include:

- `DOCUMENTATION_AND_VERSIONING_RULES.md`
- `MOBILE_UI_UX_STANDARDS.md`
- `SOLANA_SECURITY_AND_DATA_RULES.md`
- `TESTING_AND_RELEASE.md`

Historical implementation notes are archived under:

- [`../development-history/`](../development-history/)
