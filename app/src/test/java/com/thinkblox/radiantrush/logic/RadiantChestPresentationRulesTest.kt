package com.thinkblox.radiantrush.logic

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RadiantChestPresentationRulesTest {
    @Test
    fun dailyChestRarityLadderMapsToIncreasingPresentationRanks() {
        assertEquals(0, RadiantChestPresentationRules.rarityRank("Spark"))
        assertEquals(1, RadiantChestPresentationRules.rarityRank("Pulse"))
        assertEquals(2, RadiantChestPresentationRules.rarityRank("Flare"))
        assertEquals(3, RadiantChestPresentationRules.rarityRank("Aurora"))
        assertEquals(4, RadiantChestPresentationRules.rarityRank("Legendary"))
    }

    @Test
    fun visualIntensityIncreasesWithoutChangingRewardData() {
        val sparkParticles = RadiantChestPresentationRules.particleCount("Spark")
        val legendaryParticles = RadiantChestPresentationRules.particleCount("Legendary")
        val sparkRings = RadiantChestPresentationRules.shockwaveCount("Spark")
        val legendaryRings = RadiantChestPresentationRules.shockwaveCount("Legendary")

        assertTrue(legendaryParticles > sparkParticles)
        assertTrue(legendaryRings > sparkRings)
        assertEquals(760L, RadiantChestPresentationRules.MINIMUM_OPENING_MS)
    }

    @Test
    fun olderGenericRarityNamesRemainSupported() {
        assertEquals(1, RadiantChestPresentationRules.rarityRank("Uncommon"))
        assertEquals(2, RadiantChestPresentationRules.rarityRank("Rare"))
        assertEquals(3, RadiantChestPresentationRules.rarityRank("Epic"))
        assertEquals(5, RadiantChestPresentationRules.rarityRank("Mythic"))
    }
}
