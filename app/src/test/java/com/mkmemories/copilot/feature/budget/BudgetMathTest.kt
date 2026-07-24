package com.mkmemories.copilot.feature.budget

import org.junit.Assert.assertEquals
import org.junit.Test

class BudgetMathTest {

    private val items = GreeceBudget.items()

    @Test
    fun `le total correspond au recapitulatif du voyage`() {
        assertEquals(1_188_029L, BudgetMath.totalCents(items))
    }

    @Test
    fun `paye plus restant egale le total`() {
        assertEquals(839_530L, BudgetMath.paidCents(items))
        assertEquals(348_499L, BudgetMath.remainingCents(items))
        assertEquals(BudgetMath.totalCents(items), BudgetMath.paidCents(items) + BudgetMath.remainingCents(items))
    }

    @Test
    fun `formatage a la francaise`() {
        // On neutralise les espaces (le separateur de milliers francais est insecable).
        fun norm(s: String) = s.replace("[\\u00A0\\u202F ]".toRegex(), "")
        assertEquals("1448,40€", norm(BudgetMath.formatEuros(144_840L)))
        assertEquals("156,00€", norm(BudgetMath.formatEuros(15_600L)))
    }
}
