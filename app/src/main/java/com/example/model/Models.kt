package com.example.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class MessageType {
    TEXT,
    AUDIO,
    IMAGE,
    DOCUMENT,
    SYSTEM,
    STICKER
}

enum class MessageStatus {
    SENDING,
    SENT,
    DELIVERED,
    READ
}

@Entity(tableName = "conversations")
data class ConversationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val avatarColorHex: String = "#FF643D",
    val avatarInitials: String = "",
    val isGroup: Boolean = false,
    val lastMessage: String = "",
    val lastMessageTimestamp: Long = System.currentTimeMillis(),
    val unreadCount: Int = 0,
    val isPinned: Boolean = false,
    val isFavorite: Boolean = false,
    val safetyFingerprint: String = "4A8F • 91B2 • C401 • 77DE",
    val isOnline: Boolean = true,
    val statusText: String = "Messagerie privée",
    val isEphemeralActive: Boolean = false,
    val isBlocked: Boolean = false,
    val isAccepted: Boolean = true,
    val isUnknownContact: Boolean = false,
    val phoneNumber: String = "",
    val photoUrl: String = "",
    // Identifiant partagé de la conversation dans Firestore (vide = conversation locale uniquement)
    val remoteId: String = "",
    // UID Firebase de l'interlocuteur (vide = conversation locale uniquement)
    val peerUid: String = "",
    // Dernière fois que j'ai vu cette discussion (sert à compter exactement les messages non lus)
    val lastReadAt: Long = 0L
)

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val conversationId: Long,
    val senderId: String, // "me" or friend's id
    val senderName: String,
    val content: String,
    val type: MessageType = MessageType.TEXT,
    val audioDurationSec: Int = 0,
    val mediaUri: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val sentTimestamp: Long = System.currentTimeMillis(),
    val deliveredTimestamp: Long? = null,
    val readTimestamp: Long? = null,
    val status: MessageStatus = MessageStatus.READ,
    val reactionEmoji: String = "",
    val isEncrypted: Boolean = false,
    // Identifiant du message dans Firestore (vide = message local uniquement)
    val remoteId: String = ""
)

data class StatusStory(
    val id: String,
    val authorName: String,
    val authorInitials: String,
    val avatarColorHex: String,
    val timeAgo: String,
    val caption: String,
    val isViewed: Boolean = false,
    val emojiTag: String = "✨"
)

data class CustomStickerItem(
    val id: String = "",
    val title: String = "",
    val author: String = "Kris",
    val imageUri: String = "",
    val emojiBadge: String = "✨",
    val borderColorHex: String = "#FF643D",
    val isVideo: Boolean = false,
    val frameTimestampMs: Long = 0L,
    val createdAt: Long = System.currentTimeMillis()
)
