package com.thinkblox.radiantrush.logic

import java.math.BigDecimal
import java.math.BigInteger
import java.math.RoundingMode

/**
 * Phase 12B trusted Weekly Radiant Cup configuration rules.
 *
 * Android is a read-only presentation client for these values. A config may
 * describe a sponsor and an intended SKR prize, but it can never enable payout
 * or make client-reported scores trusted. Funding verification is a separate
 * Phase 12C trusted-server concern.
 */
object Phase12WeeklyCupConfigRules {
    const val SCHEMA_VERSION = 2
    const val CONFIGURATION_AUTHORITY = "trusted-admin-phase12b"

    const val STATUS_DRAFT = "DRAFT"
    const val STATUS_ANNOUNCED = "ANNOUNCED"
    const val STATUS_OPEN = "OPEN"
    const val STATUS_CLOSED = "CLOSED"
    const val STATUS_CANCELLED = "CANCELLED"

    const val FUNDING_NOT_CONFIGURED = "NOT_CONFIGURED"
    const val FUNDING_NOT_VERIFIED = "NOT_VERIFIED"
    const val FUNDING_VERIFIED = "VERIFIED"
    const val FUNDING_REJECTED = "REJECTED"

    fun presentation(
        expectedWeekKey: String,
        schemaVersion: Int?,
        weekKey: String?,
        status: String?,
        sponsorName: String?,
        sponsorNote: String?,
        prizeAssetSymbol: String?,
        prizeMint: String?,
        prizeDecimals: Int?,
        prizeAmountAtomic: String?,
        placementAllocationsBps: Map<Int, Int>,
        startsAtEpochMillis: Long?,
        endsAtEpochMillis: Long?,
        fundingWalletAddress: String?,
        fundingVerificationStatus: String?,
        configurationAuthority: String?,
        trustedResultsRequired: Boolean?,
    ): TrustedWeeklyCupConfigState {
        val normalizedStatus = status?.trim()?.uppercase().orEmpty()
        val safeSponsor = sponsorName?.trim()?.takeIf { it.isNotBlank() }?.take(80)
        val safeNote = sponsorNote?.trim()?.takeIf { it.isNotBlank() }?.take(180)
        val safeWeek = weekKey?.trim().orEmpty()
        val safeAuthority = configurationAuthority?.trim().orEmpty()
        val safeSymbol = prizeAssetSymbol?.trim()?.uppercase().orEmpty()
        val safeMint = prizeMint?.trim().orEmpty()
        val safeFundingWallet = fundingWalletAddress?.trim()?.takeIf { it.length in 32..64 }

        val baseValid = schemaVersion == SCHEMA_VERSION &&
            safeWeek == expectedWeekKey &&
            safeAuthority == CONFIGURATION_AUTHORITY &&
            normalizedStatus in setOf(
                STATUS_DRAFT,
                STATUS_ANNOUNCED,
                STATUS_OPEN,
                STATUS_CLOSED,
                STATUS_CANCELLED,
            ) &&
            safeSponsor != null &&
            safeSymbol == "SKR" &&
            safeMint == SkrTierRules.OFFICIAL_SKR_MINT &&
            prizeDecimals == SkrTierRules.DEFAULT_SKR_DECIMALS &&
            trustedResultsRequired == true &&
            startsAtEpochMillis != null &&
            endsAtEpochMillis != null &&
            startsAtEpochMillis > 0L &&
            endsAtEpochMillis > startsAtEpochMillis

        val amountAtomic = prizeAmountAtomic
            ?.trim()
            ?.takeIf { it.matches(Regex("^[0-9]{1,30}$")) }
            ?.let { runCatching { BigInteger(it) }.getOrNull() }
            ?.takeIf { it > BigInteger.ZERO }

        val allocationsValid = placementAllocationsBps.isNotEmpty() &&
            placementAllocationsBps.keys.all { it in 1..10 } &&
            placementAllocationsBps.values.all { it in 1..10_000 } &&
            placementAllocationsBps.values.sum() == 10_000

        if (!baseValid || amountAtomic == null || !allocationsValid) {
            return TrustedWeeklyCupConfigState(
                recognized = false,
                published = false,
                statusCode = "UNCONFIGURED",
                statusLabel = "No trusted Cup config",
                fundingStatusCode = FUNDING_NOT_CONFIGURED,
                fundingStatusLabel = "Funding wallet not configured",
                trustedResultsRequired = true,
                payoutEnabled = false,
            )
        }

        val fundingCode = when (fundingVerificationStatus?.trim()?.uppercase()) {
            FUNDING_VERIFIED -> FUNDING_VERIFIED
            FUNDING_REJECTED -> FUNDING_REJECTED
            FUNDING_NOT_VERIFIED -> FUNDING_NOT_VERIFIED
            FUNDING_NOT_CONFIGURED -> FUNDING_NOT_CONFIGURED
            else -> if (safeFundingWallet == null) FUNDING_NOT_CONFIGURED else FUNDING_NOT_VERIFIED
        }
        val fundingLabel = when (fundingCode) {
            FUNDING_VERIFIED -> "Funding verified on-chain"
            FUNDING_REJECTED -> "Funding verification failed"
            FUNDING_NOT_VERIFIED -> "Funding not verified"
            else -> "Funding wallet not configured"
        }

        val statusLabel = when (normalizedStatus) {
            STATUS_DRAFT -> "Cup draft"
            STATUS_ANNOUNCED -> "Cup announced"
            STATUS_OPEN -> "Cup open"
            STATUS_CLOSED -> "Cup closed"
            STATUS_CANCELLED -> "Cup cancelled"
            else -> "Cup unavailable"
        }
        val published = normalizedStatus in setOf(STATUS_ANNOUNCED, STATUS_OPEN, STATUS_CLOSED)

        return TrustedWeeklyCupConfigState(
            recognized = true,
            published = published,
            statusCode = normalizedStatus,
            statusLabel = statusLabel,
            sponsorName = safeSponsor,
            sponsorNote = safeNote,
            prizeLabel = formatSkrAmount(amountAtomic, SkrTierRules.DEFAULT_SKR_DECIMALS),
            placementAllocationLabel = allocationLabel(placementAllocationsBps),
            startsAtEpochMillis = startsAtEpochMillis,
            endsAtEpochMillis = endsAtEpochMillis,
            fundingWalletAddress = safeFundingWallet,
            fundingStatusCode = fundingCode,
            fundingStatusLabel = fundingLabel,
            trustedResultsRequired = true,
            payoutEnabled = false,
        )
    }

    private fun formatSkrAmount(amountAtomic: BigInteger, decimals: Int): String {
        val amount = BigDecimal(amountAtomic, decimals)
            .setScale(decimals, RoundingMode.DOWN)
            .stripTrailingZeros()
        return "${amount.toPlainString()} SKR"
    }

    private fun allocationLabel(allocations: Map<Int, Int>): String = allocations
        .toSortedMap()
        .entries
        .joinToString(separator = " • ") { (rank, bps) ->
            val percent = BigDecimal(bps).movePointLeft(2).stripTrailingZeros().toPlainString()
            "#$rank $percent%"
        }
        .let { "Prize split • $it" }
}

data class TrustedWeeklyCupConfigState(
    val recognized: Boolean,
    val published: Boolean,
    val statusCode: String,
    val statusLabel: String,
    val sponsorName: String? = null,
    val sponsorNote: String? = null,
    val prizeLabel: String? = null,
    val placementAllocationLabel: String? = null,
    val startsAtEpochMillis: Long? = null,
    val endsAtEpochMillis: Long? = null,
    val fundingWalletAddress: String? = null,
    val fundingStatusCode: String,
    val fundingStatusLabel: String,
    val trustedResultsRequired: Boolean,
    val payoutEnabled: Boolean,
)
