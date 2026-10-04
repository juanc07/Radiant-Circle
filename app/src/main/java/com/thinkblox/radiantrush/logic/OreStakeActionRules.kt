package com.thinkblox.radiantrush.logic

import com.thinkblox.radiantrush.data.OreStakeAction
import java.math.BigDecimal
import java.math.BigInteger
import java.math.RoundingMode

/** Pure validation/verification rules for user-signed ORE stake actions. */
object OreStakeActionRules {
    private val U64_MAX = BigInteger.ONE.shiftLeft(64).subtract(BigInteger.ONE)
    private val INPUT_PATTERN = Regex("^(?:0|[1-9]\\d*)(?:\\.(\\d{0,11}))?$")

    fun parseOreInputToRaw(input: String): BigInteger {
        val clean = input.trim()
        require(clean.isNotBlank()) { "Enter an ORE amount." }
        require(INPUT_PATTERN.matches(clean)) { "Use a normal ORE amount with up to 11 decimal places." }

        val raw = BigDecimal(clean)
            .movePointRight(OreStakingRules.TOKEN_DECIMALS)
            .setScale(0, RoundingMode.UNNECESSARY)
            .toBigIntegerExact()

        require(raw > BigInteger.ZERO) { "ORE amount must be greater than zero." }
        require(raw <= U64_MAX) { "ORE amount is too large for the staking protocol." }
        return raw
    }

    fun validateAvailable(action: OreStakeAction, requestedRaw: BigInteger, liquidRaw: String, currentStakedRaw: String, currentClaimableRaw: String): String? {
        val available = when (action) {
            OreStakeAction.Stake -> liquidRaw.toBigIntegerOrNull()
            OreStakeAction.Withdraw -> currentStakedRaw.toBigIntegerOrNull()
            OreStakeAction.Claim -> currentClaimableRaw.toBigIntegerOrNull()
        } ?: return "Current ORE balance could not be decoded. Refresh before trying again."

        if (requestedRaw <= BigInteger.ZERO) return "ORE amount must be greater than zero."
        if (requestedRaw > available) {
            return when (action) {
                OreStakeAction.Stake -> "Amount is higher than your available ORE."
                OreStakeAction.Withdraw -> "Amount is higher than your current-program stake."
                OreStakeAction.Claim -> "Amount is higher than your currently claimable rewards."
            }
        }
        return null
    }

    fun verifiedAfterRefresh(
        action: OreStakeAction,
        requestedRaw: BigInteger,
        beforeLiquidRaw: String,
        beforeCurrentStakedRaw: String,
        afterLiquidRaw: String,
        afterCurrentStakedRaw: String,
    ): Boolean {
        val beforeLiquid = beforeLiquidRaw.toBigIntegerOrNull() ?: return false
        val beforeStaked = beforeCurrentStakedRaw.toBigIntegerOrNull() ?: return false
        val afterLiquid = afterLiquidRaw.toBigIntegerOrNull() ?: return false
        val afterStaked = afterCurrentStakedRaw.toBigIntegerOrNull() ?: return false

        return when (action) {
            OreStakeAction.Stake -> afterStaked - beforeStaked >= requestedRaw
            OreStakeAction.Withdraw ->
                beforeStaked - afterStaked >= requestedRaw && afterLiquid - beforeLiquid >= requestedRaw
            OreStakeAction.Claim -> afterLiquid - beforeLiquid >= requestedRaw
        }
    }
}
