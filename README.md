# kris&her v2

Messagerie privée noir et blanc pour deux utilisateurs, avec style *new-school* et audio interactif.

## 🎯 Objectif

Une messagerie **strictement privée entre deux personnes** (pas de groupes, pas de serveur centralisé de messages), utilisant **Firebase Firestore** comme backend temps réel. Aucune donnée n'est stockée sur un serveur tiers hors Firebase.

## ✨ Fonctionnalités

| Catégorie | Détails |
|-----------|---------|
| **Conversations 1-to-1** | Discussions partagées `chats/{uidA_uidB}` — les deux membres lisent/écrivent les mêmes documents |
| **Messages** | Texte, notes vocales (≤1 min 20), photos, stickers, réactions emoji |
| **Médias** | Envoi **inline base64** dans le message (≤330 Ko) — **aucun plan Blaze requis** |
| **Audio** | Enregistrement/lecture notes vocales avec visualiseur d'amplitude temps réel |
| **Stickers** | 6 présets + création personnalisée (image + badge emoji + bordure) |
| **Stories** | Statuts texte 24h (collection `users/{uid}/stories`) |
| **Coffre (Vault)** | Code PIN 4 chiffres (PBKDF2-HMAC-SHA256, 60k itérations), verrouillage progressif |
| **Présence** | Statut *en ligne* dynamique (heartbeat 10s + onDisconnect Firestore, seuil 30s) |
| **Sécurité** | Numéros masqués (empreinte SHA-256 8 derniers chiffres), pas de téléphone en clair |
| **Notifications** | FCM via Cloud Function `notifyNewMessage` (plan Blaze requis) |
| **Email bienvenue** | Via extension Firebase *Trigger Email* (collection `mail`) |

## 🏗️ Architecture

```
app/
├── data/
│   ├── FirebaseRepositorySupport.kt      # Base partagée (db, auth, uid, erreurs)
│   ├── ChatRepositoryFirestore.kt        # Conversations + messages (chats/{chatId})
│   ├── UserProfileRepository.kt          # Profils, recherche, contacts privés, FCM
│   ├── MediaRepository.kt                # Médias inline base64 + Firebase Storage
│   ├── SocialRepository.kt               # Stories + stickers personnalisés
│   ├── PresenceManager.kt                # Présence en ligne (heartbeat + onDisconnect)
│   ├── FirestoreChatRepository.kt        # Façade de compatibilité (délégation)
│   ├── ChatRepository.kt                 # Couche locale Room (conversations/messages)
│   └── AppDatabase.kt                    # Room DB (v4, migration 3→4)
├── viewmodel/
│   ├── ChatViewModel.kt                  # Orchestration principale (conversations, messages)
│   ├── AudioManagerViewModel.kt          # Enregistrement/lecture audio, effets sonores
│   ├── StickerViewModel.kt               # Stickers personnalisés + présets
│   ├── NotificationViewModel.kt          # Bannière compte, email bienvenue
│   └── VaultSecurityManager.kt           # PIN coffre (PBKDF2, verrouillage progressif)
├── ui/
│   ├── screens/                          # AuthScreen, ChatListScreen, ChatRoomScreen, etc.
│   └── components/                       # EnteComponents (ActiveCallDialog), VaultLockScreen, etc.
└── security/
    └── VaultSecurityManager.kt           # Logique PIN extraite du ViewModel
```

**Principes :**
- **Découplage** : chaque responsabilité a son dépôt/ViewModel dédié
- **Local-first** : Room pour l'offline, Firestore pour la sync temps réel
- **Pas de god objects** : `FirestoreChatRepository` divisé en 5 dépôts spécialisés
- **Tests** : règles Firestore testées (`firestore.test.js`)

## 🔧 Prérequis

- **Android Studio** Koala (2024.1+) ou supérieur
- **JDK 17+** (pour `keytool`, builds)
- **Projet Firebase** (plan Spark gratuit suffit pour tout sauf FCM/Storage)
- **google-services.json** (voir Configuration)

## ⚙️ Configuration

### 1. google-services.json (obligatoire)

Le fichier `app/google-services.json` **n'est pas dans le dépôt** (sécurité).

1. Console Firebase > Paramètres du projet > Général > Vos applications
2. Ajouter une application Android :
   - Package : `com.aistudio.entechat.gchaor`
   - SHA-1 : `C6:F7:2C:0E:F0:C4:E5:9F:10:A4:D6:19:7A:4E:90:31:34:D0:49:80` (debug.keystore fourni)
3. Télécharger `google-services.json` → placer dans `app/`

### 2. Clés API (variables d'environnement)

Créer `local.properties` à la racine (gitignoré) :

