package com.mkmemories.copilot.feature.journal

import android.content.Context
import androidx.core.content.edit
import org.json.JSONArray
import org.json.JSONObject

/**
 * Journal de bord — chaque trajet du mode conduite enregistré localement :
 * horodatage, distance, durée. Zéro cloud, exportable.
 */
data class TripLogEntry(
    val startMillis: Long,
    val endMillis: Long,
    val distanceKm: Double,
) {
    val durationMinutes: Long get() = (endMillis - startMillis) / 60_000
}

class TripJournal(context: Context) {

    private val prefs = context.getSharedPreferences("trip_journal", Context.MODE_PRIVATE)

    fun record(entry: TripLogEntry) {
        if (entry.distanceKm < MIN_DISTANCE_KM) return // déplacer la voiture de 3 m n'est pas un trajet
        val entries = (all() + entry).takeLast(MAX_ENTRIES)
        prefs.edit { putString(KEY, toJson(entries)) }
    }

    fun all(): List<TripLogEntry> {
        val raw = prefs.getString(KEY, null) ?: return emptyList()
        return try {
            fromJson(raw)
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun totalKm(): Double = all().sumOf { it.distanceKm }

    companion object {
        private const val KEY = "entries"
        private const val MAX_ENTRIES = 500
        const val MIN_DISTANCE_KM = 0.3

        internal fun toJson(entries: List<TripLogEntry>): String {
            val array = JSONArray()
            entries.forEach {
                array.put(
                    JSONObject()
                        .put("start", it.startMillis)
                        .put("end", it.endMillis)
                        .put("km", it.distanceKm),
                )
            }
            return array.toString()
        }

        internal fun fromJson(raw: String): List<TripLogEntry> {
            val array = JSONArray(raw)
            return (0 until array.length()).map { i ->
                val entry = array.getJSONObject(i)
                TripLogEntry(entry.getLong("start"), entry.getLong("end"), entry.getDouble("km"))
            }
        }
    }
}
