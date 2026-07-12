package com.mkmemories.copilot.feature.blackbox

import android.content.Context
import java.io.File
import org.json.JSONArray
import org.json.JSONObject

/**
 * Boîte noire locale : mémoire glissante des 60 dernières secondes de
 * conduite (position, vitesse, accélération). En cas de choc, l'instantané
 * est figé dans un fichier local horodaté — 100 % privé, exportable pour
 * l'assurance. Aucune donnée ne quitte le téléphone.
 */
class BlackBox(private val windowMillis: Long = WINDOW_MS) {

    data class Sample(
        val timeMillis: Long,
        val latitude: Double?,
        val longitude: Double?,
        val speedMs: Float?,
        val accelerationMs2: Float?,
    )

    private val samples = ArrayDeque<Sample>()

    @Synchronized
    fun record(sample: Sample) {
        samples.addLast(sample)
        while (samples.isNotEmpty() && sample.timeMillis - samples.first().timeMillis > windowMillis) {
            samples.removeFirst()
        }
    }

    @Synchronized
    fun snapshot(): List<Sample> = samples.toList()

    /** Sérialisation JSON lisible (assurance, expertise). */
    fun toJson(triggerMillis: Long, magnitudeMs2: Float): String {
        val array = JSONArray()
        snapshot().forEach { s ->
            array.put(
                JSONObject()
                    .put("t_ms", s.timeMillis)
                    .putOpt("lat", s.latitude)
                    .putOpt("lng", s.longitude)
                    .putOpt("vitesse_ms", s.speedMs)
                    .putOpt("acceleration_ms2", s.accelerationMs2),
            )
        }
        return JSONObject()
            .put("app", "MK Copilot — boîte noire locale")
            .put("choc_t_ms", triggerMillis)
            .put("choc_acceleration_ms2", magnitudeMs2)
            .put("fenetre_ms", windowMillis)
            .put("echantillons", array)
            .toString(2)
    }

    /** Fige l'incident dans un fichier local ; retourne le fichier. */
    fun dump(context: Context, triggerMillis: Long, magnitudeMs2: Float): File {
        val dir = File(context.filesDir, "blackbox").apply { mkdirs() }
        val file = File(dir, "incident-$triggerMillis.json")
        file.writeText(toJson(triggerMillis, magnitudeMs2))
        return file
    }

    companion object {
        const val WINDOW_MS = 60_000L

        /** Incidents enregistrés, du plus récent au plus ancien. */
        fun incidents(context: Context): List<File> =
            File(context.filesDir, "blackbox").listFiles()
                ?.sortedByDescending { it.name }
                ?.toList()
                .orEmpty()
    }
}
