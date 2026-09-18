package com.thinkblox.radiantrush.logic

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class Phase12CompetitionVerificationRulesTest {
    @Test
    fun generatedReceiptIdIsUuidAndUnique() {
        val first = Phase12CompetitionVerificationRules.newReceiptId()
        val second = Phase12CompetitionVerificationRules.newReceiptId()

        assertTrue(Phase12CompetitionVerificationRules.isValidReceiptId(first))
        assertTrue(Phase12CompetitionVerificationRules.isValidReceiptId(second))
        assertTrue(first != second)
    }

    @Test
    fun androidInitialTrustStateCanOnlyStartUnverifiedAndNotEligible() {
        val state = Phase12CompetitionVerificationRules.clientInitialTrustState()

        assertEquals(CompetitionVerificationStatus.UNVERIFIED, state.verificationStatus)
        assertFalse(state.trustedPlacementEligible)
        assertFalse(state.payoutEligible)
        assertEquals("NOT_ELIGIBLE", state.payoutStatus)
    }

    @Test
    fun verificationModelHasExplicitTrustedTerminalStates() {
        assertTrue(CompetitionVerificationStatus.values().contains(CompetitionVerificationStatus.VERIFIED))
        assertTrue(CompetitionVerificationStatus.values().contains(CompetitionVerificationStatus.REJECTED))
    }

    @Test
    fun radiantRunResultKeepsReceiptIdentityAcrossSanitizingCopyOrRetry() {
        val result = RadiantRunResult(
            score = 1_500,
            maxCombo = 9,
            radiantHits = 12,
            corruptedHits = 2,
            perfectHits = 4,
        )
        val copied = result.copy(score = result.score.coerceAtLeast(0))

        assertTrue(Phase12CompetitionVerificationRules.isValidReceiptId(result.receiptId))
        assertEquals(result.receiptId, copied.receiptId)
        assertEquals(result.completedAtEpochMillis, copied.completedAtEpochMillis)
    }

    @Test
    fun clientScoreAuthorityStaysExplicitlyNonPayoutAuthority() {
        assertEquals(
            "client-reported-prototype-not-payout-authority",
            Phase12CompetitionVerificationRules.CLIENT_REPORTED_SCORE_AUTHORITY,
        )
    }
}
