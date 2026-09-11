package com.thinkblox.radiantrush.logic

import com.thinkblox.radiantrush.data.QuestIds
import com.thinkblox.radiantrush.data.QuestStatus

/**
 * Phase 9/11C reward-loop rules.
 *
 * This is deliberately a no-loss chest reveal, not an XP wager. Spending earned
 * XP for random outcomes can feel like gambling and can punish normal users.
 * The app keeps the exciting gacha-style reveal while making rewards safe,
 * deterministic, once-per-day, and easy to test.
 *
 * Phase 11C.1 adds SKR Passport v2 chest perks. Verified liquid SKR plus verified
 * active SKR stake may add off-chain XP/tickets/cosmetic status, but never ranked score,
 * ranked attempts, or the deterministic base rarity roll.
 */
object RewardLoopRules {
    val requiredDailyQuestIds: Set<String> = setOf(
        QuestIds.DAILY_CHECK_IN,
        QuestIds.WALLET_CONNECT,
        QuestIds.SIGN_DAILY_PROOF,
        QuestIds.ON_CHAIN_PROOF,
        QuestIds.SKR_HOLDER,
    )

    fun canClaimDailyChest(
        questStatuses: Map<String, QuestStatus>,
        alreadyClaimedToday: Boolean,
    ): Boolean {
        if (alreadyClaimedToday) return false
        return requiredDailyQuestIds.all { questStatuses[it] == QuestStatus.Completed }
    }

    /**
     * Backward-compatible Phase 9 call. A generic holder keeps the historical
     * +50 XP behavior, while production Phase 11C passes the exact tier label.
     */
    fun pickDailyChestReward(
        todayKey: String,
        userSeed: String,
        currentStreak: Int,
        hasSkr: Boolean,
    ): RadiantChestReward = pickDailyChestReward(
        todayKey = todayKey,
        userSeed = userSeed,
        currentStreak = currentStreak,
        skrTierLabel = if (hasSkr) "Radiant Holder" else "Explorer",
    )

    fun pickDailyChestReward(
        todayKey: String,
        userSeed: String,
        currentStreak: Int,
        skrTierLabel: String,
    ): RadiantChestReward {
        val perks = SkrPassportRules.perksForTierLabel(skrTierLabel)
        return pickDailyChestReward(
            todayKey = todayKey,
            userSeed = userSeed,
            currentStreak = currentStreak,
            skrTierLabel = skrTierLabel,
            skrBonusXp = perks.chestBonusXp,
            skrBonusTickets = perks.chestBonusTickets,
        )
    }

    /**
     * Phase 11C.1 production path. Exact bonus values are persisted by today's
     * verified Passport scan so an active-stake boost survives into chest claim.
     * These values are applied only after the deterministic base rarity roll.
     */
    fun pickDailyChestReward(
        todayKey: String,
        userSeed: String,
        currentStreak: Int,
        skrTierLabel: String,
        skrBonusXp: Int,
        skrBonusTickets: Int,
    ): RadiantChestReward {
        // Important fairness rule: SKR/staking does not modify the base chest roll.
        val roll = stableRoll("$todayKey|$userSeed|$currentStreak")
        val base = when {
            roll < 55 -> BaseChestReward("Spark", "Radiant Spark", 125, "A clean daily spark. Keep the streak alive.")
            roll < 80 -> BaseChestReward("Pulse", "Radiant Pulse", 175, "The rush is getting stronger.")
            roll < 94 -> BaseChestReward("Flare", "Radiant Flare", 250, "A strong proof streak bonus reveal.")
            roll < 99 -> BaseChestReward("Aurora", "Radiant Aurora", 350, "Rare glow unlocked from today’s full proof run.")
            else -> BaseChestReward("Legendary", "Radiant Supernova", 500, "Legendary daily reveal. Screenshot this one.")
        }
        val streakBonusXp = currentStreak.coerceAtLeast(0).coerceAtMost(14) * 10
        val safeBonusXp = skrBonusXp.coerceAtLeast(0)
        val safeBonusTickets = skrBonusTickets.coerceAtLeast(0)
        val totalXp = base.baseXp + streakBonusXp + safeBonusXp

        return RadiantChestReward(
            rarity = base.rarity,
            title = base.title,
            baseXp = base.baseXp,
            streakBonusXp = streakBonusXp,
            skrBonusXp = safeBonusXp,
            skrBonusTickets = safeBonusTickets,
            skrPerkTier = skrTierLabel,
            totalXp = totalXp,
            revealLine = base.revealLine,
        )
    }

    private fun stableRoll(input: String): Int {
        var hash = 1125899906842597L
        input.forEach { char ->
            hash = 31L * hash + char.code.toLong()
        }
        return ((hash and Long.MAX_VALUE) % 100L).toInt()
    }

    private data class BaseChestReward(
        val rarity: String,
        val title: String,
        val baseXp: Int,
        val revealLine: String,
    )
}

data class RadiantChestReward(
    val rarity: String,
    val title: String,
    val baseXp: Int,
    val streakBonusXp: Int,
    val skrBonusXp: Int,
    val skrBonusTickets: Int,
    val skrPerkTier: String,
    val totalXp: Int,
    val revealLine: String,
)
