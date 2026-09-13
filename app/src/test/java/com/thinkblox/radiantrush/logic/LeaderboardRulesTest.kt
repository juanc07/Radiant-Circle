package com.thinkblox.radiantrush.logic

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LeaderboardRulesTest {
    private val walletA = "9xQeWvG816bUx9EPjHmaT23yvVMdqN5WwQ1Z6J8nPABC"
    private val walletB = "7YWHMfk9JZeLMg1D8Y87xYXxKj5iM2T6zKQHFrABCD9"

    @Test
    fun sameWalletAcrossAnonymousUidsCollapsesToBestXpRow() {
        val rows = listOf(
            candidate("uid-old", walletA, "9xQe…PABC", xp = 120, updatedAtMs = 10),
            candidate("uid-new", walletA, "9xQe…PABC", xp = 480, updatedAtMs = 20),
        )

        val result = LeaderboardRules.collapseByWallet(rows)

        assertEquals(1, result.size)
        assertEquals("uid-new", result.single().sourceId)
        assertEquals(480, result.single().xp)
    }

    @Test
    fun staleFullWalletWithNoWalletTombstoneIsExcluded() {
        val rows = listOf(
            // Older clear-wallet code could leave walletAddress stale while writing
            // walletAddressShort = "No wallet". That row must never stay ranked.
            candidate("old-disconnected-uid", walletA, "No wallet", xp = 900, updatedAtMs = 5),
            candidate("current-uid", walletA, "9xQe…PABC", xp = 400, updatedAtMs = 10),
        )

        val result = LeaderboardRules.collapseByWallet(rows)

        assertEquals(1, result.size)
        assertEquals("current-uid", result.single().sourceId)
    }

    @Test
    fun shortenedAddressAccidentallyStoredInFullFieldStillCollapses() {
        val rows = listOf(
            candidate("legacy-uid", "9xQe...PABC", null, xp = 250, updatedAtMs = 5),
            candidate("current-uid", walletA, "9xQe…PABC", xp = 400, updatedAtMs = 10),
        )

        val result = LeaderboardRules.collapseByWallet(rows)

        assertEquals(1, result.size)
        assertEquals("current-uid", result.single().sourceId)
    }

    @Test
    fun legacyShortOnlyRowCollapsesIntoUniqueFullWalletRow() {
        val rows = listOf(
            candidate("legacy-uid", null, "9xQe...PABC", xp = 250, updatedAtMs = 5),
            candidate("current-uid", walletA, "9xQe…PABC", xp = 400, updatedAtMs = 10),
        )

        val result = LeaderboardRules.collapseByWallet(rows)

        assertEquals(1, result.size)
        assertEquals("current-uid", result.single().sourceId)
    }

    @Test
    fun rowsWithoutWalletAreNotPublicRanks() {
        val rows = listOf(
            candidate("anonymous-only", null, null, xp = 9999, updatedAtMs = 50),
            candidate("disconnected", null, "No wallet", xp = 8888, updatedAtMs = 40),
        )

        assertTrue(LeaderboardRules.collapseByWallet(rows).isEmpty())
    }

    @Test
    fun genuinelyDifferentFullWalletsRemainSeparate() {
        val rows = listOf(
            candidate("a", walletA, "9xQe…PABC", xp = 100, updatedAtMs = 1),
            candidate("b", walletB, "7YWH…BCD9", xp = 500, updatedAtMs = 1),
        )

        val result = LeaderboardRules.collapseByWallet(rows)

        assertEquals(2, result.size)
        assertEquals(listOf("b", "a"), result.map { it.sourceId })
    }

    @Test
    fun walletLabelUsesCanonicalShortFormAndHidesDisconnectedRow() {
        assertEquals(
            "9xQe…PABC",
            LeaderboardRules.walletLabel(candidate("a", walletA, null, xp = 1, updatedAtMs = 1)),
        )
        assertNull(
            LeaderboardRules.walletLabel(candidate("b", walletA, "No wallet", xp = 1, updatedAtMs = 1)),
        )
    }

    @Test
    fun newestProfileIdentityIsUsedWithoutChangingBestXp() {
        val oldBest = candidate("uid-old", walletA, "9xQe…PABC", xp = 900, updatedAtMs = 10).copy(
            displayName = "Old Name",
            avatarId = "fox",
        )
        val freshProfile = candidate("uid-new", walletA, "9xQe…PABC", xp = 120, updatedAtMs = 50).copy(
            displayName = "New Name",
            avatarId = "ghost",
        )

        val result = LeaderboardRules.collapseByWallet(listOf(oldBest, freshProfile)).single()

        assertEquals(900, result.xp)
        assertEquals("uid-old", result.sourceId)
        assertEquals("New Name", result.displayName)
        assertEquals("ghost", result.avatarId)
    }

    private fun candidate(
        sourceId: String,
        walletAddress: String?,
        walletAddressShort: String?,
        xp: Int,
        updatedAtMs: Long,
    ) = LeaderboardCandidate(
        sourceId = sourceId,
        displayName = "Radiant Rookie",
        walletAddress = walletAddress,
        walletAddressShort = walletAddressShort,
        xp = xp,
        streak = 1,
        tier = "Explorer",
        updatedAtMs = updatedAtMs,
    )
}
