package com.mkmemories.copilot.feature.budget

import org.junit.Assert.assertEquals
import org.junit.Test

class BudgetStatsTest {

    private val items = GreeceBudget.items()

    @Test
    fun `classification en quatre grands postes`() {
        val postes = BudgetStats.byPoste(items).map { it.poste }.toSet()
        assertEquals(setOf("Vols", "Hébergements", "Voitures", "Ferries"), postes)
    }

    @Test
    fun `les hebergements sont le plus gros poste`() {
        val biggest = BudgetStats.biggestPoste(items)!!
        assertEquals("Hébergements", biggest.poste)
        // 1448,40 + 889,74 + 746,00 + 2100,00 + 2396,80 = 7580,94 €
        assertEquals(758_094L, biggest.totalCents)
        assertEquals(5, biggest.count)
    }

    @Test
    fun `la somme des postes egale le total`() {
        val sumPostes = BudgetStats.byPoste(items).sumOf { it.totalCents }
        assertEquals(BudgetMath.totalCents(items), sumPostes)
    }

    @Test
    fun `le poste voitures est entierement a payer`() {
        val voitures = BudgetStats.byPoste(items).first { it.poste == "Voitures" }
        assertEquals(0L, voitures.paidCents)
        assertEquals(voitures.totalCents, voitures.remainingCents)
    }

    @Test
    fun `une prestation personnalisee cree son propre poste`() {
        val extra = items + BudgetItem("x", "Restaurant", "Diner", "", "", 5_000, true, custom = true)
        assertEquals("Restaurant", BudgetStats.poste(extra.last()))
        assertEquals(10, BudgetStats.paidCount(extra)) // 9 payés d'origine + 1
    }
}
