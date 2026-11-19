package com.example.data

import android.content.Context
import com.example.model.Chat
import com.example.model.ChatMessage
import com.example.model.ChatMessagesSnapshot
import com.example.model.CustomStickerDoc
import com.example.model.StatusStoryItem
import com.example.model.UserProfile
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow

/**
 * FAÇADE DE COMPATIBILITÉ.
 *
 * Le dépôt a été découpé en classes spécialisées (P1-2) :
 *
 *  - [ChatRepositoryFirestore]  : conversations et messages partagés (chats/{chatId})
 *  - [UserProfileRepository]    : profils, recherche, contacts privés, jeton FCM (users/{uid})
 *  - [MediaRepository]          : médias inline base64 et envoi Firebase Storage
 *  - [SocialRepository]         : stories et stickers personnalisés
 *
 * Cette classe ne contient plus de logique métier : elle délègue à ces dépôts
 * afin de préserver la compatibilité des appelants (MainActivity, ChatViewModel).
 * La logique commune (db, auth, uid courant) vit dans [FirebaseRepositorySupport].
 *
 * Il est recommandé de migrer progressivement les appelants vers les dépôts
 * spécialisés, puis de supprimer cette façade.
 */
class FirestoreChatRepository private constructor(
    private val db: FirebaseFirestore?,
    private val appContext: Context?
) {

    constructor(context: Context) : this(
        FirebaseRepositorySupport.resolveFirestore(context),
        context.applicationContext
    )

    constructor(db: FirebaseFirestore?) : this(db, null)

    // ------------------------------------------------------------------
    //  Dépôts spécialisés (créés paresseusement, sans état partagé)
    // ------------------------------------------------------------------

    private val chatRepository by lazy { ChatRepositoryFirestore(db) }
    private val userRepository by lazy { UserProfileRepository(db) }
    private val socialRepository by lazy { SocialRepository(db) }

    // ------------------------------------------------------------------
    //  Identité (délégué à FirebaseRepositorySupport)
    // ------------------------------------------------------------------

    /** UID de l'utilisateur connecté, ou null. */
    fun currentUserId(): String? = chatRepository.currentUserId()

    /** Renvoie l'UID courant ou lève une exception. */
    fun requireUserId(): String = chatRepository.requireUserId()

    /** Base Firestore sous-jacente (peut être null si indisponible). */
    fun getFirestore(): FirebaseFirestore? = chatRepository.getFirestore()

    // ------------------------------------------------------------------
    //  Conversations et messages (délégué à ChatRepositoryFirestore)
    // ------------------------------------------------------------------

    fun chatIdFor(uidA: String, uidB: String): String = chatRepository.chatIdFor(uidA, uidB)

    fun observeChats(): Flow<List<Chat>> = chatRepository.observeChats()

    fun observeChatMessages(chatId: String): Flow<ChatMessagesSnapshot> =
        chatRepository.observeChatMessages(chatId)

    suspend fun countUnreadFrom(chatId: String, peerUid: String, sinceMillis: Long): Int? =
        chatRepository.countUnreadFrom(chatId, peerUid, sinceMillis)

    suspend fun openChat(peerUid: String): Result<String> = chatRepository.openChat(peerUid)

    suspend fun sendChatMessage(
        chatId: String,
        messageId: String,
        content: String,
        type: String,
        audioDurationSec: Long,
        mediaUri: String?,
        senderName: String
    ): Result<Unit> = chatRepository.sendChatMessage(
        chatId, messageId, content, type, audioDurationSec, mediaUri, senderName
    )

    suspend fun setChatReaction(chatId: String, messageId: String, emoji: String): Result<Unit> =
        chatRepository.setChatReaction(chatId, messageId, emoji)

    suspend fun markChatMessagesRead(chatId: String, messageIds: List<String>): Result<Unit> =
        chatRepository.markChatMessagesRead(chatId, messageIds)

    suspend fun deleteChatMessage(chatId: String, messageId: String): Result<Unit> =
        chatRepository.deleteChatMessage(chatId, messageId)

    // ------------------------------------------------------------------
    //  Profil utilisateur (délégué à UserProfileRepository)
    // ------------------------------------------------------------------

    fun phoneHashOf(raw: String): String = userRepository.phoneHashOf(raw)

    fun observeUserProfile(userId: String? = null): Flow<UserProfile?> =
        userRepository.observeUserProfile(userId)

    suspend fun savePrivatePhone(phoneNumber: String): Result<Unit> =
        userRepository.savePrivatePhone(phoneNumber)

    suspend fun loadPrivatePhone(): String? = userRepository.loadPrivatePhone()

    suspend fun savePushToken(token: String): Result<Unit> = userRepository.savePushToken(token)

    suspend fun migrateProfilePrivacy(localPhone: String): Result<Unit> =
        userRepository.migrateProfilePrivacy(localPhone)

    suspend fun ensureUserProfile(
        displayName: String,
        photoUrl: String = "",
        phoneNumber: String = ""
    ): Result<Unit> = userRepository.ensureUserProfile(displayName, photoUrl, phoneNumber)

    suspend fun updateUserProfile(
        displayName: String,
        phoneNumber: String = "",
        photoUrl: String = "",
        statusText: String = "Messagerie privée",
        avatarColorHex: String = "#FF643D"
    ): Result<Unit> = userRepository.updateUserProfile(
        displayName, phoneNumber, photoUrl, statusText, avatarColorHex
    )

    suspend fun searchUsers(query: String): Result<List<UserProfile>> =
        userRepository.searchUsers(query)

    suspend fun searchUsersByPhoneNumbers(phoneNumbers: List<String>): Result<List<UserProfile>> =
        userRepository.searchUsersByPhoneNumbers(phoneNumbers)

    // ------------------------------------------------------------------
    //  Médias (délégué à MediaRepository)
    // ------------------------------------------------------------------

    suspend fun prepareInlineMedia(
        context: Context,
        localUri: String,
        type: String
    ): Result<String> = MediaRepository(context).prepareInlineMedia(localUri, type)

    suspend fun uploadMediaToStorage(
        chatId: String,
        messageId: String,
        localUri: String,
        type: String
    ): Result<String> {
        val context = appContext ?: return Result.failure(
            IllegalStateException("Contexte indisponible pour l'envoi de média")
        )
        return MediaRepository(context).uploadToStorage(chatId, messageId, localUri, type)
    }

    /**
     * Envoie un média de discussion vers Firebase Storage (optionnel, nécessite le plan Blaze).
     * Alias historique conservé pour les appelants existants.
     */
    suspend fun uploadChatMedia(
        context: Context,
        chatId: String,
        messageId: String,
        localUri: String,
        type: String
    ): Result<String> = MediaRepository(context).uploadToStorage(chatId, messageId, localUri, type)

    /** Récupère un profil utilisateur une seule fois (sans écoute temps réel). */
    suspend fun getUserProfile(uid: String): UserProfile? {
        val database = db ?: return null
        return try {
            database.collection("users").document(uid).get().await().toObject(UserProfile::class.java)
        } catch (e: Exception) {
            userRepository.let { handleErrorLogging(e, "users/$uid") }
            null
        }
    }

    private fun handleErrorLogging(e: Exception, path: String) {
        android.util.Log.w("FirestoreChatRepo", "GET $path impossible: ${e.message}")
    }

    // ------------------------------------------------------------------
    //  Stories et stickers (délégué à SocialRepository)
    // ------------------------------------------------------------------

    suspend fun postStory(caption: String, emoji: String): Result<String> =
        socialRepository.postStory(caption, emoji)

    fun observeStories(): Flow<List<StatusStoryItem>> = socialRepository.observeStories()

    fun observeCustomStickers(): Flow<List<CustomStickerDoc>> =
        socialRepository.observeCustomStickers()

    suspend fun createCustomSticker(
        title: String,
        imageUri: String,
        emojiBadge: String = "✨",
        borderColorHex: String = "#FF643D",
        isVideo: Boolean = false,
        frameTimestampMs: Long = 0L
    ): Result<String> = socialRepository.createCustomSticker(
        title, imageUri, emojiBadge, borderColorHex, isVideo, frameTimestampMs
    )

    suspend fun deleteCustomSticker(stickerId: String): Result<Unit> =
        socialRepository.deleteCustomSticker(stickerId)
}

/** Plafond d'un média envoyé dans un message : base64 ajoute ~33 %, on reste sous les 500 000 caractères des règles. */
const val MAX_INLINE_MEDIA_BYTES = 340_000

class MediaTooLargeException : Exception("Média trop lourd pour être envoyé dans un message")
