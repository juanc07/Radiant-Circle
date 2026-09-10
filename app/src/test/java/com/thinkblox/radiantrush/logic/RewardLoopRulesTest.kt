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
            hasSkr = false,
        )
        val withSkr = RewardLoopRules.pickDailyChestReward(
            todayKey = "2026-09-10",
            userSeed = "stable-user",
            currentStreak = 2,
            hasSkr = true,
        )

        assertEquals(0, noSkr.skrBonusXp)
        assertEquals(50, withSkr.skrBonusXp)
        assertTrue(withSkr.totalXp >= withSkr.baseXp)
    }
}
