package com.example.data

import com.example.model.Chat
import com.example.model.ChatMessage
import com.example.model.ChatMessagesSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Dépôt des discussions partagées : chats/{chatId} et chats/{chatId}/messages/{messageId}.
 *
 * Les deux interlocuteurs lisent et écrivent dans les MÊMES documents.
 * chatId = les deux UID triés, joints par « _ ».
 *
 * Extrait de FirestoreChatRepository (responsabilité « conversation + message »).
 */
class ChatRepositoryFirestore(
    db: com.google.firebase.firestore.FirebaseFirestore?
) : FirebaseRepositorySupport(db) {

    constructor(context: android.content.Context) : this(
        com.example.data.FirebaseRepositorySupport.resolveFirestore(context)
    )

    // ------------------------------------------------------------------
    //  Conversations
    // ------------------------------------------------------------------

    /** Identifiant déterministe d'une discussion à deux : les deux UID triés, joints par « _ ». */
    fun chatIdFor(uidA: String, uidB: String): String =
        listOf(uidA, uidB).sorted().joinToString("_")

    /** Toutes les discussions dont je suis membre (temps réel). */
    fun observeChats(): Flow<List<Chat>> = flow {
        val database = db ?: return@flow
        val uid = currentUserId() ?: return@flow
        val path = "chats"
        try {
            emitAll(
                database.collection("chats")
                    .whereArrayContains("members", uid)
                    .snapshots()
                    .map { snapshot -> snapshot.toObjects(Chat::class.java) }
                    .catch { error ->
                        if (error is Exception) logFirestoreError(error, OperationType.LIST, path)
                        emit(emptyList())
                    }
            )
        } catch (e: Exception) {
            logFirestoreError(e, OperationType.LIST, path)
        }
    }

    /** Nombre exact de messages de [peerUid] reçus après [sinceMillis] (agrégation serveur). */
    suspend fun countUnreadFrom(chatId: String, peerUid: String, sinceMillis: Long): Int? {
        val database = db ?: return null
        currentUserId() ?: return null
        return try {
            val snap = database.collection("chats").document(chatId).collection("messages")
                .whereEqualTo("senderId", peerUid)
                .whereGreaterThan("createdAt", com.google.firebase.Timestamp(sinceMillis / 1000, (sinceMillis % 1000 * 1_000_000).toInt()))
                .count()
                .get(com.google.firebase.firestore.AggregateSource.SERVER)
                .await()
            snap.count.toInt()
        } catch (e: Exception) {
            logFirestoreError(e, OperationType.GET, "chats/$chatId/messages")
            null
        }
    }

    /** Crée (si absente) la discussion partagée et renvoie son chatId. */
    suspend fun openChat(peerUid: String): Result<String> {
        val database = db ?: return Result.failure(IllegalStateException("Firestore indisponible"))
        val uid = currentUserId() ?: return Result.failure(IllegalStateException("Utilisateur non connecté"))
        if (peerUid.isBlank() || peerUid == uid) {
            return Result.failure(IllegalArgumentException("Interlocuteur invalide"))
        }
        val chatId = chatIdFor(uid, peerUid)
        val path = "chats/$chatId"
        return try {
            val ref = database.collection("chats").document(chatId)
            val existing = try { ref.get().await() } catch (_: Exception) { null }
            if (existing?.exists() != true) {
                val data = mapOf(
                    "id" to chatId,
                    "members" to listOf(uid, peerUid).sorted(),
                    "createdBy" to uid,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                // Hors-ligne, l'écriture reste en file d'attente : on n'attend pas indéfiniment.
                withTimeoutOrNull(8000) { ref.set(data).await() }
            }
            Result.success(chatId)
        } catch (e: Exception) {
            logFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    // ------------------------------------------------------------------
    //  Messages
    // ------------------------------------------------------------------

    /** Messages d'une discussion (temps réel), du plus ancien au plus récent. */
    fun observeChatMessages(chatId: String): Flow<ChatMessagesSnapshot> = flow {
        val database = db ?: return@flow
        currentUserId() ?: return@flow
        val path = "chats/$chatId/messages"
        try {
            emitAll(
                database.collection("chats").document(chatId).collection("messages")
                    .snapshots()
                    .map { snapshot ->
                        ChatMessagesSnapshot(
                            messages = snapshot.documents
                                .mapNotNull { doc -> doc.toObject(ChatMessage::class.java)?.copy(id = doc.id) }
                                .sortedBy { it.createdAt?.toDate()?.time ?: Long.MAX_VALUE },
                            fromCache = snapshot.metadata.isFromCache
                        )
                    }
                    .catch { error ->
                        if (error is Exception) logFirestoreError(error, OperationType.LIST, path)
                    }
            )
        } catch (e: Exception) {
            logFirestoreError(e, OperationType.LIST, path)
        }
    }

    suspend fun sendChatMessage(
        chatId: String,
        messageId: String,
        content: String,
        type: String,
        audioDurationSec: Long,
        mediaUri: String?,
        senderName: String
    ): Result<Unit> {
        val database = db ?: return Result.failure(IllegalStateException("Firestore indisponible"))
        val uid = currentUserId() ?: return Result.failure(IllegalStateException("Utilisateur non connecté"))
        val path = "chats/$chatId/messages/$messageId"
        return try {
            val chatRef = database.collection("chats").document(chatId)
            val msgRef = chatRef.collection("messages").document(messageId)
            val msgData = mutableMapOf<String, Any>(
                "id" to messageId,
                "conversationId" to chatId,
                "senderId" to uid,
                "senderName" to senderName.trim().take(50).ifBlank { "Moi" },
                "content" to content.take(5000),
                "type" to type,
                "audioDurationSec" to audioDurationSec,
                "reactionEmoji" to "",
                "status" to "SENT",
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (!mediaUri.isNullOrEmpty()) msgData["mediaUri"] = mediaUri

            val preview = when (type) {
                "AUDIO" -> "🎤 Message vocal (${audioDurationSec}s)"
                "IMAGE" -> "📷 Photo"
                else -> content
            }.replace("\n", " ").take(200)

            val batch = database.batch()
            batch.set(msgRef, msgData)
            batch.update(
                chatRef,
                mapOf(
                    "lastMessage" to preview,
                    "lastMessageTimestamp" to FieldValue.serverTimestamp(),
                    "lastSenderId" to uid,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()
            Result.success(Unit)
        } catch (e: Exception) {
            logFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun setChatReaction(chatId: String, messageId: String, emoji: String): Result<Unit> {
        val database = db ?: return Result.success(Unit)
        currentUserId() ?: return Result.success(Unit)
        val path = "chats/$chatId/messages/$messageId"
        return try {
            database.collection("chats").document(chatId).collection("messages").document(messageId)
                .update(mapOf("reactionEmoji" to emoji, "updatedAt" to FieldValue.serverTimestamp()))
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            logFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    /** Appelé par le DESTINATAIRE quand il voit des messages : passe leur statut à READ. */
    suspend fun markChatMessagesRead(chatId: String, messageIds: List<String>): Result<Unit> {
        val database = db ?: return Result.success(Unit)
        currentUserId() ?: return Result.success(Unit)
        if (messageIds.isEmpty()) return Result.success(Unit)
        val path = "chats/$chatId/messages"
        return try {
            val batch = database.batch()
            messageIds.take(400).forEach { id ->
                val ref = database.collection("chats").document(chatId).collection("messages").document(id)
                batch.update(ref, mapOf("status" to "READ", "updatedAt" to FieldValue.serverTimestamp()))
            }
            batch.commit().await()
            Result.success(Unit)
        } catch (e: Exception) {
            logFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteChatMessage(chatId: String, messageId: String): Result<Unit> {
        val database = db ?: return Result.success(Unit)
        currentUserId() ?: return Result.success(Unit)
        val path = "chats/$chatId/messages/$messageId"
        return try {
            database.collection("chats").document(chatId).collection("messages").document(messageId)
                .delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            logFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }
}
