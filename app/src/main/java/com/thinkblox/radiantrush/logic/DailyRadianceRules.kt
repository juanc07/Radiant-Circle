package com.thinkblox.radiantrush.logic

import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Pure Phase 13A rules for the lightweight Daily Radiance ritual.
 *
 * Daily Radiance is social/account progression only. It grants no token value,
 * never changes wallet identity, and is intentionally independent from Radiant
 * Rush / Weekly Cup trust and payout state.
 */
object DailyRadianceRules {
    data class Content(
        val id: String,
        val category: String,
        val message: String,
    )

    private val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    private val messages = listOf(
        Content(
            id = "courage-small-step",
            category = "Courage",
            message = "Your next opportunity may begin with one small action today.",
        ),
        Content(
            id = "focus-one-thing",
            category = "Focus",
            message = "Give one important thing your full attention before the day gets noisy.",
        ),
        Content(
            id = "kindness-soft-word",
            category = "Kindness",
            message = "A thoughtful word can travel farther than you expect today.",
        ),
        Content(
            id = "creativity-first-idea",
            category = "Creativity",
            message = "The first idea does not need to be perfect. It only needs somewhere to begin.",
        ),
        Content(
            id = "calm-own-pace",
            category = "Calm",
            message = "You do not have to match anyone else’s pace to make meaningful progress.",
        ),
        Content(
            id = "curiosity-one-question",
            category = "Curiosity",
            message = "One good question can open a door that certainty would have missed.",
        ),
        Content(
            id = "momentum-finish-small",
            category = "Momentum",
            message = "Finish one small thing today and let that win create the next one.",
        ),
        Content(
            id = "connection-reach-out",
            category = "Connection",
            message = "Someone may be glad you reached out first.",
        ),
        Content(
            id = "confidence-earned",
            category = "Confidence",
            message = "Trust the skills you have already earned, then take the next step.",
        ),
        Content(
            id = "perspective-room",
            category = "Perspective",
            message = "Leave a little room today for a better answer than the first one you see.",
        ),
        Content(
            id = "gratitude-notice",
            category = "Gratitude",
            message = "Notice one ordinary thing that quietly made your day better.",
        ),
        Content(
            id = "hope-next-page",
            category = "Hope",
            message = "A difficult chapter is still only one part of the story you are building.",
        ),
    )

    /**
     * Chooses a stable message from Radiant Circle account identity + local day.
     * Wallet address is deliberately not part of the seed, so switching wallets
     * can never change a user's Daily Radiance identity or daily message.
     */
    fun contentFor(accountId: String, dayKey: String): Content {
        val normalizedAccount = accountId.trim().ifBlank { "radiant-circle" }
        val normalizedDay = parseDay(dayKey)?.format(dateFormatter) ?: dayKey.trim()
        val seedText = "$normalizedAccount|$normalizedDay"
        var seed = 1125899906842597L
        seedText.forEach { character ->
            seed = seed * 31L + character.code.toLong()
        }
        val index = Math.floorMod(seed, messages.size.toLong()).toInt()
        return messages[index]
    }

    fun contentById(id: String?): Content? {
        val cleanId = id?.trim().orEmpty()
        if (cleanId.isBlank()) return null
        return messages.firstOrNull { it.id == cleanId }
    }

    /**
     * Streak state is intentionally nonfinancial. A same-day retry is idempotent;
     * yesterday continues the streak; any larger gap starts again at one.
     */
    fun nextStreak(
        lastOpenedDay: String?,
        todayKey: String,
        currentStreak: Int,
    ): Int {
        if (lastOpenedDay == todayKey) return currentStreak.coerceAtLeast(1)

        val last = parseDay(lastOpenedDay)
        val today = parseDay(todayKey)
        return if (last != null && today != null && last.plusDays(1) == today) {
            currentStreak.coerceAtLeast(0) + 1
        } else {
            1
        }
    }

    /**
     * Before today's reveal, yesterday's streak remains visible as momentum to
     * continue. Older streaks are shown as zero rather than pretending they are live.
     */
    fun visibleStreak(
        lastOpenedDay: String?,
        todayKey: String,
        savedStreak: Int,
    ): Int {
        val today = parseDay(todayKey) ?: return 0
        val last = parseDay(lastOpenedDay) ?: return 0
        return when {
            last == today -> savedStreak.coerceAtLeast(0)
            last.plusDays(1) == today -> savedStreak.coerceAtLeast(0)
            else -> 0
        }
    }

    private fun parseDay(value: String?): LocalDate? = runCatching {
        value?.trim()
            ?.takeIf { it.isNotBlank() }
            ?.let { LocalDate.parse(it, dateFormatter) }
    }.getOrNull()
}
