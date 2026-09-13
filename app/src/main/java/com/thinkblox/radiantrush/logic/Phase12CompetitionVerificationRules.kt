package com.thinkblox.radiantrush.logic

import java.util.UUID

/**
 * Phase 12A trust-boundary primitives for sponsored competition runs.
 *
 * Android may create only an UNVERIFIED receipt. VERIFIED/REJECTED transitions,
 * trusted placement eligibility, winner selection, funding state, and payout
 * state belong to trusted Admin/server infrastructure. Firestore rules enforce
 * that boundary even if a modified Android client tries to forge these fields.
 */
object Phase12CompetitionVerificationRules {
    const val RECEIPT_SCHEMA_VERSION = 1
    const val CLIENT_REPORTED_SCORE_AUTHORITY = "client-reported-prototype-not-payout-authority"

    fun newReceiptId(): String = UUID.randomUUID().toString()

    fun isValidReceiptId(receiptId: String): Boolean = RECEIPT_ID_PATTERN.matches(receiptId.trim())

    private val RECEIPT_ID_PATTERN = Regex(
        "^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$",
    )

    /** The only trust state a normal Android submission is allowed to create. */
    fun clientInitialTrustState(): ClientCompetitionTrustState = ClientCompetitionTrustState
}

enum class CompetitionVerificationStatus {
    UNVERIFIED,
    VERIFIED,
    REJECTED,
}

/**
 * Fixed client-side initial state. There is no Android constructor/copy path for
 * setting VERIFIED or payout eligibility; Firestore rules are the authoritative
 * enforcement boundary against modified clients.
 */
object ClientCompetitionTrustState {
    val verificationStatus = CompetitionVerificationStatus.UNVERIFIED
    const val trustedPlacementEligible = false
    const val payoutEligible = false
    const val payoutStatus = "NOT_ELIGIBLE"
}
