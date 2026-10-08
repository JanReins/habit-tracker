package com.janreins.habitude.ui

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.janreins.habitude.notify.Notifications

/**
 * Returns a function that asks for permission to post notifications (Android 13 and later)
 * if it hasn't been granted yet. Elsewhere it does nothing.
 */
@Composable
fun rememberNotificationPermissionRequest(onResult: (Boolean) -> Unit = {}): () -> Unit {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission(), onResult)
    return {
        if (Notifications.needsRuntimePermission(context)) {
            launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}
