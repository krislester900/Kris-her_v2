package com.example.security

import android.content.Context
import android.util.Base64
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import java.security.MessageDigest
import java.security.SecureRandom

/**
 * Gestion du code PIN de verrouillage de l'application (coffre « vault »).
 *
 * Extrait de ChatViewModel (P1-3) : la logique de sécurité n'a pas à vivre
 * dans un ViewModel de messagerie.
 *
 * Stockage : SharedPreferences « kris_her_prefs »
 *  - vault_pin_salt           sel aléatoire (Base64)
 *  - vault_pin_hash           PBKDF2-HMAC-SHA256, 60 000 itérations, 256 bits (Base64)
 *  - vault_pin_enabled        protection activée
 *  - vault_failed_attempts    tentatives ratées consécutives
 *  - vault_lock_until         verrouillage temporaire (epoch ms)
 *
 * Le PIN lui-même n'est jamais stocké en clair.
 */
class VaultSecurityManager(
    private val prefs: android.content.SharedPreferences,
    private val scope: CoroutineScope
) {

    // ------------------------------------------------------------------
    //  État exposé
    // ------------------------------------------------------------------

    private val _hasPin = MutableStateFlow(prefs.contains(KEY_PIN_HASH))
    val hasPin: StateFlow<Boolean> = _hasPin.asStateFlow()

    private val _isPinProtectionEnabled = MutableStateFlow(
        prefs.getBoolean(KEY_PIN_ENABLED, false) && prefs.contains(KEY_PIN_HASH)
    )
    val isPinProtectionEnabled: StateFlow<Boolean> = _isPinProtectionEnabled.asStateFlow()

    /** Verrouillé dès le lancement si le code PIN est activé. */
    private val _isAppLocked = MutableStateFlow(_isPinProtectionEnabled.value)
    val isAppLocked: StateFlow<Boolean> = _isAppLocked.asStateFlow()

    private val _enteredPin = MutableStateFlow("")
    val enteredPin: StateFlow<String> = _enteredPin.asStateFlow()

    private val _pinError = MutableStateFlow(false)
    val pinError: StateFlow<Boolean> = _pinError.asStateFlow()

    // ------------------------------------------------------------------
    //  Hachage
    // ------------------------------------------------------------------

    private fun hashPin(pin: String, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(pin.toCharArray(), salt, PBKDF2_ITERATIONS, KEY_LENGTH_BITS)
        return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
    }

    private fun verifyPin(pin: String): Boolean {
        val saltB64 = prefs.getString(KEY_PIN_SALT, null) ?: return false
        val hashB64 = prefs.getString(KEY_PIN_HASH, null) ?: return false
        val salt = Base64.decode(saltB64, Base64.NO_WRAP)
        val expected = Base64.decode(hashB64, Base64.NO_WRAP)
        // Comparaison à temps constant pour éviter les attaques temporelles.
        return MessageDigest.isEqual(expected, hashPin(pin, salt))
    }

    // ------------------------------------------------------------------
    //  Actions publiques
    // ------------------------------------------------------------------

    /**
     * Définit (ou change) le code PIN à 4 chiffres choisi par l'utilisateur.
     * Le hachage PBKDF2 est exécuté hors du thread principal.
     *
     * @return true si le PIN a été enregistré (format valide).
     */
    fun setPin(pin: String, onDone: (Boolean) -> Unit = {}) {
        if (pin.length != PIN_LENGTH || !pin.all { it.isDigit() }) {
            onDone(false)
            return
        }
        scope.launch {
            val salt = ByteArray(SALT_BYTES).also { SecureRandom().nextBytes(it) }
            val hash = withContext(Dispatchers.Default) { hashPin(pin, salt) }
            prefs.edit()
                .putString(KEY_PIN_SALT, Base64.encodeToString(salt, Base64.NO_WRAP))
                .putString(KEY_PIN_HASH, Base64.encodeToString(hash, Base64.NO_WRAP))
                .putBoolean(KEY_PIN_ENABLED, true)
                .putInt(KEY_FAILED_ATTEMPTS, 0)
                .putLong(KEY_LOCK_UNTIL, 0L)
                .apply()
            _hasPin.value = true
            _isPinProtectionEnabled.value = true
            onDone(true)
        }
    }

    /** Ajoute un chiffre au PIN en cours de saisie ; valide automatiquement à 4 chiffres. */
    fun appendPinDigit(digit: String) {
        val now = System.currentTimeMillis()
        // Blocage temporaire après trop d'essais ratés.
        if (now < prefs.getLong(KEY_LOCK_UNTIL, 0L)) {
            _pinError.value = true
            return
        }
        if (digit.length != 1 || !digit[0].isDigit()) return
        if (_enteredPin.value.length >= PIN_LENGTH) return

        _enteredPin.value += digit
        _pinError.value = false
        if (_enteredPin.value.length != PIN_LENGTH) return

        val attempt = _enteredPin.value
        scope.launch {
            val ok = withContext(Dispatchers.Default) { verifyPin(attempt) }
            if (ok) {
                prefs.edit()
                    .putInt(KEY_FAILED_ATTEMPTS, 0)
                    .putLong(KEY_LOCK_UNTIL, 0L)
                    .apply()
                _isAppLocked.value = false
                _enteredPin.value = ""
            } else {
                val failed = prefs.getInt(KEY_FAILED_ATTEMPTS, 0) + 1
                // À partir du 5e échec : 30 s, 60 s, 2 min, 4 min, 8 min maximum.
                val lockMs = if (failed >= MAX_FREE_ATTEMPTS) {
                    BASE_LOCK_MS shl minOf(failed - MAX_FREE_ATTEMPTS, MAX_LOCK_SHIFT)
                } else {
                    0L
                }
                prefs.edit()
                    .putInt(KEY_FAILED_ATTEMPTS, failed)
                    .putLong(KEY_LOCK_UNTIL, if (lockMs > 0L) System.currentTimeMillis() + lockMs else 0L)
                    .apply()
                _pinError.value = true
                delay(ERROR_RESET_DELAY_MS)
                _enteredPin.value = ""
            }
        }
    }

    /** Efface le dernier chiffre saisi. */
    fun deletePinDigit() {
        if (_enteredPin.value.isNotEmpty()) {
            _enteredPin.value = _enteredPin.value.dropLast(1)
            _pinError.value = false
        }
    }

    /** Active ou désactive la protection par PIN. Sans code choisi, l'activation est ignorée. */
    fun togglePinProtection(enabled: Boolean) {
        if (enabled && !_hasPin.value) return
        _isPinProtectionEnabled.value = enabled
        prefs.edit().putBoolean(KEY_PIN_ENABLED, enabled).apply()
        if (!enabled) {
            _isAppLocked.value = false
        }
    }

    /** Verrouille immédiatement l'application si la protection est active. */
    fun lockVaultNow() {
        if (_isPinProtectionEnabled.value) {
            _enteredPin.value = ""
            _isAppLocked.value = true
        }
    }

    companion object {
        private const val PREFS_NAME = "kris_her_prefs"

        private const val KEY_PIN_SALT = "vault_pin_salt"
        private const val KEY_PIN_HASH = "vault_pin_hash"
        private const val KEY_PIN_ENABLED = "vault_pin_enabled"
        private const val KEY_FAILED_ATTEMPTS = "vault_failed_attempts"
        private const val KEY_LOCK_UNTIL = "vault_lock_until"

        /** Longueur du code PIN. */
        const val PIN_LENGTH = 4

        private const val PBKDF2_ITERATIONS = 60_000
        private const val KEY_LENGTH_BITS = 256
        private const val SALT_BYTES = 16

        private const val MAX_FREE_ATTEMPTS = 5
        private const val BASE_LOCK_MS = 30_000L
        private const val MAX_LOCK_SHIFT = 4
        private const val ERROR_RESET_DELAY_MS = 600L

        /**
         * Crée un gestionnaire à partir d'un [Context].
         * Utilise les mêmes SharedPreferences que le reste de l'application.
         */
        fun create(context: Context, scope: CoroutineScope): VaultSecurityManager {
            val prefs = context.applicationContext
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            return VaultSecurityManager(prefs, scope)
        }
    }
}
