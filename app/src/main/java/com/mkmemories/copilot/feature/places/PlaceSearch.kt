package com.mkmemories.copilot.feature.places

import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

/**
 * Autocomplétion mondiale de lieux — hôtels, adresses, plages, îles, villes —
 * via Photon (komoot), moteur de recherche libre bâti sur OpenStreetMap :
 * gratuit, sans clé API, conçu pour la saisie au fil de l'eau.
 */
data class PlaceSuggestion(
    val name: String,
    /** Ligne de détail lisible : "Rue, Ville, Pays" ou "Fira, Grèce". */
    val detail: String,
    /** Ville / île / localité — mémorisée sur l'étape pour enrichir le PDF. */
    val locality: String?,
    val latitude: Double,
    val longitude: Double,
    /** Catégorie lisible ("Hôtel", "Plage"…) quand OpenStreetMap la connaît. */
    val category: String?,
)

object PlaceSearch {

    suspend fun search(
        query: String,
        limit: Int = 6,
        baseUrl: String = "https://photon.komoot.io",
    ): List<PlaceSuggestion> = withContext(Dispatchers.IO) {
        val encoded = URLEncoder.encode(query, "UTF-8")
        val connection =
            URL("$baseUrl/api/?q=$encoded&limit=$limit&lang=fr").openConnection() as HttpURLConnection
        try {
            connection.connectTimeout = 8_000
            connection.readTimeout = 8_000
            connection.setRequestProperty("User-Agent", "MKCopilot/0.1 (app gratuite road trip)")
            parse(connection.inputStream.bufferedReader().readText())
        } finally {
            connection.disconnect()
        }
    }

    internal fun parse(body: String): List<PlaceSuggestion> {
        val features = JSONObject(body).optJSONArray("features") ?: return emptyList()
        val results = mutableListOf<PlaceSuggestion>()
        for (i in 0 until features.length()) {
            val feature = features.getJSONObject(i)
            val props = feature.getJSONObject("properties")
            // GeoJSON : coordonnées dans l'ordre [longitude, latitude]
            val coords = feature.getJSONObject("geometry").getJSONArray("coordinates")

            val name = props.optString("name")
                .ifBlank { listOf(props.optString("street"), props.optString("housenumber")).filter { it.isNotBlank() }.joinToString(" ") }
            if (name.isBlank()) continue

            val street = listOf(props.optString("housenumber"), props.optString("street"))
                .filter { it.isNotBlank() }.joinToString(" ")
            val city = props.optString("city").ifBlank { props.optString("state") }
            val detail = listOf(street, props.optString("city"), props.optString("state"), props.optString("country"))
                .filter { it.isNotBlank() }
                .distinct()
                .joinToString(", ")

            results.add(
                PlaceSuggestion(
                    name = name,
                    detail = detail,
                    locality = city.takeIf { it.isNotBlank() },
                    latitude = coords.getDouble(1),
                    longitude = coords.getDouble(0),
                    category = CATEGORY_LABELS[props.optString("osm_value")],
                ),
            )
        }
        return results
    }

    // Catégories OpenStreetMap fréquentes en voyage, en français
    private val CATEGORY_LABELS = mapOf(
        "hotel" to "Hôtel",
        "guest_house" to "Maison d'hôtes",
        "hostel" to "Auberge",
        "apartment" to "Appartement",
        "camp_site" to "Camping",
        "restaurant" to "Restaurant",
        "cafe" to "Café",
        "attraction" to "Site à visiter",
        "museum" to "Musée",
        "viewpoint" to "Point de vue",
        "beach" to "Plage",
        "castle" to "Château",
        "island" to "Île",
        "city" to "Ville",
        "town" to "Ville",
        "village" to "Village",
        "airport" to "Aéroport",
        "ferry_terminal" to "Terminal ferry",
    )
}
