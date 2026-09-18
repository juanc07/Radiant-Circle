package com.thinkblox.radiantrush.logic

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WalletFeatureAccessRulesTest {
    private val startsAt = 1_789_344_000_000L
    private val endsAt = startsAt + 7L * 24L * 60L * 60L * 1_000L

    @Test
    fun openCupInsideWindowRequiresWalletWarningForCasualRun() {
        val access = WalletFeatureAccessRules.radiantRushAccess(
            walletConnected = false,
            cupStatusCode = "OPEN",
            startsAtEpochMillis = startsAt,
            endsAtEpochMillis = endsAt,
            nowEpochMillis = startsAt + 1_000L,
        )

        assertTrue(access.cupRunning)
        assertTrue(access.shouldWarnBeforeCasualRun)
        assertFalse(access.rankedCompetitionAvailable)
    }

    @Test
    fun connectedWalletDoesNotNeedCupWarning() {
        val access = WalletFeatureAccessRules.radiantRushAccess(
            walletConnected = true,
            cupStatusCode = "OPEN",
            startsAtEpochMillis = startsAt,
            endsAtEpochMillis = endsAt,
            nowEpochMillis = startsAt + 1_000L,
        )

        assertTrue(access.cupRunning)
        assertFalse(access.shouldWarnBeforeCasualRun)
        assertTrue(access.rankedCompetitionAvailable)
    }

    @Test
    fun expiredOpenDocumentIsNotTreatedAsRunningCup() {
        val access = WalletFeatureAccessRules.radiantRushAccess(
            walletConnected = false,
            cupStatusCode = "OPEN",
            startsAtEpochMillis = startsAt,
            endsAtEpochMillis = endsAt,
            nowEpochMillis = endsAt,
        )

        assertFalse(access.cupRunning)
        assertFalse(access.shouldWarnBeforeCasualRun)
    }

    @Test
    fun draftCupDoesNotTriggerTournamentWarning() {
        val access = WalletFeatureAccessRules.radiantRushAccess(
            walletConnected = false,
            cupStatusCode = "DRAFT",
            startsAtEpochMillis = startsAt,
            endsAtEpochMillis = endsAt,
            nowEpochMillis = startsAt + 1_000L,
        )

        assertFalse(access.cupRunning)
        assertFalse(access.shouldWarnBeforeCasualRun)
    }
}
