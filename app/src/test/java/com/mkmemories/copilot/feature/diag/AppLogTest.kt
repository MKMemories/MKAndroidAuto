package com.mkmemories.copilot.feature.diag

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Le journal de diagnostic doit capturer messages et crashs sans jamais planter. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AppLogTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun `messages et exceptions sont ecrits et lisibles`() {
        AppLog.install(context, buildNumber = 42)
        AppLog.i("test", "étape A")
        AppLog.error("test", "boum", IllegalStateException("cassé"))

        val content = AppLog.file()!!.readText()
        assertTrue(content.contains("build 42"))
        assertTrue(content.contains("I/test: étape A"))
        assertTrue(content.contains("E/test: boum"))
        // La stacktrace de l'exception est présente
        assertTrue(content.contains("IllegalStateException"))
        assertTrue(content.contains("cassé"))
    }

    @Test
    fun `hasContent et clear fonctionnent`() {
        AppLog.install(context, buildNumber = 1)
        assertTrue(AppLog.hasContent()) // install écrit déjà l'en-tête
        AppLog.clear()
        assertFalse(AppLog.hasContent())
    }

    @Test
    fun `la stacktrace formate le type et le message`() {
        val trace = AppLog.stackTrace(RuntimeException("détail"))
        assertTrue(trace.contains("java.lang.RuntimeException: détail"))
        assertTrue(trace.contains("at ")) // au moins une frame
    }

    @Test
    fun `ecrire sans installation ne plante pas`() {
        // logFile peut être null (process pas encore initialisé) : aucun crash
        AppLog.i("x", "avant install")
        AppLog.error("x", "avant install", Exception())
    }
}
