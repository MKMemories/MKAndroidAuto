package com.mkmemories.copilot.feature.carnet

import java.time.LocalDate
import java.time.LocalTime

/**
 * « L'Odyssée Grecque » — le voyage réel de la famille, encodé une fois pour
 * toutes : vols, ferries, locations de voiture, logements et — surtout — chaque
 * trajet voiture intermédiaire (domicile → Orly, aéroport → logement, logement
 * → port…). Ces trajets alimentent la navigation étape après étape.
 *
 * Coordonnées : centres de localité / ports / aéroports (précision « guidage »,
 * Google Maps affine à l'arrivée). À ajuster si une adresse exacte est connue.
 */
object GreeceOdyssey {

    // — Points clés géolocalisés —
    private val Domicile = CarnetPlace("Domicile — Andrésy", 48.9761, 2.0553, "Andrésy")
    private val Orly = CarnetPlace("Aéroport de Paris-Orly", 48.7262, 2.3652, "Orly")
    private val SantorinAeroport = CarnetPlace("Aéroport de Santorin (JTR)", 36.3992, 25.4793, "Santorin")
    private val OceanBayKamari = CarnetPlace("Ocean Bay Suites — Kamari", 36.3760, 25.4770, "Kamari, Santorin")
    private val PortAthinios = CarnetPlace("Port d'Athinios — Santorin", 36.3833, 25.4300, "Santorin")
    private val PortMilos = CarnetPlace("Port de Milos (Adamas)", 36.7250, 24.4419, "Milos")
    private val VillaMilos = CarnetPlace("Villa privée — Milos", 36.7300, 24.4400, "Adamas, Milos")
    private val LogementOia = CarnetPlace("Logement traditionnel — Oía", 36.4611, 25.3757, "Oía, Santorin")
    private val PortHeraklion = CarnetPlace("Port d'Héraklion", 35.3455, 25.1500, "Héraklion, Crète")
    private val BeachHouseAgiaMarina = CarnetPlace("Beach House — Agia Marina", 35.5110, 23.9670, "Agia Marina, La Canée")
    private val KalosIstron = CarnetPlace("Kalos Luxury Homes — Istron", 35.1175, 25.7460, "Voulisma, Istron")
    private val HeraklionAeroport = CarnetPlace("Aéroport d'Héraklion (HER)", 35.3397, 25.1803, "Héraklion, Crète")

    private fun d(month: Int, day: Int) = LocalDate.of(2026, month, day)
    private fun t(h: Int, m: Int) = LocalTime.of(h, m)

