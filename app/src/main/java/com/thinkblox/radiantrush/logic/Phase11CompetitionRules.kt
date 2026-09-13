package com.thinkblox.radiantrush.logic

import java.time.Instant
import java.time.ZoneOffset
import java.time.temporal.WeekFields
import java.util.Locale
import kotlin.math.min

/**
 * Phase 11A pure competition rules.
 *
 * Security/fairness boundary:
 * - ranked score is always the raw Radiant Rush score;
 * - gameplay XP is a separate capped progression reward;
 * - wallet/SKR wealth is deliberately absent from score calculations;
 * - client-produced run records are prototype ranking data only and can never
 *   authorize an economic SKR payout.
 *
 * This file is intentionally Android/Firebase free so the rules can be tested
 * with ordinary JVM unit tests before Phase 11B wires persistence and UI.
 */
object Phase11CompetitionRules {
    const val DAILY_GAMEPLAY_XP_CAP = 300
    const val DAILY_RANKED_ATTEMPTS = 3

    /** Stable UTC day key used for daily XP/ranked-attempt resets. */
    fun utcDayKey(epochMillis: Long = System.currentTimeMillis()): String =
        Instant.ofEpochMilli(epochMillis)
            .atZone(ZoneOffset.UTC)
            .toLocalDate()
            .toString()

    /** Stable ISO-8601 UTC week key, for example `2026-W37`. */
    fun utcWeekKey(epochMillis: Long = System.currentTimeMillis()): String {
        val date = Instant.ofEpochMilli(epochMillis)
            .atZone(ZoneOffset.UTC)
            .toLocalDate()
        val weekFields = WeekFields.ISO
        val weekBasedYear = date.get(weekFields.weekBasedYear())
        val week = date.get(weekFields.weekOfWeekBasedYear())
        return String.format(Locale.US, "%04d-W%02d", weekBasedYear, week)
    }

    /**
     * Converts run performance into deliberately controlled progression XP.
     * Score is not converted 1:1 and the result is bounded even for extreme
     * client values. Ranked boards continue to use raw score, not this XP.
     */
    fun performanceXpForRun(
        score: Int,
        maxCombo: Int,
        perfectHits: Int,
    ): Int {
        val safeScore = score.coerceAtLeast(0)
        val safeCombo = maxCombo.coerceAtLeast(0)
        val safePerfectHits = perfectHits.coerceAtLeast(0)

        val finishXp = 20
        val scoreXp = ((safeScore / 500) * 8).coerceAtMost(64)
        val comboXp = safeCombo.coerceAtMost(20) * 2
        val perfectXp = safePerfectHits.coerceAtMost(12) * 4
        return finishXp + scoreXp + comboXp + perfectXp
    }

    /**
     * Applies the UTC-daily gameplay XP cap. If [savedDayKey] is from another
     * UTC day, the saved amount is treated as zero automatically.
     */
    fun cappedGameplayXp(
        score: Int,
        maxCombo: Int,
        perfectHits: Int,
        savedDayKey: String?,
        savedEarnedToday: Int,
        completedAtEpochMillis: Long,
    ): GameplayXpAward {
        val currentDayKey = utcDayKey(completedAtEpochMillis)
        val usedBefore = gameplayXpEarnedToday(
            savedDayKey = savedDayKey,
            savedEarned = savedEarnedToday,
            currentUtcDayKey = currentDayKey,
        )
        val requested = performanceXpForRun(score, maxCombo, perfectHits)
        val granted = min(requested, DAILY_GAMEPLAY_XP_CAP - usedBefore).coerceAtLeast(0)

        return GameplayXpAward(
            utcDayKey = currentDayKey,
            requestedXp = requested,
            grantedXp = granted,
            earnedBefore = usedBefore,
            earnedAfter = usedBefore + granted,
            cap = DAILY_GAMEPLAY_XP_CAP,
        )
    }

    fun gameplayXpEarnedToday(
        savedDayKey: String?,
        savedEarned: Int,
        currentUtcDayKey: String,
    ): Int = if (savedDayKey == currentUtcDayKey) {
        savedEarned.coerceIn(0, DAILY_GAMEPLAY_XP_CAP)
    } else {
        0
    }

