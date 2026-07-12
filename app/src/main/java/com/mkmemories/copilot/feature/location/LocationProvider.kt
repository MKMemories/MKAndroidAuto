package com.mkmemories.copilot.feature.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Build
import androidx.core.content.ContextCompat
import java.util.concurrent.Executors
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Position réelle via le LocationManager du système — aucune dépendance
 * Google Play Services, fidèle au « zéro dépendance payante » du projet.
 * Toujours en meilleur effort : jamais d'exception, `null` si indisponible.
 */
object LocationProvider {

    /** Position par défaut (Paris) quand aucune position n'est disponible. */
    const val FALLBACK_LATITUDE = 48.8566
    const val FALLBACK_LONGITUDE = 2.3522

    fun hasPermission(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED

    /** Dernière position connue du système, tous fournisseurs confondus. */
    @SuppressLint("MissingPermission")
    fun lastKnown(context: Context): Location? {
        if (!hasPermission(context)) return null
        val manager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return null
        return manager.allProviders
            .mapNotNull { runCatching { manager.getLastKnownLocation(it) }.getOrNull() }
            .maxByOrNull { it.time }
    }

    /**
     * Position fraîche (fix GPS/réseau), avec délai maximal — sinon repli sur
     * la dernière connue. Sur Android 11+ utilise getCurrentLocation.
     */
    @SuppressLint("MissingPermission")
    suspend fun current(context: Context, timeoutMillis: Long = 6_000): Location? {
        if (!hasPermission(context)) return null
        val manager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            ?: return null
        val provider = when {
            manager.isProviderEnabled(LocationManager.GPS_PROVIDER) -> LocationManager.GPS_PROVIDER
            manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) -> LocationManager.NETWORK_PROVIDER
            else -> return lastKnown(context)
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return lastKnown(context)

        val fresh = withTimeoutOrNull(timeoutMillis) {
            suspendCancellableCoroutine { continuation ->
                val signal = android.os.CancellationSignal()
                continuation.invokeOnCancellation { signal.cancel() }
                runCatching {
                    manager.getCurrentLocation(provider, signal, Executors.newSingleThreadExecutor()) {
                        if (continuation.isActive) continuation.resume(it)
                    }
                }.onFailure { if (continuation.isActive) continuation.resume(null) }
            }
        }
        return fresh ?: lastKnown(context)
    }

    /** (latitude, longitude) utilisables immédiatement, avec repli Paris. */
    fun coordinatesOrFallback(context: Context): Pair<Double, Double> {
        val location = lastKnown(context)
        return if (location != null) location.latitude to location.longitude
        else FALLBACK_LATITUDE to FALLBACK_LONGITUDE
    }
}
