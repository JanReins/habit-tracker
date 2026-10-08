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
            .apply()
    }

    fun checkPin(pin: String): Boolean {
        val salt = prefs.getString(KEY_PIN_SALT, null) ?: return false
        val hash = prefs.getString(KEY_PIN_HASH, null) ?: return false
        return Pin.matches(pin, salt, hash)
    }

    fun clearPin() {
        prefs.edit().remove(KEY_PIN_SALT).remove(KEY_PIN_HASH).apply()
    }

    private companion object {
        const val KEY_NUDGE = "evening_nudge"
        const val KEY_PIN_SALT = "pin_salt"
        const val KEY_PIN_HASH = "pin_hash"
    }
}
