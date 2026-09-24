package com.thinkblox.radiantrush.data

import android.content.Context
import com.thinkblox.radiantrush.logic.OreStakingRules
import java.math.BigInteger
import java.time.LocalDate

/**
 * Device-local observation baseline only. It never substitutes for protocol state.
 * The baseline is the first live lifetime-rewards observation made for a wallet/date.
 */
class OreDailyAccrualSnapshotStore(context: Context) {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    fun observe(
        walletAddress: String,
        liveLifetimeRewardsRaw: BigInteger,
        nowClientMs: Long = System.currentTimeMillis(),
        dateKey: String = LocalDate.now().toString(),
    ): OreAccruedToday {
        val normalizedWallet = walletAddress.trim()
        require(normalizedWallet.isNotBlank()) { "ORE accrual snapshot requires a wallet address." }
        val prefix = "$normalizedWallet|$dateKey"
        val baselineKey = "$prefix|lifetimeRaw"
        val capturedKey = "$prefix|capturedAtMs"

        val existingBaseline = preferences.getString(baselineKey, null)?.toBigIntegerOrNull()
        val baseline = existingBaseline ?: liveLifetimeRewardsRaw.also {
            preferences.edit()
                .putString(baselineKey, it.toString())
                .putLong(capturedKey, nowClientMs)
                .apply()
        }
        val capturedAt = if (existingBaseline == null) nowClientMs else preferences.getLong(capturedKey, nowClientMs)
        val accruedRaw = OreStakingRules.accruedTodayRaw(liveLifetimeRewardsRaw, baseline)

        return OreAccruedToday(
            rawAmount = accruedRaw.toString(),
            displayAmount = "${OreStakingRules.formatRawOre(accruedRaw)} ORE",
            baselineDate = dateKey,
            baselineCapturedAtClientMs = capturedAt,
            isExactMidnightBaseline = false,
            helperText = "Observed since the first verified portfolio check today. On-chain lifetime rewards remain authoritative.",
        )
    }

    private companion object {
        const val PREFERENCES_NAME = "ore_daily_accrual_v1"
    }
}
