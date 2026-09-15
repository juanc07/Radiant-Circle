package com.thinkblox.radiantrush.logic

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class DailyRadianceRulesTest {
    @Test
    fun sameAccountAndDayAlwaysGetSameRadiance() {
        val first = DailyRadianceRules.contentFor(
            accountId = "firebase-user-123",
            dayKey = "2026-09-15",
        )
        val second = DailyRadianceRules.contentFor(
            accountId = "firebase-user-123",
            dayKey = "2026-09-15",
        )

        assertEquals(first, second)
        assertNotNull(DailyRadianceRules.contentById(first.id))
    }

    @Test
    fun walletIdentityIsNotNeededToChooseDailyRadiance() {
        val content = DailyRadianceRules.contentFor(
            accountId = "firebase-user-123",
            dayKey = "2026-09-15",
        )

        assertEquals(
            content,
            DailyRadianceRules.contentFor(
                accountId = "firebase-user-123",
                dayKey = "2026-09-15",
            ),
        )
    }

    @Test
    fun yesterdayContinuesStreak() {
        assertEquals(
            5,
            DailyRadianceRules.nextStreak(
                lastOpenedDay = "2026-09-14",
                todayKey = "2026-09-15",
                currentStreak = 4,
            ),
        )
    }

    @Test
    fun sameDayRetryIsIdempotent() {
        assertEquals(
            4,
            DailyRadianceRules.nextStreak(
                lastOpenedDay = "2026-09-15",
                todayKey = "2026-09-15",
                currentStreak = 4,
            ),
        )
    }

    @Test
    fun missedDayRestartsStreak() {
        assertEquals(
            1,
            DailyRadianceRules.nextStreak(
                lastOpenedDay = "2026-09-12",
                todayKey = "2026-09-15",
                currentStreak = 9,
            ),
        )
        assertEquals(
            0,
            DailyRadianceRules.visibleStreak(
                lastOpenedDay = "2026-09-12",
                todayKey = "2026-09-15",
                savedStreak = 9,
            ),
        )
    }
}
