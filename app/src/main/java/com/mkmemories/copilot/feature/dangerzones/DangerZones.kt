package com.mkmemories.copilot.feature.dangerzones

import android.content.Context
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

/**
 * Zones de danger — implémentation conforme à l'article R413-15 du Code de la
 * route : on ne signale JAMAIS la position exacte d'un radar, uniquement un
 * segment de route (300 m en ville, 2 km sur route, 4 km sur autoroute) dans
 * lequel un danger peut se trouver.
 *
 * Source de données : open data officiel des radars fixes (data.gouv.fr),
 * transformé en zones conformes. Fonction activée uniquement dans les pays où
 * elle est légale (géofencing pays — interdite p. ex. en Suisse et Allemagne).
 */
data class DangerZone(
    val centerLatitude: Double,
    val centerLongitude: Double,
    val roadType: RoadType,
    val speedLimitKmh: Int?,
) {
    val radiusMeters: Int get() = roadType.zoneLengthMeters / 2
}

enum class RoadType(val zoneLengthMeters: Int, val label: String) {
    URBAN(300, "en ville"),
    ROAD(2_000, "sur route"),
    HIGHWAY(4_000, "sur autoroute"),
}

/**
 * Open data officiel des radars fixes (data.gouv.fr), converti en zones de
 * danger conformes et mis en cache hors ligne (30 jours). L'URL du CSV est
 * résolue dynamiquement via l'API du jeu de données — pas d'UUID en dur.
 */
object RadarOpenDataRepository {

    private const val CACHE_FILE = "radars.csv"
    private const val MAX_AGE_MS = 30L * 24 * 3600 * 1000
    internal const val DATASET_PATH = "/api/1/datasets/radars-automatiques/"

    /** Rafraîchit le cache s'il est absent ou trop vieux. Meilleur effort. */
    suspend fun refreshIfStale(
        context: Context,
        baseUrl: String = "https://www.data.gouv.fr",
    ): Boolean = withContext(Dispatchers.IO) {
        val cache = File(context.filesDir, CACHE_FILE)
        if (cache.exists() && System.currentTimeMillis() - cache.lastModified() < MAX_AGE_MS) {
            return@withContext false
        }
        try {
            val datasetJson = httpGet("$baseUrl$DATASET_PATH") ?: return@withContext false
            val csvUrl = csvUrlFromDatasetJson(datasetJson) ?: return@withContext false
            val csv = httpGet(csvUrl) ?: return@withContext false
            if (parseCsv(csv).isEmpty()) return@withContext false // données invalides : on garde l'ancien cache
            File(context.filesDir, "$CACHE_FILE.tmp").apply {
                writeText(csv)
                if (!renameTo(cache)) delete()
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    /** Zones du cache hors ligne — liste vide si jamais rafraîchi. */
    fun cachedZones(context: Context): List<DangerZone> {
        val cache = File(context.filesDir, CACHE_FILE)
        if (!cache.exists()) return emptyList()
        return try {
            parseCsv(cache.readText())
        } catch (e: Exception) {
            emptyList()
        }
    }

    /** Première ressource CSV du jeu de données. */
    internal fun csvUrlFromDatasetJson(json: String): String? {
        val resources = JSONObject(json).optJSONArray("resources") ?: return null
        for (i in 0 until resources.length()) {
            val resource = resources.getJSONObject(i)
            val format = resource.optString("format").lowercase()
            val url = resource.optString("url")
            if (format == "csv" && url.isNotBlank()) return url
        }
        return null
    }

    /**
     * Parseur tolérant : colonnes repérées par leur en-tête (latitude,
     * longitude, vitesse), séparateur `;` ou `,`, lignes invalides ignorées.
     */
    internal fun parseCsv(text: String): List<DangerZone> {
        val lines = text.lineSequence().filter { it.isNotBlank() }.toList()
        if (lines.size < 2) return emptyList()

        val separator = if (lines.first().count { it == ';' } >= lines.first().count { it == ',' }) ';' else ','
        val headers = lines.first().split(separator).map { it.trim().lowercase() }
        val latIndex = headers.indexOfFirst { it.contains("latitude") }
        val lngIndex = headers.indexOfFirst { it.contains("longitude") }
        val speedIndex = headers.indexOfFirst { it.contains("vitesse") }
        if (latIndex < 0 || lngIndex < 0) return emptyList()

        return lines.drop(1).mapNotNull { line ->
            val cells = line.split(separator)
            val latitude = cells.getOrNull(latIndex)?.trim()?.replace(',', '.')?.toDoubleOrNull()
                ?: return@mapNotNull null
            val longitude = cells.getOrNull(lngIndex)?.trim()?.replace(',', '.')?.toDoubleOrNull()
                ?: return@mapNotNull null
            if (latitude == 0.0 && longitude == 0.0) return@mapNotNull null
            val speed = if (speedIndex >= 0) {
                cells.getOrNull(speedIndex)?.trim()?.toDoubleOrNull()?.toInt()
            } else {
                null
            }
            DangerZone(latitude, longitude, roadTypeForSpeed(speed), speed)
        }
    }

    /** Vitesses françaises : 110/130 autoroute, 80/90/100 route, sinon ville. */
    internal fun roadTypeForSpeed(speedKmh: Int?): RoadType = when {
        speedKmh == null -> RoadType.ROAD // prudence : zone moyenne
        speedKmh >= 110 -> RoadType.HIGHWAY
        speedKmh >= 80 -> RoadType.ROAD
        else -> RoadType.URBAN
    }

    private fun httpGet(url: String): String? {
        val connection = URL(url).openConnection() as HttpURLConnection
        return try {
            connection.connectTimeout = 15_000
            connection.readTimeout = 30_000
            connection.instanceFollowRedirects = true
            connection.setRequestProperty("User-Agent", "MKCopilot/0.1 (app gratuite road trip)")
            if (connection.responseCode != 200) null
            else connection.inputStream.bufferedReader().readText()
        } finally {
            connection.disconnect()
        }
    }
}

/** Moteur d'alerte : à brancher sur le flux de localisation pendant la conduite. */
class ZoneAlertEngine(private val zones: () -> List<DangerZone>) {

    private var lastAlerted: DangerZone? = null

    /** Retourne le texte d'alerte TTS si on entre dans une nouvelle zone, sinon null. */
    fun onLocation(latitude: Double, longitude: Double): String? {
        val zone = zones().firstOrNull {
            distanceMeters(latitude, longitude, it.centerLatitude, it.centerLongitude) <= it.radiusMeters
        }
        if (zone == null || zone == lastAlerted) {
            if (zone == null) lastAlerted = null
            return null
        }
        lastAlerted = zone
        val limit = zone.speedLimitKmh?.let { ", limite à $it kilomètres heure" } ?: ""
        return "Zone de danger ${zone.roadType.label}$limit. Prudence."
    }

    companion object {
        /** Distance haversine en mètres. */
        fun distanceMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
            val earthRadius = 6_371_000.0
            val dLat = Math.toRadians(lat2 - lat1)
            val dLon = Math.toRadians(lon2 - lon1)
            val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2) * sin(dLon / 2)
            return earthRadius * 2 * atan2(sqrt(a), sqrt(1 - a))
        }
    }
}
