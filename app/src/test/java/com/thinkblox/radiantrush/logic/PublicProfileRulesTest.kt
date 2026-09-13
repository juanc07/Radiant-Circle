package com.thinkblox.radiantrush.logic

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PublicProfileRulesTest {
    @Test
    fun displayNameIsTrimmedCollapsedAndBounded() {
        val sanitized = PublicProfileRules.sanitizeDisplayName("   Radiant\n\tExplorer with a very long name   ")
        assertTrue(sanitized.length <= PublicProfileRules.MAX_DISPLAY_NAME_LENGTH)
        assertTrue(!sanitized.contains("\n"))
        assertTrue(!sanitized.contains("  "))
    }

    @Test
    fun unknownAvatarFallsBackToFox() {
        assertEquals("fox", PublicProfileRules.normalizeAvatarId("dragon"))
        assertEquals("🦊", PublicProfileRules.avatarFor("dragon").symbol)
    }
}
