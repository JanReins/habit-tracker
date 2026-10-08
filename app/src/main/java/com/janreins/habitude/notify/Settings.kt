package com.janreins.habitude.notify

import android.content.Context
import com.janreins.habitude.domain.Pin

/** Small app preferences kept on the phone. */
class Settings(context: Context) {
    private val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    var eveningNudge: Boolean
        get() = prefs.getBoolean(KEY_NUDGE, true)
        set(value) = prefs.edit().putBoolean(KEY_NUDGE, value).apply()

    val hasPin: Boolean get() = prefs.contains(KEY_PIN_HASH)

    /** Stores a salted hash of [pin], never the PIN itself. */
    fun setPin(pin: String) {
        require(Pin.isValid(pin))
        val salt = Pin.newSalt()
        prefs.edit()
            .putString(KEY_PIN_SALT, salt)
            .putString(KEY_PIN_HASH, Pin.hash(pin, salt))
            .remove(KEY_PIN_FAILURES)
            .remove(KEY_PIN_LOCKED_UNTIL)
            .apply()
    }

    fun checkPin(pin: String): Boolean {
        val salt = prefs.getString(KEY_PIN_SALT, null) ?: return false
        val hash = prefs.getString(KEY_PIN_HASH, null) ?: return false
        return Pin.matches(pin, salt, hash)
    }

    /** Wrong PINs in a row, kept so closing the app doesn't reset the wait. */
    var pinFailures: Int
        get() = prefs.getInt(KEY_PIN_FAILURES, 0)
        set(value) = prefs.edit().putInt(KEY_PIN_FAILURES, value).apply()

    /** Wall-clock time (ms) until which PIN entry is paused after too many wrong tries. */
    var pinLockedUntil: Long
        get() = prefs.getLong(KEY_PIN_LOCKED_UNTIL, 0L)
        set(value) = prefs.edit().putLong(KEY_PIN_LOCKED_UNTIL, value).apply()

    fun clearPin() {
        prefs.edit()
            .remove(KEY_PIN_SALT)
            .remove(KEY_PIN_HASH)
            .remove(KEY_PIN_FAILURES)
            .remove(KEY_PIN_LOCKED_UNTIL)
            .apply()
    }

    private companion object {
        const val KEY_NUDGE = "evening_nudge"
        const val KEY_PIN_SALT = "pin_salt"
        const val KEY_PIN_HASH = "pin_hash"
        const val KEY_PIN_FAILURES = "pin_failures"
        const val KEY_PIN_LOCKED_UNTIL = "pin_locked_until"
    }
}