    /**
     * Resolves whether the next ticket-backed run is ranked or casual.
     * Ranked play requires a connected wallet and every wallet gets the same
     * three attempts per UTC day. Extra casual tickets never buy extra rank attempts.
     */
    fun rankedAttemptDecision(
        walletConnected: Boolean,
        savedDayKey: String?,
        savedAttemptsUsed: Int,
        completedAtEpochMillis: Long,
        rankedEntryTicketAvailable: Boolean = true,
    ): RankedAttemptDecision {
        val currentDayKey = utcDayKey(completedAtEpochMillis)
        val usedBefore = rankedAttemptsUsedToday(
            savedDayKey = savedDayKey,
            savedAttemptsUsed = savedAttemptsUsed,
            currentUtcDayKey = currentDayKey,
        )
        val ranked = walletConnected && rankedEntryTicketAvailable && usedBefore < DAILY_RANKED_ATTEMPTS
        val usedAfter = if (ranked) usedBefore + 1 else usedBefore

        return RankedAttemptDecision(
            utcDayKey = currentDayKey,
            mode = if (ranked) RunCompetitionMode.Ranked else RunCompetitionMode.Casual,
            rankedAttemptsUsedBefore = usedBefore,
            rankedAttemptsUsedAfter = usedAfter,
            rankedAttemptsRemaining = if (walletConnected) {
                (DAILY_RANKED_ATTEMPTS - usedAfter).coerceAtLeast(0)
            } else {
                0
            },
        )
    }

    fun rankedAttemptsUsedToday(
        savedDayKey: String?,
        savedAttemptsUsed: Int,
        currentUtcDayKey: String,
    ): Int = if (savedDayKey == currentUtcDayKey) {
        savedAttemptsUsed.coerceIn(0, DAILY_RANKED_ATTEMPTS)
    } else {
        0
    }

    /** Creates a sanitized client-produced run record for later persistence. */
    fun createRunScoreRecord(
        runId: String,
        ownerUid: String,
        displayName: String,
        walletAddress: String?,
        walletAddressShort: String?,
        score: Int,
        maxCombo: Int,
        perfectHits: Int,
        radiantHits: Int,
        corruptedHits: Int,
        mode: RunCompetitionMode,
        completedAtEpochMillis: Long,
    ): RunScoreRecord = RunScoreRecord(
        runId = runId,
        ownerUid = ownerUid,
        displayName = displayName.ifBlank { "Radiant Rookie" },
        walletAddress = walletAddress?.trim()?.takeIf { it.isNotBlank() },
        walletAddressShort = walletAddressShort?.trim()?.takeIf { it.isNotBlank() },
        score = score.coerceAtLeast(0),
        maxCombo = maxCombo.coerceAtLeast(0),
        perfectHits = perfectHits.coerceAtLeast(0),
        radiantHits = radiantHits.coerceAtLeast(0),
        corruptedHits = corruptedHits.coerceAtLeast(0),
        mode = mode,
        completedAtEpochMillis = completedAtEpochMillis,
        utcDayKey = utcDayKey(completedAtEpochMillis),
        utcWeekKey = utcWeekKey(completedAtEpochMillis),
    )

    /**
     * Updates the current week's personal ranked stats. Casual runs are ignored.
     * A new UTC week automatically starts a fresh stats object.
     */
    fun updateWeeklyStats(
        current: WeeklyRunStats?,
        run: RunScoreRecord,
    ): WeeklyRunStats? {
        if (run.mode != RunCompetitionMode.Ranked) return current

        val base = current?.takeIf { it.utcWeekKey == run.utcWeekKey }
            ?: WeeklyRunStats(utcWeekKey = run.utcWeekKey)
        val better = isBetterPerformance(
            score = run.score,
            combo = run.maxCombo,
            perfectHits = run.perfectHits,
            completedAtEpochMillis = run.completedAtEpochMillis,
            bestScore = base.bestScore,
            bestCombo = base.bestCombo,
            bestPerfectHits = base.perfectHitsAtBestScore,
            bestCompletedAtEpochMillis = base.bestCompletedAtEpochMillis,
        )

        return if (better) {
            base.copy(
                bestScore = run.score,
                bestCombo = run.maxCombo,
                perfectHitsAtBestScore = run.perfectHits,
                bestCompletedAtEpochMillis = run.completedAtEpochMillis,
                rankedRunsPlayed = base.rankedRunsPlayed + 1,
            )
        } else {
            base.copy(rankedRunsPlayed = base.rankedRunsPlayed + 1)
        }
    }

