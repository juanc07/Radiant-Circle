package com.thinkblox.radiantrush.logic

/**
 * Presentation-only pacing for the Daily Radiance ritual.
 *
 * Firebase remains authoritative; these values only prevent a fast server response
 * from skipping the anticipation/reveal animation.
 */
object DailyRadiancePresentationRules {
    const val MINIMUM_OPENING_MS: Long = 680L
    const val CHARGE_MS: Int = 500
    const val REVEAL_MS: Int = 420
    const val SPARK_BURST_MS: Int = 620
}
