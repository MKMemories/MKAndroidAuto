package com.mkmemories.copilot.feature.briefing

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import kotlin.math.roundToInt

/**
 * Briefing météo du jour — données Open-Meteo (gratuit, sans clé API).
 *
 * Produit un texte en français prêt à être lu par la synthèse vocale dans la
 * voiture ([BriefingPlayer]) : température actuelle, min/max du jour, ciel,
 * précipitations et vent.
 */
object WeatherBriefingGenerator {

    suspend fun generate(latitude: Double, longitude: Double): String =
        withContext(Dispatchers.IO) {
            val url = URL(
                "https://api.open-meteo.com/v1/forecast" +
                    "?latitude=$latitude&longitude=$longitude" +
                    "&current=temperature_2m,weather_code,wind_speed_10m" +
                    "&daily=temperature_2m_min,temperature_2m_max,precipitation_probability_max" +
                    "&timezone=auto&forecast_days=1",
            )
            val connection = url.openConnection() as HttpURLConnection
            try {
                connection.connectTimeout = 10_000
                connection.readTimeout = 10_000
                val json = JSONObject(connection.inputStream.bufferedReader().readText())
                buildBriefing(json)
            } finally {
                connection.disconnect()
            }
        }

    private fun buildBriefing(json: JSONObject): String {
        val current = json.getJSONObject("current")
        val daily = json.getJSONObject("daily")

        val temperature = current.getDouble("temperature_2m").roundToInt()
        val wind = current.getDouble("wind_speed_10m").roundToInt()
        val sky = skyLabel(current.getInt("weather_code"))
        val tempMin = daily.getJSONArray("temperature_2m_min").getDouble(0).roundToInt()
        val tempMax = daily.getJSONArray("temperature_2m_max").getDouble(0).roundToInt()
        val rainProbability = daily.getJSONArray("precipitation_probability_max").optInt(0)

        return buildString {
            append("Bonjour ! Météo du jour : $sky, $temperature degrés actuellement, ")
            append("entre $tempMin et $tempMax degrés dans la journée. ")
            if (rainProbability >= 40) append("Risque de pluie : $rainProbability pour cent, pensez à adapter votre conduite. ")
            if (wind >= 50) append("Attention, vent fort : $wind kilomètres heure. ")
            append("Bonne route !")
        }
    }

    /** Codes météo WMO utilisés par Open-Meteo, en français. */
    private fun skyLabel(code: Int): String = when (code) {
        0 -> "ciel dégagé"
        1, 2 -> "légèrement nuageux"
        3 -> "ciel couvert"
        45, 48 -> "brouillard"
        in 51..57 -> "bruine"
        in 61..67 -> "pluie"
        in 71..77 -> "neige"
        in 80..82 -> "averses"
        in 85..86 -> "averses de neige"
        in 95..99 -> "orages"
        else -> "conditions variables"
    }
}