    /** All-time PB uses the same raw-score-first ordering as the public board. */
    fun updatePersonalBest(
        current: RunPersonalBest?,
        run: RunScoreRecord,
    ): RunPersonalBest? {
        if (run.mode != RunCompetitionMode.Ranked) return current

        val better = current == null || isBetterPerformance(
            score = run.score,
            combo = run.maxCombo,
            perfectHits = run.perfectHits,
            completedAtEpochMillis = run.completedAtEpochMillis,
            bestScore = current.score,
            bestCombo = current.bestCombo,
            bestPerfectHits = current.perfectHits,
            bestCompletedAtEpochMillis = current.completedAtEpochMillis,
        )

        return if (better) {
            RunPersonalBest(
                score = run.score,
                bestCombo = run.maxCombo,
                perfectHits = run.perfectHits,
                completedAtEpochMillis = run.completedAtEpochMillis,
                utcWeekKey = run.utcWeekKey,
            )
        } else {
            current
        }
    }

    /**
     * Wallet-deduplicated Radiant Rush board. This mirrors the Phase 9.1 legacy
     * compatibility rules: explicit disconnect tombstones are excluded, full
     * wallets are canonical, and unique legacy short fingerprints collapse into
     * their matching full wallet when possible.
     */
    fun collapseRunLeaderboardByWallet(
        candidates: List<RunLeaderboardCandidate>,
        limit: Int = 20,
    ): List<RunLeaderboardCandidate> {
        if (limit <= 0 || candidates.isEmpty()) return emptyList()

        val eligible = candidates.filterNot(::isExplicitlyDisconnected)
        val fullWalletsByFingerprint = eligible
            .mapNotNull { candidate ->
                val full = canonicalFullWallet(candidate.walletAddress) ?: return@mapNotNull null
                walletFingerprint(full)?.let { fingerprint -> fingerprint to full }
            }
            .groupBy(keySelector = { it.first }, valueTransform = { it.second })
            .mapValues { (_, wallets) -> wallets.distinct().singleOrNull() }

        val groupedByIdentity = linkedMapOf<String, MutableList<RunLeaderboardCandidate>>()
        eligible.forEach { candidate ->
            val identity = runIdentityKey(candidate, fullWalletsByFingerprint) ?: return@forEach
            groupedByIdentity.getOrPut(identity) { mutableListOf() }.add(candidate)
        }

        val mergedRows = groupedByIdentity.values.mapNotNull { rows ->
            val bestRanked = rows.minWithOrNull(runCandidateComparator) ?: return@mapNotNull null
            val freshestIdentity = rows.maxWithOrNull(
                compareBy<RunLeaderboardCandidate> { it.updatedAtMs }
                    .thenBy { it.sourceId },
            ) ?: bestRanked
            bestRanked.copy(
                displayName = freshestIdentity.displayName,
                avatarId = freshestIdentity.avatarId,
            )
        }

        return mergedRows
            .sortedWith(runCandidateComparator)
            .take(limit)
    }

    fun walletLabel(candidate: RunLeaderboardCandidate): String? {
        if (isExplicitlyDisconnected(candidate)) return null
        canonicalFullWallet(candidate.walletAddress)?.let { return shortenWallet(it) }
        normalizeLegacyShort(candidate.walletAddressShort)?.let { return it }
        normalizeLegacyShort(candidate.walletAddress)?.let { return it }
        return null
    }

    fun walletRank(
        walletAddress: String?,
        candidates: List<RunLeaderboardCandidate>,
        limit: Int = 100,
    ): Int? {
        val canonicalWallet = canonicalFullWallet(walletAddress) ?: return null
        val targetFingerprint = walletFingerprint(canonicalWallet)
        val ranked = collapseRunLeaderboardByWallet(candidates, limit = limit)

        val index = ranked.indexOfFirst { candidate ->
            canonicalFullWallet(candidate.walletAddress) == canonicalWallet ||
                (
                    canonicalFullWallet(candidate.walletAddress) == null &&
                        targetFingerprint != null &&
                        (walletFingerprint(candidate.walletAddressShort)
                            ?: walletFingerprint(candidate.walletAddress)) == targetFingerprint
                    )
        }
        return index.takeIf { it >= 0 }?.plus(1)
    }

