package com.mkmemories.copilot.feature.roadtrip

import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Optimiseur d'étapes et départ intelligent — calculs locaux. */
class PlanningToolsTest {

    // Paris, Orléans, Blois, Tours : l'ordre géographique naturel Nord→Sud-Ouest
    private val paris = TripStop("Paris", 48.8566, 2.3522)
    private val orleans = TripStop("Orléans", 47.9029, 1.9039)
    private val blois = TripStop("Blois", 47.5861, 1.3359)
    private val tours = TripStop("Tours", 47.3941, 0.6848)

    @Test
    fun `l'optimiseur retablit l'ordre geographique naturel`() {
        val shuffled = listOf(paris, tours, orleans, blois)
        val optimized = RouteOptimizer.optimize(shuffled)
        assertEquals(listOf("Paris", "Orléans", "Blois", "Tours"), optimized.map { it.name })
    }

    @Test
    fun `l'ordre optimise n'est jamais plus long que l'ordre initial`() {
        val shuffled = listOf(blois, paris, tours, orleans)
        val optimized = RouteOptimizer.optimize(shuffled)
        assertTrue(RouteOptimizer.totalMeters(optimized) <= RouteOptimizer.totalMeters(shuffled) + 1)
        assertEquals("Toutes les étapes sont conservées", shuffled.toSet(), optimized.toSet())
    }

    @Test
    fun `les etapes a heure fixe restent des ancres immuables`() {
        val lunch = TripStop("Déjeuner réservé", 47.9029, 1.9039, time = LocalTime.of(12, 30))
        val optimized = RouteOptimizer.optimize(listOf(paris, tours, lunch, blois))
        assertEquals("Déjeuner réservé", optimized[2].name)
        assertEquals(4, optimized.size)
    }

    @Test
    fun `moins de trois etapes = ordre inchange`() {
        assertEquals(listOf(paris, tours), RouteOptimizer.optimize(listOf(paris, tours)))
    }

    // --- Départ intelligent -------------------------------------------------------

    @Test
    fun `conseil de depart pour le premier rendez-vous a venir`() {
        // Blois → Chambord ~14 km à vol d'oiseau : ~17 min estimées + 15 de marge
        val advice = DepartureAdvisor.advice(
            fromLat = 47.5861, fromLng = 1.3359,
            stops = listOf(TripStop("Chambord", 47.6161, 1.5170, time = LocalTime.of(10, 0))),
            now = LocalTime.of(8, 0),
        )!!
        assertTrue(advice.contains("Chambord à 10h00"))
        assertTrue(advice.contains("départ conseillé à 09h"))
    }

    @Test
    fun `deja en retard = message immediat`() {
        val advice = DepartureAdvisor.advice(
            47.5861, 1.3359,
            listOf(TripStop("Chambord", 47.6161, 1.5170, time = LocalTime.of(10, 0))),
            now = LocalTime.of(9, 55),
        )!!
        assertTrue(advice.contains("il est temps de partir"))
    }

    @Test
    fun `sans rendez-vous a venir = pas de conseil`() {
        assertNull(DepartureAdvisor.advice(1.0, 1.0, listOf(TripStop("Libre", 2.0, 2.0)), LocalTime.NOON))
        assertNull(
            "Rendez-vous passé : silence",
            DepartureAdvisor.advice(
                1.0, 1.0,
                listOf(TripStop("Passé", 2.0, 2.0, time = LocalTime.of(9, 0))),
                LocalTime.of(10, 0),
            ),
        )
    }
}
