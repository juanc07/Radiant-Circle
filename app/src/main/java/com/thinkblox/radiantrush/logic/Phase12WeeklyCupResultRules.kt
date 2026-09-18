package com.thinkblox.radiantrush.logic

import java.math.BigDecimal
import java.math.BigInteger
import java.math.RoundingMode

/**
 * Phase 12D read-only validation for finalized Weekly Radiant Cup results.
 *
 * Android never creates or finalizes these records. It only renders a result when
 * the trusted result + winner shape is internally consistent and payout remains disabled.
 */
object Phase12WeeklyCupResultRules {
    const val RESULT_SCHEMA_VERSION = 1
    const val RESULT_VERSION = 1
    const val FINALIZATION_STATUS = "FINALIZED"
    const val FINALIZATION_AUTHORITY = "trusted-admin-phase12d"
    const val WINNER_PAYOUT_STATUS = "NOT_ENABLED"

    fun presentation(
        expectedWeekKey: String,
        schemaVersion: Int?,
        resultVersion: Int?,
        weekKey: String?,
        finalizationStatus: String?,
        finalizationAuthority: String?,
        finalizedAtEpochMillis: Long?,
        prizeAssetSymbol: String?,
        prizeMint: String?,
        prizeDecimals: Int?,
        prizeAmountAtomic: String?,
        placementAllocationsBps: Map<Int, Int>,
        fundingVerificationStatusAtClose: String?,
        payoutEnabled: Boolean?,
        payoutReady: Boolean?,
        winnerCount: Int?,
        winners: List<TrustedWeeklyCupWinnerInput>,
    ): TrustedWeeklyCupResultState {
        val cleanWeek = weekKey?.trim().orEmpty()
        val cleanStatus = finalizationStatus?.trim()?.uppercase().orEmpty()
        val cleanAuthority = finalizationAuthority?.trim().orEmpty()
        val cleanSymbol = prizeAssetSymbol?.trim()?.uppercase().orEmpty()
        val cleanFunding = fundingVerificationStatusAtClose?.trim()?.uppercase().orEmpty()
        val prizeAtomic = parseAtomic(prizeAmountAtomic)?.takeIf { it > BigInteger.ZERO }
        val allocations = placementAllocationsBps.toSortedMap()

        val baseValid = schemaVersion == RESULT_SCHEMA_VERSION &&
            resultVersion == RESULT_VERSION &&
            cleanWeek == expectedWeekKey &&
            cleanStatus == FINALIZATION_STATUS &&
            cleanAuthority == FINALIZATION_AUTHORITY &&
            finalizedAtEpochMillis != null &&
            finalizedAtEpochMillis > 0L &&
            cleanSymbol == "SKR" &&
            prizeMint?.trim() == SkrTierRules.OFFICIAL_SKR_MINT &&
            prizeDecimals == SkrTierRules.DEFAULT_SKR_DECIMALS &&
            prizeAtomic != null &&
            allocations.isNotEmpty() &&
            allocations.keys.all { it in 1..10 } &&
            allocations.keys.toList() == (1..allocations.size).toList() &&
            allocations.values.all { it in 1..10_000 } &&
            allocations.values.sum() == 10_000 &&
            cleanFunding in setOf(
                Phase12WeeklyCupConfigRules.FUNDING_NOT_CONFIGURED,
                Phase12WeeklyCupConfigRules.FUNDING_NOT_VERIFIED,
                Phase12WeeklyCupConfigRules.FUNDING_VERIFIED,
                Phase12WeeklyCupConfigRules.FUNDING_REJECTED,
            ) &&
            payoutEnabled == false &&
            payoutReady == false &&
            winnerCount == allocations.size

        if (!baseValid) return TrustedWeeklyCupResultState()

        val winnersByPlacement = winners.associateBy { it.placement }
        if (winnersByPlacement.size != winners.size || winnersByPlacement.keys != allocations.keys) {
            return TrustedWeeklyCupResultState()
        }

        val parsedWinners = allocations.mapNotNull { (placement, bps) ->
            val winner = winnersByPlacement[placement] ?: return@mapNotNull null
            val expectedPrizeNumerator = prizeAtomic * BigInteger.valueOf(bps.toLong())
            if (expectedPrizeNumerator.mod(BPS_DENOMINATOR) != BigInteger.ZERO) return@mapNotNull null
            val expectedPrizeAtomic = expectedPrizeNumerator.divide(BPS_DENOMINATOR)
            val actualPrizeAtomic = parseAtomic(winner.prizeAmountAtomic) ?: return@mapNotNull null
            val wallet = canonicalWallet(winner.walletAddress) ?: return@mapNotNull null
            if (
                winner.schemaVersion != RESULT_SCHEMA_VERSION ||
                winner.resultVersion != RESULT_VERSION ||
                winner.weekKey?.trim() != expectedWeekKey ||
                winner.placement != placement ||
                winner.receiptId?.trim().isNullOrBlank() ||
                winner.score == null || winner.score < 0 ||
                winner.maxCombo == null || winner.maxCombo < 0 ||
                winner.perfectHits == null || winner.perfectHits < 0 ||
                actualPrizeAtomic != expectedPrizeAtomic ||
                winner.prizeAssetSymbol?.trim()?.uppercase() != "SKR" ||
                winner.payoutStatus?.trim()?.uppercase() != WINNER_PAYOUT_STATUS ||
                winner.fundingVerificationStatusAtClose?.trim()?.uppercase() != cleanFunding ||
                winner.resultAuthority?.trim() != FINALIZATION_AUTHORITY ||
                winner.payoutEnabled != false ||
                winner.payoutReady != false
            ) {
                return@mapNotNull null
            }

            WeeklyCupWinnerPresentation(
                placement = placement,
                walletAddress = wallet,
                walletLabel = shortenWallet(wallet),
                score = winner.score,
                prizeLabel = formatSkrAmount(actualPrizeAtomic),
            )
        }

        if (parsedWinners.size != allocations.size) return TrustedWeeklyCupResultState()
        val summedPrize = allocations.entries.fold(BigInteger.ZERO) { total, (placement, _) ->
            val winner = winnersByPlacement.getValue(placement)
            total + (parseAtomic(winner.prizeAmountAtomic) ?: BigInteger.ZERO)
        }
        if (summedPrize != prizeAtomic) return TrustedWeeklyCupResultState()

        return TrustedWeeklyCupResultState(
            recognized = true,
            weekKey = expectedWeekKey,
            fundingStatusCode = cleanFunding,
            fundingStatusLabel = when (cleanFunding) {
                Phase12WeeklyCupConfigRules.FUNDING_VERIFIED -> "Funding verified"
                Phase12WeeklyCupConfigRules.FUNDING_REJECTED -> "Prize unavailable"
                else -> "Funding pending"
            },
            prizeLabel = formatSkrAmount(prizeAtomic),
            winners = parsedWinners,
            payoutEnabled = false,
            payoutReady = false,
        )
    }

