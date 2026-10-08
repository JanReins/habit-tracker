package com.janreins.habitude

import android.os.Bundle
import android.os.SystemClock
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.janreins.habitude.ui.lock.LockScreen
import com.janreins.habitude.ui.navigation.HabitudeApp
import com.janreins.habitude.ui.theme.HabitudeTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Lock state that survives screen rotation but not the app being closed, so a restart
 * always asks for the PIN again.
 */
class LockState : ViewModel() {
    var locked by mutableStateOf<Boolean?>(null)

    /** When the app last went to the background, for the lock timeout. */
    var leftAt = 0L
}

class MainActivity : ComponentActivity() {
    private val settings by lazy { (application as HabitudeApplication).settings }
    private val lock: LockState by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (lock.locked == null) lock.locked = settings.hasPin
        setContent {
            HabitudeTheme {
                Box {
                    // The app stays composed underneath so you land back where you were.
                    HabitudeApp()
                    if (lock.locked == true) {
                        LockScreen(
                            checkPin = { pin -> withContext(Dispatchers.Default) { settings.checkPin(pin) } },
                            onUnlocked = { lock.locked = false },
                        )
                    }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        val away = SystemClock.elapsedRealtime() - lock.leftAt
        if (settings.hasPin && lock.leftAt != 0L && away > LOCK_AFTER_MS) lock.locked = true
    }

    override fun onStop() {
        super.onStop()
        if (!isChangingConfigurations) lock.leftAt = SystemClock.elapsedRealtime()
    }

    private companion object {
        /** A short grace period, so popping out to pick a backup file doesn't lock you out. */
        const val LOCK_AFTER_MS = 60_000L
    }
}
