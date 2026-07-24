package com.mkmemories.copilot.feature.budget

import java.util.Locale

/**
 * Suivi de budget du voyage : chaque prestation (vol, ferry, voiture, logement…)
 * avec son montant et son statut de paiement. Montants en centimes (Long) pour
 * éviter toute imprécision de virgule flottante.
 */
data class BudgetItem(
    val id: String,
    val category: String,
    val label: String,
    val dates: String,
    val schedule: String,
    val amountCents: Long,
    val paid: Boolean,
    val note: String = "",
    val custom: Boolean = false,
)

/** Calculs purs, testables : totaux payé / restant, formatage à la française. */
object BudgetMath {

    fun totalCents(items: List<BudgetItem>): Long = items.sumOf { it.amountCents }
    fun paidCents(items: List<BudgetItem>): Long = items.filter { it.paid }.sumOf { it.amountCents }
    fun remainingCents(items: List<BudgetItem>): Long = items.filterNot { it.paid }.sumOf { it.amountCents }

    /** « 1 448,40 € ». */
    fun formatEuros(cents: Long): String =
        String.format(Locale.FRANCE, "%,.2f €", cents / 100.0)
}

/** Le budget réel du voyage — pré-rempli, éditable ensuite. */
object GreeceBudget {

    private const val PLACE = "Payable sur place"

    fun items(): List<BudgetItem> = listOf(
        BudgetItem("vol-aller", "Vol aller", "Paris (Orly) → Santorin", "24 juil.", "14h50 → 19h15", 80398, true),
        BudgetItem("heb-1", "Hébergement 1", "Ocean Bay Suites (Santorin, Kamari)", "24–27 juil. (3 nuits)", "Check-in 15h00", 144840, true),
        BudgetItem("voiture-1", "Voiture 1", "CoolCars (Aéroport Santorin → Port Athinios)", "24–27 juil.", "20h00 → 14h00", 15600, false, PLACE),
        BudgetItem("ferry-1", "Ferry 1", "Seajets (Santorin, Thira → Milos)", "27 juil.", "14h25 → 16h20", 37480, true),
        BudgetItem("heb-2", "Hébergement 2", "Logement entier Αικατερινη (Milos)", "27–30 juil. (3 nuits)", "15h00 → 11h00", 88974, true),
        BudgetItem("voiture-2", "Voiture 2", "Giourgas Rent a Car (Port de Milos)", "27–30 juil.", "16h30 → 11h30", 33000, false, PLACE),
        BudgetItem("ferry-2", "Ferry 2", "Seajets (Milos → Santorin, Thira)", "30 juil.", "11h45 → 14h15", 37480, true),
        BudgetItem("heb-3", "Hébergement 3", "Logement traditionnel (Santorin, Oía)", "30 juil.–1 août (2 nuits)", "15h00 → 11h00", 74600, true),
        BudgetItem("voiture-3", "Voiture 3", "CoolCars (Port Athinios → Port Athinios)", "30 juil.–1 août", "14h30 → 14h30", 10400, false, PLACE),
        BudgetItem("ferry-3", "Ferry 3", "Seajets (Santorin, Thira → Crète, Héraklion)", "1 août", "15h45 → 17h20", 37080, true),
        BudgetItem("voiture-4", "Voiture 4", "EuroCar (Port d'Héraklion → Aéroport Héraklion)", "1–14 août", "17h00 → 18h00", 79499, false, "Payable à l'arrivée (assurance incluse)"),
        BudgetItem("heb-4", "Hébergement 4", "Beach House (Crète, Agia Marina)", "1–7 août (6 nuits)", "Check-in 15h00", 210000, false, PLACE),
        BudgetItem("heb-5", "Hébergement 5", "Kalos Luxury Homes (Crète, Istron/Voulisma)", "7–14 août (7 nuits)", "Check-in 15h00", 239680, true),
        BudgetItem("vol-retour", "Vol retour", "Crète (Héraklion) → Paris (Orly)", "14 août", "19h45 → 22h30", 98998, true),
    )
}
