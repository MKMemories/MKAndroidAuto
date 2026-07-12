package com.mkmemories.copilot.feature.fuel

import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

/**
 * Prix des carburants — open data officiel français (data.economie.gouv.fr,
 * flux instantané, gratuit et sans clé). Trouve la station la moins chère
 * autour d'une position pour un carburant donné.
 */
data class FuelStation(
    val name: String,
    val city: String,
    val priceEuro: Double,
    val fuel: String,
    val latitude: Double,
    val longitude: Double,
)

object FuelPrices {

    suspend fun cheapestNearby(
        latitude: Double,
        longitude: Double,
        fuel: String = "E10",
        radiusMeters: Int = 5_000,
        baseUrl: String = "https://data.economie.gouv.fr",
    ): FuelStation? = withContext(Dispatchers.IO) {
        try {
            val url = "$baseUrl/api/records/1.0/search/" +
                "?dataset=prix-des-carburants-en-france-flux-instantane-v2" +
                "&q=&rows=30&geofilter.distance=$latitude%2C$longitude%2C$radiusMeters" +
                "&refine.carburants_disponibles=$fuel"
            val connection = URL(url).openConnection() as HttpURLConnection
            try {
                connection.connectTimeout = 8_000
                connection.readTimeout = 8_000
                connection.setRequestProperty("User-Agent", "MKCopilot/0.1 (app gratuite)")
                if (connection.responseCode != 200) return@withContext null
                parse(connection.inputStream.bufferedReader().readText(), fuel)
            } finally {
                connection.disconnect()
            }
        } catch (e: Exception) {
            null
        }
    }

    /** La moins chère du lot pour le carburant demandé. */
    internal fun parse(body: String, fuel: String): FuelStation? {
        val records = JSONObject(body).optJSONArray("records") ?: return null
        var best: FuelStation? = null
        for (i in 0 until records.length()) {
            val fields = records.getJSONObject(i).optJSONObject("fields") ?: continue
            val price = priceForFuel(fields, fuel) ?: continue
            val geo = fields.optJSONArray("geom") ?: continue
            val station = FuelStation(
                name = fields.optString("nom", "Station"),
                city = fields.optString("ville", ""),
                priceEuro = price,
                fuel = fuel,
                latitude = geo.getDouble(0),
                longitude = geo.getDouble(1),
            )
            if (best == null || station.priceEuro < best.priceEuro) best = station
        }
        return best
    }

    /** Le prix du carburant demandé, où qu'il soit dans les champs du jeu. */
    private fun priceForFuel(fields: JSONObject, fuel: String): Double? {
        // Champ direct ("e10_prix") présent dans le flux v2
        fields.optDouble("${fuel.lowercase()}_prix", Double.NaN)
            .takeIf { !it.isNaN() }?.let { return it }
        // Repli : liste JSON "prix" [{"@nom":"E10","@valeur":"1.68"}]
        val raw = fields.optString("prix")
        if (raw.isBlank()) return null
        return try {
            val array = org.json.JSONArray(raw)
            for (i in 0 until array.length()) {
                val entry = array.getJSONObject(i)
                if (entry.optString("@nom").equals(fuel, ignoreCase = true)) {
                    return entry.optString("@valeur").toDoubleOrNull()
                }
            }
            null
        } catch (e: Exception) {
            null
        }
    }

    /** Phrase d'annonce vocale. */
    fun announcement(station: FuelStation): String {
        val price = String.format(java.util.Locale.FRENCH, "%.2f", station.priceEuro)
        val where = station.city.ifBlank { station.name }
        return "${station.fuel} à $price euros le litre chez ${station.name}" +
            (if (station.city.isNotBlank()) ", à $where" else "") + "."
    }
}
