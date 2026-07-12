package com.mkmemories.copilot.feature.guardian

import android.content.Context
import androidx.core.content.edit

/**
 * Pack Ange gardien — mémoire de stationnement intelligente.
 *
 * À la déconnexion d'Android Auto / du Bluetooth voiture, la position est
 * enregistrée automatiquement : "où est ma voiture ?" ne se pose plus.
 * TODO v1.1 : photo + étage, minuteur de stationnement payant / zone bleue
 * avec rappel avant expiration.
 */
class ParkingMemory(context: Context) {

    private val prefs = context.getSharedPreferences("parking_memory", Context.MODE_PRIVATE)

    fun saveParkingSpot(latitude: Double, longitude: Double, timestampMillis: Long) {
        prefs.edit {
            putLong(KEY_LAT, latitude.toRawBits())
            putLong(KEY_LNG, longitude.toRawBits())
            putLong(KEY_TIME, timestampMillis)
        }
    }

    fun lastParkingSpot(): ParkingSpot? {
        if (!prefs.contains(KEY_LAT)) return null
        return ParkingSpot(
            latitude = Double.fromBits(prefs.getLong(KEY_LAT, 0L)),
            longitude = Double.fromBits(prefs.getLong(KEY_LNG, 0L)),
            timestampMillis = prefs.getLong(KEY_TIME, 0L),
        )
    }

    /** Note libre (étage, place, repère visuel…). */
    var note: String?
        get() = prefs.getString(KEY_NOTE, null)?.takeIf { it.isNotBlank() }
        set(value) = prefs.edit { putString(KEY_NOTE, value?.trim()) }

    /** Oublie la place enregistrée (et sa note). */
    fun clear() {
        prefs.edit {
            remove(KEY_LAT)
            remove(KEY_LNG)
            remove(KEY_TIME)
            remove(KEY_NOTE)
        }
    }

    private companion object {
        const val KEY_LAT = "lat"
        const val KEY_LNG = "lng"
        const val KEY_TIME = "time"
        const val KEY_NOTE = "note"
    }
}

data class ParkingSpot(
    val latitude: Double,
    val longitude: Double,
    val timestampMillis: Long,
)
