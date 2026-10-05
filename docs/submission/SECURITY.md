# Radiant Circle — Security Evidence

This document summarizes security boundaries relevant to the final CLOCK IN submission.

It is not an external security audit. It documents what authority exists in the current implementation and what sensitive authority is deliberately kept out of the Android client.

---

## 1. Final Release Context

- **APK:** `RadiantCircle-v1.2.18-clock-in.apk`
- **Release tag:** `clock-in-submission-v3`
- **Frozen source:** `0a181171194403b8ec95aebfb069c6800f491fbd`
- **SHA-256:** `52ecd83059f78a5d8d25175834ee125ef42deb510f2ec8aefea39f40bba281ee`

Direct APK:  
https://github.com/juanc07/Radiant-Circle/releases/download/clock-in-submission-v3/RadiantCircle-v1.2.18-clock-in.apk

---

## 2. Trust Boundaries

Radiant Circle separates:

```text
Firebase social identity
    ↓
social profile / Circle / chat

Solana wallet
    ↓
user-approved ownership and signing

Firebase trusted backend
    ↓
AI secret handling + server-side validation

Admin/operator tooling
    ↓
competition verification / funding / payout lifecycle
```

The Android client is not treated as a trusted treasury or payout signer.

---

## 3. No Wallet Seed / Private-Key Collection

Primary evidence:

- `app/src/main/java/com/thinkblox/radiantrush/solana/MobileWalletRepository.kt`
- `docs/operating-system/SOLANA_SECURITY_AND_DATA_RULES.md`

`MobileWalletRepository.kt` explicitly states that it never asks for or stores seed phrases/private keys.

Wallet authorization/signing is delegated to the user's wallet through Solana Mobile Wallet Adapter.

The app works with public wallet addresses, signed messages/transactions, public signatures, and verified chain state.

---

## 4. Mobile Wallet Adapter / Non-Custodial Signing

`app/src/main/java/com/thinkblox/radiantrush/solana/MobileWalletRepository.kt`

For ORE Mainnet actions the app:

1. opens a Solana Mainnet MWA session
2. receives the wallet account selected by the wallet
3. verifies it matches the wallet shown in Radiant Circle
4. builds the transaction
5. requests wallet approval/sign-and-send
6. receives the transaction signature
7. waits for Mainnet confirmation

The private signing key remains in the wallet application.

---

## 5. ORE Mainnet Verification

Primary evidence:

- `app/src/main/java/com/thinkblox/radiantrush/solana/OrePortfolioRepository.kt`
- `app/src/main/java/com/thinkblox/radiantrush/solana/MobileWalletRepository.kt`
- `app/src/main/java/com/thinkblox/radiantrush/ui/screens/OrePortfolioScreen.kt`

Security-relevant behavior includes:

- expected wallet identity check before submission
- amount/range validation
- token-account balance check before Stake
- fresh Mainnet blockhash/context
- non-custodial signing
- Mainnet confirmation polling
- post-transaction portfolio refresh
- before/after receipt from fresh Mainnet reads

The receipt UI explicitly describes its values as fresh Mainnet reads rather than estimated UI balances.

---

## 6. Firebase Authentication and Circle Authorization

Primary evidence:

- `app/src/main/java/com/thinkblox/radiantrush/firebase/FirebaseRadiantRepository.kt`
- `firebase/firestore.rules`

Firestore rules require authenticated access for protected data.

Circle/chat behavior checks authenticated membership and accepted relationship state.

Protected concepts include:

- `circleEdges`
- `circleChats`
- chat `messages`
- typing/presence records
- relationship actions
- reports

Knowing another user's UID alone is not intended to grant private-chat access.

---

## 7. AI Spark Starter — Server-Side Validation

Primary evidence:

- `app/src/main/java/com/thinkblox/radiantrush/firebase/AiSparkStarterRepository.kt`
- `functions/index.js`
- `docs/submission/AI_SPARK_STARTER.md`

The Android client sends only:

```text
peerUid
```

The server then:

- requires Firebase Authentication
- validates the peer UID
- loads the deterministic Circle relationship
- requires status `ACCEPTED`
- requires both member UIDs in the relationship
- reads both profiles server-side
- derives Shared Sparks server-side
- applies rate limiting
- reserves pair-generation state transactionally
- calls OpenAI

The Android client does not decide that the relationship is trusted.

---

## 8. OpenAI Secret Handling

`functions/index.js` uses:

```text
defineSecret("OPENAI_API_KEY")
```

The deployed Cloud Function receives the OpenAI credential from Firebase Secret Manager.

The key is not:

- hardcoded in Android source
- stored as an Android app constant
- committed in `functions/index.js`
- required from judges using the official APK

The OpenAI request uses `store: false`.

---

## 9. AI Data Minimization

AI Spark Starter derives context from Shared Spark profile fields such as:

- favorite food
- music
- games
- hobbies
- books
- pets
- currently into
- weekend vibe
- conversation topics

Private Circle chat history is **not** sent to OpenAI.

The prompt also instructs the model not to invent personal facts or mention location, money, wallets, crypto holdings, or private data.

Generated text is shown to the user first and is never automatically sent.

---

## 10. AI Abuse / Cost Controls

`functions/index.js` includes:

- Firebase Auth requirement
- accepted-relationship requirement
- per-user daily generation cap
- bounded Shared Sparks context
- short output cap
- pair anti-repetition history
- Firestore transaction-based generation sequencing

