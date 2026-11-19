#!/usr/bin/env bash
# Génère un debug.keystore pour les builds de debug Android.
# Utilise keytool (fourni avec le JDK, installé avec Android Studio).
#
# Usage : ./scripts/generate-debug-keystore.sh
#
# Ce keystore est identique à celui généré par Android Studio par défaut :
#   - alias : androiddebugkey
#   - mot de passe keystore : android
#   - mot de passe clé : android
#   - DN : CN=Android Debug,O=Android,C=US

set -euo pipefail

KEYSTORE_PATH="${1:-debug.keystore}"

if [[ -f "$KEYSTORE_PATH" ]]; then
  echo "Le fichier $KEYSTORE_PATH existe déjà. Annulation."
  exit 0
fi

if ! command -v keytool >/dev/null 2>&1; then
  echo "ERREUR : keytool est introuvable. Installez un JDK (ex. openjdk-17) ou Android Studio."
  exit 1
fi

keytool -genkeypair \
  -alias androiddebugkey \
  -keyalg RSA \
  -keysize 2048 \
  -validity 10000 \
  -keystore "$KEYSTORE_PATH" \
  -storepass android \
  -keypass android \
  -dname "CN=Android Debug,O=Android,C=US"

echo "Keystore généré : $KEYSTORE_PATH"
echo "Empreinte SHA-1 :"
keytool -list -v -keystore "$KEYSTORE_PATH" -storepass android \
  | grep -A1 "SHA1:" | tail -1
