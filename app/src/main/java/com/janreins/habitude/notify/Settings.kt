package com.janreins.habitude.notify

import android.content.Context

/** Small app preferences kept on the phone. */
class Settings(context: Context) {
    private val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    var eveningNudge: Boolean
        get() = prefs.getBoolean(KEY_NUDGE, true)
        set(value) = prefs.edit().putBoolean(KEY_NUDGE, value).apply()

    private companion object {
        const val KEY_NUDGE = "evening_nudge"
    }
}
