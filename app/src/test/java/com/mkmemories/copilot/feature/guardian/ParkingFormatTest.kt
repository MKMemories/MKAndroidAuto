package com.mkmemories.copilot.feature.guardian

import org.junit.Assert.assertEquals
import org.junit.Test

class ParkingFormatTest {

    private val base = 1_752_000_000_000L

    @Test
    fun `parkedAgo - instant, minutes, heures`() {
        assertEquals("à l'instant", ParkingFormat.parkedAgo(base + 30_000, base))
        assertEquals("il y a 25 min", ParkingFormat.parkedAgo(base + 25 * 60_000, base))
        assertEquals("il y a 2 h", ParkingFormat.parkedAgo(base + 120 * 60_000, base))
        assertEquals("il y a 2 h 05", ParkingFormat.parkedAgo(base + 125 * 60_000, base))
    }

    @Test
    fun `parkedAgo - horloge incoherente ne donne pas de negatif`() {
        assertEquals("à l'instant", ParkingFormat.parkedAgo(base, base + 60_000))
    }

    @Test
    fun `distanceLabel - metres et kilometres a la francaise`() {
        assertEquals("320 m", ParkingFormat.distanceLabel(320.0))
        assertEquals("999 m", ParkingFormat.distanceLabel(999.4))
        assertEquals("1,2 km", ParkingFormat.distanceLabel(1200.0))
        assertEquals("12,5 km", ParkingFormat.distanceLabel(12_460.0))
    }
}
