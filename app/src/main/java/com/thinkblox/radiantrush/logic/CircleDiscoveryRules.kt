package com.thinkblox.radiantrush.logic

import com.thinkblox.radiantrush.data.ApproximateCircleLocation
import java.security.MessageDigest
import java.util.Locale
import kotlin.math.floor

/**
 * Privacy-preserving discovery helpers for Phase 13B.
 *
 * Exact phone coordinates remain local. Firestore receives only opaque hashes of
 * coarse geographic cells plus a short time bucket. The hashes are sufficient for
 * matching clients that compute the same nearby cells, but the discovery documents
 * never contain raw latitude/longitude, a geohash, city, province, or country code.
 */
object CircleDiscoveryRules {
    const val DISCOVERY_WINDOW_MILLIS = 2 * 60 * 1000L
    private const val WINDOW_BUCKET_MILLIS = 5 * 60 * 1000L

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

    fun presenceKeys(
        location: ApproximateCircleLocation,
        nowMillis: Long,
    ): PresenceKeys {
        val currentBucket = nowMillis / WINDOW_BUCKET_MILLIS
        val nextBucket = currentBucket + 1L
        val country = normalizedCountry(location.countryCode)
        val localCell = cellKey(location.latitude, location.longitude, 0.10)
        val regionalCell = cellKey(location.latitude, location.longitude, 0.50)
        val broadCell = cellKey(location.latitude, location.longitude, 2.50)

        fun twoWindows(prefix: String): List<String> = listOf(
            opaqueWindowKey(prefix, currentBucket),
            opaqueWindowKey(prefix, nextBucket),
        )

        return PresenceKeys(
            localWindowKeys = twoWindows("local:$localCell"),
            regionalWindowKeys = twoWindows("regional:$regionalCell"),
            broadWindowKeys = twoWindows("broad:$broadCell"),
            countryWindowKeys = twoWindows("country:$country"),
            globalWindowKeys = twoWindows("global"),
        )
    }

    fun queryKeys(
        tier: SearchTier,
        location: ApproximateCircleLocation,
        nowMillis: Long,
    ): List<String> {
        val currentBucket = nowMillis / WINDOW_BUCKET_MILLIS
        return when (tier) {
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
                    .map { cell -> opaqueWindowKey("$prefix:$cell", currentBucket) }
            }

            SearchTier.Country -> listOf(
                opaqueWindowKey("country:${normalizedCountry(location.countryCode)}", currentBucket),
            )
            SearchTier.Global -> listOf(opaqueWindowKey("global", currentBucket))
        }
    }

    fun pairId(uidA: String, uidB: String): String =
        listOf(uidA.trim(), uidB.trim())
            .sorted()
            .joinToString("__")

    private fun opaqueWindowKey(prefix: String, bucket: Long): String {
        val digest = MessageDigest.getInstance("SHA-256")
            .digest("radiant-circle-v1|$prefix|$bucket".toByteArray(Charsets.UTF_8))
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
