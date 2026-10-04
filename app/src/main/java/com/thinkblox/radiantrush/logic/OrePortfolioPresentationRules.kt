package com.thinkblox.radiantrush.logic

import java.math.BigDecimal
import java.math.BigInteger
import java.math.RoundingMode

/** Presentation-only ORE helpers. On-chain reads remain exact in [OreStakingRules]. */
object OrePortfolioPresentationRules {
    private const val OVERVIEW_DECIMALS = 4
    private val DISPLAY_SCALE = BigDecimal.TEN.pow(OreStakingRules.TOKEN_DECIMALS)
    private val SMALL_VISIBLE_AMOUNT = BigDecimal.ONE.movePointLeft(OVERVIEW_DECIMALS)

    fun totalPositionRaw(liquidRaw: String, stakedRaw: String): BigInteger =
        nonNegativeRaw(liquidRaw) + nonNegativeRaw(stakedRaw)

    fun overviewAmount(raw: String): String = overviewAmount(nonNegativeRaw(raw))

    fun overviewAmount(raw: BigInteger): String {
        val safe = raw.coerceAtLeast(BigInteger.ZERO)
        if (safe == BigInteger.ZERO) return "0 ORE"

        val exact = BigDecimal(safe).divide(DISPLAY_SCALE)
        if (exact < SMALL_VISIBLE_AMOUNT) return "<0.0001 ORE"

        val rounded = exact
            .setScale(OVERVIEW_DECIMALS, RoundingMode.HALF_UP)
            .stripTrailingZeros()
            .toPlainString()
        return "$rounded ORE"
    }

    fun stakedPercent(liquidRaw: String, stakedRaw: String): Int {
        val liquid = nonNegativeRaw(liquidRaw)
        val staked = nonNegativeRaw(stakedRaw)
        val total = liquid + staked
        if (total == BigInteger.ZERO) return 0
        return BigDecimal(staked)
            .multiply(BigDecimal(100))
            .divide(BigDecimal(total), 0, RoundingMode.HALF_UP)
            .toInt()
            .coerceIn(0, 100)
    }

    private fun nonNegativeRaw(value: String): BigInteger =
        value.toBigIntegerOrNull()?.coerceAtLeast(BigInteger.ZERO) ?: BigInteger.ZERO
}
