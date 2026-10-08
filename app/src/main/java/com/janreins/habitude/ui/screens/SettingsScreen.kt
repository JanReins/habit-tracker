package com.janreins.habitude.ui.screens

import android.content.Intent
import android.provider.Settings as AndroidSettings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.janreins.habitude.HabitudeApplication
import com.janreins.habitude.notify.Notifications
import com.janreins.habitude.ui.rememberNotificationPermissionRequest

@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    val app = context.applicationContext as HabitudeApplication
    var nudge by remember { mutableStateOf(app.settings.eveningNudge) }
    var canPost by remember { mutableStateOf(Notifications.canPost(context)) }
    val openSystemSettings = {
        context.startActivity(
            Intent(AndroidSettings.ACTION_APP_NOTIFICATION_SETTINGS)
                .putExtra(AndroidSettings.EXTRA_APP_PACKAGE, context.packageName)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }
    val askPermission = rememberNotificationPermissionRequest { granted ->
        canPost = Notifications.canPost(context)
        // The system stops showing the prompt after two refusals; send people to settings instead.
        if (!granted) openSystemSettings()
    }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { canPost = Notifications.canPost(context) }
    val version = remember {
        runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("Settings", style = MaterialTheme.typography.displaySmall)
        Spacer(Modifier.height(8.dp))

        if (!canPost) {
            SettingsCard {
                Text("Notifications are off", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Reminders and the evening streak check can't reach you until notifications are allowed for Habitude.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                FilledTonalButton(onClick = {
                    if (Notifications.needsRuntimePermission(context)) askPermission() else openSystemSettings()
                }) { Text("Turn on notifications") }
            }
        }

        SettingsCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Evening streak check", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "At 8pm, a heads-up if a streak of two days or more is about to break.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(
                    checked = nudge,
                    onCheckedChange = { on ->
                        nudge = on
                        app.settings.eveningNudge = on
                        app.reminders.scheduleNudge(on)
                        if (on) askPermission()
                    },
                )
            }
            Text(
                "Per-habit reminders are set when you add or edit a habit.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        SettingsCard {
            Text("About", style = MaterialTheme.typography.titleMedium)
            Text(
                "Habitude${version?.let { " $it" }.orEmpty()}. Build the habits you want and break the ones you don't. " +
                    "Everything stays on this phone.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SettingsCard(content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) { content() }
    }
}
