package com.thinkblox.radiantrush.logic

/** Small, testable gate for the adult-only opt-in discovery surface. */
object CircleDiscoverySafetyRules {
    const val MINIMUM_AGE = 18

    fun canDiscover(
        adultConfirmed: Boolean,
        discoveryEnabled: Boolean,
    ): Boolean = adultConfirmed && discoveryEnabled
}
