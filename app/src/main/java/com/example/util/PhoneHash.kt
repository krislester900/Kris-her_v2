package com.example.util

import java.security.MessageDigest

/**
 * Empreinte SHA-256 des 8 derniers chiffres d'un numéro (« 07 07 07 07 07 » et « +225 07 07 07 07 07 »
 * donnent la même empreinte). Renvoie "" si le numéro est trop court.
 *
 * Sert à retrouver des contacts sans publier les numéros en clair.
 *
 * ⚠️ AVERTISSEMENT DE SÉCURITÉ : ce n'est **pas** un secret cryptographique.
 * L'entropie est d'environ 26,6 bits (10^8 combinaisons possibles pour 8 chiffres).
 * Une force brute peut retrouver le numéro en quelques millisecondes.
 * Cette empreinte évite seulement l'exposition directe des numéros dans l'interface,
 * elle ne protège pas contre une détermination malveillante du numéro de téléphone.
 */
fun phoneHashOf(raw: String): String {
    val digits = raw.filter { it.isDigit() }
    if (digits.length < 8) return ""
    val canonical = digits.takeLast(8)
    return MessageDigest.getInstance("SHA-256")
        .digest(canonical.toByteArray())
        .joinToString("") { "%02x".format(it) }
}

/** Nombre de bits d'entropie de l'empreinte : ~26,6 bits (10^8 combinaisons). Faible. */
const val PHONE_HASH_ENTROPY_BITS: Double = 26.6

/** Cette empreinte n'est pas cryptographiquement sécurisée. */
const val PHONE_HASH_IS_CRYPTOGRAPHICALLY_SECURE: Boolean = false
