package com.thinkblox.radiantrush.logic

import com.thinkblox.radiantrush.data.QuestIds
import com.thinkblox.radiantrush.data.QuestStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RewardLoopRulesTest {
    @Test
    fun chestRequiresAllDailyProofsAndNoPriorClaim() {
        val completed = RewardLoopRules.requiredDailyQuestIds.associateWith { QuestStatus.Completed }

        assertTrue(
            RewardLoopRules.canClaimDailyChest(
                questStatuses = completed,
                alreadyClaimedToday = false,
            ),
        )
        assertFalse(
            RewardLoopRules.canClaimDailyChest(
                questStatuses = completed,
                alreadyClaimedToday = true,
            ),
        )
    }

    @Test
    fun chestStaysLockedWhenOneProofIsMissing() {
        val missingMemo = RewardLoopRules.requiredDailyQuestIds.associateWith { QuestStatus.Completed }
            .plus(QuestIds.ON_CHAIN_PROOF to QuestStatus.Ready)

        assertFalse(
            RewardLoopRules.canClaimDailyChest(
                questStatuses = missingMemo,
                alreadyClaimedToday = false,
            ),
        )
    }

    @Test
    fun chestRewardIsDeterministicForSameDayAndUser() {
        val first = RewardLoopRules.pickDailyChestReward(
            todayKey = "2026-09-10",
            userSeed = "stable-user",
            currentStreak = 4,
            hasSkr = false,
        )
        val second = RewardLoopRules.pickDailyChestReward(
            todayKey = "2026-09-10",
            userSeed = "stable-user",
            currentStreak = 4,
            hasSkr = false,
        )

        assertEquals(first, second)
        assertEquals(first.baseXp + first.streakBonusXp + first.skrBonusXp, first.totalXp)
    }

    @Test
    fun skrHolderGetsChestBonusWithoutChangingBaseReward() {
        val noSkr = RewardLoopRules.pickDailyChestReward(
            todayKey = "2026-09-10",
            userSeed = "stable-user",
            currentStreak = 2,
            skrTierLabel = "Explorer",
        )
        val holder = RewardLoopRules.pickDailyChestReward(
            todayKey = "2026-09-10",
            userSeed = "stable-user",
            currentStreak = 2,
            skrTierLabel = "Radiant Holder",
        )

        assertEquals(noSkr.rarity, holder.rarity)
        assertEquals(noSkr.title, holder.title)
        assertEquals(noSkr.baseXp, holder.baseXp)
        assertEquals(0, noSkr.skrBonusXp)
        assertEquals(50, holder.skrBonusXp)
        assertEquals(1, holder.skrBonusTickets)
    }

    @Test
    fun legendGetsStrongestConfiguredChestPerkButSameBaseRoll() {
        val explorer = RewardLoopRules.pickDailyChestReward(
            todayKey = "2026-09-10",
            userSeed = "stable-user",
            currentStreak = 7,
            skrTierLabel = "Explorer",
        )
        val legend = RewardLoopRules.pickDailyChestReward(
            todayKey = "2026-09-10",
            userSeed = "stable-user",
            currentStreak = 7,
            skrTierLabel = "Radiant Legend",
        )

        assertEquals(explorer.rarity, legend.rarity)
        assertEquals(explorer.baseXp, legend.baseXp)
        assertEquals(100, legend.skrBonusXp)
        assertEquals(2, legend.skrBonusTickets)
        assertTrue(legend.totalXp > explorer.totalXp)
    }
    @Test
    fun verifiedStakeBoostUsesPersistedBonusButNeverChangesBaseRoll() {
        val explorer = RewardLoopRules.pickDailyChestReward(
            todayKey = "2026-09-11",
            userSeed = "stake-user",
            currentStreak = 3,
            skrTierLabel = "Explorer",
            skrBonusXp = 0,
            skrBonusTickets = 0,
        )
        val staker = RewardLoopRules.pickDailyChestReward(
            todayKey = "2026-09-11",
            userSeed = "stake-user",
            currentStreak = 3,
            skrTierLabel = "Radiant Holder",
            skrBonusXp = 75, // Holder 50 + verified Stake Boost 25.
            skrBonusTickets = 1,
        )

        assertEquals(explorer.rarity, staker.rarity)
        assertEquals(explorer.title, staker.title)
        assertEquals(explorer.baseXp, staker.baseXp)
        assertEquals(75, staker.skrBonusXp)
        assertEquals(1, staker.skrBonusTickets)
    }

}
