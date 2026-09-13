package com.thinkblox.radiantrush.logic

import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class Phase11CompetitionRulesTest {
    private val walletA = "9xQeWvG816bUx9EPjHmaT23yvVMdqN5WwQ1Z6J8nPABC"
    private val walletB = "7YWHMfk9JZeLMg1D8Y87xYXxKj5iM2T6zKQHFrABCD9"

    @Test
    fun utcWeekKeyUsesIsoMondayBoundary() {
        val sunday = Instant.parse("2026-09-13T23:59:59Z").toEpochMilli()
        val monday = Instant.parse("2026-09-14T00:00:00Z").toEpochMilli()

        assertEquals("2026-W37", Phase11CompetitionRules.utcWeekKey(sunday))
        assertEquals("2026-W38", Phase11CompetitionRules.utcWeekKey(monday))
    }

    @Test
    fun performanceXpIsControlledAndNotScoreOneToOne() {
        val xp = Phase11CompetitionRules.performanceXpForRun(
            score = 4_000,
            maxCombo = 20,
            perfectHits = 12,
        )

        assertTrue(xp > 0)
        assertTrue(xp < 4_000)
        assertEquals(172, xp)
    }

    @Test
    fun gameplayXpCapsAtThreeHundredAndResetsOnNextUtcDay() {
        val dayOne = Instant.parse("2026-09-10T23:59:00Z").toEpochMilli()
        val capped = Phase11CompetitionRules.cappedGameplayXp(
            score = 4_000,
            maxCombo = 20,
            perfectHits = 12,
            savedDayKey = "2026-09-10",
            savedEarnedToday = 290,
            completedAtEpochMillis = dayOne,
        )
        assertEquals(10, capped.grantedXp)
        assertEquals(300, capped.earnedAfter)
        assertEquals(0, capped.remainingToday)
        assertTrue(capped.wasCapped)

        val nextDay = Instant.parse("2026-09-11T00:00:01Z").toEpochMilli()
        val reset = Phase11CompetitionRules.cappedGameplayXp(
            score = 500,
            maxCombo = 1,
            perfectHits = 0,
            savedDayKey = "2026-09-10",
            savedEarnedToday = 300,
            completedAtEpochMillis = nextDay,
        )
        assertEquals(0, reset.earnedBefore)
        assertTrue(reset.grantedXp > 0)
    }

    @Test
    fun rankedAttemptsAreEqualAndExtraRunsBecomeCasual() {
        val now = Instant.parse("2026-09-10T12:00:00Z").toEpochMilli()
        val first = Phase11CompetitionRules.rankedAttemptDecision(
            walletConnected = true,
            savedDayKey = "2026-09-10",
            savedAttemptsUsed = 0,
            completedAtEpochMillis = now,
        )
        assertEquals(RunCompetitionMode.Ranked, first.mode)
        assertEquals(1, first.rankedAttemptsUsedAfter)
        assertEquals(2, first.rankedAttemptsRemaining)

        val fourth = Phase11CompetitionRules.rankedAttemptDecision(
            walletConnected = true,
            savedDayKey = "2026-09-10",
            savedAttemptsUsed = 3,
            completedAtEpochMillis = now,
        )
        assertEquals(RunCompetitionMode.Casual, fourth.mode)
        assertEquals(3, fourth.rankedAttemptsUsedAfter)
        assertEquals(0, fourth.rankedAttemptsRemaining)
    }

    @Test
    fun noWalletMeansCasualAndDoesNotConsumeRankedAttempt() {
        val now = Instant.parse("2026-09-10T12:00:00Z").toEpochMilli()
        val decision = Phase11CompetitionRules.rankedAttemptDecision(
            walletConnected = false,
            savedDayKey = "2026-09-10",
            savedAttemptsUsed = 1,
            completedAtEpochMillis = now,
        )

        assertEquals(RunCompetitionMode.Casual, decision.mode)
        assertEquals(1, decision.rankedAttemptsUsedBefore)
        assertEquals(1, decision.rankedAttemptsUsedAfter)
        assertEquals(0, decision.rankedAttemptsRemaining)
    }

    @Test
    fun clientRunRecordCanNeverBePayoutEligible() {
        val now = Instant.parse("2026-09-10T12:00:00Z").toEpochMilli()
        val run = Phase11CompetitionRules.createRunScoreRecord(
            runId = "run-1",
            ownerUid = "uid-1",
            displayName = "Player",
            walletAddress = walletA,
            walletAddressShort = "9xQe…PABC",
            score = -50,
            maxCombo = -2,
            perfectHits = -1,
            radiantHits = 4,
            corruptedHits = 0,
            mode = RunCompetitionMode.Ranked,
            completedAtEpochMillis = now,
        )

        assertEquals(0, run.score)
        assertEquals(0, run.maxCombo)
        assertEquals(0, run.perfectHits)
        assertEquals(RunScoreAuthority.ClientReportedPrototype, run.authority)
        assertFalse(run.payoutEligible)
        assertEquals("2026-09-10", run.utcDayKey)
        assertEquals("2026-W37", run.utcWeekKey)
    }

    @Test
    fun weeklyStatsUseRankedRawScoreAndIgnoreCasualRun() {
        val ranked = run(score = 2_000, combo = 10, perfects = 4, mode = RunCompetitionMode.Ranked)
        val weekly = Phase11CompetitionRules.updateWeeklyStats(null, ranked)

        assertEquals(2_000, weekly?.bestScore)
        assertEquals(10, weekly?.bestCombo)
        assertEquals(4, weekly?.perfectHitsAtBestScore)
        assertEquals(1, weekly?.rankedRunsPlayed)

        val casual = run(score = 9_999, combo = 99, perfects = 99, mode = RunCompetitionMode.Casual)
        val unchanged = Phase11CompetitionRules.updateWeeklyStats(weekly, casual)
        assertEquals(weekly, unchanged)
    }

    @Test
    fun allTimePersonalBestPrefersRawScoreThenTieBreakers() {
        val first = Phase11CompetitionRules.updatePersonalBest(
            current = null,
            run = run(score = 2_500, combo = 8, perfects = 3),
        )
        val sameScoreBetterCombo = Phase11CompetitionRules.updatePersonalBest(
            current = first,
            run = run(score = 2_500, combo = 12, perfects = 2, millisOffset = 1_000),
        )
        val lowerScoreHugeCombo = Phase11CompetitionRules.updatePersonalBest(
            current = sameScoreBetterCombo,
            run = run(score = 2_499, combo = 99, perfects = 99, millisOffset = 2_000),
        )

        assertEquals(2_500, lowerScoreHugeCombo?.score)
        assertEquals(12, lowerScoreHugeCombo?.bestCombo)
        assertEquals(2, lowerScoreHugeCombo?.perfectHits)
    }

    @Test
    fun runBoardCollapsesDuplicateAnonymousUidsByWallet() {
        val rows = listOf(
            candidate("uid-old", walletA, "9xQe…PABC", score = 1_500),
            candidate("uid-new", walletA, "9xQe…PABC", score = 2_500),
        )

        val result = Phase11CompetitionRules.collapseRunLeaderboardByWallet(rows)

        assertEquals(1, result.size)
        assertEquals("uid-new", result.single().sourceId)
        assertEquals(2_500, result.single().score)
    }

    @Test
    fun legacyShortRunRowCollapsesIntoUniqueFullWallet() {
        val rows = listOf(
            candidate("legacy", null, "9xQe...PABC", score = 1_900),
            candidate("current", walletA, "9xQe…PABC", score = 2_100),
        )

        val result = Phase11CompetitionRules.collapseRunLeaderboardByWallet(rows)

        assertEquals(1, result.size)
        assertEquals("current", result.single().sourceId)
    }

    @Test
    fun disconnectedTombstoneNeverAppearsOnRunBoard() {
        val rows = listOf(
            candidate("stale", walletA, "No wallet", score = 99_999),
            candidate("current", walletA, "9xQe…PABC", score = 2_000),
        )

        val result = Phase11CompetitionRules.collapseRunLeaderboardByWallet(rows)

        assertEquals(1, result.size)
        assertEquals("current", result.single().sourceId)
        assertNull(Phase11CompetitionRules.walletLabel(rows.first()))
    }

    @Test
    fun differentWalletsRemainDifferentRunPlayers() {
        val rows = listOf(
            candidate("a", walletA, "9xQe…PABC", score = 2_000),
            candidate("b", walletB, "7YWH…BCD9", score = 3_000),
        )

        val result = Phase11CompetitionRules.collapseRunLeaderboardByWallet(rows)

        assertEquals(listOf("b", "a"), result.map { it.sourceId })
        assertEquals("9xQe…PABC", Phase11CompetitionRules.walletLabel(rows.first()))
    }

    @Test
    fun walletRankUsesDedupedWeeklyBoard() {
        val rows = listOf(
            candidate("wallet-b", walletB, "7YWH…BCD9", score = 3_000),
            candidate("wallet-a-old", walletA, "9xQe…PABC", score = 1_000),
            candidate("wallet-a-best", walletA, "9xQe…PABC", score = 2_500),
        )

        assertEquals(2, Phase11CompetitionRules.walletRank(walletA, rows, limit = 100))
        assertEquals(1, Phase11CompetitionRules.walletRank(walletB, rows, limit = 100))
        assertNull(Phase11CompetitionRules.walletRank(null, rows, limit = 100))
    }

    private fun run(
        score: Int,
        combo: Int,
        perfects: Int,
        mode: RunCompetitionMode = RunCompetitionMode.Ranked,
        millisOffset: Long = 0L,
    ): RunScoreRecord = Phase11CompetitionRules.createRunScoreRecord(
        runId = "run-$score-$combo-$perfects-$millisOffset",
        ownerUid = "uid",
        displayName = "Player",
        walletAddress = walletA,
        walletAddressShort = "9xQe…PABC",
        score = score,
        maxCombo = combo,
        perfectHits = perfects,
        radiantHits = 10,
        corruptedHits = 1,
        mode = mode,
        completedAtEpochMillis = Instant.parse("2026-09-10T12:00:00Z").toEpochMilli() + millisOffset,
    )

    private fun candidate(
        sourceId: String,
        walletAddress: String?,
        walletAddressShort: String?,
        score: Int,
    ) = RunLeaderboardCandidate(
        sourceId = sourceId,
        displayName = "Player",
        walletAddress = walletAddress,
        walletAddressShort = walletAddressShort,
        score = score,
        bestCombo = 10,
        perfectHits = 3,
        runsPlayed = 2,
        bestCompletedAtEpochMillis = Instant.parse("2026-09-10T12:00:00Z").toEpochMilli(),
    )

    @Test
    fun walletPersonalStatsSurviveAnonymousUidChangesAcrossDevices() {
        val weeklyRows = listOf(
            candidate("samsung-old", walletA, "9xQe…PABC", score = 2_100).copy(
                bestCombo = 11,
                perfectHits = 4,
                runsPlayed = 2,
            ),
            candidate("seeker-new", walletA, "9xQe…PABC", score = 2_450).copy(
                bestCombo = 13,
                perfectHits = 5,
                runsPlayed = 1,
            ),
            candidate("other", walletB, "7YWH…BCD9", score = 9_999),
        )
        val allTimeRows = listOf(
            candidate("samsung-old", walletA, "9xQe…PABC", score = 3_000).copy(
                bestCombo = 15,
                perfectHits = 6,
                runsPlayed = 5,
            ),
            candidate("seeker-new", walletA, "9xQe…PABC", score = 2_900).copy(
                bestCombo = 20,
                perfectHits = 9,
                runsPlayed = 1,
            ),
        )

        val stats = Phase11CompetitionRules.walletPersonalStats(
            walletAddress = walletA,
            weeklyCandidates = weeklyRows,
            allTimeCandidates = allTimeRows,
        )

        assertEquals(2_450, stats.weeklyBestScore)
        assertEquals(3_000, stats.allTimeBestScore)
        assertEquals(15, stats.bestCombo)
        assertEquals(6, stats.perfectHitsAtBest)
        assertEquals(3, stats.weeklyRankedRuns)
    }

    @Test
    fun walletPersonalStatsCanRecoverLegacyShortRowWhenFullRowIsMissing() {
        val legacy = candidate("legacy", null, "9xQe...PABC", score = 1_850).copy(
            bestCombo = 9,
            perfectHits = 2,
            runsPlayed = 2,
        )

        val stats = Phase11CompetitionRules.walletPersonalStats(
            walletAddress = walletA,
            weeklyCandidates = listOf(legacy),
            allTimeCandidates = listOf(legacy),
        )

        assertEquals(1_850, stats.weeklyBestScore)
        assertEquals(1_850, stats.allTimeBestScore)
        assertEquals(9, stats.bestCombo)
        assertEquals(2, stats.perfectHitsAtBest)
        assertEquals(2, stats.weeklyRankedRuns)
    }

    @Test
    fun casualOnlyTicketCannotConsumeRankedAttempt() {
        val decision = Phase11CompetitionRules.rankedAttemptDecision(
            walletConnected = true,
            savedDayKey = "2026-09-10",
            savedAttemptsUsed = 1,
            completedAtEpochMillis = java.time.Instant.parse("2026-09-10T12:00:00Z").toEpochMilli(),
            rankedEntryTicketAvailable = false,
        )

        assertEquals(RunCompetitionMode.Casual, decision.mode)
        assertEquals(1, decision.rankedAttemptsUsedBefore)
        assertEquals(1, decision.rankedAttemptsUsedAfter)
    }

    @Test
    fun runBoardKeepsBestScoreButUsesFreshestIdentity() {
        val oldBest = candidate("old-uid", walletA, "9xQe…PABC", score = 4_000).copy(
            displayName = "Old Runner",
            avatarId = "fox",
            updatedAtMs = 10L,
        )
        val freshIdentity = candidate("new-uid", walletA, "9xQe…PABC", score = 2_000).copy(
            displayName = "New Runner",
            avatarId = "ghost",
            updatedAtMs = 50L,
        )

        val result = Phase11CompetitionRules
            .collapseRunLeaderboardByWallet(listOf(oldBest, freshIdentity))
            .single()

        assertEquals(4_000, result.score)
        assertEquals("old-uid", result.sourceId)
        assertEquals("New Runner", result.displayName)
        assertEquals("ghost", result.avatarId)
    }

}
