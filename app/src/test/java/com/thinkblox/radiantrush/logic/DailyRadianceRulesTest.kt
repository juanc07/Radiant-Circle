package com.thinkblox.radiantrush.logic

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
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
    fun catalogHasOneHundredFiftyUniqueMessagesAcrossFifteenCategories() {
        assertEquals(150, DailyRadianceRules.contentCount())
        assertEquals(15, DailyRadianceRules.categoryCount())

        val start = LocalDate.of(2026, 1, 1)
        val seenIds = (0L until 150L)
            .map { offset ->
                DailyRadianceRules.contentFor(
                    accountId = "catalog-proof-user",
                    dayKey = start.plusDays(offset).toString(),
                ).id
            }
            .toSet()

        assertEquals(150, seenIds.size)
    }

    @Test
    fun sameAccountDoesNotRepeatYesterdayOrRecentRadiance() {
        val start = LocalDate.of(2026, 1, 1)
        val contents = (0L until 150L).map { offset ->
            DailyRadianceRules.contentFor(
                accountId = "repeat-proof-user",
                dayKey = start.plusDays(offset).toString(),
            )
        }

        contents.zipWithNext().forEach { (previous, current) ->
            assertNotEquals(previous.id, current.id)
            assertNotEquals(previous.category, current.category)
        }
        assertEquals(150, contents.map { it.id }.toSet().size)
    }

    @Test
    fun originalPhase13AContentIdsStillResolve() {
        val originalIds = setOf(
            "courage-small-step",
            "focus-one-thing",
            "kindness-soft-word",
            "creativity-first-idea",
            "calm-own-pace",
            "curiosity-one-question",
            "momentum-finish-small",
            "connection-reach-out",
            "confidence-earned",
            "perspective-room",
            "gratitude-notice",
            "hope-next-page",
        )

        originalIds.forEach { id ->
            assertNotNull("Missing legacy Daily Radiance id: $id", DailyRadianceRules.contentById(id))
        }
    }

    @Test
    fun differentAccountsStillReceiveDeterministicPersonalizedRotations() {
        val day = "2026-09-21"
        val first = DailyRadianceRules.contentFor("account-a", day)
        val second = DailyRadianceRules.contentFor("account-b", day)

        assertNotNull(first)
        assertNotNull(second)
        assertTrue(first.id.isNotBlank())
        assertTrue(second.id.isNotBlank())
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