    /**
     * Resolves wallet-scoped personal competition stats across Firebase anonymous
     * UID rows. This keeps My Stats stable when the same Solana wallet is used on
     * another device or after an anonymous-auth reinstall.
     *
     * Exact full-wallet matches are preferred. Legacy shortened rows are used
     * only when there is no exact full-wallet row for that board.
     */
    fun walletPersonalStats(
        walletAddress: String?,
        weeklyCandidates: List<RunLeaderboardCandidate>,
        allTimeCandidates: List<RunLeaderboardCandidate>,
    ): WalletRunPersonalStats {
        val canonicalWallet = canonicalFullWallet(walletAddress)
            ?: return WalletRunPersonalStats()
        val targetFingerprint = walletFingerprint(canonicalWallet)

        fun matches(
            candidates: List<RunLeaderboardCandidate>,
        ): List<RunLeaderboardCandidate> {
            val eligible = candidates.filterNot(::isExplicitlyDisconnected)
            val exact = eligible.filter { candidate ->
                canonicalFullWallet(candidate.walletAddress) == canonicalWallet
            }
            if (exact.isNotEmpty()) return exact

            return eligible.filter { candidate ->
                canonicalFullWallet(candidate.walletAddress) == null &&
                    targetFingerprint != null &&
                    (walletFingerprint(candidate.walletAddressShort)
                        ?: walletFingerprint(candidate.walletAddress)) == targetFingerprint
            }
        }

        val weeklyMatches = matches(weeklyCandidates)
        val allTimeMatches = matches(allTimeCandidates)
        val weeklyBest = weeklyMatches.minWithOrNull(runCandidateComparator)
        val allTimeBest = allTimeMatches.minWithOrNull(runCandidateComparator)
        val weeklyRuns = weeklyMatches
            .sumOf { it.runsPlayed.coerceAtLeast(0).toLong() }
            .coerceAtMost(Int.MAX_VALUE.toLong())
            .toInt()

        return WalletRunPersonalStats(
            weeklyBestScore = weeklyBest?.score?.coerceAtLeast(0) ?: 0,
            allTimeBestScore = allTimeBest?.score?.coerceAtLeast(0) ?: 0,
            bestCombo = allTimeBest?.bestCombo?.coerceAtLeast(0) ?: 0,
            perfectHitsAtBest = allTimeBest?.perfectHits?.coerceAtLeast(0) ?: 0,
            weeklyRankedRuns = weeklyRuns,
        )
    }

    private fun isBetterPerformance(
        score: Int,
        combo: Int,
        perfectHits: Int,
        completedAtEpochMillis: Long,
        bestScore: Int,
        bestCombo: Int,
        bestPerfectHits: Int,
        bestCompletedAtEpochMillis: Long,
    ): Boolean = when {
        score != bestScore -> score > bestScore
        combo != bestCombo -> combo > bestCombo
        perfectHits != bestPerfectHits -> perfectHits > bestPerfectHits
        bestCompletedAtEpochMillis <= 0L -> true
        else -> completedAtEpochMillis < bestCompletedAtEpochMillis
    }

    private fun runIdentityKey(
        candidate: RunLeaderboardCandidate,
        fullWalletsByFingerprint: Map<String, String?>,
    ): String? {
        if (isExplicitlyDisconnected(candidate)) return null
        canonicalFullWallet(candidate.walletAddress)?.let { return "wallet:$it" }

        val fingerprint = walletFingerprint(candidate.walletAddressShort)
            ?: walletFingerprint(candidate.walletAddress)
            ?: return null
        val mappedFull = fullWalletsByFingerprint[fingerprint]
        return if (mappedFull != null) "wallet:$mappedFull" else "legacy-short:$fingerprint"
    }

    private fun isExplicitlyDisconnected(candidate: RunLeaderboardCandidate): Boolean {
        val marker = candidate.walletAddressShort?.trim().orEmpty()
        return marker.equals("No wallet", ignoreCase = true) ||
            marker.equals("Wallet not connected", ignoreCase = true) ||
            marker.equals("Disconnected", ignoreCase = true)
    }

    private fun canonicalFullWallet(value: String?): String? {
        var clean = value?.trim().orEmpty()
        if (clean.isBlank() || clean.equals("No wallet", ignoreCase = true)) return null
        if (clean.startsWith("solana:", ignoreCase = true)) {
            clean = clean.substringAfter(':').substringBefore('?').trim()
        }

        val shortened = clean.contains('…') ||
            clean.contains('⋯') ||
            clean.contains("..") ||
            clean.any(Char::isWhitespace)
        if (shortened) return null
        if (clean.length !in 32..64) return null
        if (!clean.all { it in BASE58_CHARS }) return null
        return clean
    }

