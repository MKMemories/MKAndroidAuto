package com.mkmemories.copilot.feature.carnet

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class CarnetStoreTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private lateinit var store: CarnetStore
    private val base = GreeceOdyssey.itinerary()

    @Before
    fun setup() {
        store = CarnetStore(context)
        store.clear()
    }

    @Test
    fun `un lien ajoute se retrouve sur l etape effective`() {
        store.addLink("vol-aller", CarnetLink("Billet", "https://transavia.com/abc"))
        val entry = store.overlayOnto(base).entries.first { it.id == "vol-aller" }
        assertEquals(1, entry.links.size)
        assertEquals("Billet", entry.links.first().label)
    }

    @Test
    fun `une photo ajoutee se retrouve sur l etape effective`() {
        store.addPhoto("lodge-kamari", "/data/photos/kamari.jpg")
        val entry = store.overlayOnto(base).entries.first { it.id == "lodge-kamari" }
        assertEquals(listOf("/data/photos/kamari.jpg"), entry.photos)
    }

    @Test
    fun `un lieu ajoute geolocalise alimente la navigation`() {
        store.addCustom(
            CustomEntry(
                id = "custom-1",
                phaseId = "kamari",
                title = "Plage rouge de Kamari",
                detail = "Coucher de soleil",
                date = LocalDate.of(2026, 7, 25),
                latitude = 36.351,
                longitude = 25.480,
                locality = "Kamari",
            ),
        )
        val carnet = store.overlayOnto(base)
        val added = carnet.entries.firstOrNull { it.id == "custom-1" }
        assertTrue(added != null && added.custom)
        // Le lieu géolocalisé apparaît dans le voyage navigable
        val stop = carnet.toTrip().days.flatMap { it.stops }.firstOrNull { it.name == "Plage rouge de Kamari" }
        assertTrue(stop != null && stop.latitude == 36.351)
    }

    @Test
    fun `supprimer un lieu retire aussi ses liens et photos`() {
        store.addCustom(CustomEntry("custom-2", "milos", "Sarakiniko", "", LocalDate.of(2026, 7, 28)))
        store.addLink("custom-2", CarnetLink("Photo", "https://x"))
        store.addPhoto("custom-2", "/data/photos/s.jpg")
        store.removeCustom("custom-2")
        val carnet = store.overlayOnto(base)
        assertNull(carnet.entries.firstOrNull { it.id == "custom-2" })
    }

    @Test
    fun `l edition survit a une nouvelle instance du store`() {
        store.addLink("lodge-oia", CarnetLink("Airbnb", "https://airbnb.com/x"))
        val reloaded = CarnetStore(context).overlayOnto(base).entries.first { it.id == "lodge-oia" }
        assertEquals("Airbnb", reloaded.links.first().label)
    }
}
