package com.thinkblox.radiantrush.logic

import java.time.Instant
import java.time.ZoneOffset
import java.time.temporal.TemporalAdjusters

/**
 * Pure Phase 11D Weekly Radiant Cup rules.
 *
 * The Cup is competitive presentation around the existing client-reported
 * Ranked Radiant Run board. It deliberately has no token payout authority.
 * Sponsor metadata is display-only until a future trusted backend verifies
 * scores, funding, winners, and payout instructions.
 */
object WeeklyRadiantCupRules {
    fun seasonEndsAtEpochMillis(epochMillis: Long = System.currentTimeMillis()): Long {
        val now = Instant.ofEpochMilli(epochMillis).atZone(ZoneOffset.UTC)
        val currentDate = now.toLocalDate()
        val nextMonday = currentDate.with(TemporalAdjusters.next(java.time.DayOfWeek.MONDAY))
        return nextMonday.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
    }

    fun previousWeekKey(epochMillis: Long = System.currentTimeMillis()): String =
        Phase11CompetitionRules.utcWeekKey(epochMillis - 7L * 24L * 60L * 60L * 1_000L)

    fun rewardForPlacement(rank: Int?): WeeklyCupReward = when (rank) {
        1 -> WeeklyCupReward(
            title = "Radiant Champion Crest",
            detail = "Finish the season at #1 to earn the Champion Crest.",
        )
        2, 3 -> WeeklyCupReward(
            title = "Radiant Podium Crest",
            detail = "Finish the season in the Top 3 to earn the Podium Crest.",
        )
        in 4..10 -> WeeklyCupReward(
            title = "Radiant Top 10 Ribbon",
            detail = "Finish the season in the Top 10 to earn the Top 10 Ribbon.",
        )
        null -> WeeklyCupReward(
            title = "Cup Finisher Mark",
            detail = "Finish a Ranked run to join this week’s Cup.",
        )
        else -> WeeklyCupReward(
            title = "Cup Finisher Mark",
            detail = "Stay ranked through season close to earn the Finisher Mark.",
        )
    }

    /**
     * Remote sponsor metadata is intentionally narrow. Even a malformed or
     * over-privileged config can never switch token payout on in the Android app.
     */
    fun sponsorState(
        status: String?,
        sponsorName: String?,
        prizeLabel: String?,
        note: String?,
    ): WeeklyCupSponsorState {
        val normalizedStatus = status?.trim()?.lowercase().orEmpty()
        val safeSponsor = sponsorName?.trim()?.takeIf { it.isNotBlank() }?.take(80)
        val safePrize = prizeLabel?.trim()?.takeIf { it.isNotBlank() }?.take(80)
        val active = normalizedStatus == "announced" && safeSponsor != null && safePrize != null

        return if (active) {
            WeeklyCupSponsorState(
                sponsorName = safeSponsor,
                prizeLabel = safePrize,
                statusLabel = "Sponsored prize announced",
                note = note?.trim()?.takeIf { it.isNotBlank() }?.take(160),
                active = true,
                payoutEnabled = false,
            )
        } else {
            WeeklyCupSponsorState(
                statusLabel = "No sponsored prize this week",
                active = false,
                payoutEnabled = false,
            )
        }
    }
}

data class WeeklyCupReward(
    val title: String,
    val detail: String,
)

data class WeeklyCupSponsorState(
    val sponsorName: String? = null,
    val prizeLabel: String? = null,
    val statusLabel: String,
    val note: String? = null,
    val active: Boolean,
    val payoutEnabled: Boolean,
)
