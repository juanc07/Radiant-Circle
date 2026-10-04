package com.thinkblox.radiantrush.logic

import org.junit.Assert.assertTrue
import org.junit.Test

class DailyRadiancePresentationRulesTest {
    @Test
    fun openingGateLeavesTimeForAnticipationBeforeReveal() {
        assertTrue(
            DailyRadiancePresentationRules.MINIMUM_OPENING_MS >
                DailyRadiancePresentationRules.CHARGE_MS.toLong(),
        )
        assertTrue(DailyRadiancePresentationRules.REVEAL_MS in 300..700)
        assertTrue(DailyRadiancePresentationRules.SPARK_BURST_MS in 400..900)
    }
}
