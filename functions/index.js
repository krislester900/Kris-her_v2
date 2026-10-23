// Envoie une notification push au destinataire quand un message est créé dans chats/{chatId}/messages.
// Déploiement : cd functions && npm install && firebase deploy --only functions
// (nécessite le plan Blaze de Firebase)
const { onDocumentCreated } = require("firebase-functions/v2/firestore");
const { initializeApp } = require("firebase-admin/app");
const { getFirestore } = require("firebase-admin/firestore");
const { getMessaging } = require("firebase-admin/messaging");

// Base Firestore utilisée par l'app (identique à firebase.json / strings.xml)
const DATABASE_ID = "ai-studio-android-remixkri-5e20fb90-aca8-4a6c-9427-e49c2f1a40ba";
// IMPORTANT : doit être la région de VOTRE base Firestore (Console Firebase > Firestore > Données > emplacement).
const REGION = "us-central1";

initializeApp();
const db = getFirestore(DATABASE_ID);

function previewOf(msg) {
  switch (msg.type) {
    case "AUDIO": return "🎤 Message vocal";
    case "IMAGE": return "📷 Photo";
    case "STICKER": return "Sticker";
    case "DOCUMENT": return "📎 Document";
    case "SYSTEM": return "";
    default: return String(msg.content || "").slice(0, 200);
  }
}

exports.notifyNewMessage = onDocumentCreated(
  { document: "chats/{chatId}/messages/{messageId}", database: DATABASE_ID, region: REGION },
  async (event) => {
    const msg = event.data && event.data.data();
    if (!msg || !msg.senderId) return;
    const body = previewOf(msg);
    if (!body) return;

    const chatSnap = await db.doc(`chats/${event.params.chatId}`).get();
    const members = (chatSnap.exists && chatSnap.data().members) || [];
    const recipients = members.filter((uid) => uid !== msg.senderId);

    await Promise.all(recipients.map(async (uid) => {
      const tokenRef = db.doc(`users/${uid}/private/push`);
      const tokenSnap = await tokenRef.get();
      const token = tokenSnap.exists && tokenSnap.data().token;
      if (!token) return;
      try {
        await getMessaging().send({
          token,
          data: {
            title: String(msg.senderName || "kris&her"),
            body,
            chatId: event.params.chatId,
          },
          android: { priority: "high" },
        });
      } catch (err) {
        // Jeton périmé (app désinstallée) : on le supprime
        if (err.code === "messaging/registration-token-not-registered") {
          await tokenRef.delete();
        } else {
          console.error("Envoi FCM impossible pour", uid, err.message);
        }
      }
    }));
  }
);