    private fun walletFingerprint(value: String?): String? {
        val raw = value?.trim().orEmpty()
        if (raw.isBlank() || raw.equals("No wallet", ignoreCase = true)) return null
        canonicalFullWallet(raw)?.let { full -> return "${full.take(4)}|${full.takeLast(4)}" }

        val compact = raw.replace(" ", "")
        val separator = listOf("…", "⋯", "...", "..").firstOrNull { compact.contains(it) }
            ?: return null
        val left = compact.substringBefore(separator)
        val right = compact.substringAfter(separator)
        if (left.length < 4 || right.length < 4) return null

        val prefix = left.take(4)
        val suffix = right.takeLast(4)
        if (!prefix.all { it in BASE58_CHARS } || !suffix.all { it in BASE58_CHARS }) return null
        return "$prefix|$suffix"
    }

    private fun normalizeLegacyShort(value: String?): String? {
        val raw = value?.trim().orEmpty()
        if (raw.isBlank() || raw.equals("No wallet", ignoreCase = true)) return null
        canonicalFullWallet(raw)?.let { return shortenWallet(it) }
        val fingerprint = walletFingerprint(raw) ?: return null
        val (prefix, suffix) = fingerprint.split('|', limit = 2)
        return "$prefix…$suffix"
    }

    private fun shortenWallet(wallet: String): String = if (wallet.length <= 12) {
        wallet
    } else {
        "${wallet.take(4)}…${wallet.takeLast(4)}"
    }

    private val runCandidateComparator =
        compareByDescending<RunLeaderboardCandidate> { it.score }
            .thenByDescending { it.bestCombo }
            .thenByDescending { it.perfectHits }
            // Earlier achievement wins a true score/combo/PERFECT tie.
            .thenBy { it.bestCompletedAtEpochMillis.takeIf { value -> value > 0L } ?: Long.MAX_VALUE }
            .thenBy { it.sourceId }

    private const val BASE58_CHARS = "123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz"
}

enum class RunCompetitionMode {
    Ranked,
    Casual,
}

enum class RunScoreAuthority {
    /** Android-produced score; valid for prototype UI/progression, never token payout authority. */
    ClientReportedPrototype,
}

data class RunScoreRecord(
    val runId: String,
    val ownerUid: String,
    val displayName: String,
    val walletAddress: String?,
    val walletAddressShort: String?,
    val score: Int,
    val maxCombo: Int,
    val perfectHits: Int,
    val radiantHits: Int,
    val corruptedHits: Int,
    val mode: RunCompetitionMode,
    val completedAtEpochMillis: Long,
    val utcDayKey: String,
    val utcWeekKey: String,
    val authority: RunScoreAuthority = RunScoreAuthority.ClientReportedPrototype,
) {
    /** Deliberately hard-coded false until a future trusted payout authority exists. */
    val payoutEligible: Boolean
        get() = false
}

data class WeeklyRunStats(
    val utcWeekKey: String,
    val bestScore: Int = 0,
    val bestCombo: Int = 0,
    val perfectHitsAtBestScore: Int = 0,
    val bestCompletedAtEpochMillis: Long = 0L,
    val rankedRunsPlayed: Int = 0,
)

data class RunPersonalBest(
    val score: Int,
    val bestCombo: Int,
    val perfectHits: Int,
    val completedAtEpochMillis: Long,
    val utcWeekKey: String,
)

data class WalletRunPersonalStats(
    val weeklyBestScore: Int = 0,
    val allTimeBestScore: Int = 0,
    val bestCombo: Int = 0,
    val perfectHitsAtBest: Int = 0,
    val weeklyRankedRuns: Int = 0,
)

data class GameplayXpAward(
    val utcDayKey: String,
    val requestedXp: Int,
    val grantedXp: Int,
    val earnedBefore: Int,
    val earnedAfter: Int,
    val cap: Int,
) {
    val remainingToday: Int
        get() = (cap - earnedAfter).coerceAtLeast(0)

    val wasCapped: Boolean
        get() = grantedXp < requestedXp
}

data class RankedAttemptDecision(
    val utcDayKey: String,
    val mode: RunCompetitionMode,
    val rankedAttemptsUsedBefore: Int,
    val rankedAttemptsUsedAfter: Int,
    val rankedAttemptsRemaining: Int,
)

data class RunLeaderboardCandidate(
    val sourceId: String,
    val displayName: String,
    val walletAddress: String?,
    val walletAddressShort: String?,
    val score: Int,
    val bestCombo: Int,
    val perfectHits: Int,
    val runsPlayed: Int,
    val avatarId: String = "fox",
    val bestCompletedAtEpochMillis: Long = 0L,
    val updatedAtMs: Long = 0L,
)
