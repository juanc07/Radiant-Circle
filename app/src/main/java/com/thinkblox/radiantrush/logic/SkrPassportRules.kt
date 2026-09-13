package com.thinkblox.radiantrush.logic

import java.math.BigDecimal

/**
 * Phase 11C.1 SKR Passport v2 perk rules.
 *
 * Passport tiering may use client-observed, read-only Mainnet liquid SKR plus a
 * verified active stake read from the official Solana Mobile SKR staking
 * program. Unstaking/cooldown SKR is displayed but intentionally excluded from
 * tier/perk calculations because it no longer earns staking rewards.
 *
 * Every benefit remains off-chain and non-ranked: casual-play tickets, Daily
 * Radiant Chest enhancement, and cosmetic status only. Staking never changes
 * raw ranked score, tie-breakers, or the three-ranked-attempt daily limit.
 */
object SkrPassportRules {
    const val PASSPORT_VERSION = 2
    const val STAKED_STATUS_NOT_VERIFIED = "Staking read not verified"
    const val STAKE_BOOST_DAILY_CASUAL_TICKETS = 1
    const val STAKE_BOOST_CHEST_XP = 25
    const val STAKE_BOOST_LABEL = "Guardian Stake Boost"

    fun perksForBalance(balance: BigDecimal): SkrPassportPerks =
        perksForTierLabel(SkrTierRules.tierForBalance(balance).label)

    /**
     * Builds the Passport decision from independently observed liquid and active
     * staked SKR. Staked SKR counts only when the staking read is verified.
     */
    fun perksForBalances(
        liquidBalance: BigDecimal,
        activeStakedBalance: BigDecimal,
        stakedVerified: Boolean,
    ): SkrPassportPerks {
        val safeLiquid = liquidBalance.coerceAtLeast(BigDecimal.ZERO)
        val safeStaked = if (stakedVerified) {
            activeStakedBalance.coerceAtLeast(BigDecimal.ZERO)
        } else {
            BigDecimal.ZERO
        }
        val eligibleBalance = safeLiquid + safeStaked
        val base = perksForTierLabel(SkrTierRules.tierForBalance(eligibleBalance).label)
        val stakeBoostActive = stakedVerified && safeStaked > BigDecimal.ZERO

        return if (stakeBoostActive) {
            base.copy(
                dailyCasualTicketBonus = base.dailyCasualTicketBonus + STAKE_BOOST_DAILY_CASUAL_TICKETS,
                chestBonusXp = base.chestBonusXp + STAKE_BOOST_CHEST_XP,
                auraLabel = if (base.tierLabel == "Explorer") {
                    "Guardian Glow Aura"
                } else {
                    "${base.auraLabel} + Guardian Glow"
                },
                stakeBoostActive = true,
                stakeBoostLabel = STAKE_BOOST_LABEL,
            )
        } else {
            base
        }
    }

    fun eligibleBalance(
        liquidBalance: BigDecimal,
        activeStakedBalance: BigDecimal,
        stakedVerified: Boolean,
    ): BigDecimal = liquidBalance.coerceAtLeast(BigDecimal.ZERO) +
        if (stakedVerified) activeStakedBalance.coerceAtLeast(BigDecimal.ZERO) else BigDecimal.ZERO

    fun perksForTierLabel(tierLabel: String): SkrPassportPerks = when (tierLabel) {
        "Radiant Legend" -> SkrPassportPerks(
            tierLabel = "Radiant Legend",
            dailyCasualTicketBonus = 4,
            chestBonusXp = 100,
            chestBonusTickets = 2,
            frameLabel = "Radiant Crown Frame",
            auraLabel = "Crownflare Aura",
            holderCollectibleLabel = "Legend Sigil",
        )
        "Radiant Elite" -> SkrPassportPerks(
            tierLabel = "Radiant Elite",
            dailyCasualTicketBonus = 3,
            chestBonusXp = 75,
            chestBonusTickets = 1,
            frameLabel = "Aurora Circuit Frame",
            auraLabel = "Aurora Pulse Aura",
            holderCollectibleLabel = "Elite Sigil",
        )
        "Radiant Holder" -> SkrPassportPerks(
            tierLabel = "Radiant Holder",
            dailyCasualTicketBonus = 2,
            chestBonusXp = 50,
            chestBonusTickets = 1,
            frameLabel = "Neon Halo Frame",
            auraLabel = "Neon Halo Aura",
            holderCollectibleLabel = "Holder Sigil",
        )
        "Radiant Scout" -> SkrPassportPerks(
            tierLabel = "Radiant Scout",
            dailyCasualTicketBonus = 1,
            chestBonusXp = 25,
            chestBonusTickets = 0,
            frameLabel = "Solar Spark Frame",
            auraLabel = "Spark Aura",
            holderCollectibleLabel = "Scout Sigil",
        )
        else -> SkrPassportPerks(
            tierLabel = "Explorer",
            dailyCasualTicketBonus = 0,
            chestBonusXp = 0,
            chestBonusTickets = 0,
            frameLabel = "Explorer Frame",
            auraLabel = "No holder aura",
            holderCollectibleLabel = "No holder collectible",
        )
    }

    /**
     * Grants only the missing portion of today's total Passport entitlement.
     * Repeated scans are idempotent while a newly verified stake/tier increase
     * can grant only the missing difference later in the same day.
     */
    fun dailyTicketGrantDelta(
        targetDailyBonus: Int,
        grantDate: String?,
        grantedToday: Int,
        todayKey: String,
    ): Int {
        val alreadyGranted = if (grantDate == todayKey) grantedToday.coerceAtLeast(0) else 0
        return (targetDailyBonus.coerceAtLeast(0) - alreadyGranted).coerceAtLeast(0)
    }
}

data class SkrPassportPerks(
    val tierLabel: String,
    val dailyCasualTicketBonus: Int,
    val chestBonusXp: Int,
    val chestBonusTickets: Int,
    val frameLabel: String,
    val auraLabel: String,
    val holderCollectibleLabel: String,
    val stakeBoostActive: Boolean = false,
    val stakeBoostLabel: String = "Stake Boost inactive",
) {
    val isHolder: Boolean
        get() = tierLabel != "Explorer"

    val chestEnhancementLabel: String
        get() = when {
            chestBonusXp > 0 && chestBonusTickets > 0 -> "+$chestBonusXp XP • +$chestBonusTickets ticket${if (chestBonusTickets == 1) "" else "s"}"
            chestBonusXp > 0 -> "+$chestBonusXp XP"
            else -> "Standard chest"
        }

    val dailyTicketLabel: String
        get() = if (dailyCasualTicketBonus > 0) {
            "+$dailyCasualTicketBonus bonus ticket${if (dailyCasualTicketBonus == 1) "" else "s"}/day"
        } else {
            "No holder ticket bonus"
        }
}
