package com.thinkblox.radiantrush.logic

import com.thinkblox.radiantrush.data.CircleProfilePreview
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SharedSparkRulesTest {
    @Test
    fun findsSharedInterestsAcrossCommaAndSlashSeparatedValues() {
        val mine = CircleProfilePreview(
            favoriteFood = "Ramen, Pizza",
            games = "RPG / Minecraft",
            hobbies = "Coding, Drawing",
        )
        val theirs = CircleProfilePreview(
            favoriteFood = "ramen",
            games = "Minecraft / Racing",
            hobbies = "Coding",
        )

        val sparks = SharedSparkRules.sharedSparks(mine, theirs, limit = 5)

        assertEquals(listOf("Favorite food", "Games", "Hobbies"), sparks.map { it.label })
        assertEquals(listOf("Ramen", "Minecraft", "Coding"), sparks.map { it.value })
    }

    @Test
    fun ignoresBlankOrDifferentInterests() {
        val mine = CircleProfilePreview(music = "Rock", pets = "Dogs")
        val theirs = CircleProfilePreview(music = "K-Pop", pets = "")

        assertTrue(SharedSparkRules.sharedSparks(mine, theirs).isEmpty())
    }

    @Test
    fun sanitizesProfileLengthsAndWhitespace() {
        val profile = SharedSparkRules.sanitizeProfile(
            CircleProfilePreview(
                motto = "  Keep   moving  ",
                hobbies = "  Coding   and   art  ",
                talkAbout = "  Technology   forever  ",
            ),
        )

        assertEquals("Keep moving", profile.motto)
        assertEquals("Coding and art", profile.hobbies)
        assertEquals("Technology forever", profile.talkAbout)
    }
}
