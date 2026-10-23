package com.example.util

import java.security.MessageDigest

/**
 * Empreinte SHA-256 des 8 derniers chiffres d'un numéro (« 07 07 07 07 07 » et « +225 07 07 07 07 07 »
 * donnent la même empreinte). Renvoie "" si le numéro est trop court.
 *
 * Sert à retrouver des contacts sans publier les numéros en clair. Ce n'est pas un secret cryptographique
 * (8 chiffres se devinent par force brute) : cela évite seulement l'exposition directe des numéros.
 */
fun phoneHashOf(raw: String): String {
    val digits = raw.filter { it.isDigit() }
    if (digits.length < 8) return ""
    val canonical = digits.takeLast(8)
    return MessageDigest.getInstance("SHA-256")
        .digest(canonical.toByteArray())
        .joinToString("") { "%02x".format(it) }
}
