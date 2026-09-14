package com.thinkblox.radiantrush.logic

/**
 * Pure client-side wallet access rules.
 *
 * This is a UX guard only. Firestore/Admin trust rules remain authoritative.
 * Radiant Rush itself stays playable without a wallet, but a walletless run is
 * Casual and must never be presented as a Weekly Radiant Cup entry.
 */
object WalletFeatureAccessRules {
    fun isCupRunning(
        cupStatusCode: String?,
        startsAtEpochMillis: Long,
        endsAtEpochMillis: Long,
        nowEpochMillis: Long = System.currentTimeMillis(),
    ): Boolean {
        if (cupStatusCode?.trim()?.uppercase() != Phase12WeeklyCupConfigRules.STATUS_OPEN) return false
        if (startsAtEpochMillis <= 0L || endsAtEpochMillis <= startsAtEpochMillis) return false
        return nowEpochMillis >= startsAtEpochMillis && nowEpochMillis < endsAtEpochMillis
    }

    fun radiantRushAccess(
        walletConnected: Boolean,
        cupStatusCode: String?,
        startsAtEpochMillis: Long,
        endsAtEpochMillis: Long,
        nowEpochMillis: Long = System.currentTimeMillis(),
    ): RadiantRushWalletAccess {
        val cupRunning = isCupRunning(
            cupStatusCode = cupStatusCode,
            startsAtEpochMillis = startsAtEpochMillis,
            endsAtEpochMillis = endsAtEpochMillis,
            nowEpochMillis = nowEpochMillis,
        )
        return RadiantRushWalletAccess(
            cupRunning = cupRunning,
            walletConnected = walletConnected,
            shouldWarnBeforeCasualRun = cupRunning && !walletConnected,
        )
    }
}

data class RadiantRushWalletAccess(
    val cupRunning: Boolean,
    val walletConnected: Boolean,
    val shouldWarnBeforeCasualRun: Boolean,
) {
    val rankedCompetitionAvailable: Boolean
        get() = walletConnected
}
