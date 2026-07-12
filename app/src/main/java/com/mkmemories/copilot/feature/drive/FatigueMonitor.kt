package com.mkmemories.copilot.feature.drive

/**
 * Alerte fatigue — logique pure. Première suggestion de pause après 2 h de
 * conduite, puis rappel chaque heure. La nuit (0 h–6 h), le premier seuil
 * descend à 1 h 30 et le message insiste.
 */
class FatigueMonitor {

    private var alertedCount = 0

    /** À appeler régulièrement ; retourne le message vocal ou null. */
    fun check(drivingMillis: Long, hourOfDay: Int): String? {
        val night = hourOfDay in 0..5
        val firstThreshold = if (night) NIGHT_FIRST_MS else FIRST_MS
        val due = firstThreshold + alertedCount * REPEAT_MS
        if (drivingMillis < due) return null
        alertedCount++

        val hours = drivingMillis / 3_600_000
        val minutes = (drivingMillis % 3_600_000) / 60_000
        val duration = if (hours > 0) "$hours heure${if (hours > 1) "s" else ""}" +
            (if (minutes >= 5) " ${minutes} minutes" else "") else "$minutes minutes"

        return if (night) {
            "Vous conduisez de nuit depuis $duration. La somnolence est traître à cette heure : " +
                "une pause s'impose dès que possible."
        } else {
            "Vous conduisez depuis $duration. Une pause de quelques minutes ferait du bien — " +
                "pensez à la prochaine aire."
        }
    }

    companion object {
        const val FIRST_MS = 2 * 3_600_000L
        const val NIGHT_FIRST_MS = 90 * 60_000L
        const val REPEAT_MS = 3_600_000L
    }
}
