package com.thinkblox.radiantrush.logic

import com.thinkblox.radiantrush.data.ApproximateCircleLocation
import java.security.MessageDigest
import java.util.Locale
import kotlin.math.floor

/**
 * Privacy-preserving discovery helpers.
 *
 * Exact phone coordinates remain local. Firestore receives only opaque hashes of
 * coarse geographic cells. Phase 14B.2 removes the short-lived time bucket so a
 * user who explicitly enables discovery stays discoverable until they turn it off.
 * No background location tracking is introduced; the stored area is refreshed when
 * the user actively uses discovery.
 */
object CircleDiscoveryRules {
    enum class SearchTier(
        val fieldName: String,
        val cellDegrees: Double?,
        val distanceLabel: String,
    ) {
        Local("localWindowKeys", 0.10, "Roughly within 30 km"),
        Regional("regionalWindowKeys", 0.50, "Roughly within 150 km"),
        Broad("broadWindowKeys", 2.50, "Roughly within 700 km"),
        Country("countryWindowKeys", null, "Same country"),
        Global("globalWindowKeys", null, "Across Radiant Circle"),
    }

    data class PresenceKeys(
        val localWindowKeys: List<String>,
        val regionalWindowKeys: List<String>,
        val broadWindowKeys: List<String>,
        val countryWindowKeys: List<String>,
        val globalWindowKeys: List<String>,
    )

    @Suppress("UNUSED_PARAMETER")
    fun presenceKeys(
        location: ApproximateCircleLocation,
        nowMillis: Long,
    ): PresenceKeys {
        val country = normalizedCountry(location.countryCode)
        val localCell = cellKey(location.latitude, location.longitude, 0.10)
        val regionalCell = cellKey(location.latitude, location.longitude, 0.50)
        val broadCell = cellKey(location.latitude, location.longitude, 2.50)

        // Keep two opaque values per field for backward-compatible Firestore shape.
        // Querying uses the primary key; the secondary key gives us a reserved
        // version slot without exposing a raw geographic identifier.
        fun persistentKeys(prefix: String): List<String> = listOf(
            opaquePresenceKey(prefix, "primary"),
            opaquePresenceKey(prefix, "secondary"),
        )

        return PresenceKeys(
            localWindowKeys = persistentKeys("local:$localCell"),
            regionalWindowKeys = persistentKeys("regional:$regionalCell"),
            broadWindowKeys = persistentKeys("broad:$broadCell"),
            countryWindowKeys = persistentKeys("country:$country"),
            globalWindowKeys = persistentKeys("global"),
        )
    }

    @Suppress("UNUSED_PARAMETER")
    fun queryKeys(
        tier: SearchTier,
        location: ApproximateCircleLocation,
        nowMillis: Long,
    ): List<String> = when (tier) {
        SearchTier.Local,
        SearchTier.Regional,
        SearchTier.Broad,
        -> {
            val degrees = requireNotNull(tier.cellDegrees)
            val prefix = when (tier) {
                SearchTier.Local -> "local"
                SearchTier.Regional -> "regional"
                SearchTier.Broad -> "broad"
                else -> error("Unexpected tier")
            }
            neighborCellKeys(location.latitude, location.longitude, degrees)
                .map { cell -> opaquePresenceKey("$prefix:$cell", "primary") }
        }

        SearchTier.Country -> listOf(
            opaquePresenceKey("country:${normalizedCountry(location.countryCode)}", "primary"),
        )
        SearchTier.Global -> listOf(opaquePresenceKey("global", "primary"))
    }

    fun pairId(uidA: String, uidB: String): String =
        listOf(uidA.trim(), uidB.trim())
            .sorted()
            .joinToString("__")

    private fun opaquePresenceKey(prefix: String, slot: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
            .digest("radiant-circle-v2|$prefix|$slot".toByteArray(Charsets.UTF_8))
        return digest.take(16).joinToString("") { byte -> "%02x".format(byte) }
    }

    private fun normalizedCountry(raw: String): String =
        raw.trim().uppercase(Locale.US).takeIf { it.length == 2 } ?: "ZZ"

    private fun neighborCellKeys(latitude: Double, longitude: Double, degrees: Double): List<String> {
        val latIndex = latitudeIndex(latitude, degrees)
        val lonIndex = longitudeIndex(longitude, degrees)
        return buildList(9) {
            for (latOffset in -1..1) {
                for (lonOffset in -1..1) {
                    add(cellKeyFromIndexes(latIndex + latOffset, lonIndex + lonOffset, degrees))
                }
            }
        }.distinct()
    }

    private fun cellKey(latitude: Double, longitude: Double, degrees: Double): String =
        cellKeyFromIndexes(
            latitudeIndex(latitude, degrees),
            longitudeIndex(longitude, degrees),
            degrees,
        )

    private fun latitudeIndex(latitude: Double, degrees: Double): Int =
        floor((latitude.coerceIn(-89.999999, 89.999999) + 90.0) / degrees).toInt()

    private fun longitudeIndex(longitude: Double, degrees: Double): Int {
        val normalized = ((longitude + 180.0) % 360.0 + 360.0) % 360.0
        return floor(normalized / degrees).toInt()
    }

    private fun cellKeyFromIndexes(latIndex: Int, lonIndex: Int, degrees: Double): String {
        val precision = when (degrees) {
            0.10 -> "010"
            0.50 -> "050"
            2.50 -> "250"
            else -> error("Unsupported discovery cell size")
        }
        return "$precision:$latIndex:$lonIndex"
    }
}
