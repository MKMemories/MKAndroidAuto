package com.mkmemories.copilot.feature.budget

/** Statistiques du budget : classification par poste de dépenses. Pur, testable. */
object BudgetStats {

    /** Regroupe une prestation dans un grand poste de dépenses. */
    fun poste(item: BudgetItem): String {
        val c = item.category.trim().lowercase()
        return when {
            c.startsWith("vol") -> "Vols"
            c.startsWith("hébergement") || c.startsWith("hebergement") || c.startsWith("logement") -> "Hébergements"
            c.startsWith("voiture") || c.startsWith("location") -> "Voitures"
            c.startsWith("ferry") || c.startsWith("bateau") || c.startsWith("traversée") -> "Ferries"
            item.category.isBlank() -> "Extras"
            else -> item.category.trim()
        }
    }

    /** Un poste agrégé : total, payé, restant, nombre de prestations. */
    data class PosteStat(
        val poste: String,
        val totalCents: Long,
        val paidCents: Long,
        val count: Int,
    ) {
        val remainingCents: Long get() = totalCents - paidCents
    }

    /** Postes agrégés, triés du plus coûteux au moins coûteux. */
    fun byPoste(items: List<BudgetItem>): List<PosteStat> =
        items.groupBy { poste(it) }
            .map { (name, group) ->
                PosteStat(
                    poste = name,
                    totalCents = group.sumOf { it.amountCents },
                    paidCents = group.filter { it.paid }.sumOf { it.amountCents },
                    count = group.size,
                )
            }
            .sortedByDescending { it.totalCents }

    /** Part d'un montant dans le total, entre 0 et 1. */
    fun share(cents: Long, totalCents: Long): Float =
        if (totalCents <= 0L) 0f else (cents.toDouble() / totalCents).toFloat()

    /** Le poste le plus coûteux, si le budget n'est pas vide. */
    fun biggestPoste(items: List<BudgetItem>): PosteStat? = byPoste(items).firstOrNull()

    /** Nombre de prestations déjà réglées. */
    fun paidCount(items: List<BudgetItem>): Int = items.count { it.paid }
}
