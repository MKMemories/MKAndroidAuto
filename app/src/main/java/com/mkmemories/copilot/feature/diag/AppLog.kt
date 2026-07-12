package com.mkmemories.copilot.feature.diag

import android.content.Context
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Journal persistant de diagnostic : chaque étape et chaque crash sont écrits
 * dans un fichier local, conservés d'une exécution à l'autre, et exportables
 * depuis les Réglages. Objectif : capturer les VRAIS logs (y compris les
 * exceptions non rattrapées) pour diagnostiquer, notamment sur Android Auto
 * où on n'a pas accès à Logcat. Meilleur effort absolu : n'échoue jamais.
 */
object AppLog {

    private const val MAX_BYTES = 512L * 1024 // 512 Ko : largement assez, borne la taille
    private val formatter = SimpleDateFormat("MM-dd HH:mm:ss.SSS", Locale.US)

    @Volatile
    private var logFile: File? = null

    /** À appeler au démarrage du process. Installe aussi le capteur de crash global. */
    fun install(context: Context, buildNumber: Int) {
        val dir = File(context.filesDir, "logs").apply { mkdirs() }
        val file = File(dir, "mkcopilot.log")
        logFile = file

        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            error("CRASH", "Exception non rattrapée sur le thread « ${thread.name} »", throwable)
            previous?.uncaughtException(thread, throwable)
        }
        i("app", "──────── nouvelle exécution (build $buildNumber) ────────")
    }

    fun i(tag: String, message: String) = write("I", tag, message, null)

    fun error(tag: String, message: String, throwable: Throwable? = null) =
        write("E", tag, message, throwable)

    @Synchronized
    private fun write(level: String, tag: String, message: String, throwable: Throwable?) {
        val file = logFile ?: return
        try {
            val line = buildString {
                append(formatter.format(Date()))
                append(' ').append(level).append('/').append(tag).append(": ").append(message).append('\n')
                if (throwable != null) append(stackTrace(throwable)).append('\n')
            }
            file.appendText(line)
            if (file.length() > MAX_BYTES) rotate(file)
        } catch (e: Exception) {
            // Un journal ne doit jamais faire tomber l'app.
        }
    }

    /** Garde la moitié la plus récente quand le fichier dépasse la limite. */
    private fun rotate(file: File) {
        try {
            val kept = file.readText().takeLast((MAX_BYTES / 2).toInt())
            file.writeText("… (début du journal tronqué)\n$kept")
        } catch (e: Exception) {
        }
    }

    internal fun stackTrace(throwable: Throwable): String {
        val writer = StringWriter()
        throwable.printStackTrace(PrintWriter(writer))
        return writer.toString()
    }

    fun file(): File? = logFile

    fun hasContent(): Boolean = logFile?.let { it.exists() && it.length() > 0 } == true

    fun clear() {
        logFile?.let { runCatching { it.writeText("") } }
    }
}
