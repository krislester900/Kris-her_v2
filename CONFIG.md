# Configuration requise — kris&her v2

## 1. google-services.json (obligatoire)

Le fichier `app/google-services.json` est **absent du dépôt** par mesure de sécurité. Il contient les identifiants de votre projet Firebase et ne doit jamais être commité.

### Comment l'obtenir

1. Ouvrez la [console Firebase](https://console.firebase.google.com/).
2. Sélectionnez votre projet (ou créez-en un).
3. Allez dans **Paramètres du projet > Général > Vos applications**.
4. Ajoutez une application Android si elle n'existe pas :
   - Nom du package : `com.aistudio.entechat.gchaor`
   - Certificat SHA-1 : empreinte de votre `debug.keystore` (voir ci-dessous)
5. Téléchargez `google-services.json`.
6. Placez-le à la racine du module : `app/google-services.json`.

### Empreinte SHA-1 du debug.keystore

Le `debug.keystore` fourni avec le dépôt est signé avec la clé par défaut Android. Son empreinte SHA-1 est :

```
C6:F7:2C:0E:F0:C4:E5:9F:10:A4:D6:19:7A:4E:90:31:34:D0:49:80
```

Déclarez cette empreinte dans la console Firebase pour que l'APK de debug puisse communiquer avec Firebase.

## 2. Clés API et secrets (obligatoire en release)

- `AccountEmailService.kt` ne doit **pas** contenir de clé API hardcodée. Consultez `P0-3` dans l'historique des commits pour la procédure de migration vers `local.properties` / Secrets Gradle.
- Le fichier `.env` est gitignorée. Copiez `.env.example` vers `.env` et remplissez les valeurs réelles avant tout build release.

## 3. Keystore de release

- `debug.keystore` est fourni à la racine (déjà présent) pour les builds de debug.
- Pour les builds de release, générez votre propre `my-upload-key.jks` (voir `P0-2`) et déclarez les variables d'environnement `KEYSTORE_PATH`, `STORE_PASSWORD`, `KEY_PASSWORD`.

## 4. Firestore et Storage

- Déployez les règles de sécurité : `firestore.rules` et `storage.rules` (console Firebase > Firestore > Règles).
- Créez l'index composite `messages(senderId ASC, createdAt ASC)` (console Firebase > Firestore > Index).
- La fonction Cloud `notifyNewMessage` nécessite le **plan Blaze** (payant) pour l'envoi de notifications FCM.

## 4. Keystore de debug et de release

### Debug (`debug.keystore`)

Le fichier `debug.keystore` est référencé par `build.gradle.kts` pour signer les APK de debug. S'il est absent, générez-le avec le script fourni :

```bash
./scripts/generate-debug-keystore.sh
```

Ce script crée un keystore avec l'alias `androiddebugkey` et les mots de passe `android` / `android`, identique à celui d'Android Studio.

### Release (`my-upload-key.jks`)

Pour les builds de release, générez votre propre keystore :

```bash
keytool -genkeypair \
  -alias upload \
  -keyalg RSA \
  -keysize 2048 \
  -validity 10000 \
  -keystore my-upload-key.jks \
  -storepass <votre-mot-de-passe> \
  -keypass <votre-mot-de-passe> \
  -dname "CN=kris&her,O=kris&her,C=FR"
```

Déclarez ensuite les variables d'environnement avant le build release :

```bash
export KEYSTORE_PATH="$PWD/my-upload-key.jks"
export STORE_PASSWORD=<votre-mot-de-passe>
export KEY_PASSWORD=<votre-mot-de-passe>
```

`my-upload-key.jks` **ne doit jamais être commité** dans le dépôt (gitignoré).

## 5. Build de l'APK

```bash
# Debug
./gradlew assembleDebug

# Release (nécessite my-upload-key.jks + variables d'environnement)
./gradlew assembleRelease
```

Le workflow GitHub Actions (`.github/workflows/build.yml`) doit être créé manuellement sur GitHub pour automatiser le build.
