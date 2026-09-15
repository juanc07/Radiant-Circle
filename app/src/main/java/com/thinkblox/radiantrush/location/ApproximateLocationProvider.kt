package com.thinkblox.radiantrush.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.CancellationSignal
import android.os.Looper
import androidx.core.content.ContextCompat
import com.thinkblox.radiantrush.data.ApproximateCircleLocation
import java.util.Locale

/**
 * Foreground-only approximate location for social discovery.
 *
 * Radiant Circle requests ACCESS_COARSE_LOCATION only. Coordinates returned by
 * Android are used transiently to derive coarse discovery cells and are never
 * written directly to Firestore by this provider.
 */
class ApproximateLocationProvider(
    private val context: Context,
) {
    sealed interface Result {
        data class Success(val location: ApproximateCircleLocation) : Result
        data class Failure(val message: String) : Result
    }

    fun hasPermission(): Boolean =
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED

    @SuppressLint("MissingPermission")
    fun requestCurrentLocation(onResult: (Result) -> Unit) {
        if (!hasPermission()) {
            onResult(Result.Failure("Approximate location permission is needed for Circle discovery."))
            return
        }

        val manager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
        if (manager == null) {
            onResult(Result.Failure("Location is unavailable on this device."))
            return
        }

        val provider = preferredProvider(manager)
        if (provider == null) {
            onResult(Result.Failure("Turn on location services to discover the Circle."))
            return
        }

        val lastKnown = runCatching { manager.getLastKnownLocation(provider) }.getOrNull()
        if (lastKnown != null && System.currentTimeMillis() - lastKnown.time <= MAX_LAST_KNOWN_AGE_MILLIS) {
            onResult(Result.Success(lastKnown.toCircleLocation()))
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            manager.getCurrentLocation(
                provider,
                CancellationSignal(),
                ContextCompat.getMainExecutor(context),
            ) { location ->
                if (location != null) {
                    onResult(Result.Success(location.toCircleLocation()))
                } else if (lastKnown != null) {
                    onResult(Result.Success(lastKnown.toCircleLocation()))
                } else {
                    onResult(Result.Failure("Couldn't get your approximate location. Try again."))
                }
            }
        } else {
            @Suppress("DEPRECATION")
            manager.requestSingleUpdate(
                provider,
                object : LocationListener {
                    override fun onLocationChanged(location: Location) {
                        onResult(Result.Success(location.toCircleLocation()))
                    }

                    @Deprecated("Deprecated in Android")
                    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) = Unit

                    override fun onProviderEnabled(provider: String) = Unit
                    override fun onProviderDisabled(provider: String) = Unit
                },
                Looper.getMainLooper(),
            )
        }
    }

    private fun preferredProvider(manager: LocationManager): String? {
        val enabled = runCatching { manager.getProviders(true) }.getOrDefault(emptyList())
        return when {
            LocationManager.NETWORK_PROVIDER in enabled -> LocationManager.NETWORK_PROVIDER
            LocationManager.GPS_PROVIDER in enabled -> LocationManager.GPS_PROVIDER
            LocationManager.PASSIVE_PROVIDER in enabled -> LocationManager.PASSIVE_PROVIDER
            else -> null
        }
    }

    private fun Location.toCircleLocation(): ApproximateCircleLocation = ApproximateCircleLocation(
        latitude = latitude,
        longitude = longitude,
        countryCode = Locale.getDefault().country.ifBlank { "ZZ" },
    )

    private companion object {
        const val MAX_LAST_KNOWN_AGE_MILLIS = 10 * 60 * 1000L
    }
}
