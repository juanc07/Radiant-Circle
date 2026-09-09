package com.thinkblox.radiantrush.logic

import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Pure SKR tiering rules used by production code and JVM unit tests.
 *
 * This file intentionally has no Android, Compose, Firebase, wallet, or network
 * dependencies so it can run quickly on CI without an emulator or phone.
 */
object SkrTierRules {
    const val OFFICIAL_SKR_MINT = "SKRbvo6Gf7GondiT3BbTfuRDPqLWei4j2Qy2NPGZhW3"
    const val MAINNET_NETWORK_LABEL = "mainnet-beta"
    const val DEFAULT_SKR_DECIMALS = 6

    fun tierForBalance(balance: BigDecimal): SkrTierDecision = when {
        balance >= BigDecimal("10000") -> SkrTierDecision("Radiant Legend", "1.35x", 1.35, hasSkr = true)
        balance >= BigDecimal("1000") -> SkrTierDecision("Radiant Elite", "1.20x", 1.20, hasSkr = true)
        balance >= BigDecimal("100") -> SkrTierDecision("Radiant Holder", "1.10x", 1.10, hasSkr = true)
        balance > BigDecimal.ZERO -> SkrTierDecision("Radiant Scout", "1.05x", 1.05, hasSkr = true)
        else -> SkrTierDecision("Explorer", "1.00x", 1.00, hasSkr = false)
    }

    fun formatBalance(value: BigDecimal): String {
        if (value.compareTo(BigDecimal.ZERO) == 0) return "0"
        val normalized = value.stripTrailingZeros()
        return if (normalized.scale() <= 4) {
            normalized.toPlainString()
        } else {
            normalized.setScale(4, RoundingMode.DOWN).stripTrailingZeros().toPlainString()
        }
    }
}

data class SkrTierDecision(
    val label: String,
    val multiplierLabel: String,
    val multiplierValue: Double,
    val hasSkr: Boolean,
)
