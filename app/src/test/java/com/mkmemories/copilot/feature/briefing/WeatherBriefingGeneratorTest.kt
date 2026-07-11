package com.mkmemories.copilot.feature.briefing

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Construction du texte de briefing à partir de données Open-Meteo maîtrisées. */
class WeatherBriefingGeneratorTest {

    private fun payload(
        temperature: Double = 21.4,
        weatherCode: Int = 0,
        wind: Double = 12.0,
        tempMin: Double = 14.6,
        tempMax: Double = 25.3,
        rainProbability: Int = 10,
    ): JSONObject = JSONObject(
        """
        {
          "current": {
            "temperature_2m": $temperature,
            "weather_code": $weatherCode,
            "wind_speed_10m": $wind
          },
          "daily": {
            "temperature_2m_min": [$tempMin],
            "temperature_2m_max": [$tempMax],
            "precipitation_probability_max": [$rainProbability]
          }
        }
        """.trimIndent(),
    )

    @Test
    fun `briefing par beau temps calme - ni alerte pluie ni alerte vent`() {
        val text = WeatherBriefingGenerator.buildBriefing(payload())
        assertEquals(
            "Bonjour ! Météo du jour : ciel dégagé, 21 degrés actuellement, " +
                "entre 15 et 25 degrés dans la journée. Bonne route !",
            text,
        )
        assertFalse(text.contains("pluie"))
        assertFalse(text.contains("vent"))
    }

    @Test
    fun `alerte pluie a partir de 40 pour cent de probabilite`() {
        assertFalse(WeatherBriefingGenerator.buildBriefing(payload(rainProbability = 39)).contains("pluie"))
        val text = WeatherBriefingGenerator.buildBriefing(payload(rainProbability = 40))
        assertTrue(text.contains("Risque de pluie : 40 pour cent"))
    }

    @Test
    fun `alerte vent fort a partir de 50 kmh`() {
        assertFalse(WeatherBriefingGenerator.buildBriefing(payload(wind = 49.4)).contains("vent fort"))
        val text = WeatherBriefingGenerator.buildBriefing(payload(wind = 62.0))
        assertTrue(text.contains("vent fort : 62 kilomètres heure"))
    }

    @Test
    fun `les temperatures sont arrondies a l'entier le plus proche`() {
        val text = WeatherBriefingGenerator.buildBriefing(payload(temperature = 19.6))
        assertTrue(text.contains("20 degrés actuellement"))
    }

    @Test
    fun `tous les codes meteo WMO utiles ont un libelle francais`() {
        val expected = mapOf(
            0 to "ciel dégagé", 1 to "légèrement nuageux", 2 to "légèrement nuageux",
            3 to "ciel couvert", 45 to "brouillard", 48 to "brouillard",
            51 to "bruine", 57 to "bruine", 61 to "pluie", 67 to "pluie",
            71 to "neige", 77 to "neige", 80 to "averses", 82 to "averses",
            85 to "averses de neige", 86 to "averses de neige",
            95 to "orages", 99 to "orages",
        )
        expected.forEach { (code, label) ->
            assertEquals("code $code", label, WeatherBriefingGenerator.skyLabel(code))
        }
        // Un code inconnu ne doit jamais faire planter le briefing
        assertEquals("conditions variables", WeatherBriefingGenerator.skyLabel(42))
    }
}
