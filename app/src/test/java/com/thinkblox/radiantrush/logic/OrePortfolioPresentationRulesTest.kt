package com.thinkblox.radiantrush.logic

import java.math.BigInteger
import org.junit.Assert.assertEquals
import org.junit.Test

class OrePortfolioPresentationRulesTest {
    @Test
    fun totalPositionExcludesClaimableRewards() {
        assertEquals(
            BigInteger("150000000000"),
            OrePortfolioPresentationRules.totalPositionRaw(
                liquidRaw = "50000000000",
                stakedRaw = "100000000000",
            ),
        )
    }

    @Test
    fun overviewAmountUsesHumanScaleWithoutLongProtocolDecimals() {
        assertEquals("0.1189 ORE", OrePortfolioPresentationRules.overviewAmount("11885999082"))
        assertEquals("12.3457 ORE", OrePortfolioPresentationRules.overviewAmount("1234567890000"))
        assertEquals("0 ORE", OrePortfolioPresentationRules.overviewAmount("0"))
    }

    @Test
    fun overviewAmountDoesNotHidePositiveDustAsZero() {
        assertEquals("<0.0001 ORE", OrePortfolioPresentationRules.overviewAmount("1"))
    }

    @Test
    fun stakedPercentUsesLiquidPlusStakedOnly() {
        assertEquals(75, OrePortfolioPresentationRules.stakedPercent("250", "750"))
        assertEquals(0, OrePortfolioPresentationRules.stakedPercent("0", "0"))
    }
}