```properties
# Clé API Google Identity Toolkit (pour email bienvenue - optionnel)
IDENTITY_TOOLKIT_API_KEY=VOTRE_CLE_ICI

# Mot de passe keystore release (si build release)
STORE_PASSWORD=VOTRE_MOT_DE_PASSE
KEY_PASSWORD=VOTRE_MOT_DE_PASSE
```

### 3. Keystore debug (fourni)

Un `debug.keystore` est à la racine (SHA-1 ci-dessus). Si absent :

```bash
./scripts/generate-debug-keystore.sh
```

### 4. Keystore release (à générer)

```bash
keytool -genkeypair -alias upload -keyalg RSA -keysize 2048 -validity 10000 \
  -keystore my-upload-key.jks -storepass <MOT_DE_PASSE> -keypass <MOT_DE_PASSE> \
  -dname "CN=kris&her,O=kris&her,C=FR"
```

### 5. Firestore & Storage

1. Console Firebase > Firestore > Règles → coller `firestore.rules` → Publier
2. Firestore > Index > Composite → ajouter `messages(senderId ASC, createdAt ASC)` (collection)
3. Storage > Règles → coller `storage.rules` → Publier

### 6. Cloud Functions (optionnel, pour notifications)

```bash
cd functions
npm install
# Configurer .env depuis .env.example
firebase deploy --only functions
```

Nécessite **plan Blaze** (payant) pour FCM.

## 🚀 Build

```bash
# Debug (utilise debug.keystore)
./gradlew assembleDebug

# Release (nécessite my-upload-key.jks + variables d'env)
./gradlew assembleRelease
```

APK dans `app/build/outputs/apk/`.

## 🧪 Tests

```bash
# Tests unitaires règles Firestore
cd functions && npm install && node firestore.test.js

# Tests Android (nécessite émulateur/device)
./gradlew test
./gradlew connectedAndroidTest
```

## 📁 Règles Firestore (résumé)

| Collection | Accès |
|------------|-------|
| `chats/{chatId}` | Membres seulement (lecture/écriture), créateur imposé |
| `chats/{chatId}/messages` | Membres (lecture), expéditeur imposé (écriture), contenu immuable |
| `users/{uid}` | Propriétaire (lecture/écriture), pas de téléphone en clair |
| `users/{uid}/private/contact` | Propriétaire seulement (téléphone en clair) |
| `users/{uid}/private/push` | Propriétaire + serveur FCM (jeton) |
| `users/{uid}/stories` | Propriétaire (écriture), amis (lecture) |
| `users/{uid}/stickers` | Propriétaire (CRUD) |
| `mail/{mailId}` | Personne (lecture), utilisateur connecté (création vers son propre email) |

## 📦 Dépendances principales

| Lib | Version | Usage |
|-----|---------|-------|
| Kotlin | 2.2.10 | Langage |
| Compose BOM | 2024.09.00 | UI |
| Room | 2.7.0 | Base locale |
| Firebase BOM | 34.17.0 | Auth, Firestore, Messaging, Storage |
| Credential Manager | 1.5.0 | Google Sign-In |
| Coil | 2.7.0 | Images |
| Moshi | 1.15.2 | JSON parsing |

## 🔒 Sécurité & Confidentialité

- **Pas de E2EE** : messages en clair dans Firestore (HTTPS + chiffrement serveur Google uniquement)
- **Numéros** : jamais en clair dans `users/` — empreinte SHA-256 (8 derniers chiffres, ~26 bits d'entropie)
- **PIN coffre** : PBKDF2-HMAC-SHA256, 60k itérations, sel 16 octets, verrouillage progressif
- **Médias** : inline base64 (≤330 Ko) ou Firebase Storage (Blaze)
- **Aucune télémétrie** : pas d'analytics, pas de crashlytics

## ⚠️ Limitations connues

| Limitation | Statut |
|------------|--------|
| Pas d'appels audio/vidéo (WebRTC) | Icônes retirées pour éviter fausses attentes |
| Pas de groupes | Par conception (1-to-1 uniquement) |
| Pas de E2EE | Messages lisibles par Firebase/Google |
| Notifications FCM | Nécessitent plan Blaze |
| Stockage médias >330 Ko | Nécessite plan Blaze + Storage |
| Email bienvenue | Nécessite extension Trigger Email (Blaze) |

## 📋 TODO / Roadmap

- [ ] Tests CI/CD GitHub Actions
- [ ] Authentification email/mot de passe (alternative Google)
- [ ] Recherche full-text messages (Algolia/Meilisearch)
- [ ] Sauvegarde/restauration locale (Room export)
- [ ] Thèmes personnalisés (au-delà noir/blanc)
- [ ] Support tablettes/pliables

## 📄 Licence

Projet privé — usage personnel uniquement.

---

**Généré automatiquement** — voir `CORRECTIONS.md` pour l'historique des corrections v2.