Current generation cap:

```text
20 generations per user per UTC day
```

These controls reduce anonymous abuse, runaway cost, and duplicate outputs.

---

## 11. Firebase App Check Boundary

The final release does **not** claim Firebase App Check enforcement for the AI callable.

Observed device logs showed:

```text
auth: VALID
app: MISSING
```

Therefore the enforced AI boundary in this release is:

- Firebase Authentication
- accepted Circle relationship validation
- server-side Shared Sparks derivation
- rate limiting

App Check is a future hardening option, not a current claim.

---

## 12. Firebase Android Client Configuration

The current public source and frozen v3 source exclude:

```text
app/google-services.json
```

Relevant commits:

```text
836675c security: remove Firebase client config from public repository
540089c docs: clarify Firebase setup and judge APK testing
```

The official signed APK contains the compiled client configuration required for the configured Firebase project.

A fresh clone/build requires the developer's own matching Firebase Android/OAuth configuration.

### Historical Git note

Older commits before `836675c` may contain the Android Firebase client configuration because Git history was **not** rewritten.

Firebase Android client identifiers are not equivalent to a Firebase Admin private key, but they should still be restricted appropriately and are intentionally excluded from the current public tree.

The repository does not claim that historical Git objects were purged.

---

## 13. Firebase Admin Credential Boundary

Trusted competition tooling lives under:

- `scripts/firebase-admin/`

Repository guidance expects Firebase Admin credentials outside the repository, commonly via:

```text
GOOGLE_APPLICATION_CREDENTIALS
```

Service-account private-key JSON must not be committed.

Trusted operator authority is not shipped in the Android APK.

---

## 14. Android Release Signing Key

The Android release keystore/password are not committed to the public repository.

Final signer certificate SHA-256:

```text
1f579b2703386ea0c34a5b70db165371b2f7564a0c9b404e9b191f2e0abc7973
```

The final APK was verified with APK Signature Scheme v2 and v3.

---

## 15. Competition / Payout Separation

Primary evidence:

- `firebase/firestore.rules`
- `scripts/firebase-admin/competition-run-verification.mjs`
- `scripts/firebase-admin/competition-wallet-lock.mjs`
- `scripts/firebase-admin/skr-funding-verification.mjs`
- `scripts/firebase-admin/weekly-cup-finalization.mjs`
- `scripts/firebase-admin/weekly-cup-payout-lifecycle.mjs`
- `scripts/firebase-admin/weekly-cup-skr-transfer.mjs`
- `scripts/firebase-admin/execute-weekly-cup-skr-transfer.mjs`
- `scripts/firebase-admin/reconcile-weekly-cup-skr-transfer.mjs`

Trusted flow:

```text
competition evidence
    ↓
verification
    ↓
wallet lock
    ↓
funding verification
    ↓
trusted finalization
    ↓
payout preparation / approval
    ↓
transfer / reconciliation
```

Client-reported gameplay does not directly authorize a sponsored payout.

---

## 16. Competition Wallet Lock

Primary evidence:

- `app/src/main/java/com/thinkblox/radiantrush/logic/Phase12CompetitionWalletLockRules.kt`
- `firebase/firestore.rules`
- `scripts/firebase-admin/competition-wallet-lock.mjs`

The first ranked Cup receipt for an account/week establishes the competition wallet.

Later ranked entries for that Firebase account/week must use the same locked wallet.

Trusted admin validation checks the lock shape rather than trusting Android UI state alone.

---

## 17. Repository Secret Hygiene

Before the final AI commit, staged files/diffs were checked for:

- `google-services.json`
- plaintext OpenAI `sk-...` keys
- `.jks`
- `.keystore`
- known local plaintext-key filenames

No such secret/keystore file was included in the final AI feature commit.

Frozen AI/source commit:

```text
0a181171194403b8ec95aebfb069c6800f491fbd
```

---

## 18. Known Security Boundaries / Non-Claims

Radiant Circle does not claim:

- Android is a trusted payout signer
- Firebase App Check is enforced in the final AI callable
- old Git history was fully rewritten/purged
- the app can access user wallet private keys
- opening a wallet UI means an onchain action succeeded
- client score alone can trigger SKR payout
- every component has undergone an independent external security audit

These boundaries are documented so judges can evaluate actual implementation rather than implied claims.

---

## 19. Judge Security Checklist

| Question | Current answer |
|---|---|
| Seed/private key inside Android? | No |
| OpenAI key inside Android/public source? | No |
| Firebase Admin private key inside Android? | No |
| Release keystore committed? | No |
| Wallet approval required for MWA signing? | Yes |
| ORE uses Mainnet confirmation/post-state? | Yes |
| AI requires authenticated user? | Yes |
| AI requires accepted Circle relationship? | Yes |
| AI sends private chat history to OpenAI? | No |
| AI auto-sends generated text? | No |
| AI has per-user rate limit? | Yes |
| Firebase App Check enforced for AI? | No / not claimed |
| Client score alone authorizes payout? | No |
| Current tree contains `app/google-services.json`? | No |
| Historical Git fully purged? | No / not claimed |

---

## Final Security Position

The final release is designed around **least authority in the Android client**:

- social access is authenticated and rule-gated
- AI secrets/checks run server-side
- wallet signing stays inside the user's wallet
- onchain actions are verified after submission
- competition payout authority is separated into trusted operator tooling
