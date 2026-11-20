package com.example.util

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object AccountEmailService {
    private const val TAG = "AccountEmailService"

    /**
     * Envoie un email de confirmation de création de compte.
     *
     * L'envoi s'effectue uniquement via la collection Firestore `mail`, traitée par
     * l'extension Firebase "Trigger Email" (plan Blaze requis).
     *
     * Les appels directs à Google Identity Toolkit sendOobCode (requestType PASSWORD_RESET),
     * Firebase Auth sendPasswordResetEmail ou sendEmailVerification sont des détournements d'API :
     * ils envoient un lien de réinitialisation de mot de passe ou de vérification, et non un
     * email de bienvenue. Ils ne doivent pas être utilisés à cette fin.
     */
    suspend fun dispatchAccountCreationEmail(
        email: String,
        displayName: String,
        phoneNumber: String,
        firestore: FirebaseFirestore? = null
    ): Boolean = withContext(Dispatchers.IO) {
        var emailSent = false

        // Méthode : Firestore mail queue for cloud email extension
        if (firestore != null) {
            try {
                val mailDoc = hashMapOf(
                    "to" to listOf(email),
                    "message" to hashMapOf(
                        "subject" to "🎉 [kris&her] Confirmation de création de votre compte",
                        "text" to "Bonjour $displayName,\n\nVotre compte de messagerie kris&her a été créé avec succès et rattaché à votre compte Google : $email.\nNuméro de téléphone : $phoneNumber.\n\nBienvenue sur kris&her !",
                        "html" to """
                            <div style="font-family: Arial, sans-serif; max-width: 500px; padding: 24px; border: 1px solid #e2e8f0; border-radius: 12px; background-color: #ffffff;">
                                <h2 style="color: #0f172a; margin-top: 0;">🎉 Bienvenue sur kris&her</h2>
                                <p style="color: #334155; font-size: 15px; line-height: 1.5;">Bonjour <strong>$displayName</strong>,</p>
                                <p style="color: #334155; font-size: 14px; line-height: 1.5;">Votre compte de messagerie a été créé avec succès et lié à votre compte Google :</p>
                                <p style="background: #f1f5f9; padding: 10px 14px; border-radius: 8px; font-family: monospace; color: #0284c7; font-weight: bold;">$email</p>
                                <p style="color: #334155; font-size: 14px;">Numéro associé : <strong>$phoneNumber</strong></p>
                                <p style="color: #64748b; font-size: 12px; margin-top: 24px; border-top: 1px solid #e2e8f0; padding-top: 12px;">kris&her • Messagerie kris&her</p>
                            </div>
                        """.trimIndent()
                    ),
                    "createdAt" to com.google.firebase.Timestamp.now()
                )
                firestore.collection("mail").add(mailDoc)
                Log.i(TAG, "Document mail ajouté à Firestore pour expédition")
                emailSent = true
            } catch (e: Exception) {
                Log.w(TAG, "Ajout document mail Firestore impossible: ${e.message}")
            }
        }

        emailSent
    }
}
