# Roadmap MK Copilot

Périmètre validé : app 100 % gratuite, sans serveur, IA on-device/Mistral.

## v1 — Fondations (squelette actuel → MVP)

- [x] Projet Android + module Android Auto (Car App Library, catégorie POI)
- [x] Écran voiture : étapes du jour + handoff navigation Google Maps
- [x] Modèles Road Trip (voyage / journées / étapes)
- [x] Briefing météo du jour (Open-Meteo) + lecture TTS
- [x] Ange gardien : détecteur d'accident (accéléromètre) + escalade SOS SMS
- [x] Ange gardien : « J'arrive bien » (SMS à l'arrivée)
- [x] Ange gardien : mémoire de stationnement (position auto-enregistrée)
- [x] Zones de danger : modèles conformes R413-15 + moteur d'alerte
- [x] Couche IA à 3 niveaux (on-device / clé Mistral utilisateur / gabarits)
- [ ] Position réelle (FusedLocationProvider) branchée sur briefing et alertes
- [ ] Persistance des voyages (Room) + écran d'édition sur téléphone
- [ ] Chargement de l'open data radars (data.gouv.fr) + cache hors ligne
- [ ] Géofencing d'arrivée (étape visitée → proposer la suivante)
- [ ] Réglages : contacts d'urgence, proches « J'arrive bien », clé Mistral
- [ ] Demandes de permissions (localisation, SMS) avec explications
- [ ] Test DHU (Desktop Head Unit) + allowlist hosts pour la release

## v1.1 — Confort

- [ ] Import de voyages : KML/KMZ (Google My Maps), GPX, CSV
- [ ] Briefing enrichi : agenda du jour, temps de trajet, actus (RSS)
- [ ] IA on-device : AICore (Gemini Nano) puis petit modèle local en repli
- [ ] Minuteur stationnement payant / zone bleue avec rappel
- [ ] Affinage détection d'accident (vitesse GPS, fenêtre glissante)
- [ ] Géofencing pays pour les zones de danger (désactivation auto à l'étranger)
- [ ] Prix carburant « sur ma route » (open data roulez-eco / data.gouv.fr)

## v2 — Extension

- [ ] Hub audio (MediaBrowserService) : podcasts, radios, reprise de lecture
- [ ] Résumés IA de podcasts (on-device ou clé utilisateur)
- [ ] Messagerie unifiée voiture (MessagingStyle) + « Ne pas déranger » avec ETA
- [ ] Journal de bord automatique (km pro/perso, export PDF/CSV)
- [ ] Rappels géolocalisés (« le colis en passant au point relais »)
- [ ] Santé véhicule : OBD-II Bluetooth, codes défaut expliqués, entretien prédictif
- [ ] Score d'éco-conduite + coaching
- [ ] Alertes météo route (Vigilance Météo-France sur l'itinéraire du jour)
- [ ] Signalement communautaire des zones de danger (radars mobiles)

## Garde-fous permanents

- **Gratuit** : aucune dépendance payante, aucun serveur à héberger.
- **Légal** : zones de danger conformes R413-15, fonction coupée là où interdite.
- **Privé** : données de localisation et capteurs traitées sur l'appareil.
- **Play Store** : rester dans les catégories Android Auto validées (POI,
  puis média/messagerie en v2).
