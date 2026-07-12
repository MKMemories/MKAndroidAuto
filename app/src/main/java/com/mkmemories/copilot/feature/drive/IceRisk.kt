package com.mkmemories.copilot.feature.drive

/**
 * Risque verglas / brouillard — décision pure à partir de la météo du départ.
 * Prudent sans être alarmiste : ne parle que si les conditions s'y prêtent.
 */
object IceRisk {

    /**
     * @param temperatureC température actuelle
     * @param weatherCode code WMO Open-Meteo (45/48 = brouillard)
     * @param hourOfDay heure locale
     */
    fun warning(temperatureC: Double, weatherCode: Int, hourOfDay: Int): String? {
        val fog = weatherCode == 45 || weatherCode == 48
        val freezing = temperatureC <= 2.0
        val coldHours = hourOfDay in 0..9 || hourOfDay >= 20

        return when {
            freezing && fog ->
                "Attention : brouillard et température proche de zéro — risque de verglas et " +
                    "visibilité réduite. Allongez vos distances."
            freezing && coldHours ->
                "Température proche de zéro : du verglas est possible, surtout sur les ponts " +
                    "et les zones ombragées. Prudence."
            fog ->
                "Brouillard signalé : feux adaptés et distances allongées."
            else -> null
        }
    }
}
