package com.example.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R

object NotificationHelper {
    private const val CHANNEL_ID = "kris_security_account_channel"
    private const val CHANNEL_NAME = "Sécurité & Compte kris&her"
    private const val NOTIFICATION_ID = 1001
    private const val MESSAGES_CHANNEL_ID = "kris_messages_channel"
    private const val MESSAGES_CHANNEL_NAME = "Messages kris&her"

    /** Vrai quand l'app est à l'écran : on n'affiche alors pas de notification (l'utilisateur voit déjà le message). */
    @Volatile
    var appInForeground: Boolean = false

    fun showMessageNotification(context: Context, chatId: String, title: String, body: String) {
        if (appInForeground) return
        try {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(MESSAGES_CHANNEL_ID, MESSAGES_CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH).apply {
                    description = "Nouveaux messages"
                    enableVibration(true)
                }
                manager.createNotificationChannel(channel)
            }
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                chatId.hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val notification = NotificationCompat.Builder(context, MESSAGES_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(NotificationCompat.BigTextStyle().bigText(body))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .build()
            // Une notification par discussion (la suivante remplace la précédente)
            manager.notify(chatId.hashCode(), notification)
        } catch (e: Exception) {
            android.util.Log.w("NotificationHelper", "Could not post message notification: ${e.message}")
        }
    }

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = "Notifications officielles de sécurité et confirmation de compte Google"
                enableLights(true)
                enableVibration(true)
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            notificationManager?.createNotificationChannel(channel)
        }
    }

    fun showAccountCreatedNotification(context: Context, googleEmail: String) {
        try {
            createNotificationChannel(context)

            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle("🎉 Compte kris&her créé avec succès")
                .setContentText("Un message de confirmation a été envoyé dans la boîte mail de votre compte Google ($googleEmail).")
                .setStyle(
                    NotificationCompat.BigTextStyle()
                        .bigText("Votre compte de messagerie a été créé avec succès et associé à votre compte Google ($googleEmail).\nUn message officiel de confirmation a été envoyé directement dans votre boîte mail.")
                )
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .build()

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            notificationManager?.notify(NOTIFICATION_ID, notification)
        } catch (e: Exception) {
            android.util.Log.w("NotificationHelper", "Could not post notification: ${e.message}")
        }
    }
}
