package com.thinkblox.radiantrush.data

import android.content.Context

/**
 * Local-only safety choices for Shake to Discover.
 *
 * We intentionally do not collect or persist a birth date. The user confirms the
 * adult-only requirement locally, and discovery visibility remains an explicit
 * opt-in that can be turned off at any time.
 */
class CircleDiscoverySafetyPreferences(context: Context) {
    data class Snapshot(
        val adultConfirmed: Boolean,
        val discoveryEnabled: Boolean,
    )

    private val preferences = context.getSharedPreferences(
        PREFERENCES_NAME,
        Context.MODE_PRIVATE,
    )

    fun snapshot(): Snapshot = Snapshot(
        adultConfirmed = preferences.getBoolean(KEY_ADULT_CONFIRMED, false),
        discoveryEnabled = preferences.getBoolean(KEY_DISCOVERY_ENABLED, false),
    )

    fun confirmAdultAndEnable(): Snapshot {
        preferences.edit()
            .putBoolean(KEY_ADULT_CONFIRMED, true)
            .putBoolean(KEY_DISCOVERY_ENABLED, true)
            .apply()
        return snapshot()
    }

    fun setDiscoveryEnabled(enabled: Boolean): Snapshot {
        val adultConfirmed = preferences.getBoolean(KEY_ADULT_CONFIRMED, false)
        preferences.edit()
            .putBoolean(KEY_DISCOVERY_ENABLED, enabled && adultConfirmed)
            .apply()
        return snapshot()
    }

    private companion object {
        const val PREFERENCES_NAME = "circle_discovery_safety_v1"
        const val KEY_ADULT_CONFIRMED = "adult_confirmed"
        const val KEY_DISCOVERY_ENABLED = "discovery_enabled"
    }
}