    private fun parseAtomic(value: String?): BigInteger? = value
        ?.trim()
        ?.takeIf { it.matches(Regex("^[0-9]{1,30}$")) }
        ?.let { runCatching { BigInteger(it) }.getOrNull() }

    private fun canonicalWallet(value: String?): String? {
        val clean = value?.trim().orEmpty()
        if (clean.length !in 32..44) return null
        if (!clean.all { it in BASE58_CHARS }) return null
        return clean
    }

    private fun shortenWallet(wallet: String): String = if (wallet.length <= 12) {
        wallet
    } else {
        "${wallet.take(4)}…${wallet.takeLast(4)}"
    }

    private fun formatSkrAmount(amountAtomic: BigInteger): String {
        val amount = BigDecimal(amountAtomic, SkrTierRules.DEFAULT_SKR_DECIMALS)
            .setScale(SkrTierRules.DEFAULT_SKR_DECIMALS, RoundingMode.DOWN)
            .stripTrailingZeros()
        return "${amount.toPlainString()} SKR"
    }

    private val BPS_DENOMINATOR = BigInteger.valueOf(10_000L)
    private const val BASE58_CHARS = "123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz"
}

data class TrustedWeeklyCupWinnerInput(
    val schemaVersion: Int?,
    val resultVersion: Int?,
    val weekKey: String?,
    val placement: Int,
    val walletAddress: String?,
    val receiptId: String?,
    val score: Int?,
    val maxCombo: Int?,
    val perfectHits: Int?,
    val prizeAmountAtomic: String?,
    val prizeAssetSymbol: String?,
    val payoutStatus: String?,
    val fundingVerificationStatusAtClose: String?,
    val resultAuthority: String?,
    val payoutEnabled: Boolean?,
    val payoutReady: Boolean?,
)

data class WeeklyCupWinnerPresentation(
    val placement: Int,
    val walletAddress: String,
    val walletLabel: String,
    val score: Int,
    val prizeLabel: String,
)

data class TrustedWeeklyCupResultState(
    val recognized: Boolean = false,
    val weekKey: String? = null,
    val fundingStatusCode: String = Phase12WeeklyCupConfigRules.FUNDING_NOT_CONFIGURED,
    val fundingStatusLabel: String = "Funding pending",
    val prizeLabel: String? = null,
    val winners: List<WeeklyCupWinnerPresentation> = emptyList(),
    val payoutEnabled: Boolean = false,
    val payoutReady: Boolean = false,
)
