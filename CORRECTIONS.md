# Corrections apportées (v2)

## 1. Messagerie réellement partagée entre deux utilisateurs
Avant : chaque message était écrit uniquement dans `users/{moi}/conversations/...`, lisible seulement par son auteur. Rien n'arrivait à l'autre personne.
Maintenant :
- `chats/{chatId}` (chatId = les deux UID triés, joints par `_`) et `chats/{chatId}/messages/{id}`.
- Les deux membres lisent et écrivent dans les mêmes documents ; un tiers n'a aucun accès.
- Une discussion ouverte par quelqu'un d'autre apparaît chez le destinataire comme une demande à accepter ou bloquer.
- Accusés de lecture réels : le destinataire passe le message en READ quand il ouvre la discussion.
- Réactions synchronisées ; suppression d'un de MES messages = pour tout le monde ; suppression d'un message reçu = pour moi seulement.
- Supprimer une discussion la masque localement (elle revient si un nouveau message arrive).
- Base locale Room v3 -> v4 avec migration (les données locales, ex. « Moi », sont conservées).

## 2. Faux comportements retirés
- Plus de faux accusés « délivré/lu » (delay) ni de faux « en train d'écrire ».
- Plus de faux message d'accueil « envoyé » par un ami.
- Ouvrir une discussion ne marque plus MES messages comme lus.
- Conversation avec un numéro non inscrit : message système honnête (rien n'est délivré).
- Plus d'usurpation « Google Sécurité » : conversation renommée « Bienvenue sur kris&her ».

## 3. Fausses promesses de sécurité retirées
Les messages sont stockés en clair dans Firestore (connexion HTTPS + chiffrement serveur Google, rien de plus).
Tous les textes « chiffré de bout en bout / E2EE / Zero-Knowledge / chiffrement matériel » ont été remplacés par des formulations exactes.

## 4. Médias réellement envoyés — SANS plan Blaze (mode par défaut)
- Photos et notes vocales sont envoyés DIRECTEMENT dans le message Firestore (gratuit, Firebase Storage n'est pas nécessaire).
- Photos : réduites et recompressées automatiquement (jusqu'à 1024 px, JPEG) pour tenir sous ~330 Ko.
- Notes vocales : enregistrées en 16 kHz / 32 kbps (~4 Ko par seconde) => environ 1 min 20 maximum. Au-delà, un message système explique que le fichier est trop lourd.
- À la réception, le média est sauvegardé dans le téléphone (dossier privé de l'app) puis affiché ou lu normalement.
- Si l'envoi échoue, le message est retiré et un message système l'indique (plus d'échec silencieux).
- Limites : pas de gros fichiers ; l'historique d'une discussion est rechargé en entier à la première ouverture (les médias comptent dans le quota Firestore gratuit : 1 Go stocké, 10 Go/mois de téléchargement).
- Le bouton « Document » de l'app envoie seulement un texte factice (« Document kris&her.pdf ») : ce n'est pas un vrai fichier (non modifié).
- Plus tard, avec le plan Blaze : mettre `useCloudStorage = true` dans `ChatViewModel.kt` pour passer par Firebase Storage (déjà codé, règles dans `storage.rules`).

## 5. Code PIN réel
- Fini le code `1234`, le bouton « déverrouiller avec clé secrète » (qui tapait le code à votre place) et la fausse « clé de récupération ».
- L'utilisateur choisit son code à 4 chiffres (stocké salé + haché PBKDF2, jamais en clair). Changement possible dans l'écran Coffre.
- Le coffre est verrouillé dès le lancement si le PIN est activé (l'activation est maintenant mémorisée).
- Blocage temporaire à partir du 5e échec (30 s, 60 s, 2 min, 4 min, 8 min).
- Texte « 0 fuite cloud / stockage 100 % sur l'appareil » corrigé : les messages passent par Firebase.

## 6. Numéros de téléphone privés + recherche qui tient la charge
- Le numéro n'est plus dans le profil public `users/{uid}` : il est dans `users/{uid}/private/contact` (propriétaire seulement).
- Le profil public contient une empreinte SHA-256 (`phoneHash`) pour retrouver un contact, et `displayNameLower`.
- `searchUsers` : recherche par préfixe de nom (20 résultats max) ou par numéro complet ; plus de téléchargement de toute la collection.
- Contacts du téléphone : correspondance par empreinte, par paquets de 10.
- Les anciens profils sont migrés automatiquement au lancement (numéro public supprimé).
- Honnêteté : l'empreinte d'un numéro à 8 chiffres peut être retrouvée par force brute ; cela évite l'exposition directe, pas une attaque déterminée.

## 7. Notifications quand l'app est fermée (NÉCESSITE le plan Blaze)
- `FcmService` (Android) + `functions/index.js` (fonction `notifyNewMessage`, déclenchée à chaque nouveau message).
- Le jeton de l'appareil est stocké dans `users/{uid}/private/push` (lisible seulement par son propriétaire et par le serveur).
- Demande de la permission « notifications » (Android 13+) au lancement ; pas de notification quand l'app est au premier plan.

## 8. Compteur de non-lus exact
- Comptage côté serveur (agrégation) des messages reçus depuis votre dernière lecture ; +1 en secours si hors-ligne.
- Nécessite l'index `firestore.indexes.json`.

## 9. Règles Firestore (`firestore.rules`) — À DÉPLOYER
- Règles `chats` (membres uniquement, expéditeur imposé, contenu immuable, « lu » réservé au destinataire).
- `users` : plus de numéro en clair ; sous-collection `private` réservée au propriétaire.
- `mail` : non lisible par les clients ; on ne peut s'envoyer un e-mail qu'à soi-même.

## Déploiement SANS Blaze (dans l'ordre)
1. Console Firebase > Firestore > onglet Sécurité : coller `firestore.rules` et Publier.
2. Firestore > onglet Index > Composite : collection `messages`, champs `senderId` (croissant) puis `createdAt` (croissant), portée Collection.
3. Android Studio / build : Sync Gradle, puis lancer l'app.
Rien d'autre : Storage et les notifications serveur sont facultatifs et exigent Blaze.

## Ce qui reste hors de portée
- Le chiffrement de bout en bout n'existe toujours pas : les messages sont lisibles par Firebase/Google. Pour en avoir un vrai, il faut un protocole de clés (ex. Signal) : c'est un projet à part.
- Rien n'a pu être compilé ni exécuté dans cet environnement : syntaxe Kotlin et JS vérifiée seulement. Les tests (`firestore.test.js`, `ChatRepositoryRuleTest.kt`) sont adaptés mais non lancés. Faites un build et un test à deux téléphones avant de publier.
