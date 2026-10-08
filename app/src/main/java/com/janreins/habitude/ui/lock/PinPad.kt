package com.janreins.habitude.ui.lock

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Backspace
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.janreins.habitude.domain.Pin

/**
 * Six dots and a number pad. Calls [onComplete] once all six digits are in, then clears.
 * Changing [resetKey] also clears whatever was typed.
 */
@Composable
fun PinPad(
    title: String,
    message: String?,
    isError: Boolean,
    onComplete: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    resetKey: Int = 0,
) {
    var entry by rememberSaveable(resetKey) { mutableStateOf("") }
    val haptics = LocalHapticFeedback.current

    fun press(key: String) {
        if (!enabled) return
        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        if (key == BACKSPACE) {
            entry = entry.dropLast(1)
            return
        }
        if (entry.length >= Pin.LENGTH) return
        entry += key
        if (entry.length == Pin.LENGTH) {
            val pin = entry
            entry = ""
            onComplete(pin)
        }
    }

    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(title, style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
        Spacer(Modifier.height(24.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.semantics { contentDescription = "${entry.length} of ${Pin.LENGTH} digits entered" },
        ) {
            repeat(Pin.LENGTH) { index ->
                val filled = index < entry.length
                Box(
                    Modifier
                        .size(14.dp)
                        .then(
                            if (filled) Modifier.background(MaterialTheme.colorScheme.onSurface, CircleShape)
                            else Modifier.border(1.5.dp, MaterialTheme.colorScheme.outline, CircleShape),
                        ),
                )
            }
        }
        Spacer(Modifier.height(16.dp))
        Text(
            message.orEmpty(),
            style = MaterialTheme.typography.bodyMedium,
            color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.height(40.dp),
        )
        Spacer(Modifier.height(8.dp))
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            KEYS.forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(22.dp)) {
                    row.forEach { key -> Key(key, enabled) { press(key) } }
                }
            }
        }
    }
}

@Composable
private fun Key(key: String, enabled: Boolean, onClick: () -> Unit) {
    if (key.isEmpty()) {
        Spacer(Modifier.size(KEY_SIZE))
        return
    }
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = CircleShape,
        color = if (key == BACKSPACE) MaterialTheme.colorScheme.background else MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.size(KEY_SIZE),
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (key == BACKSPACE) {
                Icon(Icons.AutoMirrored.Outlined.Backspace, contentDescription = "Delete")
            } else {
                Text(key, style = MaterialTheme.typography.headlineSmall)
            }
        }
    }
}

private const val BACKSPACE = "⌫"
private val KEY_SIZE = 72.dp
private val KEYS = listOf(
    listOf("1", "2", "3"),
    listOf("4", "5", "6"),
    listOf("7", "8", "9"),
    listOf("", "0", BACKSPACE),
)
