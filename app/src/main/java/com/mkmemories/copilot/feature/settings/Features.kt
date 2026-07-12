package com.mkmemories.copilot.feature.settings

/**
 * Registre des fonctionnalités activables/désactivables dans les Réglages.
 * Défauts prudents : tout ce qui envoie des SMS, lit des messages ou parle
 * spontanément est OPT-IN ; les aides silencieuses et les outils sont actifs.
 */
enum class Feature(
    val title: String,
    val description: String,
    val defaultEnabled: Boolean,
) {
    FATIGUE_ALERT(
        "Alerte fatigue",
        "Après 2 h de conduite, une voix suggère une pause (puis toutes les heures, renforcé la nuit)",
        true,
    ),
    ROUTE_WEATHER(
        "Météo d'itinéraire",
        "Au départ, annonce la pluie qui attend plus loin sur les étapes du jour",
        true,
    ),
    FUEL_PRICES(
        "Carburant le moins cher",
        "Au départ, annonce la station la moins chère autour de vous (open data officiel)",
        false,
    ),
    MESSAGE_READER(
        "Messages lus en conduite",
        "Les SMS reçus sont lus à voix haute pendant la conduite",
        false,
    ),
    AUTO_REPLY(
        "Réponse automatique avec ETA",
        "Répond aux SMS : « Je conduis, j'arrive vers 18 h 40 » (au plus un par contact et par demi-heure)",
        false,
    ),
    BLACK_BOX(
        "Boîte noire locale",
        "Conserve les 60 dernières secondes de capteurs en cas de choc — 100 % privé, exportable",
        true,
    ),
    TRIP_TRACKING(
        "Voyage suivi par SMS",
        "Position envoyée aux proches toutes les 30 min pendant la conduite — zéro cloud",
        false,
    ),
    ICE_ALERT(
        "Alerte verglas / brouillard",
        "Au départ, prévient si les conditions s'y prêtent (température, saison, heure)",
        true,
    ),
    PARKING_TOOLS(
        "Ma voiture (parking +)",
        "Retrouver la voiture, note, minuteur zone bleue avec rappel unique",
        true,
    ),
    DETOURS(
        "Détours qui valent le coup",
        "Suggestions de sites remarquables proches de votre route (Wikipédia)",
        true,
    ),
    SMART_DEPARTURE(
        "Départ intelligent",
        "Heure de départ conseillée pour chaque rendez-vous, dans le briefing et le planificateur",
        true,
    ),
    ROUTE_OPTIMIZER(
        "Optimiseur d'étapes",
        "Bouton « Ordre optimal » dans le planificateur (calcul local)",
        true,
    ),
    TRIP_IMPORT_SHARE(
        "Import & partage de voyage",
        "Import KML/GPX/JSON et partage du voyage en fichier",
        true,
    ),
    PHOTO_BOOK(
        "Carnet photos",
        "Attachez des photos aux étapes : le carnet PDF devient un livre de voyage",
        true,
    ),
    TRIP_JOURNAL(
        "Journal de bord",
        "Chaque trajet enregistré localement (distance, durée), exportable",
        true,
    ),
    TOURIST_GUIDE(
        "Guide du territoire",
        "En roulant, raconte les lieux traversés (Wikipédia + voix) — au plus toutes les 10 min",
        false,
    ),
    VOICE_COMMANDS(
        "Commandes vocales",
        "Micro sur l'accueil : « prochaine étape », « où est ma voiture », « préviens que j'arrive »",
        true,
    ),
}
