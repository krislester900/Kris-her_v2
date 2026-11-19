package com.example.data

import com.example.model.UserProfile
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

/**
 * Dépôt des profils utilisateurs.
 *
 * Le numéro de téléphone n'est PLUS dans le profil public :
 *  - users/{uid}                   : displayName, displayNameLower, phoneHash, photo, statut...
 *  - users/{uid}/private/contact   : phoneNumber en clair, lisible par son propriétaire seulement
 *  - users/{uid}/private/push      : jeton FCM, lisible par le serveur d'envoi seulement
 *
 * Extrait de FirestoreChatRepository (responsabilité « utilisateur »).
 */
class UserProfileRepository(
    db: com.google.firebase.firestore.FirebaseFirestore?
) : FirebaseRepositorySupport(db) {

    constructor(context: android.content.Context) : this(
        com.example.data.FirebaseRepositorySupport.resolveFirestore(context)
    )

    fun phoneHashOf(raw: String): String = com.example.util.phoneHashOf(raw)

    fun observeUserProfile(userId: String? = null): Flow<UserProfile?> = flow {
        val database = db ?: return@flow
        val targetUid = userId ?: currentUserId() ?: return@flow
        val path = "users/$targetUid"
        try {
            emitAll(
                database.collection("users").document(targetUid)
                    .snapshots()
                    .map { snapshot -> if (snapshot.exists()) snapshot.toObject(UserProfile::class.java) else null }
                    .catch { error ->
                        if (error is Exception) logFirestoreError(error, OperationType.GET, path)
                        emit(null)
                    }
            )
        } catch (e: Exception) {
            logFirestoreError(e, OperationType.GET, path)
        }
    }

    suspend fun savePrivatePhone(phoneNumber: String): Result<Unit> {
        val database = db ?: return Result.success(Unit)
        val uid = currentUserId() ?: return Result.success(Unit)
        return try {
            database.collection("users").document(uid).collection("private").document("contact")
                .set(mapOf("phoneNumber" to phoneNumber.trim().take(32))).await()
            Result.success(Unit)
        } catch (e: Exception) {
            logFirestoreError(e, OperationType.WRITE, "users/$uid/private/contact")
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
            logFirestoreError(e, OperationType.GET, "users/$uid/private/contact")
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
            logFirestoreError(e, OperationType.WRITE, "users/$uid/private/push")
            Result.failure(e)
        }
    }

    /** Anciens profils : retire le numéro public, ajoute l'empreinte et le nom en minuscules. */
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
            logFirestoreError(e, OperationType.WRITE, path)
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
                if (photoUrl.isNotBlank()) profile["photoUrl"] = photoUrl
                docRef.set(profile).await()
                if (phoneNumber.isNotBlank()) savePrivatePhone(phoneNumber)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            logFirestoreError(e, OperationType.WRITE, path)
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
            logFirestoreError(e, OperationType.WRITE, path)
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
            logFirestoreError(e, OperationType.LIST, "users")
            Result.failure(e)
        }
    }

    /** Retrouve les contacts du téléphone inscrits sur kris&her, par empreinte de numéro. */
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
            logFirestoreError(e, OperationType.LIST, "users")
            Result.failure(e)
        }
    }
}
