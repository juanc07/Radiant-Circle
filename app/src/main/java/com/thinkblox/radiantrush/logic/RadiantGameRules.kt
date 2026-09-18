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
    const val COLLECTION_TOTAL = 12

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
            id = "pulse-core",
            title = "Pulse Core",
            rarity = "Common",
            symbol = "◈",
            description = "A compact core that hums with the rhythm of a finished Rush.",
            power = 2,
            duplicateShards = 5,
            rarityXp = 13,
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
            id = "flux-crystal",
            title = "Flux Crystal",
            rarity = "Uncommon",
            symbol = "✧",
            description = "A restless crystal that shifts as your combo climbs.",
            power = 4,
            duplicateShards = 9,
            rarityXp = 20,
        ),
        CollectibleDefinition(
            id = "echo-fragment",
            title = "Echo Fragment",
            rarity = "Uncommon",
            symbol = "◫",
            description = "A fragment that carries the echo of a near-perfect run.",
            power = 4,
            duplicateShards = 10,
            rarityXp = 22,
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
            id = "orbit-sigil",
            title = "Orbit Sigil",
            rarity = "Rare",
            symbol = "◎",
            description = "A precise orbital mark that appears after disciplined runs.",
            power = 6,
            duplicateShards = 14,
            rarityXp = 32,
        ),
        CollectibleDefinition(
            id = "aurora-lens",
            title = "Aurora Lens",
            rarity = "Rare",
            symbol = "◐",
            description = "A luminous lens that catches the color of your strongest streaks.",
            power = 7,
            duplicateShards = 16,
            rarityXp = 36,
        ),
        CollectibleDefinition(
            id = "phantom-halo",
            title = "Phantom Halo",
            rarity = "Epic",
            symbol = "◉",
            description = "A violet halo that shimmers with every high-energy run.",
            power = 8,
            duplicateShards = 20,
            rarityXp = 45,
        ),
        CollectibleDefinition(
            id = "seeker-relic",
            title = "Seeker Relic",
            rarity = "Epic",
            symbol = "✺",
            description = "A rare Seeker relic resonating with repeated mastery of the Rush.",
            power = 10,
            duplicateShards = 25,
            rarityXp = 55,
        ),
        CollectibleDefinition(
            id = "radiant-crown",
            title = "Radiant Crown",
            rarity = "Legendary",
            symbol = "♛",
            description = "A legendary relic earned by pushing your best score higher.",
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

        // Preserve the original rarity curve while widening each tier's collection pool.
        // Expanding the Vault should create more variety, not silently make high-rarity
        // rewards easier to obtain.
        val rarity = when {
            safeScore >= 3_500 && roll < 850 -> "Legendary"
            safeScore >= 2_600 && roll < 1_550 -> "Epic"
            safeScore >= 2_000 && roll < 2_900 -> "Rare"
            safeScore >= 1_300 && roll < 4_400 -> "Uncommon"
            else -> "Common"
        }
        val rarityPool = collectibles.filter { it.rarity == rarity }
        check(rarityPool.isNotEmpty()) { "Radiant collectible rarity pool must not be empty: $rarity" }
        val itemSeed = "$userSeed|$runSerial|$safeScore|$safeCombo|$rarity|collectible".hashCode() and Int.MAX_VALUE
        val collectible = rarityPool[itemSeed % rarityPool.size]
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

    init {
        check(collectibles.size == COLLECTION_TOTAL) {
            "Radiant Vault definition mismatch: expected $COLLECTION_TOTAL collectibles, found ${collectibles.size}"
        }
        check(collectibles.map { it.id }.distinct().size == COLLECTION_TOTAL) {
            "Radiant Vault collectible IDs must be unique."
        }
    }
}

data class RadiantRunResult(
    val score: Int,
    val maxCombo: Int,
    val radiantHits: Int,
    val corruptedHits: Int,
    val perfectHits: Int = 0,
    /** Stable client receipt identity. The same result object reuses it on retries. */
    val receiptId: String = Phase12CompetitionVerificationRules.newReceiptId(),
    /** Client-reported finish time. Trusted verification must never accept it blindly. */
    val completedAtEpochMillis: Long = System.currentTimeMillis(),
)
