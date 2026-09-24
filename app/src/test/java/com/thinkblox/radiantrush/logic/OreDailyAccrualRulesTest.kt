package com.thinkblox.radiantrush.logic

import java.math.BigInteger
import org.junit.Assert.assertEquals
import org.junit.Test

class OreDailyAccrualRulesTest {
    @Test
    fun accruedTodayUsesLifetimeDelta() {
        assertEquals(
            BigInteger.valueOf(250),
            OreStakingRules.accruedTodayRaw(BigInteger.valueOf(1250), BigInteger.valueOf(1000)),
        )
    }

    @Test
    fun accruedTodayNeverInventsNegativeRewards() {
        assertEquals(
            BigInteger.ZERO,
            OreStakingRules.accruedTodayRaw(BigInteger.valueOf(900), BigInteger.valueOf(1000)),
        )
    }
}
