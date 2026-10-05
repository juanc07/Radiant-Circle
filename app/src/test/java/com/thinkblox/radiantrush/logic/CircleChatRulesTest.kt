package com.thinkblox.radiantrush.logic

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CircleChatRulesTest {
    @Test
    fun messageValidationRejectsBlankAndOverLimitText() {
        assertFalse(CircleChatRules.isValidMessage("   "))
        assertTrue(CircleChatRules.isValidMessage("Hello Circle"))
        assertFalse(CircleChatRules.isValidMessage("x".repeat(CircleChatRules.MAX_MESSAGE_LENGTH + 1)))
    }

    @Test
    fun sanitizationTrimsAndCapsMessage() {
        val input = "   " + "x".repeat(CircleChatRules.MAX_MESSAGE_LENGTH + 50) + "   "
        val clean = CircleChatRules.sanitizeMessage(input)
        assertEquals(CircleChatRules.MAX_MESSAGE_LENGTH, clean.length)
        assertTrue(clean.all { it == 'x' })
    }

    @Test
    fun rateLimitRequiresOneSecondBetweenMessages() {
        assertFalse(CircleChatRules.canSendAfter(10_000L, 10_999L))
        assertTrue(CircleChatRules.canSendAfter(10_000L, 11_000L))
        assertTrue(CircleChatRules.canSendAfter(0L, 1L))
    }

    @Test
    fun connectionCutoffHidesMessagesFromPreviousAcceptedSession() {
        assertFalse(CircleChatRules.isAtOrAfterConnectionStart(9_999L, 10_000L))
        assertTrue(CircleChatRules.isAtOrAfterConnectionStart(10_000L, 10_000L))
        assertTrue(CircleChatRules.isAtOrAfterConnectionStart(10_001L, 10_000L))
        assertTrue(CircleChatRules.isAtOrAfterConnectionStart(1L, 0L))
        assertFalse(CircleChatRules.isAtOrAfterConnectionStart(0L, 10_000L))
    }

    @Test
    fun reportReasonsAreNormalizedToKnownValues() {
        assertEquals("Spam", CircleChatRules.normalizeReportReason("spam"))
        assertEquals("Unsafe behavior", CircleChatRules.normalizeReportReason("Unsafe behavior"))
        assertEquals("Other", CircleChatRules.normalizeReportReason("something else"))
    }

    @Test
    fun typingPresenceUsesBoundedRefreshAndExpiryWindows() {
        assertTrue(CircleChatRules.TYPING_REFRESH_INTERVAL_MILLIS > 0L)
        assertTrue(CircleChatRules.TYPING_STALE_AFTER_MILLIS > CircleChatRules.TYPING_REFRESH_INTERVAL_MILLIS)
    }
}
