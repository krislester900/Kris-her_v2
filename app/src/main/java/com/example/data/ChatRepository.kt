package com.example.data

import com.example.model.ConversationEntity
import com.example.model.MessageEntity
import com.example.model.MessageStatus
import com.example.model.MessageType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class ChatRepository(
    private val conversationDao: ConversationDao,
    private val messageDao: MessageDao
) {
    private val repositoryScope = CoroutineScope(Dispatchers.IO)

    val allConversations: Flow<List<ConversationEntity>> = conversationDao.getAllConversations()

    fun getConversation(id: Long): Flow<ConversationEntity?> =
        conversationDao.getConversationById(id)

    fun getMessages(conversationId: Long): Flow<List<MessageEntity>> =
        messageDao.getMessagesForConversation(conversationId)

    suspend fun insertMessage(message: MessageEntity): Long =
        messageDao.insertMessage(message)

    suspend fun seedInitialDataIfNeeded() {
        // Clear demo conversations so kris&her starts pristine
        val demoNames = listOf("Sophie Martin", "Alexandre Dubois", "Léa Chen", "Weekend à la montagne 🏔️", "Thomas Bernard", "Camille Roy")
        try {
            val currentConvs = conversationDao.getAllConversations().first()
            for (conv in currentConvs) {
                if (conv.title in demoNames) {
                    conversationDao.deleteConversation(conv.id)
                    messageDao.clearMessagesForConversation(conv.id)
                }
            }

            // Ensure "Moi" space exists as a pinned personal library/storage
            val hasMoi = currentConvs.any { it.title.equals("Moi", ignoreCase = true) }
            if (!hasMoi) {
                val moiConv = ConversationEntity(
                    id = 1L,
                    title = "Moi",
                    avatarColorHex = "#00F0FF",
                    avatarInitials = "MOI",
                    isPinned = true,
                    isFavorite = true,
                    lastMessage = "Bibliothèque personnelle • Stockez vos notes, photos et fichiers 📁",
                    statusText = "Bibliothèque personnelle (aucun message externe)",
                    safetyFingerprint = "ESPACE • PERSONNEL • KRIS • HER"
                )
                conversationDao.insertConversation(moiConv)
            }
        } catch (_: Exception) {}
    }

    suspend fun sendMessage(
        conversationId: Long,
        content: String,
        type: MessageType = MessageType.TEXT,
        audioDurationSec: Int = 0,
        mediaUri: String? = null,
        remoteId: String = ""
    ): Long {
        val now = System.currentTimeMillis()
        val newMessage = MessageEntity(
            conversationId = conversationId,
            senderId = "me",
            senderName = "Moi",
            content = content,
            type = type,
            audioDurationSec = audioDurationSec,
            mediaUri = mediaUri,
            timestamp = now,
            sentTimestamp = now,
            deliveredTimestamp = null,
            readTimestamp = null,
            status = MessageStatus.SENT,
            isEncrypted = false,
            remoteId = remoteId
        )

        val insertedId = messageDao.insertMessage(newMessage)

        val preview = if (type == MessageType.AUDIO) "🎤 Message vocal (${formatDuration(audioDurationSec)})" else content
        conversationDao.updateLastMessage(
            id = conversationId,
            lastMessage = preview,
            timestamp = now,
            unreadCount = 0
        )
        return insertedId
    }

    suspend fun markAsRead(conversationId: Long) {
        conversationDao.markAsRead(conversationId, System.currentTimeMillis())
    }

    suspend fun updateMessageStatus(messageId: Long, status: MessageStatus) {
        val now = System.currentTimeMillis()
        messageDao.updateMessageDelivered(messageId, status, now)
    }

    suspend fun updateMessageRead(messageId: Long, readTimestamp: Long) {
        messageDao.updateMessageRead(messageId, MessageStatus.READ, readTimestamp)
    }

    suspend fun togglePin(conversationId: Long) {
        conversationDao.togglePin(conversationId)
    }

    suspend fun deleteConversation(conversationId: Long) {
        messageDao.clearMessagesForConversation(conversationId)
        conversationDao.deleteConversation(conversationId)
    }

    suspend fun addReaction(messageId: Long, reactionEmoji: String) {
        messageDao.updateReaction(messageId, reactionEmoji)
    }

    suspend fun deleteMessage(messageId: Long) {
        messageDao.deleteMessage(messageId)
    }

    fun getConversationIdsWithMatchingMessages(query: String): Flow<List<Long>> {
        return messageDao.getConversationIdsWithMatchingMessages(query)
    }

    fun searchMessages(query: String): Flow<List<MessageEntity>> {
        return messageDao.searchMessages(query)
    }

    suspend fun setBlockedStatus(id: Long, isBlocked: Boolean) {
        conversationDao.updateBlockedStatus(id, isBlocked)
    }

    suspend fun setAcceptedStatus(id: Long, isAccepted: Boolean, isUnknownContact: Boolean = false) {
        conversationDao.updateAcceptedStatus(id, isAccepted, isUnknownContact)
    }

    suspend fun findConversationByPhone(phone: String): ConversationEntity? {
        val clean = phone.trim()
        if (clean.isBlank()) return null
        return conversationDao.findConversationByPhone(clean)
    }

    suspend fun findConversationByTitle(title: String): ConversationEntity? {
        val clean = title.trim()
        if (clean.isBlank()) return null
        return conversationDao.findConversationByTitle(clean)
    }

    suspend fun createNewConversation(
        name: String,
        initials: String,
        colorHex: String,
        statusText: String,
        phoneNumber: String = "",
        photoUrl: String = "",
        isUnknownContact: Boolean = false,
        isAccepted: Boolean = true,
        remoteId: String = "",
        peerUid: String = "",
        showUnregisteredNotice: Boolean = true
    ): Long {
        val isRealChat = remoteId.isNotBlank()
        val newConv = ConversationEntity(
            title = name,
            avatarColorHex = colorHex,
            avatarInitials = initials,
            isGroup = false,
            lastMessage = when {
                isRealChat -> "Discussion démarrée"
                isUnknownContact && !isAccepted -> "Contact non inscrit sur kris&her"
                else -> "Discussion démarrée"
            },
            lastMessageTimestamp = System.currentTimeMillis(),
            unreadCount = 0,
            isPinned = false,
            isFavorite = false,
            safetyFingerprint = generateSafetyNumber(),
            isOnline = false,
            statusText = statusText,
            isBlocked = false,
            isAccepted = isAccepted,
            isUnknownContact = isUnknownContact,
            phoneNumber = phoneNumber,
            photoUrl = photoUrl,
            remoteId = remoteId,
            peerUid = peerUid,
            lastReadAt = System.currentTimeMillis()
        )
        val id = conversationDao.insertConversation(newConv)

        // Plus de faux message d'accueil « envoyé » par l'ami.
        // Pour un numéro non inscrit, on explique honnêtement que rien ne sera délivré.
        if (!isRealChat && showUnregisteredNotice) {
            messageDao.insertMessage(
                MessageEntity(
                    conversationId = id,
                    senderId = "system",
                    senderName = "kris&her",
                    content = "Ce contact n'est pas encore inscrit sur kris&her : vos messages ne lui seront pas délivrés tant qu'il n'a pas de compte.",
                    type = MessageType.SYSTEM,
                    timestamp = System.currentTimeMillis(),
                    status = MessageStatus.READ
                )
            )
        }
        return id
    }

    // ---- Aides pour la synchronisation avec Firestore ----
    suspend fun getConversationOnce(id: Long): ConversationEntity? = conversationDao.getConversationOnce(id)

    suspend fun findConversationByRemoteId(remoteId: String): ConversationEntity? =
        conversationDao.findByRemoteId(remoteId)

    suspend fun insertConversation(conversation: ConversationEntity): Long =
        conversationDao.insertConversation(conversation)

    suspend fun updateConversation(conversation: ConversationEntity) =
        conversationDao.updateConversation(conversation)

    suspend fun incrementUnread(id: Long) = conversationDao.incrementUnread(id)

    suspend fun getMessageOnce(id: Long): MessageEntity? = messageDao.getMessageOnce(id)

    suspend fun findMessageByRemoteId(remoteId: String): MessageEntity? =
        messageDao.findByRemoteId(remoteId)

    suspend fun getRemoteIdsForConversation(conversationId: Long): List<String> =
        messageDao.getRemoteIdsForConversation(conversationId)

    suspend fun deleteMessageByRemoteId(conversationId: Long, remoteId: String) =
        messageDao.deleteByRemoteId(conversationId, remoteId)

    suspend fun updateMessageFromRemote(id: Long, reaction: String, status: MessageStatus, readTimestamp: Long?) =
        messageDao.updateFromRemote(id, reaction, status, readTimestamp)

    private fun generateSafetyNumber(): String {
        val chars = "0123456789ABCDEF"
        fun chunk() = (1..4).map { chars.random() }.joinToString("")
        return "${chunk()} • ${chunk()} • ${chunk()} • ${chunk()}"
    }

    private fun formatDuration(sec: Int): String {
        val m = sec / 60
        val s = sec % 60
        return "%d:%02d".format(m, s)
    }
}
