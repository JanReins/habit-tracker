package com.janreins.habitude.ui.today

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.janreins.habitude.domain.HabitType

@Composable
fun HabitCard(
    state: HabitCardState,
    onOpen: () -> Unit,
    onToggleDone: () -> Unit,
    onSlip: () -> Unit,
    onUndoSlip: () -> Unit,
    modifier: Modifier = Modifier,
    onAddCount: (Int) -> Unit = {},
) {
    val isBuild = state.type == HabitType.BUILD
    val accent = if (isBuild) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer

    Card(
        onClick = onOpen,
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(accent, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(state.emoji, fontSize = 22.sp)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    state.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    subtitle(state),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            val target = state.dailyTarget
            if (isBuild && target != null) {
                if (state.countToday > 0) {
                    IconButton(onClick = { onAddCount(-1) }, modifier = Modifier.size(36.dp)) {
                        Icon(
                            Icons.Rounded.Remove,
                            contentDescription = "Take one off ${state.name}",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                CountButton(name = state.name, count = state.countToday, target = target, onClick = { onAddCount(1) })
            } else if (isBuild) {
                CheckButton(name = state.name, done = state.loggedToday, onClick = onToggleDone)
            } else if (state.loggedToday) {
                TextButton(onClick = onUndoSlip) { Text("Undo") }
            } else {
                OutlinedButton(
                    onClick = onSlip,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary),
                ) { Text("Slipped", color = MaterialTheme.colorScheme.secondary) }
            }
        }
    }
}

@Composable
private fun CheckButton(name: String, done: Boolean, onClick: () -> Unit) {
    val haptics = LocalHapticFeedback.current
    val container by animateColorAsState(
        if (done) MaterialTheme.colorScheme.primary else Color.Transparent,
        label = "checkContainer",
    )
    Surface(
        onClick = {
            if (!done) haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            onClick()
        },
        shape = CircleShape,
        color = container,
        border = if (done) null else BorderStroke(2.dp, MaterialTheme.colorScheme.outline),
        // Read as a checkbox by screen readers, whether or not it's ticked yet.
        modifier = Modifier
            .size(44.dp)
            .semantics {
                role = Role.Checkbox
                contentDescription = "$name, done today"
                stateDescription = if (done) "Done" else "Not done"
            },
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (done) {
                Icon(
                    Icons.Rounded.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                )
            }
        }
    }
}

/** A ring that fills as a counted habit is counted up. Tap it to add one. */
@Composable
private fun CountButton(name: String, count: Int, target: Int, onClick: () -> Unit) {
    val haptics = LocalHapticFeedback.current
    val done = count >= target
    val container by animateColorAsState(
        if (done) MaterialTheme.colorScheme.primary else Color.Transparent,
        label = "countContainer",
    )
    val progress by animateFloatAsState((count.toFloat() / target).coerceAtMost(1f), label = "countProgress")
    Surface(
        onClick = {
            if (count + 1 == target) haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            onClick()
        },
        shape = CircleShape,
        color = container,
        modifier = Modifier
            .size(44.dp)
            .semantics {
                role = Role.Button
                contentDescription = "$name, add one"
                stateDescription = "$count of $target today"
            },
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (!done) {
                CircularProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.size(44.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.outlineVariant,
                    strokeWidth = 3.dp,
                    strokeCap = StrokeCap.Round,
                )
            }
            Text(
                "$count",
                style = MaterialTheme.typography.labelLarge,
                color = if (done) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

private fun days(n: Int) = if (n == 1) "1 day" else "$n days"

private fun weeks(n: Int) = if (n == 1) "1 week" else "$n weeks"

private fun subtitle(state: HabitCardState): String {
    val best = if (state.best > 0) " · best ${days(state.best)}" else ""
    state.weeklyTarget?.let { target ->
        val streak = if (state.current > 0) " · 🔥 ${weeks(state.current)}" else ""
        return "${state.doneThisWeek} of $target this week$streak"
    }
    state.dailyTarget?.let { target ->
        val streak = if (state.current > 0) " · 🔥 ${days(state.current)}" else ""
        return if (!state.scheduledToday && state.countToday == 0) "Rest day$streak" else "${state.countToday} of $target today$streak"
    }
    return when (state.type) {
        HabitType.BUILD -> when {
            state.current > 0 -> "🔥 ${days(state.current)}$best"
            !state.scheduledToday -> "Rest day$best"
            else -> "Start your streak today$best"
        }
        HabitType.BREAK -> when {
            state.loggedToday -> "Slipped today. Tomorrow's a fresh start."
            else -> "${days(state.current)} clean$best"
        }
    }
}
