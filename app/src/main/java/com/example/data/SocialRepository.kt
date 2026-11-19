package com.example.data

import com.example.model.CustomStickerDoc
import com.example.model.StatusStoryItem
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.UUID

/**
 * Dépôt des stories (statuts) et des stickers personnalisés de l'utilisateur.
 *
 *   users/{uid}/stories/{storyId}
 *   users/{uid}/stickers/{stickerId}
 *
 * Extrait de FirestoreChatRepository (responsabilités « story » et « sticker »).
 */
class SocialRepository(
    db: com.google.firebase.firestore.FirebaseFirestore?
) : FirebaseRepositorySupport(db) {

    constructor(context: android.content.Context) : this(
        com.example.data.FirebaseRepositorySupport.resolveFirestore(context)
    )

    // ------------------------------------------------------------------
    //  Stories (statuts)
    // ------------------------------------------------------------------

    /** Publie une story texte (24 h côté serveur, pas de média). */
    suspend fun postStory(caption: String, emoji: String): Result<String> {
        val database = db ?: return Result.success(UUID.randomUUID().toString())
        val uid = currentUserId() ?: return Result.success(UUID.randomUUID().toString())
        val storyId = UUID.randomUUID().toString()
        val path = "users/$uid/stories/$storyId"
        return try {
            val storyData = mapOf(
                "id" to storyId,
                "userId" to uid,
                "authorName" to (auth?.currentUser?.displayName ?: "Moi"),
                "authorInitials" to "ME",
                "avatarColorHex" to "#FF643D",
                "caption" to caption,
                "emojiTag" to emoji,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            database.collection("users").document(uid).collection("stories").document(storyId).set(storyData).await()
            Result.success(storyId)
        } catch (e: Exception) {
            logFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    fun observeStories(): Flow<List<StatusStoryItem>> = flow {
        val database = db ?: return@flow
        val uid = currentUserId() ?: return@flow
        val path = "users/$uid/stories"
        try {
            emitAll(
                database.collection("users").document(uid).collection("stories")
                    .snapshots()
                    .map { snapshot ->
                        snapshot.toObjects(StatusStoryItem::class.java).sortedByDescending { it.createdAt }
                    }
                    .catch { error ->
                        if (error is Exception) logFirestoreError(error, OperationType.LIST, path)
                        emit(emptyList())
                    }
            )
        } catch (e: Exception) {
            logFirestoreError(e, OperationType.LIST, path)
        }
    }

    // ------------------------------------------------------------------
    //  Stickers personnalisés
    // ------------------------------------------------------------------

    fun observeCustomStickers(): Flow<List<CustomStickerDoc>> = flow {
        val database = db ?: return@flow
        val uid = currentUserId() ?: return@flow
        val path = "users/$uid/stickers"
        try {
            emitAll(
                database.collection("users").document(uid).collection("stickers")
                    .snapshots()
                    .map { snapshot ->
                        snapshot.toObjects(CustomStickerDoc::class.java).sortedByDescending { it.createdAt }
                    }
                    .catch { error ->
                        if (error is Exception) logFirestoreError(error, OperationType.LIST, path)
                        emit(emptyList())
                    }
            )
        } catch (e: Exception) {
            logFirestoreError(e, OperationType.LIST, path)
        }
    }

    suspend fun createCustomSticker(
        title: String,
        imageUri: String,
        emojiBadge: String = "✨",
        borderColorHex: String = "#FF643D",
        isVideo: Boolean = false,
        frameTimestampMs: Long = 0L
    ): Result<String> {
        val database = db ?: return Result.success("stk_local")
        val uid = currentUserId() ?: return Result.success("stk_local")
        val stickerId = "stk_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(6)}"
        val path = "users/$uid/stickers/$stickerId"
        return try {
            val authorName = auth?.currentUser?.displayName ?: "Kris"
            val stickerData = mapOf(
                "id" to stickerId,
                "userId" to uid,
                "title" to title.take(60),
                "author" to authorName,
                "imageUri" to imageUri,
                "emojiBadge" to emojiBadge,
                "borderColorHex" to borderColorHex,
                "isVideo" to isVideo,
                "frameTimestampMs" to frameTimestampMs,
                "createdAt" to FieldValue.serverTimestamp()
            )
            database.collection("users").document(uid).collection("stickers").document(stickerId).set(stickerData).await()
            Result.success(stickerId)
        } catch (e: Exception) {
            logFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteCustomSticker(stickerId: String): Result<Unit> {
        val database = db ?: return Result.success(Unit)
        val uid = currentUserId() ?: return Result.success(Unit)
        val path = "users/$uid/stickers/$stickerId"
        return try {
            database.collection("users").document(uid).collection("stickers").document(stickerId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            logFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }
}
