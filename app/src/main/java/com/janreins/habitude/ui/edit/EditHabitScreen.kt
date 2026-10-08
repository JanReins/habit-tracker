package com.janreins.habitude.ui.edit

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.janreins.habitude.domain.HabitType
import android.app.TimePickerDialog
import android.text.format.DateFormat
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.NotificationsOff
import androidx.compose.material3.Switch
import androidx.compose.ui.platform.LocalContext
import com.janreins.habitude.ui.rememberNotificationPermissionRequest
import java.time.DayOfWeek
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditHabitScreen(
    onDone: () -> Unit,
    onDeleted: () -> Unit,
    viewModel: EditHabitViewModel = viewModel(factory = EditHabitViewModel.Factory),
) {
    val state = viewModel.state
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    val isBuild = state.type == HabitType.BUILD

    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onDone) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
            }
            Text(
                if (state.isNew) "New habit" else "Edit habit",
                style = MaterialTheme.typography.headlineSmall,
            )
        }

        if (state.isNew) {
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                HabitType.entries.forEachIndexed { index, type ->
                    SegmentedButton(
                        selected = state.type == type,
                        onClick = { viewModel.setType(type) },
                        shape = SegmentedButtonDefaults.itemShape(index, HabitType.entries.size),
                    ) {
                        Text(if (type == HabitType.BUILD) "Build" else "Break")
                    }
                }
            }
        }

        OutlinedTextField(
            value = state.name,
            onValueChange = viewModel::setName,
            label = { Text(if (isBuild) "I want to…" else "I want to stop…") },
            placeholder = { Text(if (isBuild) "Drink 2L of water" else "Doomscrolling in bed") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            modifier = Modifier.fillMaxWidth(),
        )

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            FieldLabel("Icon")
            HabitEmojis.chunked(8).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEach { emoji ->
                        val selected = emoji == state.emoji
                        Surface(
                            onClick = { viewModel.setEmoji(emoji) },
                            shape = CircleShape,
                            color = if (selected) {
                                if (isBuild) MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.secondaryContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant
                            },
                            border = if (selected) {
                                BorderStroke(
                                    2.dp,
                                    if (isBuild) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                                )
                            } else {
                                null
                            },
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f),
                        ) {
                            Box(contentAlignment = Alignment.Center) { Text(emoji, fontSize = 18.sp) }
                        }
                    }
                }
            }
        }

        if (isBuild) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                FieldLabel("On these days")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    DayOfWeek.entries.forEach { day ->
                        val selected = day in state.schedule
                        Surface(
                            onClick = { viewModel.toggleDay(day) },
                            shape = CircleShape,
                            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f),
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    day.getDisplayName(TextStyle.NARROW, Locale.getDefault()),
                                    style = MaterialTheme.typography.labelLarge,
                                )
                            }
                        }
                    }
                }
            }
        } else {
            Text(
                "Every day you don't slip adds to your clean run. If you do slip, log it honestly and the count starts again.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        ReminderRow(minutes = state.reminderMinutes, onChange = viewModel::setReminder)

        Spacer(Modifier.height(8.dp))
        Button(
            onClick = { viewModel.save(onDone) },
            enabled = state.canSave,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
        ) {
            Text(if (state.isNew) "Start this habit" else "Save changes")
        }

        if (!state.isNew) {
            TextButton(
                onClick = { confirmDelete = true },
                modifier = Modifier.align(Alignment.CenterHorizontally),
            ) {
                Text("Delete habit", color = MaterialTheme.colorScheme.error)
            }
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete \"${state.name}\"?") },
            text = { Text("Its whole history goes with it. This can't be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    viewModel.delete(onDeleted)
                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text("Keep it") }
            },
        )
    }
}

@Composable
private fun ReminderRow(minutes: Int?, onChange: (Int?) -> Unit) {
    val context = LocalContext.current
    val askPermission = rememberNotificationPermissionRequest()
    val pickTime = { initial: Int ->
        TimePickerDialog(
            context,
            { _, hour, minute ->
                onChange(hour * 60 + minute)
                askPermission()
            },
            initial / 60,
            initial % 60,
            DateFormat.is24HourFormat(context),
        ).show()
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        FieldLabel("Reminder")
        Surface(
            onClick = { pickTime(minutes ?: DEFAULT_REMINDER) },
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surfaceVariant,
        ) {
            Row(
                modifier = Modifier.padding(start = 16.dp, end = 12.dp, top = 8.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Icon(
                    if (minutes == null) Icons.Outlined.NotificationsOff else Icons.Outlined.Notifications,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    if (minutes == null) "No reminder" else "Remind me at ${formatMinutes(minutes)}",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f),
                )
                Switch(
                    checked = minutes != null,
                    onCheckedChange = { on -> if (on) pickTime(DEFAULT_REMINDER) else onChange(null) },
                )
            }
        }
    }
}

private const val DEFAULT_REMINDER = 9 * 60

private fun formatMinutes(minutes: Int): String =
    LocalTime.of(minutes / 60, minutes % 60).format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT))

@Composable
private fun FieldLabel(text: String) {
    Text(
        text.uppercase(),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}
