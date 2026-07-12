package com.mkmemories.copilot.feature.carnet

import java.time.LocalDate
import java.time.LocalTime

/**
 * Carnet de voyage — le compagnon complet d'un voyage, structuré en phases
 * (chaque escale = une phase). Les entrées « trajet voiture » portent une
 * destination géolocalisée qui alimente automatiquement la navigation
 * (Google Maps sur le téléphone et l'écran Android Auto), étape après étape.
 */
data class CarnetVoyage(
    val title: String,
    val subtitle: String,
    val travelers: String,
    val start: LocalDate,
    val end: LocalDate,
    val phases: List<CarnetPhase>,
) {
    /** Toutes les entrées, à plat, dans l'ordre du carnet. */
    val entries: List<CarnetEntry> get() = phases.flatMap { it.entries }

    /** Les trajets roulables (ceux qui nourrissent la navigation). */
    val drives: List<CarnetEntry> get() = entries.filter { it.kind == EntryKind.DRIVE && it.destination != null }

    /** Tous les points navigables (trajets + lieux à visiter géolocalisés). */
    val navStops: List<CarnetEntry>
        get() = entries.filter { (it.kind == EntryKind.DRIVE || it.kind == EntryKind.VISIT) && it.destination != null }
}

data class CarnetPhase(
    val id: String,
    val title: String,
    val place: String,
    val dateLabel: String,
    val entries: List<CarnetEntry>,
    val inspirations: List<String> = emptyList(),
) {
    /** Identifiants des entrées de la phase — pour le suivi de progression. */
    val entryIds: List<String> get() = entries.map { it.id }

    /** Le premier grand transport de la phase (vol/ferry/voiture) — glyphe du ruban. */
    val leadTransport: EntryKind?
        get() = entries.firstOrNull { it.kind == EntryKind.FLIGHT || it.kind == EntryKind.FERRY }?.kind
            ?: entries.firstOrNull { it.kind == EntryKind.CAR || it.kind == EntryKind.DRIVE }?.kind

    /** Premier trajet roulable de la phase — action « naviguer » du nœud. */
    val firstDrive: CarnetEntry? get() = entries.firstOrNull { it.kind == EntryKind.DRIVE && it.destination != null }
}

/** Nature d'une entrée du carnet — pilote l'icône et la mise en forme. */
enum class EntryKind { DRIVE, FLIGHT, FERRY, CAR, LODGING, VISIT }

/** Lieu géolocalisé, destination d'un trajet voiture. */
data class CarnetPlace(
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val locality: String? = null,
)

/** Lien attaché à une étape (réservation, site, billet, Google Maps…). */
data class CarnetLink(val label: String, val url: String)

data class CarnetEntry(
    val id: String,
    val kind: EntryKind,
    val title: String,
    val detail: String,
    val date: LocalDate,
    val time: LocalTime? = null,
    /** Présente pour les trajets voiture / lieux à visiter : la navigation guide vers ce point. */
    val destination: CarnetPlace? = null,
    /** Liens attachés par l'utilisateur (réservations, billets, cartes…). */
    val links: List<CarnetLink> = emptyList(),
    /** Chemins locaux des photos souvenirs attachées à l'étape. */
    val photos: List<String> = emptyList(),
    /** Vrai si l'étape a été ajoutée par l'utilisateur (modifiable / supprimable). */
    val custom: Boolean = false,
)
