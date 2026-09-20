package com.reelblocker.security

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.security.MessageDigest
import java.security.SecureRandom

/**
 * Stores the app-open/self-protection PIN (hashed, never stored or shown in the clear) and the
 * PIN-recovery code (stored retrievably, since the user can view it again in Settings).
 *
 * Backed by [EncryptedSharedPreferences]; this file is explicitly excluded from Android's
 * auto-backup (see data_extraction_rules.xml) since its Keystore-bound encryption key never
 * transfers to a new device or restore.
 */
class PinManager(context: Context) {

    private val prefs: SharedPreferences by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            "reelblocker_secure_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    fun isPinSet(): Boolean = prefs.contains(KEY_PIN_HASH)

    fun setPin(pin: String) {
        val salt = randomSalt()
        prefs.edit()
            .putString(KEY_PIN_SALT, salt)
            .putString(KEY_PIN_HASH, hash(pin, salt))
            .apply()
    }

    fun verifyPin(pin: String): Boolean {
        val salt = prefs.getString(KEY_PIN_SALT, null) ?: return false
        val storedHash = prefs.getString(KEY_PIN_HASH, null) ?: return false
        return hash(pin, salt) == storedHash
    }

    /** Creates a recovery code on first call; returns the existing one on later calls. */
    fun ensureRecoveryCode(): String {
        prefs.getString(KEY_RECOVERY_CODE, null)?.let { return it }
        val code = generateRecoveryCode()
        prefs.edit().putString(KEY_RECOVERY_CODE, code).apply()
        return code
    }

    fun getRecoveryCode(): String? = prefs.getString(KEY_RECOVERY_CODE, null)

    fun resetPinWithRecoveryCode(code: String, newPin: String): Boolean {
        val stored = prefs.getString(KEY_RECOVERY_CODE, null) ?: return false
        if (!stored.equals(code.trim(), ignoreCase = true)) return false
        setPin(newPin)
        return true
    }

    private fun randomSalt(): String {
        val bytes = ByteArray(16)
        SecureRandom().nextBytes(bytes)
        return bytes.joinToString("") { "%02x".format(it) }
    }

    private fun hash(pin: String, salt: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        digest.update(salt.toByteArray(Charsets.UTF_8))
        val bytes = digest.digest(pin.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    private fun generateRecoveryCode(): String {
        val alphabet = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789" // no 0/O/1/I to avoid ambiguity
        val random = SecureRandom()
        val raw = (1..10).map { alphabet[random.nextInt(alphabet.length)] }.joinToString("")
        return "${raw.substring(0, 5)}-${raw.substring(5, 10)}"
    }

    companion object {
        private const val KEY_PIN_HASH = "pin_hash"
        private const val KEY_PIN_SALT = "pin_salt"
        private const val KEY_RECOVERY_CODE = "recovery_code"
    }
}
