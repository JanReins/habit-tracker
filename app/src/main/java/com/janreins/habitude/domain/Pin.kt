package com.janreins.habitude.domain

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * PIN handling. The PIN itself is never stored: only a salted PBKDF2 hash, so reading the
 * app's files doesn't reveal it.
 */
object Pin {
    const val LENGTH = 6
    private const val ITERATIONS = 60_000
    private const val KEY_BITS = 256

    fun isValid(pin: String): Boolean = pin.length == LENGTH && pin.all { it in '0'..'9' }

    fun newSalt(): String = ByteArray(16).also { SecureRandom().nextBytes(it) }.encode()

    fun hash(pin: String, salt: String): String {
        val spec = PBEKeySpec(pin.toCharArray(), Base64.getDecoder().decode(salt), ITERATIONS, KEY_BITS)
        return try {
            SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded.encode()
        } finally {
            spec.clearPassword()
        }
    }

    fun matches(pin: String, salt: String, expectedHash: String): Boolean =
        isValid(pin) && MessageDigest.isEqual(hash(pin, salt).toByteArray(), expectedHash.toByteArray())

    /** Seconds to wait after [failedAttempts] wrong PINs in a row: none for the first 4, then 30s, doubling. */
    fun lockoutSeconds(failedAttempts: Int): Int =
        if (failedAttempts < 5) 0 else (30 shl (failedAttempts - 5).coerceAtMost(4))

    private fun ByteArray.encode(): String = Base64.getEncoder().encodeToString(this)
}
