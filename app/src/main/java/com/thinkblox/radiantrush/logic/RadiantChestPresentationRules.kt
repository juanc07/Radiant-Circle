package com.thinkblox.radiantrush.logic

/**
 * Phase 11E presentation-only rules for the Daily Radiant Chest.
 *
 * These values control pacing and visual intensity only. They do not change the
 * deterministic reward roll, XP, tickets, SKR perks, ranked attempts, or payout
 * eligibility.
 */
object RadiantChestPresentationRules {
    /**
     * The repository request starts immediately. This is only the minimum amount
     * of time the opening pose stays visible before a very fast save may reveal.
     */
    const val MINIMUM_OPENING_MS: Long = 760L

    const val CHARGE_MS: Int = 360
    const val SHAKE_MS: Int = 260
    const val LID_PREOPEN_MS: Int = 180
    const val ANTICIPATION_HOLD_MS: Long = 100L
    const val REVEAL_LID_MS: Int = 180
    const val REVEAL_BURST_MS: Int = 420
    const val REWARD_LIFT_MS: Int = 300

    fun rarityRank(rarity: String?): Int = when (rarity?.trim()?.lowercase()) {
        // Daily Radiant Chest rarity ladder.
        "spark" -> 0
        "pulse" -> 1
        "flare" -> 2
        "aurora" -> 3
        "legendary" -> 4

        // Compatibility with older/generic reward naming.
        "common" -> 0
        "uncommon" -> 1
        "rare" -> 2
        "epic" -> 3
        "mythic" -> 5
        else -> 0
    }

    fun particleCount(rarity: String?): Int = when (rarityRank(rarity)) {
        0 -> 8
        1 -> 10
        2 -> 12
        3 -> 16
        4 -> 20
        else -> 22
    }

    fun shockwaveCount(rarity: String?): Int = when (rarityRank(rarity)) {
        0, 1 -> 0
        2, 3 -> 1
        else -> 2
    }

    fun revealTagline(rarity: String?): String = when (rarityRank(rarity)) {
        0 -> "A bright start for today."
        1 -> "The Radiant pulse is building."
        2 -> "A brilliant flare joined your streak."
        3 -> "Rare aurora energy secured."
        4 -> "Legendary pull — a Radiant Supernova!"
        else -> "Mythic radiance secured."
    }
}
