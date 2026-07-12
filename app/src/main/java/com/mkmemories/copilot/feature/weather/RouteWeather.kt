package com.mkmemories.copilot.feature.weather

import com.mkmemories.copilot.feature.roadtrip.TripStop
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

/**
 * Météo d'itinéraire : au départ, regarde le ciel au-dessus de chaque étape
 * restante du jour et prévient de la première dégradation à venir.
 * Open-Meteo, gratuit, une requête légère par étape (max 5).
 */
object RouteWeather {

    /** Codes WMO considérés comme dégradés pour la conduite. */
    internal fun isBadWeather(code: Int): Boolean =
        code in 51..67 || code in 71..77 || code in 80..86 || code in 95..99 || code == 45 || code == 48

    internal fun label(code: Int): String = when (code) {
        45, 48 -> "du brouillard"
        in 51..57 -> "de la bruine"
        in 61..67 -> "de la pluie"
        in 71..77 -> "de la neige"
        in 80..82 -> "des averses"
        in 85..86 -> "des averses de neige"
        in 95..99 -> "des orages"
        else -> "un temps dégradé"
    }

    /**
     * Première étape restante sous mauvais temps → phrase d'annonce, sinon null.
     * Meilleur effort : toute erreur réseau rend null.
     */
    suspend fun alertForRoute(
        stops: List<TripStop>,
        baseUrl: String = "https://api.open-meteo.com",
    ): String? = withContext(Dispatchers.IO) {
        stops.filter { !it.visited }.take(MAX_POINTS).forEach { stop ->
            val code = try {
                weatherCodeAt(stop.latitude, stop.longitude, baseUrl)
            } catch (e: Exception) {
                null
            }
            if (code != null && isBadWeather(code)) {
                return@withContext "Plus loin sur votre route, ${label(code)} vous attend vers ${stop.name}. " +
                    "Adaptez votre allure."
            }
        }
        null
    }

    private fun weatherCodeAt(latitude: Double, longitude: Double, baseUrl: String): Int {
        val connection = URL(
            "$baseUrl/v1/forecast?latitude=$latitude&longitude=$longitude&current=weather_code&timezone=auto",
        ).openConnection() as HttpURLConnection
        return try {
            connection.connectTimeout = 6_000
            connection.readTimeout = 6_000
            JSONObject(connection.inputStream.bufferedReader().readText())
                .getJSONObject("current")
                .getInt("weather_code")
        } finally {
            connection.disconnect()
        }
    }

    private const val MAX_POINTS = 5
}
