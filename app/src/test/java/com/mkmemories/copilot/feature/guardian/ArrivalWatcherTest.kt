package com.mkmemories.copilot.feature.guardian

import com.mkmemories.copilot.feature.roadtrip.TripStop
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Géofencing d'arrivée : détecter une fois, jamais en boucle, réarmable. */
class ArrivalWatcherTest {

    // Chambord ; un point ~100 m au nord est dans le rayon de 150 m
    private val chambord = TripStop("Château de Chambord", 47.6161, 1.5170)
    private val cheverny = TripStop("Château de Cheverny", 47.5006, 1.4579)
    private val nearChambordLat = 47.6170

    @Test
    fun `entrer dans le rayon d'une etape non visitee la declare atteinte`() {
        val watcher = ArrivalWatcher()
        assertEquals(chambord, watcher.onLocation(nearChambordLat, 1.5170, listOf(chambord, cheverny)))
    }

    @Test
    fun `rester dans le rayon ne declenche qu'une seule arrivee`() {
        val watcher = ArrivalWatcher()
        watcher.onLocation(nearChambordLat, 1.5170, listOf(chambord))
        assertNull(watcher.onLocation(47.6165, 1.5171, listOf(chambord)))
    }

    @Test
    fun `une etape deja visitee est ignoree`() {
        val watcher = ArrivalWatcher()
        assertNull(watcher.onLocation(nearChambordLat, 1.5170, listOf(chambord.copy(visited = true))))
    }

    @Test
    fun `loin de toute etape, rien ne se passe`() {
        val watcher = ArrivalWatcher()
        assertNull(watcher.onLocation(48.85, 2.35, listOf(chambord, cheverny))) // Paris
    }

    @Test
    fun `sortir du rayon puis atteindre l'etape suivante fonctionne`() {
        val watcher = ArrivalWatcher()
        watcher.onLocation(nearChambordLat, 1.5170, listOf(chambord, cheverny))
        assertNull("Sur la route entre les deux : silence", watcher.onLocation(47.56, 1.49, listOf(chambord, cheverny)))
        assertEquals(
            cheverny,
            watcher.onLocation(47.5007, 1.4580, listOf(chambord.copy(visited = true), cheverny)),
        )
    }

    @Test
    fun `a 200 m on n'est pas encore arrive (rayon 150 m)`() {
        val watcher = ArrivalWatcher()
        // ~200 m au nord de Chambord
        assertNull(watcher.onLocation(47.6179, 1.5170, listOf(chambord)))
    }
}
