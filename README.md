<p align="center">
  <img src="docs/icon.png" width="128" alt="Icône de Pournotes">
</p>

<h1 align="center">Pournotes</h1>

<p align="center">
  <a href="https://github.com/myvoow/pournotes/releases/latest"><img src="https://img.shields.io/github/v/release/myvoow/pournotes?style=for-the-badge&color=00897B&label=version" alt="Dernière version"></a>
  <img src="https://img.shields.io/badge/Android-8%2B-00897B?style=for-the-badge&logo=android&logoColor=white" alt="Android 8+">
  <img src="https://img.shields.io/badge/Kotlin-Compose-FFCA28?style=for-the-badge&logo=kotlin&logoColor=black" alt="Kotlin et Jetpack Compose">
  <img src="https://img.shields.io/badge/Material%203-Expressive-FFCA28?style=for-the-badge&logo=materialdesign&logoColor=black" alt="Material 3 Expressive">
  <a href="https://github.com/myvoow/pournotes/actions/workflows/build.yml"><img src="https://img.shields.io/github/actions/workflow/status/myvoow/pournotes/build.yml?style=for-the-badge&label=build" alt="Build"></a>
</p>

<p align="center">
  <a href="https://github.com/myvoow/pournotes/releases/latest"><img src="https://img.shields.io/badge/T%C3%A9l%C3%A9charger%20l'APK-00695C?style=for-the-badge&logo=android&logoColor=white" height="44" alt="Télécharger l'APK"></a>
</p>

Un client PRONOTE pour Android, fait pour mon usage perso.

J'aimais pas l'interface de l'appli officielle : c'est une page web dans une appli, ça se sent, et ça ressemble à rien sur un Android récent. Moi je voulais du Material 3, le vrai, avec les couleurs du fond d'écran, des grosses cartes bien arrondies et des animations qui rebondissent un peu. Du coup j'ai refait l'appli de zéro en Kotlin et Jetpack Compose, avec Material 3 Expressive.

Ce qu'elle fait pour l'instant :

- connexion avec identifiant et mot de passe, ou par QR code (celui que PRONOTE génère dans Mes données)
- un accueil avec le prochain cours, la moyenne, les devoirs à cocher et les dernières notes
- l'emploi du temps complet, semaine par semaine
- un écran de paramètres pour changer à peu près tout : thème, couleurs, coins, police, taille du texte, sections affichées

Pas encore géré : la connexion par ENT, les comptes parents et la double authentification avec code PIN.

La partie qui parle au serveur PRONOTE est adaptée de [pronotepy](https://github.com/bain3/pronotepy). L'appli n'a aucun lien avec Index Éducation.

Pour compiler :

```bash
./gradlew assembleDebug
```

L'APK sort dans `app/build/outputs/apk/debug/`.
