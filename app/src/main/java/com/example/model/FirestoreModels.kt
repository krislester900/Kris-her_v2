package com.example.model

import com.google.firebase.Timestamp

data class UserProfile(
    val userId: String = "",
    val displayName: String = "",
    // Jamais présent dans le profil public : renseigné seulement localement pour MON profil
    val phoneNumber: String = "",
    val displayNameLower: String = "",
    val phoneHash: String = "",
    val photoUrl: String = "",
    val avatarColorHex: String = "#FF643D",
    val statusText: String = "Messagerie privée",
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
)

data class Conversation(
    val id: String = "",
    val userId: String = "",
    val title: String = "",
    val avatarColorHex: String = "#FF643D",
    val avatarInitials: String = "",
    val isGroup: Boolean = false,
    val lastMessage: String = "",
    val lastMessageTimestamp: Timestamp? = null,
    val unreadCount: Long = 0L,
    val isPinned: Boolean = false,
    val isFavorite: Boolean = false,
    val safetyFingerprint: String = "4A8F • 91B2 • C401 • 77DE",
    val isOnline: Boolean = true,
    val statusText: String = "Messagerie privée",
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
)

data class ChatMessage(
    val id: String = "",
    val conversationId: String = "",
    val userId: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val content: String = "",
    val type: String = "TEXT",
    val audioDurationSec: Long = 0L,
    val mediaUri: String? = null,
    val reactionEmoji: String = "",
    val isEncrypted: Boolean = false,
    val status: String = "SENT",
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
)

data class StatusStoryItem(
    val id: String = "",
    val userId: String = "",
    val authorName: String = "",
    val authorInitials: String = "",
    val avatarColorHex: String = "#FF643D",
    val caption: String = "",
    val emojiTag: String = "✨",
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
)

data class CustomStickerDoc(
    val id: String = "",
    val userId: String = "",
    val title: String = "",
    val author: String = "Kris",
    val imageUri: String = "",
    val emojiBadge: String = "✨",
    val borderColorHex: String = "#FF643D",
    val isVideo: Boolean = false,
    val frameTimestampMs: Long = 0L,
    val createdAt: Timestamp? = null
)


/** Discussion partagée entre deux utilisateurs : chats/{chatId} (chatId = les deux UID triés, joints par « _ »). */
data class Chat(
    val id: String = "",
    val members: List<String> = emptyList(),
    val lastMessage: String = "",
    val lastMessageTimestamp: Timestamp? = null,
    val lastSenderId: String = "",
    val createdBy: String = "",
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
)

/** Instantané des messages d'une discussion (fromCache = true tant que le serveur n'a pas répondu). */
data class ChatMessagesSnapshot(
    val messages: List<ChatMessage>,
    val fromCache: Boolean
)