    fun itinerary(): CarnetVoyage = CarnetVoyage(
        title = "L'Odyssée Grecque",
        subtitle = "Santorin · Milos · Crète",
        travelers = "Mohamed, Saoussen, Lilya & Sara",
        start = d(7, 24),
        end = d(8, 14),
        phases = listOf(
            CarnetPhase(
                id = "depart",
                title = "Le Grand Départ",
                place = "Andrésy → Santorin",
                dateLabel = "Vendredi 24 juillet 2026",
                entries = listOf(
                    CarnetEntry(
                        "drive-orly", EntryKind.DRIVE,
                        "Route vers l'aéroport d'Orly",
                        "Domicile (Andrésy) → Orly. Départ conseillé vers 11h30 pour un vol à 14h50.",
                        d(7, 24), t(11, 30), Orly,
                    ),
                    CarnetEntry(
                        "vol-aller", EntryKind.FLIGHT,
                        "Vol aller — Transavia TO3558",
                        "Orly 14h50 → Santorin 19h15. Famille Khelij (4 personnes).",
                        d(7, 24), t(14, 50),
                    ),
                    CarnetEntry(
                        "car-coolcars-1", EntryKind.CAR,
                        "Voiture 1 — CoolCars",
                        "Prise en charge 20h00 à l'aéroport de Santorin. Nissan Pulsar Auto (ou similaire). 156 € sur place. Restitution 27 juil. 14h00 au port d'Athinios.",
                        d(7, 24), t(20, 0),
                    ),
                    CarnetEntry(
                        "drive-kamari", EntryKind.DRIVE,
                        "Route vers le logement",
                        "Aéroport de Santorin → Ocean Bay Suites, Kamari.",
                        d(7, 24), t(20, 20), OceanBayKamari,
                    ),
                ),
            ),
            CarnetPhase(
                id = "kamari",
                title = "Premier Souffle à Santorin",
                place = "Kamari",
                dateLabel = "24 – 27 juillet 2026",
                entries = listOf(
                    CarnetEntry(
                        "lodge-kamari", EntryKind.LODGING,
                        "Ocean Bay Suites",
                        "Kamari, Santorin. 3 nuits (24 → 27 juil.). 1 448,40 € — confirmé.",
                        d(7, 24),
                    ),
                ),
            ),
            CarnetPhase(
                id = "milos",
                title = "La Magie Blanche de Milos",
                place = "Île de Milos",
                dateLabel = "27 – 30 juillet 2026",
                entries = listOf(
                    CarnetEntry(
                        "drive-athinios-1", EntryKind.DRIVE,
                        "Route vers le port",
                        "Kamari → Port d'Athinios. Restitution CoolCars à 14h00.",
                        d(7, 27), t(13, 30), PortAthinios,
                    ),
                    CarnetEntry(
                        "ferry-milos", EntryKind.FERRY,
                        "Traversée — SeaJets (Olympic Champion)",
                        "Santorin (Thira) 14h25 → Milos 16h20. Durée 1h55.",
                        d(7, 27), t(14, 25),
                    ),
                    CarnetEntry(
                        "car-giourgas", EntryKind.CAR,
                        "Voiture 2 — Giourgas Rent a Car",
                        "Prise en charge 16h30 au port de Milos. Jeep Renegade Automatic 1.6. Restitution 30 juil. 11h30.",
                        d(7, 27), t(16, 30),
                    ),
                    CarnetEntry(
                        "drive-villa-milos", EntryKind.DRIVE,
                        "Route vers la villa",
                        "Port de Milos → Villa privée (hôte Αικατερινη).",
                        d(7, 27), t(16, 45), VillaMilos,
                    ),
                    CarnetEntry(
                        "lodge-milos", EntryKind.LODGING,
                        "Villa privée — Milos",
                        "Hôte Αικατερινη. 3 nuits. Arrivée 15h00 / départ 11h00.",
                        d(7, 27),
                    ),
                ),
                inspirations = listOf(
                    "Sarakiniko — falaises de craie blanche plongeant dans une mer saphir, paysage lunaire incontournable.",
                    "Kleftiko — louez un petit bateau pour explorer ces anciennes cachettes de pirates aux eaux cristallines.",
                    "Klima — flânez devant les « Syrmata », cabanes de pêcheurs aux portes multicolores creusées dans la roche.",
                ),
            ),
            CarnetPhase(
                id = "oia",
                title = "Retour au Joyau de l'Égée",
                place = "Oía",
                dateLabel = "30 juillet – 1er août 2026",
                entries = listOf(
                    CarnetEntry(
                        "drive-port-milos", EntryKind.DRIVE,
                        "Route vers le port",
                        "Villa → Port de Milos. Restitution Giourgas à 11h30.",
                        d(7, 30), t(11, 15), PortMilos,
                    ),
                    CarnetEntry(
                        "ferry-santorin-retour", EntryKind.FERRY,
                        "Traversée retour — SeaJets",
                        "Milos 11h45 → Santorin (Thira) 14h15.",
                        d(7, 30), t(11, 45),
                    ),
                    CarnetEntry(
                        "car-coolcars-2", EntryKind.CAR,
                        "Voiture 3 — CoolCars",
                        "Port d'Athinios 14h30 → 1ᵉʳ août 14h30. Nissan Pulsar Auto. 104 €.",
                        d(7, 30), t(14, 30),
                    ),
                    CarnetEntry(
                        "drive-oia", EntryKind.DRIVE,
                        "Route vers Oía",
                        "Port d'Athinios → Logement traditionnel, Nikolaou Nomikou, Oía.",
                        d(7, 30), t(14, 45), LogementOia,
                    ),
                    CarnetEntry(
                        "lodge-oia", EntryKind.LODGING,
                        "Logement traditionnel — Oía",
                        "Nikolaou Nomikou, Oía, Santorin. 2 nuits (30 juil. → 1ᵉʳ août).",
                        d(7, 30),
                    ),
                ),
            ),
            CarnetPhase(
                id = "crete-ouest",
                title = "L'Éveil de la Crète",
                place = "Agia Marina (Ouest)",
                dateLabel = "1er – 7 août 2026",
                entries = listOf(
                    CarnetEntry(
                        "drive-athinios-2", EntryKind.DRIVE,
                        "Route vers le port",
                        "Oía → Port d'Athinios. Restitution CoolCars à 14h30.",
                        d(8, 1), t(14, 30), PortAthinios,
                    ),
                    CarnetEntry(
                        "ferry-heraklion", EntryKind.FERRY,
                        "Grand départ — SeaJets (WorldChampion)",
                        "Santorin 15h45 → Héraklion (Crète) 17h20.",
                        d(8, 1), t(15, 45),
                    ),
                    CarnetEntry(
                        "car-eurocar", EntryKind.CAR,
                        "Voiture 4 — Eurocar",
                        "Prise en charge 17h00 au port d'Héraklion (gare principale). Peugeot 301 Automatique. 794,99 € (assurance complète incluse). Restitution 14 août 18h00 à l'aéroport.",
                        d(8, 1), t(17, 30),
                    ),
                    CarnetEntry(
                        "drive-agia-marina", EntryKind.DRIVE,
                        "Route vers Agia Marina",
                        "Port d'Héraklion → Beach House, Agia Marina (La Canée).",
                        d(8, 1), t(18, 30), BeachHouseAgiaMarina,
                    ),
                    CarnetEntry(
                        "lodge-agia-marina", EntryKind.LODGING,
                        "Beach House — Agia Marina",
                        "Agia Marina 730 14, La Canée. 6 nuits. Hôte : Vicky.",
                        d(8, 1),
                    ),
                ),
            ),
            CarnetPhase(
                id = "crete-est",
                title = "L'Est Paisible",
                place = "Istron",
                dateLabel = "7 – 14 août 2026",
                entries = listOf(
                    CarnetEntry(
                        "drive-istron", EntryKind.DRIVE,
                        "Traversée de la Crète",
                        "Agia Marina (Ouest) → Kalos Luxury Homes, Istron (Est).",
                        d(8, 7), t(11, 0), KalosIstron,
                    ),
                    CarnetEntry(
                        "lodge-istron", EntryKind.LODGING,
                        "Kalos Luxury Homes — Istron",
                        "Voulisma, Istron (près d'Agios Nikolaos). 7 nuits (7 → 14 août). 2 396,80 € — confirmé.",
                        d(8, 7),
                    ),
                ),
                inspirations = listOf(
                    "Voulisma Beach (Istron) — eaux turquoise étincelantes et sable blanc, idéales en famille.",
                    "Agios Nikolaos — l'atmosphère vibrante autour du lac Voulismeni, au cœur de la ville.",
                    "Elafonissi (Ouest) — excursion mythique pour s'émerveiller devant le sable aux reflets roses.",
                ),
            ),
            CarnetPhase(
                id = "retour",
                title = "Le Retour — Fin de l'Odyssée",
                place = "Héraklion → Andrésy",
                dateLabel = "Vendredi 14 août 2026",
                entries = listOf(
                    CarnetEntry(
                        "drive-her-aeroport", EntryKind.DRIVE,
                        "Route vers l'aéroport",
                        "Istron → Aéroport d'Héraklion. Restitution Eurocar à 18h00 maximum.",
                        d(8, 14), t(17, 0), HeraklionAeroport,
                    ),
                    CarnetEntry(
                        "vol-retour", EntryKind.FLIGHT,
                        "Vol retour — Transavia",
                        "Héraklion 19h45 → Paris (Orly) 22h30. Souvenirs éternels — voyage complet, partagé avec amour.",
                        d(8, 14), t(19, 45),
                    ),
                    CarnetEntry(
                        "drive-domicile", EntryKind.DRIVE,
                        "Retour à la maison",
                        "Aéroport d'Orly → Domicile (Andrésy). Kalo Taxidi !",
                        d(8, 14), t(23, 30), Domicile,
                    ),
                ),
            ),
        ),
    )
}
