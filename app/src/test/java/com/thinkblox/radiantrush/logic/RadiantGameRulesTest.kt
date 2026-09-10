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
}
