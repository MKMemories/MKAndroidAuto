package com.mkmemories.copilot.feature.dangerzones

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DangerZonesTest {

    // Une zone urbaine autour de la place Bellecour à Lyon, limite 50
    private val urbanZone = DangerZone(45.7578, 4.8320, RoadType.URBAN, 50)

    @Test
    fun `les longueurs de zone respectent la conformite legale R413-15`() {
        // Jamais de position exacte : 300 m ville, 2 km route, 4 km autoroute
        assertEquals(300, RoadType.URBAN.zoneLengthMeters)
        assertEquals(2_000, RoadType.ROAD.zoneLengthMeters)
        assertEquals(4_000, RoadType.HIGHWAY.zoneLengthMeters)
        assertEquals(150, urbanZone.radiusMeters)
        assertEquals(2_000, DangerZone(0.0, 0.0, RoadType.HIGHWAY, null).radiusMeters)
    }

    @Test
    fun `la distance haversine est exacte sur une reference connue`() {
        // Paris (Notre-Dame) → Lyon (Bellecour) ≈ 392 km
        val d = ZoneAlertEngine.distanceMeters(48.8530, 2.3499, 45.7578, 4.8320)
        assertTrue("distance calculée : $d", d in 385_000.0..400_000.0)
    }

    @Test
    fun `distance nulle entre un point et lui-meme`() {
        assertEquals(0.0, ZoneAlertEngine.distanceMeters(45.0, 4.0, 45.0, 4.0), 0.001)
    }

    @Test
    fun `entrer dans une zone declenche une alerte TTS avec la limite de vitesse`() {
        val engine = ZoneAlertEngine { listOf(urbanZone) }
        // ~100 m du centre : dans le rayon de 150 m
        val alert = engine.onLocation(45.7587, 4.8320)
        assertEquals("Zone de danger en ville, limite à 50 kilomètres heure. Prudence.", alert)
    }

    @Test
    fun `l'alerte ne se repete pas tant qu'on reste dans la meme zone`() {
        val engine = ZoneAlertEngine { listOf(urbanZone) }
        assertNotNull(engine.onLocation(45.7587, 4.8320))
        assertNull("Pas de spam d'alerte", engine.onLocation(45.7580, 4.8321))
    }

    @Test
    fun `sortir puis revenir dans la zone rearme l'alerte`() {
        val engine = ZoneAlertEngine { listOf(urbanZone) }
        assertNotNull(engine.onLocation(45.7587, 4.8320))
        assertNull("Hors zone : silence", engine.onLocation(45.80, 4.90))
        assertNotNull("Retour en zone : nouvelle alerte", engine.onLocation(45.7587, 4.8320))
    }

    @Test
    fun `hors de toute zone, aucune alerte`() {
        val engine = ZoneAlertEngine { listOf(urbanZone) }
        assertNull(engine.onLocation(48.85, 2.35))
    }

    @Test
    fun `zone sans limite de vitesse connue - alerte sans mention de limite`() {
        val engine = ZoneAlertEngine { listOf(DangerZone(45.0, 4.0, RoadType.HIGHWAY, null)) }
        assertEquals("Zone de danger sur autoroute. Prudence.", engine.onLocation(45.0, 4.0))
    }
}
