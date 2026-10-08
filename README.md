# KnockDetector — Détecteur de cliquetis pour moteur 2 temps

Application Android qui utilise le microphone du téléphone pour détecter le cliquetis
d'un moteur deux temps, afficher le spectre en temps réel et compter les événements.

## Fonctionnalités
- Analyse FFT en temps réel
- Spectre 0–12 kHz avec bande de cliquetis en rouge
- Compteur de cliquetis avec Reset
- Curseurs : sensibilité, fréquence basse, fréquence haute, anti-rebond

## Compilation
Via GitHub Actions (onglet Actions → Artifacts).

## Formule de fréquence
f ≈ (3 × 340) / (2 × π × alésage en m)
b.loret cortex
