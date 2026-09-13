package com.thinkblox.radiantrush.logic

/**
 * Pure public-leaderboard identity rules.
 *
 * Firebase Anonymous Auth remains the private storage/session owner, while a connected
 * Solana wallet is the public rank identity. Reinstalls may create new Firebase UIDs,
 * and older builds can leave a stale full wallet address in a leaderboard document after
 * writing `walletAddressShort = "No wallet"`. This class treats that explicit disconnect
 * marker as authoritative and collapses legacy short/full representations safely.
 */
data class LeaderboardCandidate(
    val sourceId: String,
    val displayName: String,
    val walletAddress: String?,
    val walletAddressShort: String?,
    val xp: Int,
    val streak: Int,
    val tier: String,
    val avatarId: String = "fox",
    val updatedAtMs: Long = 0L,
)

object LeaderboardRules {
    /**
     * Returns at most [limit] unique public-wallet rows, ordered by XP descending.
     *
     * Important legacy rules:
     * - `walletAddressShort == "No wallet"` is a disconnect tombstone. The row is excluded
     *   even when an older app version accidentally left a stale `walletAddress` field.
     * - A genuinely full wallet address is canonical.
     * - A shortened address stored in either field is mapped to a full wallet when the
     *   first-4/last-4 fingerprint identifies exactly one full wallet in this query.
     * - Duplicate anonymous Firebase UIDs for one wallet collapse to the best XP row.
     * - If two rows contain genuinely different full wallet addresses, they remain two
     *   public players; the app cannot safely infer that two different wallets are one human.
     */
    fun collapseByWallet(
        candidates: List<LeaderboardCandidate>,
        limit: Int = 20,
    ): List<LeaderboardCandidate> {
        if (limit <= 0 || candidates.isEmpty()) return emptyList()

        val eligible = candidates.filterNot(::isExplicitlyDisconnected)

        val fullWalletsByFingerprint = eligible
            .mapNotNull { candidate ->
                val full = canonicalFullWallet(candidate.walletAddress) ?: return@mapNotNull null
                walletFingerprint(full)?.let { fingerprint -> fingerprint to full }
            }
            .groupBy(keySelector = { it.first }, valueTransform = { it.second })
            .mapValues { (_, wallets) -> wallets.distinct().singleOrNull() }

        val ranked = eligible.sortedWith(candidateComparator)
        val bestByIdentity = linkedMapOf<String, LeaderboardCandidate>()

        ranked.forEach { candidate ->
            val identity = identityKey(candidate, fullWalletsByFingerprint) ?: return@forEach
            val current = bestByIdentity[identity]
            if (current == null || candidateComparator.compare(candidate, current) < 0) {
                bestByIdentity[identity] = candidate
            }
        }

        return bestByIdentity.values
            .sortedWith(candidateComparator)
            .take(limit)
    }

    /** Short, non-sensitive public label used by the ranking UI for diagnosis/transparency. */
    fun walletLabel(candidate: LeaderboardCandidate): String? {
        if (isExplicitlyDisconnected(candidate)) return null

        canonicalFullWallet(candidate.walletAddress)?.let { return shortenWallet(it) }
        normalizeLegacyShort(candidate.walletAddressShort)?.let { return it }
        normalizeLegacyShort(candidate.walletAddress)?.let { return it }
        return null
    }

    private fun identityKey(
        candidate: LeaderboardCandidate,
        fullWalletsByFingerprint: Map<String, String?>,
    ): String? {
        if (isExplicitlyDisconnected(candidate)) return null

        canonicalFullWallet(candidate.walletAddress)?.let { return "wallet:$it" }

        val fingerprint = walletFingerprint(candidate.walletAddressShort)
            ?: walletFingerprint(candidate.walletAddress)
            ?: return null

        val mappedFull = fullWalletsByFingerprint[fingerprint]
        return if (mappedFull != null) {
            "wallet:$mappedFull"
        } else {
            "legacy-short:$fingerprint"
        }
    }

    private fun isExplicitlyDisconnected(candidate: LeaderboardCandidate): Boolean {
        val marker = candidate.walletAddressShort?.trim().orEmpty()
        return marker.equals("No wallet", ignoreCase = true) ||
            marker.equals("Wallet not connected", ignoreCase = true) ||
            marker.equals("Disconnected", ignoreCase = true)
    }

    /**
     * Accept only an unshortened wallet-like token as the canonical full identity.
     * This deliberately rejects legacy values containing ellipsis/dots/spaces so they
     * are handled through fingerprint matching instead of becoming a second identity.
     */
    private fun canonicalFullWallet(value: String?): String? {
        var clean = value?.trim().orEmpty()
        if (clean.isBlank()) return null
        if (clean.equals("No wallet", ignoreCase = true)) return null

        if (clean.startsWith("solana:", ignoreCase = true)) {
            clean = clean.substringAfter(':').substringBefore('?').trim()
        }

        val looksShortened = clean.contains('…') ||
            clean.contains('⋯') ||
            clean.contains("..") ||
            clean.any(Char::isWhitespace)
        if (looksShortened) return null

        // Solana public keys are base58 strings. A broad length window keeps the rule
        // resilient while rejecting labels/debug strings that are not wallet identities.
        if (clean.length !in 32..64) return null
        if (!clean.all { it in BASE58_CHARS }) return null
        return clean
    }

    /**
     * Produces a comparable first-4/last-4 fingerprint from either a full wallet or a
     * legacy shortened form such as `AbCd…Wxyz`, `AbCd...Wxyz`, or `AbCd⋯Wxyz`.
     */
    private fun walletFingerprint(value: String?): String? {
        val raw = value?.trim().orEmpty()
        if (raw.isBlank() || raw.equals("No wallet", ignoreCase = true)) return null

        canonicalFullWallet(raw)?.let { full ->
            return "${full.take(4)}|${full.takeLast(4)}"
        }

        val compact = raw.replace(" ", "")
        val separators = listOf("…", "⋯", "...", "..")
        val separator = separators.firstOrNull { compact.contains(it) } ?: return null
        val left = compact.substringBefore(separator)
        val right = compact.substringAfter(separator)
        if (left.length < 4 || right.length < 4) return null

        val prefix = left.take(4)
        val suffix = right.takeLast(4)
        if (!prefix.all { it in BASE58_CHARS } || !suffix.all { it in BASE58_CHARS }) return null
        return "$prefix|$suffix"
    }

    private fun normalizeLegacyShort(value: String?): String? {
        val raw = value?.trim().orEmpty()
        if (raw.isBlank() || raw.equals("No wallet", ignoreCase = true)) return null

        canonicalFullWallet(raw)?.let { return shortenWallet(it) }
        val fingerprint = walletFingerprint(raw) ?: return null
        val (prefix, suffix) = fingerprint.split('|', limit = 2)
        return "$prefix…$suffix"
    }

    private fun shortenWallet(wallet: String): String = if (wallet.length <= 12) {
        wallet
    } else {
        "${wallet.take(4)}…${wallet.takeLast(4)}"
    }

    private val candidateComparator =
        compareByDescending<LeaderboardCandidate> { it.xp }
            .thenByDescending { it.updatedAtMs }
            .thenBy { it.sourceId }

    private const val BASE58_CHARS = "123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz"
}
