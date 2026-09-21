package com.thinkblox.radiantrush.logic

import com.thinkblox.radiantrush.data.ApproximateCircleLocation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CircleDiscoveryRulesTest {
    private val location = ApproximateCircleLocation(
        latitude = 14.30,
        longitude = 120.95,
        countryCode = "PH",
    )

    @Test
    fun presenceContainsOnlyOpaqueLocationKeys() {
        val keys = CircleDiscoveryRules.presenceKeys(location, nowMillis = 1_000_000L)
        val joined = listOf(
            keys.localWindowKeys,
            keys.regionalWindowKeys,
            keys.broadWindowKeys,
            keys.countryWindowKeys,
            keys.globalWindowKeys,
        ).flatten().joinToString("|")

        assertTrue("14.3" !in joined)
        assertTrue("120.95" !in joined)
        assertTrue("PH" !in joined)
        assertTrue(joined.split('|').all { it.matches(Regex("[0-9a-f]{32}")) })
    }

    @Test
    fun persistentPresenceKeysDoNotRotateWithTime() {
        val first = CircleDiscoveryRules.presenceKeys(location, nowMillis = 1_000_000L)
        val muchLater = CircleDiscoveryRules.presenceKeys(
            location,
            nowMillis = 1_000_000L + (30L * 24 * 60 * 60 * 1000),
        )
        assertEquals(first, muchLater)
    }

    @Test
    fun localQueryUsesNineOpaqueNeighborCells() {
        val keys = CircleDiscoveryRules.queryKeys(
            CircleDiscoveryRules.SearchTier.Local,
            location,
            nowMillis = 10 * 60 * 1000L,
        )
        assertEquals(9, keys.size)
        assertTrue(keys.all { it.matches(Regex("[0-9a-f]{32}")) })
    }

    @Test
    fun queryKeysDoNotRotateWithTime() {
        val first = CircleDiscoveryRules.queryKeys(
            CircleDiscoveryRules.SearchTier.Local,
            location,
            nowMillis = 1_000_000L,
        )
        val muchLater = CircleDiscoveryRules.queryKeys(
            CircleDiscoveryRules.SearchTier.Local,
            location,
            nowMillis = 1_000_000L + (30L * 24 * 60 * 60 * 1000),
        )
        assertEquals(first, muchLater)
    }

    @Test
    fun pairIdIsOrderIndependent() {
        assertEquals(
            CircleDiscoveryRules.pairId("user-a", "user-b"),
            CircleDiscoveryRules.pairId("user-b", "user-a"),
        )
    }
}
