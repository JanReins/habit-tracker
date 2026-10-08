package com.janreins.habitude.ui.lock

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.janreins.habitude.domain.Pin
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Covers the whole app until the right PIN is entered. [initialFailures] and
 * [initialLockedUntil] (wall-clock ms) come from storage, and every attempt is reported through
 * [onAttempt], so closing the app doesn't reset the wait after too many wrong tries.
 */
@Composable
fun LockScreen(
    checkPin: suspend (String) -> Boolean,
    onUnlocked: () -> Unit,
    initialFailures: Int,
    initialLockedUntil: Long,
    onAttempt: (failures: Int, lockedUntil: Long) -> Unit,
    onUseBiometrics: (() -> Unit)? = null,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var failures by rememberSaveable { mutableIntStateOf(initialFailures) }
    var waitUntil by rememberSaveable { mutableLongStateOf(initialLockedUntil) }
    var secondsLeft by rememberSaveable { mutableIntStateOf(0) }
    var checking by rememberSaveable { mutableStateOf(false) }
    var resetKey by rememberSaveable { mutableIntStateOf(0) }

    // With fingerprint or face unlock on, ask for it straight away, once each time the lock shows.
    var prompted by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (onUseBiometrics != null && !prompted) {
            prompted = true
            onUseBiometrics()
        }
    }

    // Back leaves the app rather than reaching the screens underneath.
    BackHandler { (context as? Activity)?.moveTaskToBack(true) }

    LaunchedEffect(waitUntil) {
        // Capped at the longest wait this many failures earns, in case the clock was set back.
        val longest = Pin.lockoutSeconds(failures) * 1000L
        while (true) {
            val left = (waitUntil - System.currentTimeMillis()).coerceIn(0L, longest)
            secondsLeft = ((left + 999) / 1000).toInt()
            if (secondsLeft == 0) break
            delay(250)
        }
    }

    val message = when {
        secondsLeft > 0 -> "Too many tries. Wait ${secondsLeft}s."
        resetKey > 0 -> "Wrong PIN. Try again."
        else -> null
    }

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .systemBarsPadding()
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                "Habitude",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                PinPad(
                    title = "Enter your PIN",
                    message = message,
                    isError = resetKey > 0 || secondsLeft > 0,
                    enabled = secondsLeft == 0 && !checking,
                    resetKey = resetKey,
                    onComplete = { pin ->
                        checking = true
                        scope.launch {
                            val ok = checkPin(pin)
                            checking = false
                            if (ok) {
                                failures = 0
                                waitUntil = 0L
                                onAttempt(0, 0L)
                                onUnlocked()
                            } else {
                                failures++
                                resetKey++
                                val wait = Pin.lockoutSeconds(failures)
                                if (wait > 0) waitUntil = System.currentTimeMillis() + wait * 1000L
                                onAttempt(failures, waitUntil)
                            }
                        }
                    },
                )
                if (onUseBiometrics != null) {
                    TextButton(onClick = onUseBiometrics) { Text("Use fingerprint or face") }
                }
            }
            Text(
                "Forgot your PIN? The only way back in is to clear Habitude's storage in Android settings, " +
                    "which also erases your habits.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}
