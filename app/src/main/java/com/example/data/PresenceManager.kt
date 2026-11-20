package com.example.data

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Gestionnaire de présence en ligne (online/offline) via Firestore.
 *
 * Utilise la fonctionnalité onDisconnect de Firestore pour marquer l'utilisateur
 * comme hors-ligne quand la connexion est perdue, et un heartbeat pour mettre
 * à jour le timestamp "lastActive" périodiquement.
 *
 * Un utilisateur est considéré "en ligne" si son dernier heartbeat date de
 * moins de ONLINE_THRESHOLD_MS (défaut 30 secondes).
 */
class PresenceManager(
    private val db: FirebaseFirestore?,
    private val scope: CoroutineScope
) {

    private var heartbeatJob: Job? = null

    companion object {
        /** Seuil en ms pour considérer un utilisateur comme en ligne (30 secondes). */
        const val ONLINE_THRESHOLD_MS = 30_000L

        /** Intervalle du heartbeat (10 secondes). */
        const val HEARTBEAT_INTERVAL_MS = 10_000L
    }

    /**
     * Active la présence pour l'utilisateur courant.
     * - Crée/maj le document users/{uid}/presence/online avec lastActive=now
     * - Configure onDisconnect pour mettre isOnline=false + lastSeen=timestamp
     * - Démarre le heartbeat périodique
     */
    fun startPresence(userId: String) {
        val database = db ?: return
        val presenceRef = database.collection("users").document(userId).collection("presence").document("online")

        scope.launch(Dispatchers.IO) {
            try {
                // 1. Marquer comme en ligne maintenant
                presenceRef.set(mapOf(
                    "isOnline" to true,
                    "lastActive" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )).await()

                // 2. Configurer onDisconnect : quand la connexion coupe, marquer hors-ligne
                // Note: onDisconnect ne fonctionne que si la persistance est activée (par défaut sur Android)
                presenceRef.onDisconnect().update(mapOf(
                    "isOnline" to false,
                    "lastSeen" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                ))

                // 3. Démarrer le heartbeat
                startHeartbeat(userId)
            } catch (e: Exception) {
                android.util.Log.w("PresenceManager", "startPresence error: ${e.message}")
            }
        }
    }

    /**
     * Arrête la présence (appelé à la déconnexion explicite).
     * - Annule le heartbeat
     * - Supprime le document de présence ou marque isOnline=false
     */
    fun stopPresence(userId: String) {
        heartbeatJob?.cancel()
        heartbeatJob = null

        val database = db ?: return
        val presenceRef = database.collection("users").document(userId).collection("presence").document("online")

        scope.launch(Dispatchers.IO) {
            try {
                // Annuler le onDisconnect programmé
                presenceRef.onDisconnect().cancel()

                // Marquer explicitement hors-ligne
                presenceRef.update(mapOf(
                    "isOnline" to false,
                    "lastSeen" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )).await()
            } catch (e: Exception) {
                android.util.Log.w("PresenceManager", "stopPresence error: ${e.message}")
            }
        }
    }

    /**
     * Heartbeat périodique pour mettre à jour lastActive.
     */
    private fun startHeartbeat(userId: String) {
        heartbeatJob?.cancel()
        heartbeatJob = scope.launch(Dispatchers.IO) {
            val database = db ?: return@launch
            val presenceRef = database.collection("users").document(userId).collection("presence").document("online")

            while (true) {
                delay(HEARTBEAT_INTERVAL_MS)
                try {
                    presenceRef.update(mapOf(
                        "lastActive" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )).await()
                } catch (e: Exception) {
                    // Ignorer les erreurs de heartbeat, on réessaiera au prochain cycle
                    android.util.Log.w("PresenceManager", "heartbeat error: ${e.message}")
                }
            }
        }
    }

    /**
     * Vérifie si un utilisateur est en ligne basé sur son document de présence.
     * Note: Cette méthode ne fait qu'une lecture ponctuelle. Pour du temps réel,
     * utiliser observePresence().
     */
    suspend fun isUserOnline(userId: String): Boolean {
        val database = db ?: return false
        return try {
            val snap = database.collection("users").document(userId).collection("presence").document("online").get().await()
            snap.exists && (snap.getBoolean("isOnline") == true)
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Observe la présence d'un utilisateur en temps réel.
     * L'utilisateur est considéré en ligne si isOnline=true ET lastActive < threshold.
     */
    fun observePresence(userId: String): kotlinx.coroutines.flow.Flow<Boolean> = kotlinx.coroutines.flow.flow {
        val database = db ?: return@flow
        val presenceRef = database.collection("users").document(userId).collection("presence").document("online")

        try {
            emitAll(
                presenceRef.snapshots()
                    .map { snap ->
                        if (!snap.exists) false
                        else {
                            val isOnline = snap.getBoolean("isOnline") == true
                            val lastActive = snap.getTimestamp("lastActive")?.toDate()?.time ?: 0L
                            val now = System.currentTimeMillis()
                            // En ligne si le flag est true ET le dernier heartbeat est récent
                            isOnline && (now - lastActive < ONLINE_THRESHOLD_MS)
                        }
                    }
                    .catch { e ->
                        android.util.Log.w("PresenceManager", "observePresence error: ${e.message}")
                        emit(false)
                    }
            )
        } catch (e: Exception) {
            android.util.Log.w("PresenceManager", "observePresence setup error: ${e.message}")
            emit(false)
        }
    }
}
