package com.thinkblox.radiantrush.logic

/**
 * Phase 12G account -> competition-wallet lock contract.
 *
 * The first Ranked Cup receipt successfully persisted for a Firebase UID/week
 * creates one immutable Firestore lock. Later receipts for the same account/week
 * must use exactly that wallet. Firestore rules are authoritative; Android only
 * mirrors the contract for the atomic receipt+lock write and user-facing errors.
 */
object Phase12CompetitionWalletLockRules {
    const val LOCK_SCHEMA_VERSION = 1
    const val LOCK_AUTHORITY = "firestore-first-ranked-entry-phase12g"

    fun normalizedWallet(value: String?): String? = value?.trim()?.takeIf { it.isNotBlank() }

    fun isTrustedLockShape(
        schemaVersion: Int?,
        weekKey: String?,
        ownerUid: String?,
        lockAuthority: String?,
        expectedWeekKey: String,
        expectedOwnerUid: String,
    ): Boolean =
        schemaVersion == LOCK_SCHEMA_VERSION &&
            weekKey?.trim() == expectedWeekKey &&
            ownerUid?.trim() == expectedOwnerUid &&
            lockAuthority == LOCK_AUTHORITY

    fun walletMatchesLock(lockedWallet: String?, candidateWallet: String?): Boolean {
        val locked = normalizedWallet(lockedWallet) ?: return false
        val candidate = normalizedWallet(candidateWallet) ?: return false
        return locked == candidate
    }

    fun allowsRankedEntry(
        lockExists: Boolean,
        trustedLockShape: Boolean,
        lockedWallet: String?,
        candidateWallet: String?,
    ): Boolean = !lockExists || (trustedLockShape && walletMatchesLock(lockedWallet, candidateWallet))
}
