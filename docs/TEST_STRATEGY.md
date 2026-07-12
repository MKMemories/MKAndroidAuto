# Stratégie de test MK Copilot

Objectif : chaque fonctionnalité est vérifiée avec des **données maîtrisées**
(jamais dépendantes du réseau extérieur ni de l'heure réelle), les scénarios
critiques pour la **sécurité** (SOS, zones de danger) sont couverts en premier,
et toute régression casse le build.

## Pyramide

| Niveau | Outil | Ce qui est couvert | Commande |
|---|---|---|---|
| 1. Unitaire pur (JVM) | JUnit 4 | Toute la logique métier : voyages, URLs Maps, texte des briefings, messages SOS/arrivée, seuil de choc, zones de danger, routeur IA | `gradle testDebugUnitTest` |
| 2. Unitaire Android | Robolectric | Ce qui touche l'API Android sans device : `Uri`, intents de navigation, `SharedPreferences` (mémoire parking) | idem (même tâche) |
| 3. Intégration HTTP | MockWebServer | Le générateur météo bout-en-bout : requête réelle, paramètres vérifiés, réponses nominale / 500 / JSON invalide | idem |
| 4. Statique | Android Lint | Manifest, ressources, compatibilité (erreur = build rouge) | `gradle lintDebug` |
| 5. Assemblage | AGP | APK debug complet, non-régression de packaging | `gradle assembleDebug` |

Le tout s'exécute d'un trait : `gradle testDebugUnitTest lintDebug assembleDebug`.

## Principes de fiabilité des données

- **Zéro réseau extérieur dans les tests** : Open-Meteo est simulé par un
  serveur local (MockWebServer) aux réponses contrôlées au bit près.
- **Zéro horloge réelle** : le compte à rebours SOS (30 s) tourne en temps
  virtuel (`kotlinx-coroutines-test`), les messages d'arrivée reçoivent
  l'heure en paramètre.
- **Zéro matériel simulé approximativement** : la détection d'accident est
  testée sur sa fonction de seuil pure (norme du vecteur, 6 g), pas sur un
  capteur simulé.
- **Round-trip exacts** : la mémoire de parking vérifie que les coordonnées
  ressortent au bit près (`toRawBits`/`fromBits`), aucun mètre perdu.
- **Conformité légale testée** : les longueurs de zones (300 m / 2 km / 4 km,
  article R413-15) sont verrouillées par des assertions — toute modification
  involontaire casse le build.

## Couverture par fonctionnalité

| Fonctionnalité | Classes de test | Scénarios clés |
|---|---|---|
| Road trip | `TripTest`, `NavigationLauncherUrlTest`, `NavigationLauncherRobolectricTest` | étapes du jour, jour vide, limite 9 waypoints Google, encodage `%7C`, intent `google.navigation:` |
| Planificateur (édition) | `TripOpsTest` | ajout (jour existant / nouveau / voyage vide, tri des jours), suppression (jour vidé supprimé, doublons : une seule occurrence, mauvaise date : no-op), modification (heure posée / changée / effacée, hors bornes : no-op), réordonnancement (haut/bas, bords), isolation entre journées, heures parlées du briefing |
| Persistance du voyage | `TripStoreTest` | round-trip exact (nom, dates, heures, localités, visites), donnée corrompue → repli démo sans plantage, tri des jours au rechargement |
| Recherche de lieux | `PlaceSearchTest` | parsing GeoJSON Photon (ordre lng/lat inversé), catégories francisées (Hôtel, Plage…), adresse sans nom → rue+numéro, liste vide, 503 → exception, paramètres q/lang/limit transmis |
| Enrichissement Wikipédia | `PlaceEnrichmentTest` | extrait restitué, homonymies écartées, 404 et extrait absent → null (jamais bloquant pour l'export) |
| Carnet PDF | `TripPdfLayoutTest` | sections triées et datées en français, numérotation par journée, heure de rendez-vous dans le sous-titre, description par localité, sous-titre redondant omis, une requête d'enrichissement par localité unique |
| Mise à jour intégrée | `UpdateCheckerTest` | build le plus récent proposé avec lien APK, même build/plus ancien ignoré, ordre de liste indifférent, release sans APK ou tag inattendu ignorée |
| Briefing météo | `WeatherBriefingGeneratorTest`, `WeatherBriefingIntegrationTest` | seuils pluie (40 %) et vent (50 km/h), arrondis, 19 codes WMO, erreurs serveur → exception (jamais de briefing mensonger) |
| SOS / Ange gardien | `SosManagerTest`, `CrashDetectorTest` | 30 ticks puis SMS à tous les contacts, annulation → zéro SMS, anti-doublon, lien Maps, seuil ~6 g jamais atteint en conduite normale |
| J'arrive bien | `ArrivalNotifierTest` | horodatage français, minutes sur 2 chiffres, tous les proches notifiés |
| Parking | `ParkingMemoryTest` | vide au départ, précision exacte, remplacement, coordonnées négatives |
| Zones de danger | `DangerZonesTest` | conformité légale, haversine ±2 % sur Paris→Lyon, alerte à l'entrée, anti-spam, réarmement, zone sans limite connue |
| IA | `AiEngineTest` | ordre de préférence des backends, repli sans IA, disponibilité Mistral liée à la clé |

Cas volontairement hors périmètre automatique : le déplacement d'une étape
vers une **autre date** et le changement de **lieu** d'une étape se font par
suppression + re-ajout (choix v1) ; `CalendarImporter` est une lecture fine
du CalendarProvider, vérifiée via la checklist manuelle (permission,
événements journée entière, rendez-vous sans lieu ignorés avec décompte).

## Ce que l'automatisation ne peut pas couvrir (checklist manuelle)

À dérouler sur device + [Desktop Head Unit (DHU)](https://developer.android.com/training/cars/testing/dhu)
avant chaque release :

1. **Android Auto** : l'écran carte affiche les repères numérotés or ; un tap
   sur une étape bascule vers Google Maps ; l'action « Briefing » parle dans
   les haut-parleurs ; étapes visitées en vert en fin de liste.
2. **TTS** : voix française présente, lecture complète du briefing, arrêt
   propre à la fermeture (`release()`).
3. **Splash + accueil** : emblème sur charbon nuit, parallaxe fluide 60 fps,
   retours haptiques, edge-to-edge sans texte sous les barres système.
4. **SMS réels** (carte SIM de test) : SOS après 30 s sans réponse,
   « J'arrive bien » à l'arrivée.
5. **Capteurs** : détection d'accident sur choc simulé (chute contrôlée du
   téléphone = faux positif attendu en v1, cf. TODO vitesse GPS).
6. **Planificateur sur device** : autocomplétion réelle (hôtel à Santorin,
   adresse en Crète, lieu français), latence de frappe, import agenda
   (permission au premier usage, rendez-vous sans lieu comptés comme
   ignorés, événements journée entière sans heure).
7. **Carnet PDF** : rendu visuel (couverture, coupures de page, accents),
   ouverture dans un lecteur externe via la feuille de partage.
8. **Mise à jour intégrée** : le bandeau apparaît quand une release plus
   récente existe, le tap télécharge l'APK, l'installation par-dessus
   fonctionne (signature stable).

## Bugs corrigés par ce plan (première exécution)

1. **BriefingPlayer** : la langue française était configurée *avant* la fin
   d'initialisation du moteur TTS (perdue silencieusement selon les devices).
   Corrigé : configuration dans le callback `onInit`.
2. **Testabilité sécurité** : l'envoi SMS était couplé en dur à
   `SmsManager` — le scénario SOS (le plus critique de l'app) était
   invérifiable. Corrigé : injection de l'envoi (`smsSender`), production
   inchangée par défaut.
3. **WeatherBriefingGenerator** : URL Open-Meteo en dur — aucun test
   d'intégration possible. Corrigé : `baseUrl` injectable.
4. **NavigationLauncher** : construction d'URL couplée à `android.net.Uri` —
   la limite des 9 waypoints et l'encodage du séparateur n'étaient pas
   testables en JVM pure. Corrigé : builder pur + wrapper `Uri`.
