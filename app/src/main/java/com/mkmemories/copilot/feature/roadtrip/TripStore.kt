package com.mkmemories.copilot.feature.roadtrip

import android.content.Context
import androidx.core.content.edit
import java.time.LocalDate
import java.time.LocalTime
import org.json.JSONArray
import org.json.JSONObject

/**
 * Persistance locale du voyage planifié (zéro serveur) : le voyage créé dans
 * le planificateur survit au redémarrage et remplace le voyage de démonstration
 * sur le téléphone comme sur l'écran voiture.
 */
class TripStore(context: Context) {

    private val prefs = context.getSharedPreferences("trip_store", Context.MODE_PRIVATE)

    fun save(trip: Trip) {
        prefs.edit { putString(KEY_TRIP, toJson(trip)) }
    }

    fun load(): Trip? = prefs.getString(KEY_TRIP, null)?.let { raw ->
        try {
            fromJson(raw)
        } catch (e: Exception) {
            null // donnée corrompue : on retombe proprement sur la démo
        }
    }

    fun clear() {
        prefs.edit { remove(KEY_TRIP) }
    }

    companion object {
        private const val KEY_TRIP = "trip"

        internal fun toJson(trip: Trip): String {
            val days = JSONArray()
            trip.days.forEach { day ->
                val stops = JSONArray()
                day.stops.forEach { stop ->
                    stops.put(
                        JSONObject()
                            .put("name", stop.name)
                            .put("lat", stop.latitude)
                            .put("lng", stop.longitude)
                            .put("visited", stop.visited)
                            .putOpt("locality", stop.locality)
                            .putOpt("time", stop.time?.toString()),
                    )
                }
                days.put(JSONObject().put("date", day.date.toString()).put("stops", stops))
            }
            return JSONObject().put("name", trip.name).put("days", days).toString()
        }

        internal fun fromJson(raw: String): Trip {
            val root = JSONObject(raw)
            val days = mutableListOf<TripDay>()
            val daysJson = root.getJSONArray("days")
            for (d in 0 until daysJson.length()) {
                val dayJson = daysJson.getJSONObject(d)
                val stops = mutableListOf<TripStop>()
                val stopsJson = dayJson.getJSONArray("stops")
                for (s in 0 until stopsJson.length()) {
                    val stopJson = stopsJson.getJSONObject(s)
                    stops.add(
                        TripStop(
                            name = stopJson.getString("name"),
                            latitude = stopJson.getDouble("lat"),
                            longitude = stopJson.getDouble("lng"),
                            visited = stopJson.optBoolean("visited", false),
                            locality = stopJson.optString("locality").takeIf { it.isNotBlank() },
                            time = stopJson.optString("time").takeIf { it.isNotBlank() }
                                ?.let { LocalTime.parse(it) },
                        ),
                    )
                }
                days.add(TripDay(LocalDate.parse(dayJson.getString("date")), stops))
            }
            return Trip(name = root.getString("name"), days = days.sortedBy { it.date })
        }
    }
}
