package com.mkmemories.copilot.feature.budget

import android.content.Context
import androidx.test.core.app.ApplicationProvider
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
class BudgetStoreTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private lateinit var store: BudgetStore
    private val base = GreeceBudget.items()

    @Before
    fun setup() {
        store = BudgetStore(context)
        store.clear()
    }

    @Test
    fun `marquer paye reduit le reste a payer`() {
        val before = BudgetMath.remainingCents(store.effectiveItems(base))
        store.setPaid("voiture-1", true) // 156,00 € étaient à payer
        val after = BudgetMath.remainingCents(store.effectiveItems(base))
        assertEquals(before - 15_600L, after)
    }

    @Test
    fun `ajouter une prestation augmente le total`() {
        store.addCustom(BudgetItem("extra-1", "Extra", "Excursion bateau Kleftiko", "28 juil.", "", 12_000, false, custom = true))
        val items = store.effectiveItems(base)
        assertEquals(BudgetMath.totalCents(base) + 12_000L, BudgetMath.totalCents(items))
        assertTrue(items.any { it.id == "extra-1" && it.custom })
    }

    @Test
    fun `supprimer une prestation ajoutee la retire`() {
        store.addCustom(BudgetItem("extra-2", "Extra", "Restaurant", "1 août", "", 8_000, true, custom = true))
        store.removeCustom("extra-2")
        assertNull(store.effectiveItems(base).firstOrNull { it.id == "extra-2" })
    }

    @Test
    fun `le statut modifie survit a une nouvelle instance`() {
        store.setPaid("heb-4", true)
        val reloaded = BudgetStore(context).effectiveItems(base).first { it.id == "heb-4" }
        assertTrue(reloaded.paid)
    }
}
