# Phase 12D — Trusted Season Close + Winners

**Project:** Radiant Circle
**Game:** Radiant Rush
**Implementation status:** code complete in the Phase 12D patch; live Firestore/admin/device proof still required before commit/tag
**Payout status:** disabled; no SKR transfer exists in this phase

## 1. Source audit conclusion

Phase 12C has no trusted mechanism that verifies a Radiant Rush run and promotes an Android-created `competitionRunSubmissions/{receiptId}` from `UNVERIFIED` to trusted placement eligibility. Android creates an immutable client submission with `trustedPlacementEligible=false`, and Firestore denies client update/delete authority.

Therefore Phase 12D does **not** rank raw client receipts as trusted winners. The close path fails closed unless a receipt has a matching Phase 12D Admin-only verification decision.

## 2. Small trusted prerequisite added in Phase 12D

`scripts/firebase-admin/verify-competition-run.mjs` is an explicit manual-evidence attestation tool. It does not auto-verify client scores.

For `VERIFIED`, an operator must independently inspect evidence and supply the checked:

- public wallet address,
- score,
- max combo,
- PERFECT hits,
- radiant hits,
- corrupted hits,
- client completion epoch,
- evidence reference.

Every supplied fact must exactly match the immutable client receipt. The Cup must be trusted Phase 12B schema, `OPEN`, require trusted results, and keep payout disabled. The receipt must still be the original Phase 12A `UNVERIFIED` shape and fall within the Cup window.

A successful apply writes both:

1. the receipt promotion fields (`VERIFIED`, `trustedPlacementEligible=true`, Phase 12D authority/method/reference), while keeping `payoutEligible=false` and `payoutStatus=NOT_ELIGIBLE`; and
2. immutable Admin-only `competitionRunVerifications/{receiptId}` evidence metadata.

Finalization requires these two records to agree exactly. A bare `VERIFIED` string or directly edited receipt fields are insufficient.

## 3. Finalization contract

`scripts/firebase-admin/finalize-weekly-cup.mjs` is dry-run by default. A Cup may finalize only when:

- `weeklyCupConfigs/{weekKey}` is trusted Phase 12B schema v2,
- status is `OPEN`,
- the configured `endsAt` has passed,
- `trustedResultsRequired=true`,
- `payoutEnabled=false`,
- no trusted result/finalization already exists,
- SKR mint/decimals/prize/allocation are valid,
- enough distinct eligible wallets exist to fill every configured placement.

Eligibility requires the Phase 12D verified receipt **and** matching immutable verification audit record, both decided no later than the finalization cutoff.

## 4. Ranking and wallet dedupe

Phase 12D reuses the existing Radiant Rush competition order rather than inventing a new tiebreaker:

1. score descending,
2. max combo descending,
3. PERFECT hits descending,
4. earlier valid completion,
5. receipt id lexical order as the final deterministic tie breaker.

All Phase 12A receipts contain a full Solana wallet address, so trusted finalization dedupes by the exact wallet address. Only the best trusted-eligible result for each wallet participates in the ranked wallet list.

## 5. Frozen result schema

Finalization creates:

```text
weeklyCupResults/{weekKey}
weeklyCupResults/{weekKey}/eligibleReceipts/{receiptId}
weeklyCupResults/{weekKey}/winners/{placement}
```

The parent result stores result/finalization version, source and eligible counts, exact prize configuration, funding state at close, SHA-256 snapshot digests, and explicit `payoutEnabled=false` / `payoutReady=false`.

The `eligibleReceipts` subcollection is Admin-only and freezes every trusted-eligible receipt plus the verification audit reference, selection-for-wallet-ranking flag, and final rank where applicable.

The `winners` subcollection is Android-readable but client-write-denied. Each winner records placement, wallet, source receipt, score/tiebreak metrics, exact SKR allocation, finalization authority, funding state at close, and `payoutStatus=NOT_ENABLED`.

## 6. Immutable / duplicate-close behavior

- `weeklyCupResults/{weekKey}` is created, never client-written.
- A pre-existing result causes immediate refusal.
- Phase 12B config tooling refuses to reopen/mutate a Cup that already contains Phase 12D finalization evidence.
- Apply re-reads Cup, receipts, and verification audits in the Firestore transaction before any write.
- If the eligible-receipt or ranking digest differs from the reviewed plan, apply aborts and requires another dry run.

## 7. Funding interaction

Phase 12D snapshots the Phase 12C funding state independently from competition ranking.

A valid `VERIFIED` funding state is accepted only when the complete Phase 12C evidence shape is valid. A bare `VERIFIED` string degrades to `NOT_VERIFIED`.

