package com.example.push

import com.example.data.FirestoreChatRepository
import com.example.util.NotificationHelper
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Reçoit les notifications de nouveaux messages (envoyées par la fonction serveur « notifyNewMessage »)
 * même quand l'application est fermée.
 */
class FcmService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        // Si personne n'est connecté, l'enregistrement est ignoré ; le ViewModel le refera à la connexion.
        CoroutineScope(Dispatchers.IO).launch {
            try {
                FirestoreChatRepository(applicationContext).savePushToken(token)
            } catch (e: Exception) {
                android.util.Log.w("FcmService", "onNewToken warning: ${e.message}")
            }
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        val data = message.data
        val title = data["title"]?.takeIf { it.isNotBlank() } ?: return
        NotificationHelper.showMessageNotification(
            context = applicationContext,
            chatId = data["chatId"].orEmpty(),
            title = title,
            body = data["body"].orEmpty()
        )
    }
}
