# Cloud Functions — kris&her

## Fonctions disponibles

### `notifyNewMessage`

Envoie une notification push FCM au destinataire quand un message est créé dans `chats/{chatId}/messages`.

- **Déclencheur** : `onDocumentCreated` sur `chats/{chatId}/messages/{messageId}`
- **Base Firestore** : configurable via `FIRESTORE_DATABASE_ID` (défaut : `ai-studio-android-remixkri-5e20fb90-aca8-4a6c-9427-e49c2f1a40ba`)
- **Région** : configurable via `FIRESTORE_REGION` (défaut : `us-central1`)

## Prérequis

- **Plan Blaze** requis pour Firebase Cloud Messaging (FCM).
- Node.js 18+ et npm installés.
- Firebase CLI installé (`npm install -g firebase-tools`).

## Installation

```bash
cd functions
npm install
```

## Configuration

Copiez `.env.example` vers `.env` et ajustez les valeurs si nécessaire :

```bash
cp .env.example .env
```

Les variables d'environnement peuvent aussi être définies directement lors du déploiement :

```bash
firebase functions:config:set \
  firestore.database_id="votre-base-id" \
  firestore.region="europe-west1"
```

## Déploiement

```bash
# Depuis la racine du projet
firebase deploy --only functions

# Ou depuis le dossier functions
firebase deploy --only functions:notifyNewMessage
```

## Test local

Utilisez l'émulateur Firestore et l'émulateur Functions :

```bash
firebase emulators:start --only functions,firestore
```

## Variables d'environnement

| Variable | Description | Défaut |
|----------|-------------|--------|
| `FIRESTORE_DATABASE_ID` | ID de la base Firestore à écouter | `ai-studio-android-remixkri-5e20fb90-aca8-4a6c-9427-e49c2f1a40ba` |
| `FIRESTORE_REGION` | Région de la base Firestore | `us-central1` |

## Nettoyage des jetons FCM

Les jetons FCM périmés (app désinstallée) sont automatiquement supprimés de `users/{uid}/private/push` lors de l'envoi (code `messaging/registration-token-not-registered`).
