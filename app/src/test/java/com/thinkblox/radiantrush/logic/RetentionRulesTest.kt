package com.thinkblox.radiantrush.logic

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RetentionRulesTest {
    private fun input(day: String = "2026-09-12") = RetentionRules.RetentionInput(
        utcDayKey = day,
        currentStreak = 4,
        collectionOwned = 2,
        collectionTotal = 6,
        dailyRuns = 1,
        dailyPerfectHits = 7,
        dailyBestScore = 1400,
        dailyBestCombo = 9,
        dailyGameplayXp = 120,
        weeklyRuns = 2,
        weeklyRankedRuns = 1,
        weeklyChests = 1,
    )

    @Test
    fun dailyRotationIsStableAndHasThreeUniqueGoals() {
        val first = RetentionRules.dailyGoals(input())
        val second = RetentionRules.dailyGoals(input())
        assertEquals(first.map { it.id }, second.map { it.id })
        assertEquals(3, first.map { it.id }.distinct().size)
    }

    @Test
    fun weeklyGoalsUseRealProgress() {
        val goals = RetentionRules.weeklyGoals(input())
        assertEquals(2, goals.first { it.id == "weekly_runs" }.progress)
        assertFalse(goals.first { it.id == "weekly_runs" }.completed)
    }

    @Test
    fun milestonesAdvanceWithoutEconomicEffect() {
        val streak = RetentionRules.nextStreakMilestone(4)
        assertEquals(7, streak.target)
        assertEquals("Radiant Flame", streak.title)
        assertFalse(streak.unlocked)

        val collection = RetentionRules.nextCollectionMilestone(6, 6)
        assertTrue(collection.unlocked)
    }

    @Test
    fun twelveItemVaultUsesMidCollectionMilestone() {
        val early = RetentionRules.nextCollectionMilestone(collectionOwned = 2, collectionTotal = 12)
        assertEquals(3, early.target)
        assertEquals("Collector Crest", early.title)

        val middle = RetentionRules.nextCollectionMilestone(collectionOwned = 3, collectionTotal = 12)
        assertEquals(6, middle.target)
        assertEquals("Vault Seeker", middle.title)

        val final = RetentionRules.nextCollectionMilestone(collectionOwned = 6, collectionTotal = 12)
        assertEquals(12, final.target)
        assertEquals("Radiant Vault Complete", final.title)
    }

}
