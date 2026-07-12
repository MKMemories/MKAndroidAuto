package com.mkmemories.copilot.feature.drive

import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DriveAlertsTest {

    // --- Fatigue -----------------------------------------------------------

    @Test
    fun `pas d'alerte avant 2 h de conduite en journee`() {
        val monitor = FatigueMonitor()
        assertNull(monitor.check(drivingMillis = 119 * 60_000L, hourOfDay = 14))
    }

    @Test
    fun `alerte a 2 h, puis rappel une heure plus tard, sans spam entre les deux`() {
        val monitor = FatigueMonitor()
        assertNotNull(monitor.check(2 * 3_600_000L, 14))
        assertNull("Pas de répétition immédiate", monitor.check(2 * 3_600_000L + 60_000, 14))
        assertNull(monitor.check(2 * 3_600_000L + 59 * 60_000L, 15))
        assertNotNull("Rappel après une heure", monitor.check(3 * 3_600_000L, 15))
    }

    @Test
    fun `la nuit, premier seuil a 1 h 30 et message renforce`() {
        val monitor = FatigueMonitor()
        assertNull(monitor.check(89 * 60_000L, hourOfDay = 3))
        val alert = monitor.check(90 * 60_000L, hourOfDay = 3)
        assertNotNull(alert)
        assertTrue(alert!!.contains("nuit"))
        assertTrue(alert.contains("somnolence"))
    }

    @Test
    fun `la duree est enoncee naturellement`() {
        val alert = FatigueMonitor().check(2 * 3_600_000L + 20 * 60_000L, 14)!!
        assertTrue(alert.contains("2 heures 20 minutes"))
    }

    // --- Verglas / brouillard ------------------------------------------------

    @Test
    fun `verglas + brouillard = alerte maximale`() {
        val warning = IceRisk.warning(temperatureC = 0.5, weatherCode = 48, hourOfDay = 8)!!
        assertTrue(warning.contains("verglas"))
        assertTrue(warning.contains("visibilité"))
    }

    @Test
    fun `froid matinal seul = alerte verglas ponts et zones ombragees`() {
        val warning = IceRisk.warning(1.0, weatherCode = 0, hourOfDay = 7)!!
        assertTrue(warning.contains("ponts"))
    }

    @Test
    fun `froid en pleine apres-midi claire = silence (pas d'alarmisme)`() {
        assertNull(IceRisk.warning(1.0, weatherCode = 0, hourOfDay = 14))
    }

    @Test
    fun `brouillard doux = conseil feux et distances`() {
        assertTrue(IceRisk.warning(10.0, 45, 12)!!.contains("distances"))
    }

    @Test
    fun `beau temps tempere = rien`() {
        assertNull(IceRisk.warning(18.0, 1, 12))
    }
}
