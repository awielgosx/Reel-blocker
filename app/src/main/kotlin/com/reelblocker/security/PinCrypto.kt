package com.reelblocker.security

import java.security.MessageDigest
import java.security.SecureRandom

/**
 * Pure PIN hashing / recovery-code logic, pulled out of [PinManager] so it's unit-testable
 * without needing Android Keystore (which EncryptedSharedPreferences depends on, and which isn't
 * practical to exercise in a plain JVM test).
 */
internal object PinCrypto {
    private const val RECOVERY_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789" // no 0/O/1/I - avoids ambiguity

    fun randomSalt(random: SecureRandom = SecureRandom()): String {
        val bytes = ByteArray(16)
        random.nextBytes(bytes)
        return bytes.joinToString("") { "%02x".format(it) }
    }

    fun hash(pin: String, salt: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        digest.update(salt.toByteArray(Charsets.UTF_8))
        val bytes = digest.digest(pin.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    fun generateRecoveryCode(random: SecureRandom = SecureRandom()): String {
        val raw = (1..10).map { RECOVERY_ALPHABET[random.nextInt(RECOVERY_ALPHABET.length)] }.joinToString("")
        return "${raw.substring(0, 5)}-${raw.substring(5, 10)}"
    }

    fun matchesRecoveryCode(stored: String, entered: String): Boolean =
        stored.equals(entered.trim(), ignoreCase = true)
}
