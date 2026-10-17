# Pournotes

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
