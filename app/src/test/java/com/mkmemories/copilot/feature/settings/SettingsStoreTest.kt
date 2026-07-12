package com.mkmemories.copilot.feature.settings

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Les réglages portent la sécurité : leur persistance doit être irréprochable. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SettingsStoreTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun `valeurs par defaut sures - gardien et zones actives, SMS arrivee opt-in`() {
        val store = SettingsStore(context)
        assertTrue(store.guardianEnabled)
        assertTrue(store.dangerZonesEnabled)
        assertFalse("Envoyer des SMS automatiques doit être un choix explicite", store.arrivalSmsEnabled)
        assertTrue(store.emergencyContacts.isEmpty())
        assertNull(store.mistralKey)
    }

    @Test
    fun `contacts d'urgence - ajout avec nettoyage, doublon ignore, suppression`() {
        val store = SettingsStore(context)
        store.addEmergencyContact("  +33600000001 ")
        store.addEmergencyContact("+33600000001") // doublon
        store.addEmergencyContact("+33600000002")
        assertEquals(listOf("+33600000001", "+33600000002"), store.emergencyContacts)

        store.removeEmergencyContact("+33600000001")
        assertEquals(listOf("+33600000002"), store.emergencyContacts)
    }

    @Test
    fun `les listes survivent a une reouverture du store`() {
        SettingsStore(context).addArrivalRecipient("+33611111111")
        assertEquals(listOf("+33611111111"), SettingsStore(context).arrivalRecipients)
    }

    @Test
    fun `cle Mistral - vide ou blanche = null, sinon rognee`() {
        val store = SettingsStore(context)
        store.mistralKey = "   "
        assertNull(store.mistralKey)
        store.mistralKey = "  ma-clé  "
        assertEquals("ma-clé", store.mistralKey)
    }

    @Test
    fun `les interrupteurs persistent`() {
        val store = SettingsStore(context)
        store.guardianEnabled = false
        store.arrivalSmsEnabled = true
        assertFalse(SettingsStore(context).guardianEnabled)
        assertTrue(SettingsStore(context).arrivalSmsEnabled)
    }
}