`NOT_VERIFIED` funding **does not block** freezing competitive results. It does block any payout-ready interpretation. Phase 12D never changes `payoutEnabled` to true and never transfers SKR.

For the current W37 development Cup, the truthful Phase 12C state remains `NOT_VERIFIED` because the tested sponsor wallet had 0 transferable liquid SKR against the configured 1000 SKR prize.

## 8. Android behavior

Android remains read-only for Phase 12D authority. Firestore rules deny client writes to:

```text
competitionRunVerifications/*
weeklyCupResults/*
weeklyCupResults/*/winners/*
weeklyCupResults/*/eligibleReceipts/*
```

Android only renders a finalized result after strict schema/authority/prize/allocation/payout validation. Final winner copy is player-facing only: `Final winners`, placement, shortened wallet, score, prize, and simple funding label. The current weekly prototype board is explicitly labeled `Live standings` and is not presented as a trusted final podium.

## 9. Test coverage

Firebase Admin pure tests cover:

- wrong/missing Cup schema,
- non-closeable status and early close,
- already-finalized protection,
- exclusion of `UNVERIFIED` and `REJECTED`,
- rejection of a forged `VERIFIED` receipt,
- required matching immutable verification audit,
- audit mismatch rejection,
- exact best result per wallet,
- deterministic existing tiebreak order,
- exact integer SKR placement allocation,
- insufficient eligible winners,
- `NOT_VERIFIED` funding with competitive freeze but no payout readiness,
- Phase 12B refusal to mutate a finalized Cup.

Android unit coverage validates accepted final-result presentation, malformed result rejection, exact allocation requirements, and non-payout-ready `NOT_VERIFIED` winner presentation.

## 10. Live acceptance boundary

Do not commit/tag Phase 12D solely from pure tests. Before checkpointing:

1. run the full Android unit/build/device gate;
2. deploy the changed Firestore rules;
3. verify Android cannot write Phase 12D collections;
4. keep the Cup OPEN while independently attesting intended run receipts;
5. dry-run every receipt decision before apply;
6. after the configured Cup end, dry-run finalization and review eligible counts, winners, allocations, funding state, and SHA-256 digests;
7. apply finalization once;
8. prove a second finalization is refused;
9. inspect Firestore frozen result/winners/audit records;
10. verify the Seeker UI shows clean final winners and `Funding pending` for W37 while no payout-ready claim appears.

## 11. Admin operator command reference

The complete live Admin workflow, including Git Bash-safe read commands, temporary credential handling, Cup configuration, receipt inspection, independent run attestation, fail-closed finalization, duplicate-close proof, and credential cleanup is documented in:

```text
docs/PHASE_12_ADMIN_OPERATOR_RUNBOOK.md
```

Use that runbook rather than reconstructing trusted Admin commands from chat history.

## 12. W37 live-proof checkpoint — 2026-09-14

The Phase 12D live audit observed five `competitionRunSubmissions` receipts for `2026-W37`, but only two distinct full wallet addresses. All five receipts remained `UNVERIFIED` with `trustedPlacementEligible=false`. W37 is configured for three placements.

That means the current W37 source set cannot truthfully produce #1/#2/#3 trusted winners. After the configured Cup end boundary, the live Phase 12D finalization dry run correctly refused with `No trusted-placement-eligible VERIFIED receipts exist for this Cup. Refusing to invent winners.` No trusted result/winner records were created. W37 therefore serves as the live fail-closed proof while a later controlled Cup can provide the successful three-winner finalization proof.


## 13. Wallet-gated competition UX

Radiant Circle may be used without a connected wallet for wallet-optional features such as Daily Check-In and non-financial social/retention flows. Wallet-required features remain disabled until a wallet is connected.

For Radiant Rush specifically:

- the game itself remains playable without a wallet as **Casual**;
- a walletless run can never become Ranked or enter the Weekly Radiant Cup;
- when a trusted Cup is actually `OPEN` and the configured UTC window is active, the launcher shows a prominent warning before a walletless run;
- the player must explicitly choose either `Connect Wallet` or `Play Casual`;
- the Radiant Rush briefing repeats that the run is Casual and will not count toward the Cup;
- an expired `OPEN` document does not trigger the live-Cup warning merely because its status string was not yet changed;
- this UI protection is not the trust boundary: server/repository competition rules still decide Ranked eligibility and Phase 12D still requires trusted verified receipts plus immutable Admin verification audits.

Wallet-dependent proof quests remain blocked in the existing quest state until a wallet is connected: signed daily proof, on-chain memo proof, and SKR Passport refresh. Daily Check-In intentionally remains wallet-optional.
