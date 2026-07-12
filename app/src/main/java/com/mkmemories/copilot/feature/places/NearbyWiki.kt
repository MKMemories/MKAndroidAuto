package com.mkmemories.copilot.feature.places

import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

/**
 * Sites remarquables autour d'un point — geosearch Wikipédia (gratuit).
 * Sert aux « détours qui valent le coup » et au guide du territoire.
 */
data class NearbySite(
    val title: String,
    val latitude: Double,
    val longitude: Double,
    val distanceMeters: Double,
)

object NearbyWiki {

    suspend fun around(
        latitude: Double,
        longitude: Double,
        radiusMeters: Int = 10_000,
        limit: Int = 5,
        baseUrl: String = "https://fr.wikipedia.org",
    ): List<NearbySite> = withContext(Dispatchers.IO) {
        try {
            val url = "$baseUrl/w/api.php?action=query&list=geosearch" +
                "&gscoord=$latitude%7C$longitude&gsradius=$radiusMeters&gslimit=$limit&format=json"
            val connection = URL(url).openConnection() as HttpURLConnection
            try {
                connection.connectTimeout = 8_000
                connection.readTimeout = 8_000
                connection.setRequestProperty("User-Agent", "MKCopilot/0.1 (app gratuite road trip)")
                if (connection.responseCode != 200) return@withContext emptyList()
                parse(connection.inputStream.bufferedReader().readText())
            } finally {
                connection.disconnect()
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    internal fun parse(body: String): List<NearbySite> {
        val results = JSONObject(body).optJSONObject("query")?.optJSONArray("geosearch")
            ?: return emptyList()
        val sites = mutableListOf<NearbySite>()
        for (i in 0 until results.length()) {
            val entry = results.getJSONObject(i)
            val title = entry.optString("title")
            if (title.isBlank()) continue
            sites.add(
                NearbySite(
                    title = title,
                    latitude = entry.getDouble("lat"),
                    longitude = entry.getDouble("lon"),
                    distanceMeters = entry.optDouble("dist", 0.0),
                ),
            )
        }
        return sites
    }
}
