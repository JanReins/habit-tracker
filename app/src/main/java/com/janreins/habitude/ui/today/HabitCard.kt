package com.janreins.habitude.ui.today

import androidx.compose.animation.animateColorAsState
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
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
            if (isBuild) {
                CheckButton(done = state.loggedToday, onClick = onToggleDone)
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
private fun CheckButton(done: Boolean, onClick: () -> Unit) {
    val container by animateColorAsState(
        if (done) MaterialTheme.colorScheme.primary else Color.Transparent,
        label = "checkContainer",
    )
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = container,
        border = if (done) null else BorderStroke(2.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier.size(44.dp),
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (done) {
                Icon(
                    Icons.Rounded.Check,
                    contentDescription = "Done today",
                    tint = MaterialTheme.colorScheme.onPrimary,
                )
            }
        }
    }
}

private fun days(n: Int) = if (n == 1) "1 day" else "$n days"

private fun subtitle(state: HabitCardState): String {
    val best = if (state.best > 0) " · best ${days(state.best)}" else ""
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
