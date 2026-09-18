package com.thinkblox.radiantrush.logic

import com.thinkblox.radiantrush.data.CircleProfilePreview
import com.thinkblox.radiantrush.data.SharedSparkPreview
import java.util.Locale

object SharedSparkRules {
    const val MAX_MOTTO_LENGTH = 100
    const val MAX_SHORT_FIELD_LENGTH = 60
    const val MAX_TALK_ABOUT_LENGTH = 90

    private data class InterestField(
        val label: String,
        val symbol: String,
        val first: (CircleProfilePreview) -> String,
    )

    private val interestFields = listOf(
        InterestField("Favorite food", "🍜") { it.favoriteFood },
        InterestField("Music", "🎵") { it.music },
        InterestField("Games", "🎮") { it.games },
        InterestField("Hobbies", "🎨") { it.hobbies },
        InterestField("Books", "📚") { it.books },
        InterestField("Pets", "🐾") { it.pets },
        InterestField("Currently into", "✨") { it.currentlyInto },
        InterestField("Weekend vibe", "🌤") { it.weekendVibe },
        InterestField("Talk for hours", "💬") { it.talkAbout },
    )

    fun sanitizeMotto(value: String): String = clean(value, MAX_MOTTO_LENGTH)

    fun sanitizeShort(value: String): String = clean(value, MAX_SHORT_FIELD_LENGTH)

    fun sanitizeTalkAbout(value: String): String = clean(value, MAX_TALK_ABOUT_LENGTH)

    fun sanitizeProfile(profile: CircleProfilePreview): CircleProfilePreview = profile.copy(
        motto = sanitizeMotto(profile.motto),
        favoriteFood = sanitizeShort(profile.favoriteFood),
        music = sanitizeShort(profile.music),
        games = sanitizeShort(profile.games),
        hobbies = sanitizeShort(profile.hobbies),
        books = sanitizeShort(profile.books),
        pets = sanitizeShort(profile.pets),
        currentlyInto = sanitizeShort(profile.currentlyInto),
        weekendVibe = sanitizeShort(profile.weekendVibe),
        talkAbout = sanitizeTalkAbout(profile.talkAbout),
    )

    fun sharedSparks(
        mine: CircleProfilePreview,
        theirs: CircleProfilePreview,
        limit: Int = 3,
    ): List<SharedSparkPreview> {
        if (limit <= 0) return emptyList()

        return interestFields.mapNotNull { field ->
            val mineTokens = tokens(field.first(mine))
            val theirTokens = tokens(field.first(theirs))
            val shared = mineTokens.firstOrNull { token -> token.normalized in theirTokens.map { it.normalized }.toSet() }
                ?: return@mapNotNull null

            SharedSparkPreview(
                label = field.label,
                value = shared.display,
                symbol = field.symbol,
            )
        }.take(limit)
    }

    fun filledInterestCount(profile: CircleProfilePreview): Int = interestFields.count {
        it.first(profile).isNotBlank()
    }

    private data class Token(val display: String, val normalized: String)

    private fun tokens(value: String): List<Token> = value
        .split(',', '/', ';', '|', '•', '\n')
        .map { clean(it, MAX_TALK_ABOUT_LENGTH) }
        .filter { it.isNotBlank() }
        .distinctBy { it.lowercase(Locale.US) }
        .map { Token(it, it.lowercase(Locale.US)) }

    private fun clean(value: String, maxLength: Int): String = value
        .replace(Regex("\\s+"), " ")
        .trim()
        .take(maxLength)
}
