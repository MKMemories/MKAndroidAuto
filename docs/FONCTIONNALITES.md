# MK Copilot — Description complète des fonctionnalités

> **Le copilote qui veille sur vous.** Application Android + Android Auto,
> 100 % gratuite, sans serveur, sans publicité, respectueuse de la vie privée.
> Identité visuelle nordique : le bouclier (protection) ouvert sur la route (voyage).

---

## 🗺️ 1. Mode Road Trip — le voyage orchestré

Planifiez un voyage de plusieurs jours sur le téléphone, vivez-le sur l'écran
de la voiture.

- **Voyages multi-jours** : chaque journée contient ses étapes précises
  (châteaux, restaurants, hôtels, points de vue), datées et ordonnées.
- **Écran Android Auto dédié** : au volant, l'app affiche uniquement les
  étapes du jour. Interface à gros boutons, validée pour la conduite
  (Car App Library, catégorie POI).
- **Handoff Google Maps** : un tap sur une étape → Google Maps (ou Waze)
  prend le relais sur l'écran voiture avec le guidage complet. MK Copilot
  orchestre, Maps navigue — le meilleur des deux mondes.
- **Journée complète dans Maps** : depuis le téléphone, envoi de l'itinéraire
  du jour entier (jusqu'à 9 étapes intermédiaires) en un geste.
- **Enchaînement automatique** : à l'arrivée à une étape (géofencing),
  elle est marquée « visitée ✓ » ; au redémarrage, l'app propose la suivante.
- **Import de voyages** *(v1.1)* : KML/KMZ (Google My Maps), GPX, CSV.

## 🌤️ 2. Briefing du jour — votre départ en musique

- **Météo intelligente** au démarrage : température actuelle, min/max,
  état du ciel, avec **conseils de conduite** (risque de pluie ≥ 40 %,
  vent fort ≥ 50 km/h).
- **Lu à voix haute** en français par la synthèse vocale, dans les
  haut-parleurs de la voiture — les yeux restent sur la route.
- Données **Open-Meteo** : gratuites, sans clé API, sans compte.
- **Extension prévue** *(v1.1)* : agenda du jour, temps de trajet estimé,
  actualités choisies (RSS) — le vrai « flash matinal » personnalisé.

## 🚨 3. Pack Ange gardien — la sécurité premium pour tous

Des fonctions réservées aux voitures haut de gamme et aux téléphones
premium, offertes à tous, **100 % locales** (aucun serveur, aucune donnée
transmise à un tiers).

### 3a. Détection d'accident + SOS automatique
- L'accéléromètre surveille les chocs violents (~6 g, inatteignable en
  conduite normale).
- En cas de choc : alerte « **Accident détecté — tout va bien ?** » avec
  compte à rebours de 30 secondes.
- Sans réponse : **SMS automatique aux contacts d'urgence** avec la position
  GPS exacte (lien Google Maps) + affichage du 112 prêt à appeler.
- Fonctionne **par SMS, même sans données mobiles** — crucial en zone blanche,
  là où les accidents sont les plus isolés.

### 3b. « J'arrive bien » automatique
- À l'arrivée à destination, SMS automatique aux proches choisis :
  *« Bien arrivé à Lyon, 18 h 42 🚗 »*.
- Fini les « tu es bien arrivé ? » — la famille est rassurée sans que
  le conducteur touche son téléphone.

### 3c. Mémoire de stationnement
- À la déconnexion d'Android Auto / du Bluetooth voiture, la position de
  stationnement est **enregistrée automatiquement**.
- Retrouvez la voiture en un tap, même trois jours plus tard.
- *(v1.1)* : photo + étage du parking, **minuteur de stationnement payant /
  zone bleue** avec rappel avant expiration.

## ⚠️ 4. Zones de danger — la vigilance légale

- **Alertes vocales** à l'approche d'une zone de danger : *« Zone de danger
  sur route, limite à 80 km/h. Prudence. »*
- **Strictement conforme à la loi française** (article R413-15) : jamais la
  position exacte d'un radar — uniquement des segments de 300 m (ville),
  2 km (route) ou 4 km (autoroute), comme Waze et Coyote.
- Données issues de l'**open data officiel de l'État** (data.gouv.fr),
  mises en cache pour fonctionner hors ligne.
- **Compatible avec toutes les apps de navigation** : les alertes sont
  audio, elles fonctionnent pendant que Maps ou Waze navigue.
- **Désactivation automatique à l'étranger** *(v1.1)* là où la fonction est
  interdite (Suisse, Allemagne…).

## 🧠 5. IA embarquée — gratuite et privée

Trois niveaux, du plus privé au plus simple, jamais de coût :

1. **On-device** *(v1.1)* : Gemini Nano (AICore) sur les téléphones
   compatibles, sinon petit modèle local — hors ligne, illimité, privé.
2. **Clé Mistral personnelle** *(optionnelle)* : l'utilisateur peut coller sa
   clé gratuite « Experiment » de La Plateforme pour des briefings enrichis.
3. **Sans IA** : l'app est 100 % fonctionnelle avec ses gabarits intégrés.

## 📱 6. Expérience et design

- **Écran d'accueil spectaculaire** : hero fjord sous l'aurore boréale,
  palette nordique (charbon nuit, cyan glacier, orange braise, or).
- **Icône « bouclier-fjord »** adaptative (tous les launchers Android).
- **8 icônes de fonctionnalités** assorties, style médaillon viking.
- Interface **Jetpack Compose**, sombre par défaut (idéale en conduite de nuit).

## 🛣️ 7. Prévu en v2

- **Hub audio** : podcasts, radios web, livres audio avec reprise de lecture
  téléphone ↔ voiture ; résumés IA de podcasts.
- **Messagerie apaisée** : SMS/WhatsApp lus à voix haute, réponse dictée,
  « Ne pas déranger » avec ETA automatique, résumé de fin de trajet.
- **Journal de bord automatique** : détection des trajets, km pro/perso,
  export PDF/CSV pour notes de frais.
- **Santé du véhicule** : OBD-II Bluetooth, codes défaut expliqués en clair,
  carnet d'entretien prédictif.
- **Score d'éco-conduite** avec coaching.
- **Prix carburant « sur ma route »** (open data) et rappels géolocalisés.
- **Alertes météo route** (Vigilance Météo-France sur l'itinéraire).

---

## Les engagements MK Copilot

| Engagement | Concrètement |
|---|---|
| **100 % gratuit** | Aucun abonnement, aucun achat intégré, aucune pub |
| **Zéro serveur** | Tout tourne sur le téléphone ; données gratuites (Open-Meteo, data.gouv.fr) |
| **Privé par conception** | Localisation et capteurs traités localement, jamais revendus |
| **Légal partout** | Zones de danger conformes R413-15, coupées là où interdites |
| **Hors ligne d'abord** | SOS par SMS, radars en cache, TTS local |
