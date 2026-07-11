package com.mkmemories.copilot.feature.guardian

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Mémoire de stationnement : la position doit survivre sans perte de précision. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ParkingMemoryTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun `aucune position avant le premier stationnement`() {
        assertNull(ParkingMemory(context).lastParkingSpot())
    }

    @Test
    fun `la position sauvegardee est restituee a l'identique, sans arrondi`() {
        val memory = ParkingMemory(context)
        memory.saveParkingSpot(latitude = 48.858370123456789, longitude = 2.294481098765432, timestampMillis = 1_752_000_000_000)

        val spot = memory.lastParkingSpot()!!
        // toRawBits/fromBits : round-trip exact du Double, aucun mètre perdu
        assertEquals(48.858370123456789, spot.latitude, 0.0)
        assertEquals(2.294481098765432, spot.longitude, 0.0)
        assertEquals(1_752_000_000_000, spot.timestampMillis)
    }

    @Test
    fun `un nouveau stationnement remplace l'ancien`() {
        val memory = ParkingMemory(context)
        memory.saveParkingSpot(1.0, 1.0, 1L)
        memory.saveParkingSpot(-33.8688, 151.2093, 2L) // coordonnées négatives aussi
        val spot = memory.lastParkingSpot()!!
        assertEquals(-33.8688, spot.latitude, 0.0)
        assertEquals(151.2093, spot.longitude, 0.0)
    }
}
