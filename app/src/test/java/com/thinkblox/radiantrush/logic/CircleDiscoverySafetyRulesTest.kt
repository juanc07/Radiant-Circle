package com.thinkblox.radiantrush.logic

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CircleDiscoverySafetyRulesTest {
    @Test
    fun discoveryRequiresAdultConfirmationAndExplicitOptIn() {
        assertFalse(CircleDiscoverySafetyRules.canDiscover(adultConfirmed = false, discoveryEnabled = false))
        assertFalse(CircleDiscoverySafetyRules.canDiscover(adultConfirmed = true, discoveryEnabled = false))
        assertFalse(CircleDiscoverySafetyRules.canDiscover(adultConfirmed = false, discoveryEnabled = true))
        assertTrue(CircleDiscoverySafetyRules.canDiscover(adultConfirmed = true, discoveryEnabled = true))
    }
}
