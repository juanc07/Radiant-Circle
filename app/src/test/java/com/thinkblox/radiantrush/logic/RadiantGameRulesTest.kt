package com.thinkblox.radiantrush.logic

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RadiantGameRulesTest {
    @Test
    fun starterTicketsAllowImmediateFirstRun() {
        assertTrue(RadiantGameRules.STARTER_TICKETS >= RadiantGameRules.RUN_TICKET_COST)
    }

    @Test
    fun rewardIsDeterministicForSameSubmittedRun() {
        val first = RadiantGameRules.pickRunReward(
            score = 2450,
            maxCombo = 11,
            userSeed = "unit-user",
            runSerial = 3,
            ownedCounts = emptyMap(),
        )
        val second = RadiantGameRules.pickRunReward(
            score = 2450,
            maxCombo = 11,
            userSeed = "unit-user",
            runSerial = 3,
            ownedCounts = emptyMap(),
        )

        assertEquals(first, second)
    }

    @Test
    fun firstDiscoveryDoesNotCreateDuplicateShards() {
        val reward = RadiantGameRules.pickRunReward(
            score = 1800,
            maxCombo = 8,
            userSeed = "new-collector",
            runSerial = 1,
            ownedCounts = emptyMap(),
        )

        assertFalse(reward.duplicate)
        assertEquals(0, reward.duplicateShards)
    }

    @Test
    fun ownedRewardConvertsDuplicateToShards() {
        val first = RadiantGameRules.pickRunReward(
            score = 1800,
            maxCombo = 8,
            userSeed = "duplicate-collector",
            runSerial = 2,
            ownedCounts = emptyMap(),
        )
        val second = RadiantGameRules.pickRunReward(
            score = 1800,
            maxCombo = 8,
            userSeed = "duplicate-collector",
            runSerial = 2,
            ownedCounts = mapOf(first.collectible.id to 1),
        )

        assertTrue(second.duplicate)
        assertTrue(second.duplicateShards > 0)
    }

    @Test
    fun capsuleTierTracksScoreBand() {
        assertEquals("Spark Capsule", RadiantGameRules.capsuleTierForScore(500))
        assertEquals("Pulse Capsule", RadiantGameRules.capsuleTierForScore(1200))
        assertEquals("Nova Capsule", RadiantGameRules.capsuleTierForScore(2200))
        assertEquals("Radiant Capsule", RadiantGameRules.capsuleTierForScore(3500))
    }

    @Test
    fun collectionPreviewKeepsLockedSilhouettesAndOwnedCounts() {
        val ownedId = RadiantGameRules.collectibles.first().id
        val preview = RadiantGameRules.collectionPreview(mapOf(ownedId to 2))

        assertEquals(RadiantGameRules.collectibles.size, preview.size)
        assertTrue(preview.first { it.id == ownedId }.discovered)
        assertEquals(2, preview.first { it.id == ownedId }.count)
        assertTrue(preview.any { !it.discovered })
    }
    @Test
    fun vaultContainsTwelveUniqueCollectiblesWithStableRarityCurve() {
        assertEquals(12, RadiantGameRules.collectibles.size)
        assertEquals(12, RadiantGameRules.collectibles.map { it.id }.distinct().size)

        val counts = RadiantGameRules.collectibles.groupingBy { it.rarity }.eachCount()
        assertEquals(3, counts["Common"])
        assertEquals(3, counts["Uncommon"])
        assertEquals(3, counts["Rare"])
        assertEquals(2, counts["Epic"])
        assertEquals(1, counts["Legendary"])
    }

    @Test
    fun legacyCollectibleIdsRemainAvailableAfterVaultExpansion() {
        val ids = RadiantGameRules.collectibles.map { it.id }.toSet()
        assertTrue(ids.contains("spark-bit"))
        assertTrue(ids.contains("neon-circuit"))
        assertTrue(ids.contains("solar-shard"))
        assertTrue(ids.contains("nova-prism"))
        assertTrue(ids.contains("phantom-halo"))
        assertTrue(ids.contains("radiant-crown"))
    }

    @Test
    fun `vault catalog is exactly twelve unique collectibles and keeps legacy ids`() {
        assertEquals(12, RadiantGameRules.COLLECTION_TOTAL)
        assertEquals(RadiantGameRules.COLLECTION_TOTAL, RadiantGameRules.collectibles.size)
        assertEquals(
            RadiantGameRules.COLLECTION_TOTAL,
            RadiantGameRules.collectibles.map { it.id }.distinct().size,
        )

        val ids = RadiantGameRules.collectibles.map { it.id }.toSet()
        assertTrue("spark-bit" in ids)
        assertTrue("neon-circuit" in ids)
        assertTrue("solar-shard" in ids)
        assertTrue("nova-prism" in ids)
        assertTrue("phantom-halo" in ids)
        assertTrue("radiant-crown" in ids)
    }

    @Test
    fun `collection preview always exposes all twelve slots for old profiles`() {
        val preview = RadiantGameRules.collectionPreview(
            mapOf(
                "spark-bit" to 2,
                "nova-prism" to 1,
            ),
        )

        assertEquals(RadiantGameRules.COLLECTION_TOTAL, preview.size)
        assertEquals(2, preview.first { it.id == "spark-bit" }.count)
        assertEquals(1, preview.first { it.id == "nova-prism" }.count)
        assertEquals(0, preview.first { it.id == "pulse-core" }.count)
    }

}
