package com.thinkblox.radiantrush.logic

import java.math.BigDecimal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SkrPassportRulesTest {
    @Test
    fun explorerGetsNoHolderBenefits() {
        val perks = SkrPassportRules.perksForBalance(BigDecimal.ZERO)

        assertEquals("Explorer", perks.tierLabel)
        assertEquals(0, perks.dailyCasualTicketBonus)
        assertEquals(0, perks.chestBonusXp)
        assertEquals(0, perks.chestBonusTickets)
        assertFalse(perks.isHolder)
        assertFalse(perks.stakeBoostActive)
    }

    @Test
    fun holderTiersScaleCasualAndChestPerksWithoutRankedFields() {
        val scout = SkrPassportRules.perksForBalance(BigDecimal("1"))
        val holder = SkrPassportRules.perksForBalance(BigDecimal("100"))
        val elite = SkrPassportRules.perksForBalance(BigDecimal("1000"))
        val legend = SkrPassportRules.perksForBalance(BigDecimal("10000"))

        assertEquals(1, scout.dailyCasualTicketBonus)
        assertEquals(2, holder.dailyCasualTicketBonus)
        assertEquals(3, elite.dailyCasualTicketBonus)
        assertEquals(4, legend.dailyCasualTicketBonus)
        assertEquals(25, scout.chestBonusXp)
        assertEquals(50, holder.chestBonusXp)
        assertEquals(75, elite.chestBonusXp)
        assertEquals(100, legend.chestBonusXp)
        assertEquals(0, scout.chestBonusTickets)
        assertEquals(1, holder.chestBonusTickets)
        assertEquals(1, elite.chestBonusTickets)
        assertEquals(2, legend.chestBonusTickets)
        assertTrue(legend.isHolder)
    }

    @Test
    fun verifiedActiveStakeCountsTowardPassportTierAndGetsStakeBoost() {
        val perks = SkrPassportRules.perksForBalances(
            liquidBalance = BigDecimal.ZERO,
            activeStakedBalance = BigDecimal("1000"),
            stakedVerified = true,
        )

        assertEquals("Radiant Elite", perks.tierLabel)
        assertEquals(4, perks.dailyCasualTicketBonus) // Elite 3 + Stake Boost 1.
        assertEquals(100, perks.chestBonusXp) // Elite 75 + Stake Boost 25.
        assertEquals(1, perks.chestBonusTickets)
        assertTrue(perks.stakeBoostActive)
        assertEquals(SkrPassportRules.STAKE_BOOST_LABEL, perks.stakeBoostLabel)
    }

    @Test
    fun liquidAndVerifiedStakeCombineForTier() {
        assertEquals(
            BigDecimal("110"),
            SkrPassportRules.eligibleBalance(
                liquidBalance = BigDecimal("10"),
                activeStakedBalance = BigDecimal("100"),
                stakedVerified = true,
            ),
        )
        assertEquals(
            "Radiant Holder",
            SkrPassportRules.perksForBalances(
                liquidBalance = BigDecimal("10"),
                activeStakedBalance = BigDecimal("100"),
                stakedVerified = true,
            ).tierLabel,
        )
    }

    @Test
    fun unverifiedStakeIsNeverInventedOrCounted() {
        val perks = SkrPassportRules.perksForBalances(
            liquidBalance = BigDecimal("10"),
            activeStakedBalance = BigDecimal("10000"),
            stakedVerified = false,
        )

        assertEquals("Radiant Scout", perks.tierLabel)
        assertEquals(1, perks.dailyCasualTicketBonus)
        assertEquals(25, perks.chestBonusXp)
        assertFalse(perks.stakeBoostActive)
        assertEquals(
            BigDecimal("10"),
            SkrPassportRules.eligibleBalance(BigDecimal("10"), BigDecimal("10000"), false),
        )
    }

    @Test
    fun dailyTicketGrantIsIdempotentAndAllowsOnlyEntitlementDelta() {
        assertEquals(
            3,
            SkrPassportRules.dailyTicketGrantDelta(
                targetDailyBonus = 3,
                grantDate = null,
                grantedToday = 0,
                todayKey = "2026-09-11",
            ),
        )
        assertEquals(
            0,
            SkrPassportRules.dailyTicketGrantDelta(
                targetDailyBonus = 3,
                grantDate = "2026-09-11",
                grantedToday = 3,
                todayKey = "2026-09-11",
            ),
        )
        assertEquals(
            1,
            SkrPassportRules.dailyTicketGrantDelta(
                targetDailyBonus = 4,
                grantDate = "2026-09-11",
                grantedToday = 3,
                todayKey = "2026-09-11",
            ),
        )
        assertEquals(
            3,
            SkrPassportRules.dailyTicketGrantDelta(
                targetDailyBonus = 3,
                grantDate = "2026-09-10",
                grantedToday = 3,
                todayKey = "2026-09-11",
            ),
        )
    }
}
