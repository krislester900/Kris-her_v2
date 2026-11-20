package com.example.data

import android.content.Context
import com.example.R
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore

/**
 * Support partagé par tous les dépôts Firestore spécialisés.
 *
 * Extrait de FirestoreChatRepository pour éviter la duplication de la résolution
 * de la base Firestore, de l'accès à l'utilisateur courant et de la gestion d'erreurs.
 */
open class FirebaseRepositorySupport(
    protected val db: FirebaseFirestore?
) {

    protected constructor(context: Context) : this(resolveFirestore(context))

    /** Authentification Firebase ; null si Firebase n'est pas initialisé. */
    protected val auth: FirebaseAuth?
        get() = try { Firebase.auth } catch (e: Exception) { null }

    /** UID de l'utilisateur connecté, ou null. */
    fun currentUserId(): String? {
        return try { auth?.currentUser?.uid } catch (_: Exception) { null }
    }

    /** Renvoie l'UID courant ou lève une exception. */
    fun requireUserId(): String {
        return currentUserId() ?: throw IllegalStateException("User must be signed in")
    }

    /** Base Firestore sous-jacente (peut être null si indisponible). */
    fun getFirestore(): FirebaseFirestore? {
        return db
    }

    /**
     * Journalise une erreur Firestore de façon centralisée.
     * Délègue à la fonction top-level [handleFirestoreError] du même paquet.
     */
    protected fun logFirestoreError(e: Exception, operationType: OperationType, path: String) {
        com.example.data.handleFirestoreError(e, operationType, path)
    }

    companion object {
        /**
         * Résout l'instance Firestore à utiliser.
         * Si `R.string.firestore_database_id` désigne une base nommée (non "(default)"),
         * on l'utilise ; sinon on retombe sur l'instance par défaut.
         */
        internal fun resolveFirestore(context: Context): FirebaseFirestore? {
            return try {
                val dbId = try {
                    context.applicationContext.getString(R.string.firestore_database_id)
                } catch (_: Exception) { null }

                if (!dbId.isNullOrBlank() && dbId != "(default)") {
                    try {
                        FirebaseFirestore.getInstance(dbId)
                    } catch (e: Exception) {
                        android.util.Log.w(
                            "FirestoreRepo",
                            "Could not get named firestore database '$dbId', falling back to default instance: ${e.message}"
                        )
                        FirebaseFirestore.getInstance()
                    }
                } else {
                    FirebaseFirestore.getInstance()
                }
            } catch (e: Exception) {
                android.util.Log.w("FirestoreRepo", "Firestore unavailable: ${e.message}")
                null
            }
        }
    }
}
