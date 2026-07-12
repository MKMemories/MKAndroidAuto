package com.mkmemories.copilot.feature.journal

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.mkmemories.copilot.feature.settings.Feature
import com.mkmemories.copilot.feature.settings.SettingsStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class TripJournalTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun `les trajets s'accumulent et survivent a une reouverture`() {
        TripJournal(context).record(TripLogEntry(0L, 30 * 60_000L, 42.5))
        TripJournal(context).record(TripLogEntry(60_000L, 90 * 60_000L, 12.0))
        val journal = TripJournal(context)
        assertEquals(2, journal.all().size)
        assertEquals(54.5, journal.totalKm(), 0.001)
        assertEquals(30L, journal.all()[0].durationMinutes)
    }

    @Test
    fun `deplacer la voiture de quelques metres n'est pas un trajet`() {
        TripJournal(context).record(TripLogEntry(0L, 60_000L, 0.1))
        assertTrue(TripJournal(context).all().isEmpty())
    }

    @Test
    fun `donnee corrompue = journal vide, jamais de plantage`() {
        context.getSharedPreferences("trip_journal", Context.MODE_PRIVATE)
            .edit().putString("entries", "{invalide").apply()
        assertTrue(TripJournal(context).all().isEmpty())
    }

    // --- Interrupteurs du registre (même contexte Robolectric) -----------------

    @Test
    fun `defauts du registre - opt-in pour tout ce qui parle ou envoie des SMS`() {
        val store = SettingsStore(context)
        // Opt-in obligatoires
        assertFalse(store.isEnabled(Feature.MESSAGE_READER))
        assertFalse(store.isEnabled(Feature.AUTO_REPLY))
        assertFalse(store.isEnabled(Feature.TRIP_TRACKING))
        assertFalse(store.isEnabled(Feature.TOURIST_GUIDE))
        assertFalse(store.isEnabled(Feature.FUEL_PRICES))
        // Aides silencieuses actives
        assertTrue(store.isEnabled(Feature.FATIGUE_ALERT))
        assertTrue(store.isEnabled(Feature.BLACK_BOX))
        assertTrue(store.isEnabled(Feature.SMART_DEPARTURE))
    }

    @Test
    fun `un interrupteur bascule et persiste`() {
        SettingsStore(context).setEnabled(Feature.TOURIST_GUIDE, true)
        assertTrue(SettingsStore(context).isEnabled(Feature.TOURIST_GUIDE))
        SettingsStore(context).setEnabled(Feature.FATIGUE_ALERT, false)
        assertFalse(SettingsStore(context).isEnabled(Feature.FATIGUE_ALERT))
    }
}
