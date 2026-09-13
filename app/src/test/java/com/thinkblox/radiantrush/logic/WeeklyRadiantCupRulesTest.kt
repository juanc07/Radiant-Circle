package com.thinkblox.radiantrush.logic

import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WeeklyRadiantCupRulesTest {
    @Test
    fun seasonEndsAtNextUtcMonday() {
        val thursday = Instant.parse("2026-09-10T12:34:56Z").toEpochMilli()
        val expected = Instant.parse("2026-09-14T00:00:00Z").toEpochMilli()

        assertEquals(expected, WeeklyRadiantCupRules.seasonEndsAtEpochMillis(thursday))
    }

    @Test
    fun previousWeekKeyTracksIsoWeek() {
        val current = Instant.parse("2026-09-14T00:00:01Z").toEpochMilli()

        assertEquals("2026-W37", WeeklyRadiantCupRules.previousWeekKey(current))
    }

    @Test
    fun placementRewardsAreCosmeticTiers() {
        assertEquals("Radiant Champion Crest", WeeklyRadiantCupRules.rewardForPlacement(1).title)
        assertEquals("Radiant Podium Crest", WeeklyRadiantCupRules.rewardForPlacement(3).title)
        assertEquals("Radiant Top 10 Ribbon", WeeklyRadiantCupRules.rewardForPlacement(10).title)
        assertEquals("Cup Finisher Mark", WeeklyRadiantCupRules.rewardForPlacement(42).title)
        assertEquals("Cup Finisher Mark", WeeklyRadiantCupRules.rewardForPlacement(null).title)
    }

    @Test
    fun sponsorConfigCanNeverEnablePayout() {
        val active = WeeklyRadiantCupRules.sponsorState(
            status = "announced",
            sponsorName = "Example Sponsor",
            prizeLabel = "1,000 SKR",
            note = "Final results require verification.",
        )

        assertTrue(active.active)
        assertEquals("Example Sponsor", active.sponsorName)
        assertEquals("1,000 SKR", active.prizeLabel)
        assertFalse(active.payoutEnabled)

        val malformed = WeeklyRadiantCupRules.sponsorState(
            status = "funded-and-pay-now",
            sponsorName = "Example Sponsor",
            prizeLabel = "1,000 SKR",
            note = null,
        )
        assertFalse(malformed.active)
        assertFalse(malformed.payoutEnabled)
    }
}
