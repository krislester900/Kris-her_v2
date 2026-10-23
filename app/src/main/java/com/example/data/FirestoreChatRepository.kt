package com.example.data

import android.content.Context
import com.example.R
import com.example.model.Chat
import com.example.model.ChatMessage
import com.example.model.ChatMessagesSnapshot
import com.example.model.CustomStickerDoc
import com.example.model.StatusStoryItem
import com.example.model.UserProfile
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull
import java.util.UUID

class FirestoreChatRepository(
    private val db: FirebaseFirestore?
) {
    constructor(context: Context) : this(
        resolveFirestore(context)
    )

    companion object {
        private fun resolveFirestore(context: Context): FirebaseFirestore? {
            return try {
                val dbId = try {
                    context.applicationContext.getString(R.string.firestore_database_id)
                } catch (_: Exception) { null }

                if (!dbId.isNullOrBlank() && dbId != "(default)") {
                    try {
                        FirebaseFirestore.getInstance(dbId)
                    } catch (e: Exception) {
                        android.util.Log.w("FirestoreChatRepo", "Could not get named firestore database '$dbId', falling back to default instance: ${e.message}")
                        FirebaseFirestore.getInstance()
                    }
                } else {
                    FirebaseFirestore.getInstance()
                }
            } catch (e: Exception) {
                android.util.Log.e("FirestoreChatRepo", "Failed to obtain FirebaseFirestore instance: ${e.message}", e)
                try {
                    FirebaseFirestore.getInstance()
                } catch (_: Exception) {
                    null
                }
            }
        }
    }

    private val auth: com.google.firebase.auth.FirebaseAuth?
        get() = try { Firebase.auth } catch (e: Exception) { null }

    fun currentUserId(): String? {
        return try { auth?.currentUser?.uid } catch (_: Exception) { null }
    }

    fun requireUserId(): String {
        return currentUserId() ?: throw IllegalStateException("User must be signed in")
    }

    fun getFirestore(): FirebaseFirestore? {
        return db
    }

    // ------------------------------------------------------------------
    //  Discussions partagées : chats/{chatId} et chats/{chatId}/messages/{messageId}
    //  Les deux interlocuteurs lisent et écrivent dans les MÊMES documents.
    // ------------------------------------------------------------------

    /** Identifiant déterministe d'une discussion à deux : les deux UID triés, joints par « _ ». */
    fun chatIdFor(uidA: String, uidB: String): String =
        listOf(uidA, uidB).sorted().joinToString("_")

    /** Toutes les discussions dont je suis membre (temps réel). */
    fun observeChats(): Flow<List<Chat>> = flow {
        val database = db ?: return@flow
        val uid = currentUserId() ?: return@flow
        try {
            emitAll(
                database.collection("chats")
                    .whereArrayContains("members", uid)
                    .snapshots()
                    .map { snapshot -> snapshot.toObjects(Chat::class.java) }
                    .catch { error ->
                        if (error is Exception) handleFirestoreError(error, OperationType.LIST, "chats")
                        emit(emptyList())
                    }
            )
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, "chats")
        }
    }

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
                        if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                    }
            )
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, path)
        }
    }

    /** Nombre exact de messages de [peerUid] reçus après [sinceMillis] (agrégation côté serveur, sans les télécharger). */
    suspend fun countUnreadFrom(chatId: String, peerUid: String, sinceMillis: Long): Int? {
        val database = db ?: return null
        currentUserId() ?: return null
        return try {
            val snap = database.collection("chats").document(chatId).collection("messages")
                .whereEqualTo("senderId", peerUid)
                .whereGreaterThan("createdAt", com.google.firebase.Timestamp(java.util.Date(sinceMillis)))
                .count()
                .get(com.google.firebase.firestore.AggregateSource.SERVER)
                .await()
            snap.count.toInt()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, "chats/$chatId/messages")
            null
        }
    }

    suspend fun getUserProfile(uid: String): UserProfile? {
        val database = db ?: return null
        return try {
            database.collection("users").document(uid).get().await().toObject(UserProfile::class.java)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, "users/$uid")
            null
        }
    }

    /** Crée la discussion avec [peerUid] si elle n'existe pas encore et renvoie son identifiant partagé. */
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
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    /**
     * Prépare un média pour l'envoyer DIRECTEMENT dans le message Firestore (adresse « data: » en base64),
     * sans Firebase Storage (donc sans plan Blaze). Plafond : [MAX_INLINE_MEDIA_BYTES] octets.
     * Photos : réduites et recompressées automatiquement. Vocaux : ~1 min 20 max (4 Ko/s).
     */
    suspend fun prepareInlineMedia(
        context: android.content.Context,
        localUri: String,
        type: String
    ): Result<String> = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        try {
            val resolver = context.contentResolver
            val sourceUri: android.net.Uri = when {
                localUri.startsWith("content://") || localUri.startsWith("file://") -> android.net.Uri.parse(localUri)
                else -> android.net.Uri.fromFile(java.io.File(localUri))
            }
            var mime = resolver.getType(sourceUri)
                ?: when (localUri.substringAfterLast('.', "").lowercase()) {
                    "m4a", "mp4", "aac" -> "audio/mp4"
                    "wav" -> "audio/wav"
                    "mp3" -> "audio/mpeg"
                    "jpg", "jpeg" -> "image/jpeg"
                    "png" -> "image/png"
                    "webp" -> "image/webp"
                    "pdf" -> "application/pdf"
                    else -> "application/octet-stream"
                }

            val bytes: ByteArray
            if (type == "IMAGE" || type == "STICKER") {
                var result: ByteArray? = null
                for (maxSide in listOf(1024, 800, 640, 480)) {
                    val bitmap = decodeBitmapForSending(resolver, sourceUri, maxSide) ?: continue
                    val longest = maxOf(bitmap.width, bitmap.height).toFloat()
                    val scale = maxSide / longest
                    val scaled = if (scale < 1f) {
                        android.graphics.Bitmap.createScaledBitmap(
                            bitmap,
                            (bitmap.width * scale).toInt().coerceAtLeast(1),
                            (bitmap.height * scale).toInt().coerceAtLeast(1),
                            true
                        )
                    } else bitmap
                    for (quality in listOf(80, 65, 50)) {
                        val out = java.io.ByteArrayOutputStream()
                        scaled.compress(android.graphics.Bitmap.CompressFormat.JPEG, quality, out)
                        if (out.size() <= MAX_INLINE_MEDIA_BYTES) {
                            result = out.toByteArray()
                            break
                        }
                    }
                    if (scaled !== bitmap) scaled.recycle()
                    bitmap.recycle()
                    if (result != null) break
                }
                bytes = result ?: throw MediaTooLargeException()
                mime = "image/jpeg"
            } else {
                val raw = resolver.openInputStream(sourceUri)?.use { it.readBytes() }
                    ?: throw java.io.IOException("Fichier illisible")
                if (raw.size > MAX_INLINE_MEDIA_BYTES) throw MediaTooLargeException()
                bytes = raw
            }
            Result.success("data:$mime;base64," + android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP))
        } catch (e: Exception) {
            android.util.Log.w("FirestoreChatRepository", "prepareInlineMedia warning: ${e.message}")
            Result.failure(e)
        }
    }

    private fun decodeBitmapForSending(
        resolver: android.content.ContentResolver,
        uri: android.net.Uri,
        maxSide: Int
    ): android.graphics.Bitmap? {
        val bounds = android.graphics.BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { android.graphics.BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        var sample = 1
        while (bounds.outWidth / sample > maxSide * 2 || bounds.outHeight / sample > maxSide * 2) sample *= 2
        val opts = android.graphics.BitmapFactory.Options().apply { inSampleSize = sample }
        return resolver.openInputStream(uri)?.use { android.graphics.BitmapFactory.decodeStream(it, null, opts) }
    }

    /**
     * Téléverse un média local (photo, vocal, document, sticker) dans Firebase Storage
     * et renvoie son adresse de téléchargement https (lisible par l'autre personne).
     */
    suspend fun uploadChatMedia(
        context: android.content.Context,
        chatId: String,
        messageId: String,
        localUri: String,
        type: String
    ): Result<String> {
        currentUserId() ?: return Result.failure(IllegalStateException("Utilisateur non connecté"))
        val path = "chats/$chatId/media/$messageId"
        var tempFile: java.io.File? = null
        return try {
            val resolver = context.contentResolver
            val sourceUri: android.net.Uri = when {
                localUri.startsWith("content://") || localUri.startsWith("file://") -> android.net.Uri.parse(localUri)
                else -> android.net.Uri.fromFile(java.io.File(localUri))
            }

            var uploadUri = sourceUri
            var mime = resolver.getType(sourceUri)
                ?: when (localUri.substringAfterLast('.', "").lowercase()) {
                    "m4a", "mp4", "aac" -> "audio/mp4"
                    "wav" -> "audio/wav"
                    "mp3" -> "audio/mpeg"
                    "jpg", "jpeg" -> "image/jpeg"
                    "png" -> "image/png"
                    "webp" -> "image/webp"
                    "pdf" -> "application/pdf"
                    else -> "application/octet-stream"
                }

            // Photos : réduites (1280 px max, JPEG 80 %) pour un envoi rapide
            if (type == "IMAGE" || type == "STICKER") {
                try {
                    val opts = android.graphics.BitmapFactory.Options().apply { inJustDecodeBounds = true }
                    resolver.openInputStream(sourceUri)?.use { android.graphics.BitmapFactory.decodeStream(it, null, opts) }
                    var sample = 1
                    while (opts.outWidth / sample > 1600 || opts.outHeight / sample > 1600) sample *= 2
                    val decodeOpts = android.graphics.BitmapFactory.Options().apply { inSampleSize = sample }
                    val bitmap = resolver.openInputStream(sourceUri)?.use {
                        android.graphics.BitmapFactory.decodeStream(it, null, decodeOpts)
                    }
                    if (bitmap != null) {
                        val out = java.io.File.createTempFile("upload_", ".jpg", context.cacheDir)
                        out.outputStream().use { bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 80, it) }
                        bitmap.recycle()
                        tempFile = out
                        uploadUri = android.net.Uri.fromFile(out)
                        mime = "image/jpeg"
                    }
                } catch (_: Exception) {
                    // on envoie l'original si la réduction échoue
                }
            }

            val ref = com.google.firebase.storage.FirebaseStorage.getInstance()
                .reference.child("chats/$chatId/media/$messageId")
            val metadata = com.google.firebase.storage.StorageMetadata.Builder().setContentType(mime).build()
            ref.putFile(uploadUri, metadata).await()
            val url = ref.downloadUrl.await().toString()
            Result.success(url)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        } finally {
            tempFile?.delete()
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
            handleFirestoreError(e, OperationType.CREATE, path)
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
            handleFirestoreError(e, OperationType.UPDATE, path)
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
            handleFirestoreError(e, OperationType.UPDATE, path)
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
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    fun observeUserProfile(userId: String? = null): Flow<UserProfile?> = flow {
        val database = db ?: return@flow
        val targetUid = userId ?: currentUserId() ?: return@flow
        val path = "users/$targetUid"
        try {
            emitAll(
                database.collection("users").document(targetUid)
                    .snapshots()
                    .map { snapshot ->
                        if (snapshot.exists()) {
                            snapshot.toObject(UserProfile::class.java)
                        } else {
                            null
                        }
                    }
                    .catch { error ->
                        if (error is Exception) handleFirestoreError(error, OperationType.GET, path)
                        emit(null)
                    }
            )
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, path)
        }
    }

    // ------------------------------------------------------------------
    //  Profils : le numéro de téléphone n'est PLUS dans le profil public.
    //  - users/{uid}            : displayName, displayNameLower, phoneHash (empreinte), photo, statut...
    //  - users/{uid}/private/contact : phoneNumber en clair, lisible par son propriétaire seulement
    // ------------------------------------------------------------------

    fun phoneHashOf(raw: String): String = com.example.util.phoneHashOf(raw)

    suspend fun savePrivatePhone(phoneNumber: String): Result<Unit> {
        val database = db ?: return Result.success(Unit)
        val uid = currentUserId() ?: return Result.success(Unit)
        return try {
            database.collection("users").document(uid).collection("private").document("contact")
                .set(mapOf("phoneNumber" to phoneNumber.trim().take(32))).await()
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "users/$uid/private/contact")
            Result.failure(e)
        }
    }

    suspend fun loadPrivatePhone(): String? {
        val database = db ?: return null
        val uid = currentUserId() ?: return null
        return try {
            database.collection("users").document(uid).collection("private").document("contact")
                .get().await().getString("phoneNumber")
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, "users/$uid/private/contact")
            null
        }
    }

    /** Enregistre le jeton de notification (FCM) de cet appareil ; lu seulement par le serveur d'envoi. */
    suspend fun savePushToken(token: String): Result<Unit> {
        val database = db ?: return Result.success(Unit)
        val uid = currentUserId() ?: return Result.success(Unit)
        return try {
            database.collection("users").document(uid).collection("private").document("push")
                .set(mapOf("token" to token, "updatedAt" to FieldValue.serverTimestamp())).await()
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "users/$uid/private/push")
            Result.failure(e)
        }
    }

    /** Anciens profils : retire le numéro public, ajoute l'empreinte et le nom en minuscules. À appeler au démarrage. */
    suspend fun migrateProfilePrivacy(localPhone: String): Result<Unit> {
        val database = db ?: return Result.success(Unit)
        val uid = currentUserId() ?: return Result.success(Unit)
        val path = "users/$uid"
        return try {
            val docRef = database.collection("users").document(uid)
            val doc = docRef.get().await()
            if (!doc.exists()) return Result.success(Unit)
            val phone = localPhone.ifBlank { doc.getString("phoneNumber").orEmpty() }
            val hash = phoneHashOf(phone)
            val needsMigration = doc.contains("phoneNumber") ||
                !doc.contains("displayNameLower") ||
                (hash.isNotBlank() && doc.getString("phoneHash") != hash)
            if (needsMigration) {
                val updates = mutableMapOf<String, Any>(
                    "displayNameLower" to doc.getString("displayName").orEmpty().lowercase().take(50),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                if (doc.contains("phoneNumber")) updates["phoneNumber"] = FieldValue.delete()
                if (hash.isNotBlank()) updates["phoneHash"] = hash
                docRef.update(updates).await()
                if (phone.isNotBlank()) savePrivatePhone(phone)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun ensureUserProfile(displayName: String, photoUrl: String = "", phoneNumber: String = ""): Result<Unit> {
        val database = db ?: return Result.success(Unit)
        val uid = currentUserId() ?: return Result.success(Unit)
        val path = "users/$uid"
        return try {
            val docRef = database.collection("users").document(uid)
            val doc = docRef.get().await()
            if (!doc.exists()) {
                val name = displayName.ifBlank { "Kris" }.take(50)
                val profile = mutableMapOf<String, Any>(
                    "userId" to uid,
                    "displayName" to name,
                    "displayNameLower" to name.lowercase(),
                    "avatarColorHex" to "#FF643D",
                    "statusText" to "Messagerie privée",
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                phoneHashOf(phoneNumber).takeIf { it.isNotBlank() }?.let { profile["phoneHash"] = it }
                if (photoUrl.isNotBlank()) {
                    profile["photoUrl"] = photoUrl
                }
                docRef.set(profile).await()
                if (phoneNumber.isNotBlank()) savePrivatePhone(phoneNumber)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun updateUserProfile(
        displayName: String,
        phoneNumber: String = "",
        photoUrl: String = "",
        statusText: String = "Messagerie privée",
        avatarColorHex: String = "#FF643D"
    ): Result<Unit> {
        val database = db ?: return Result.success(Unit)
        val uid = currentUserId() ?: return Result.success(Unit)
        val path = "users/$uid"
        return try {
            val docRef = database.collection("users").document(uid)
            val doc = docRef.get().await()
            val name = displayName.ifBlank { "Kris" }.take(50)
            val hash = phoneHashOf(phoneNumber)
            if (!doc.exists()) {
                val profile = mutableMapOf<String, Any>(
                    "userId" to uid,
                    "displayName" to name,
                    "displayNameLower" to name.lowercase(),
                    "photoUrl" to photoUrl,
                    "avatarColorHex" to avatarColorHex,
                    "statusText" to statusText,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                if (hash.isNotBlank()) profile["phoneHash"] = hash
                docRef.set(profile).await()
            } else {
                val updates = mutableMapOf<String, Any>(
                    "displayName" to name,
                    "displayNameLower" to name.lowercase(),
                    "photoUrl" to photoUrl,
                    "statusText" to statusText,
                    "avatarColorHex" to avatarColorHex,
                    "phoneHash" to hash,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                // Anciens profils : on retire le numéro public au passage
                if (doc.contains("phoneNumber")) updates["phoneNumber"] = FieldValue.delete()
                docRef.update(updates).await()
            }
            savePrivatePhone(phoneNumber)
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    /** Recherche par préfixe de nom, ou par numéro complet (comparé par empreinte). 20 résultats maximum. */
    suspend fun searchUsers(query: String): Result<List<UserProfile>> {
        val q = query.trim()
        if (q.isBlank()) return Result.success(emptyList())
        val database = db ?: return Result.success(emptyList())
        val myUid = currentUserId()
        return try {
            val results = LinkedHashMap<String, UserProfile>()
            val digitsOnly = q.filter { it.isDigit() }
            val looksLikePhone = digitsOnly.length >= 8 && digitsOnly.length >= q.length - 4

            if (looksLikePhone) {
                val snap = database.collection("users")
                    .whereEqualTo("phoneHash", phoneHashOf(q))
                    .limit(20).get().await()
                snap.toObjects(UserProfile::class.java).forEach { results[it.userId] = it }
            } else {
                val prefix = q.removePrefix("@").trim().lowercase()
                if (prefix.isNotBlank()) {
                    val snap = database.collection("users")
                        .orderBy("displayNameLower")
                        .startAt(prefix)
                        .endAt(prefix + "\uf8ff")
                        .limit(20).get().await()
                    snap.toObjects(UserProfile::class.java).forEach { results[it.userId] = it }
                }
            }
            Result.success(results.values.filter { it.userId.isNotBlank() && it.userId != myUid })
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, "users")
            Result.failure(e)
        }
    }

    /** Retrouve les contacts du téléphone inscrits sur kris&her, par empreinte de numéro (jamais le numéro en clair). */
    suspend fun searchUsersByPhoneNumbers(phoneNumbers: List<String>): Result<List<UserProfile>> {
        if (phoneNumbers.isEmpty()) return Result.success(emptyList())
        val database = db ?: return Result.success(emptyList())
        val myUid = currentUserId()
        return try {
            val hashes = phoneNumbers.map { phoneHashOf(it) }.filter { it.isNotBlank() }.distinct().take(500)
            val found = LinkedHashMap<String, UserProfile>()
            for (chunk in hashes.chunked(10)) {
                val snap = database.collection("users").whereIn("phoneHash", chunk).get().await()
                snap.toObjects(UserProfile::class.java).forEach { found[it.userId] = it }
            }
            Result.success(found.values.filter { it.userId.isNotBlank() && it.userId != myUid })
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, "users")
            Result.failure(e)
        }
    }

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
            handleFirestoreError(e, OperationType.CREATE, path)
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
                        if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                        emit(emptyList())
                    }
            )
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, path)
        }
    }

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
                        if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                        emit(emptyList())
                    }
            )
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, path)
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
            handleFirestoreError(e, OperationType.CREATE, path)
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
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }
}

/** Plafond d'un média envoyé dans un message : base64 ajoute ~33 %, on reste sous les 500 000 caractères des règles. */
const val MAX_INLINE_MEDIA_BYTES = 340_000

class MediaTooLargeException : Exception("Média trop lourd pour être envoyé dans un message")
