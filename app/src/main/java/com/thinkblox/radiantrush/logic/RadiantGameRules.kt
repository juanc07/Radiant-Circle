package com.thinkblox.radiantrush.logic

import com.thinkblox.radiantrush.data.RadiantCollectiblePreview

/**
 * Pure Phase 10 game/reward rules.
 *
 * Nothing here moves SOL, SKR, or any token. Rush Tickets and Radiant Shards are
 * in-app progression only. The Android game client is intentionally not an
 * economic authority and rewards have no cash/token redemption path.
 */
object RadiantGameRules {
    const val STARTER_TICKETS = 3
    const val QUEST_TICKET_REWARD = 1
    const val CHEST_TICKET_REWARD = 2
    const val RUN_TICKET_COST = 1
    const val RUN_SECONDS = 20

    data class CollectibleDefinition(
        val id: String,
        val title: String,
        val rarity: String,
        val symbol: String,
        val description: String,
        val power: Int,
        val duplicateShards: Int,
        val rarityXp: Int,
    )

    data class RunReward(
        val collectible: CollectibleDefinition,
        val xpReward: Int,
        val duplicate: Boolean,
        val duplicateShards: Int,
        val capsuleTier: String,
    )

    val collectibles: List<CollectibleDefinition> = listOf(
        CollectibleDefinition(
            id = "spark-bit",
            title = "Spark Bit",
            rarity = "Common",
            symbol = "✦",
            description = "A tiny charge left behind by a clean Radiant hit.",
            power = 1,
            duplicateShards = 4,
            rarityXp = 10,
        ),
        CollectibleDefinition(
            id = "neon-circuit",
            title = "Neon Circuit",
            rarity = "Common",
            symbol = "◇",
            description = "A glowing loop that remembers your fastest combo.",
            power = 2,
            duplicateShards = 5,
            rarityXp = 12,
        ),
        CollectibleDefinition(
            id = "solar-shard",
            title = "Solar Shard",
            rarity = "Uncommon",
            symbol = "◆",
            description = "Warm crystalline energy formed from chained hits.",
            power = 3,
            duplicateShards = 8,
            rarityXp = 18,
        ),
        CollectibleDefinition(
            id = "nova-prism",
            title = "Nova Prism",
            rarity = "Rare",
            symbol = "⬢",
            description = "A rare prism that fractures Rush light into score bursts.",
            power = 5,
            duplicateShards = 12,
            rarityXp = 28,
        ),
        CollectibleDefinition(
            id = "phantom-halo",
            title = "Phantom Halo",
            rarity = "Epic",
            symbol = "◉",
            description = "A violet halo inspired by the wallet handoff that powers your proof run.",
            power = 8,
            duplicateShards = 20,
            rarityXp = 45,
        ),
        CollectibleDefinition(
            id = "radiant-crown",
            title = "Radiant Crown",
            rarity = "Legendary",
            symbol = "♛",
            description = "The rarest Phase 10 relic. Earn it by pushing a high-score run.",
            power = 13,
            duplicateShards = 35,
            rarityXp = 70,
        ),
    )

    fun capsuleTierForScore(score: Int): String = when {
        score >= 3_500 -> "Radiant Capsule"
        score >= 2_200 -> "Nova Capsule"
        score >= 1_200 -> "Pulse Capsule"
        else -> "Spark Capsule"
    }

    /**
     * Deterministic reveal for a submitted run. Better scores widen the pool,
     * while never promising or selling odds.
     */
    fun pickRunReward(
        score: Int,
        maxCombo: Int,
        userSeed: String,
        runSerial: Int,
        ownedCounts: Map<String, Int>,
    ): RunReward {
        val safeScore = score.coerceAtLeast(0)
        val safeCombo = maxCombo.coerceAtLeast(0)
        val seed = "$userSeed|$runSerial|$safeScore|$safeCombo".hashCode() and Int.MAX_VALUE
        val roll = seed % 10_000

        val index = when {
            // A strong run gets a real shot at the showcase items.
            safeScore >= 3_500 && roll < 850 -> 5
            safeScore >= 2_600 && roll < 1_550 -> 4
            safeScore >= 2_000 && roll < 2_900 -> 3
            safeScore >= 1_300 && roll < 4_400 -> 2
            roll < 7_100 -> 1
            else -> 0
        }

        val collectible = collectibles[index]
        val oldCount = ownedCounts[collectible.id] ?: 0
        val duplicate = oldCount > 0
        val comboBonus = (safeCombo.coerceAtMost(30) * 2)
        val scoreXp = (safeScore / 80).coerceIn(10, 80)
        val xpReward = scoreXp + comboBonus + collectible.rarityXp

        return RunReward(
            collectible = collectible,
            xpReward = xpReward,
            duplicate = duplicate,
            duplicateShards = if (duplicate) collectible.duplicateShards else 0,
            capsuleTier = capsuleTierForScore(safeScore),
        )
    }

    fun collectionPreview(ownedCounts: Map<String, Int>): List<RadiantCollectiblePreview> =
        collectibles.map { collectible ->
            RadiantCollectiblePreview(
                id = collectible.id,
                title = collectible.title,
                rarity = collectible.rarity,
                symbol = collectible.symbol,
                description = collectible.description,
                power = collectible.power,
                count = (ownedCounts[collectible.id] ?: 0).coerceAtLeast(0),
            )
        }

    fun ownedUniqueCount(ownedCounts: Map<String, Int>): Int =
        collectibles.count { (ownedCounts[it.id] ?: 0) > 0 }
}

data class RadiantRunResult(
    val score: Int,
    val maxCombo: Int,
    val radiantHits: Int,
    val corruptedHits: Int,
)
