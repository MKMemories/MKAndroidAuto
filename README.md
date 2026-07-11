# MK Copilot 🚗

**Le copilote Android Auto qui veille sur vous — 100 % gratuit.**

Application Android (téléphone) + Android Auto qui orchestre vos trajets et vos
voyages : road trip planifié avec handoff vers Google Maps, briefing météo du
jour lu en voiture, zones de danger conformes à la loi, et pack Ange gardien
(détection d'accident, « J'arrive bien », mémoire de stationnement).

## Les piliers de la v1

### 🗺️ Mode Road Trip (catégorie Android Auto : POI)
- Voyage planifié sur plusieurs jours, étapes précises datées.
- Sur l'écran voiture : les étapes du jour ; un tap → **handoff officiel vers
  Google Maps** (`CarContext.ACTION_NAVIGATE`) qui assure le guidage.
- Sur téléphone : envoi de la journée complète dans Maps (URL officielle,
  jusqu'à 9 étapes intermédiaires).
- Détection d'arrivée par géofencing → étape marquée visitée, proposition de
  l'étape suivante.
- Import prévu : KML/KMZ (Google My Maps), GPX, CSV.

### 🌤️ Briefing du jour
- Météo via **Open-Meteo** (gratuit, sans clé API) : température, ciel, pluie,
  vent — avec conseils de conduite.
- Lu par la **synthèse vocale native Android** dans les haut-parleurs de la
  voiture.
- Extension prévue : agenda, temps de trajet, actualités choisies.

### 🚨 Pack Ange gardien (100 % on-device, zéro serveur)
- **Détection d'accident** : accéléromètre + GPS, seuil de choc ~6 g ;
  « Tout va bien ? » + compte à rebours 30 s → **SMS SOS automatique** aux
  contacts d'urgence avec position GPS. Fonctionne sans données mobiles.
- **« J'arrive bien » automatique** : SMS aux proches à l'arrivée.
- **Mémoire de stationnement** : position enregistrée à la déconnexion de la
  voiture ; minuteur zone bleue prévu en v1.1.

### ⚠️ Zones de danger (conformité légale stricte)
- Jamais la position exacte d'un radar (article R413-15 du Code de la route) :
  uniquement des **zones de danger** de 300 m (ville), 2 km (route) ou 4 km
  (autoroute), comme Waze/Coyote.
- Données : open data officiel des radars fixes (**data.gouv.fr**).
- Alertes audio via TTS, compatibles avec n'importe quelle app de navigation.
- Activée uniquement dans les pays où c'est légal (interdite p. ex. en Suisse
  et en Allemagne) — géofencing par pays.

### 🧠 IA 100 % gratuite (couche `feature/ai`)
1. **On-device d'abord** : Gemini Nano via AICore, ou petit modèle local
   quantisé (privé, hors ligne, illimité) — v1.1.
2. **Clé Mistral personnelle (optionnelle)** : l'utilisateur colle sa clé
   gratuite « Experiment » de La Plateforme dans les réglages (~2 req/min,
   suffisant pour un briefing par trajet).
3. **Repli sans IA** : briefings par gabarits — l'app reste 100 %
   fonctionnelle sans aucune IA.

## Architecture

```
app/
 └── src/main/java/com/mkmemories/copilot/
     ├── MainActivity.kt              # UI téléphone (Jetpack Compose)
     ├── car/                         # Android Auto (Car App Library, POI)
     │   ├── CopilotCarAppService.kt
     │   └── RoadTripScreen.kt
     └── feature/
         ├── roadtrip/                # Voyages, étapes, handoff Google Maps
         ├── briefing/                # Météo Open-Meteo + TTS
         ├── guardian/                # Accident, SOS, arrivée, parking
         ├── dangerzones/             # Zones de danger légales
         └── ai/                      # Backends IA (on-device / Mistral)
```

- **Kotlin + Jetpack Compose** (téléphone), **androidx.car.app 1.4** (voiture).
- Aucune dépendance payante, aucun serveur : données gratuites (Open-Meteo,
  data.gouv.fr, OpenStreetMap), traitement local.

## Compiler

```bash
# Prérequis : JDK 17+, Android SDK (platform 35)
./gradlew assembleDebug
# APK : app/build/outputs/apk/debug/app-debug.apk
```

Test Android Auto sans voiture : [Desktop Head Unit (DHU)](https://developer.android.com/training/cars/testing/dhu).

## Roadmap

Voir [docs/ROADMAP.md](docs/ROADMAP.md).
