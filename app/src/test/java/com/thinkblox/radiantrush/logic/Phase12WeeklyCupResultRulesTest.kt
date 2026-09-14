package com.thinkblox.radiantrush.logic

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class Phase12WeeklyCupResultRulesTest {
    private val walletA = "J86vtTs7twTUuS4xfo8H8zaUeFyMNHWPPeEXyL5DscPg"
    private val walletB = "HbyQrE2N1V8TPs5HJ9wGDq3M85Zm1i21RmgbLFk39xkS"
    private val walletC = "9xQeWvG816bUx9EPjHmaT23yvVMdqN5WwQ1Z6J8nPABC"

    private fun winners() = listOf(
        winner(1, walletA, "500000000", 5000),
        winner(2, walletB, "300000000", 4000),
        winner(3, walletC, "200000000", 3000),
    )

    private fun winner(placement: Int, wallet: String, prize: String, score: Int) = TrustedWeeklyCupWinnerInput(
        schemaVersion = 1,
        resultVersion = 1,
        weekKey = "2026-W37",
        placement = placement,
        walletAddress = wallet,
        receiptId = "receipt-$placement",
        score = score,
        maxCombo = 20,
        perfectHits = 8,
        prizeAmountAtomic = prize,
        prizeAssetSymbol = "SKR",
        payoutStatus = "NOT_ENABLED",
        fundingVerificationStatusAtClose = "NOT_VERIFIED",
        resultAuthority = "trusted-admin-phase12d",
        payoutEnabled = false,
        payoutReady = false,
    )

    private fun state(
        payoutReady: Boolean = false,
        funding: String = "NOT_VERIFIED",
        winnerRows: List<TrustedWeeklyCupWinnerInput> = winners(),
    ) = Phase12WeeklyCupResultRules.presentation(
        expectedWeekKey = "2026-W37",
        schemaVersion = 1,
        resultVersion = 1,
        weekKey = "2026-W37",
        finalizationStatus = "FINALIZED",
        finalizationAuthority = "trusted-admin-phase12d",
        finalizedAtEpochMillis = 1_789_500_000_000L,
        prizeAssetSymbol = "SKR",
        prizeMint = SkrTierRules.OFFICIAL_SKR_MINT,
        prizeDecimals = 6,
        prizeAmountAtomic = "1000000000",
        placementAllocationsBps = mapOf(1 to 5000, 2 to 3000, 3 to 2000),
        fundingVerificationStatusAtClose = funding,
        payoutEnabled = false,
        payoutReady = payoutReady,
        winnerCount = winnerRows.size,
        winners = winnerRows,
    )

    @Test
    fun trustedFinalizedResultRendersExactWinnersWhilePayoutStaysDisabled() {
        val state = state()
        assertTrue(state.recognized)
        assertEquals(3, state.winners.size)
        assertEquals("500 SKR", state.winners.first().prizeLabel)
        assertEquals("Funding pending", state.fundingStatusLabel)
        assertFalse(state.payoutEnabled)
        assertFalse(state.payoutReady)
    }

    @Test
    fun payoutReadyTrueFailsClosed() {
        assertFalse(state(payoutReady = true).recognized)
    }

    @Test
    fun forgedOrMissingWinnerAllocationFailsClosed() {
        val bad = winners().toMutableList().apply {
            this[0] = this[0].copy(prizeAmountAtomic = "499999999")
        }
        assertFalse(state(winnerRows = bad).recognized)
        assertFalse(state(winnerRows = winners().dropLast(1)).recognized)
    }

    @Test
    fun nonContiguousPlacementsFailClosed() {
        val result = Phase12WeeklyCupResultRules.presentation(
            expectedWeekKey = "2026-W37",
            schemaVersion = 1,
            resultVersion = 1,
            weekKey = "2026-W37",
            finalizationStatus = "FINALIZED",
            finalizationAuthority = "trusted-admin-phase12d",
            finalizedAtEpochMillis = 1_789_500_000_000L,
            prizeAssetSymbol = "SKR",
            prizeMint = SkrTierRules.OFFICIAL_SKR_MINT,
            prizeDecimals = 6,
            prizeAmountAtomic = "1000000000",
            placementAllocationsBps = mapOf(1 to 5000, 3 to 3000, 4 to 2000),
            fundingVerificationStatusAtClose = "NOT_VERIFIED",
            payoutEnabled = false,
            payoutReady = false,
            winnerCount = 3,
            winners = winners(),
        )
        assertFalse(result.recognized)
    }

    @Test
    fun fundingNotVerifiedCanStillRenderFinalCompetitiveWinners() {
        val pending = state(funding = "NOT_VERIFIED")
        assertTrue(pending.recognized)
        assertEquals("Funding pending", pending.fundingStatusLabel)
    }
}
