package com.janreins.habitude.notify

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.janreins.habitude.HabitudeApplication
import com.janreins.habitude.domain.HabitType
import com.janreins.habitude.domain.Reminders
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.time.LocalDate

/** Runs [block] off the main thread while keeping the receiver alive until it finishes. */
private fun BroadcastReceiver.work(block: suspend () -> Unit) {
    val pending = goAsync()
    CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
        try {
            block()
        } finally {
            pending.finish()
        }
    }
}

/** Fires a habit reminder or the evening streak check, then sets tomorrow's alarm. */
class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val app = context.applicationContext as HabitudeApplication
        work {
            val today = LocalDate.now()
            val items = app.repository.snapshot()
            when (intent.action) {
                ACTION_HABIT -> {
                    val id = intent.getLongExtra(EXTRA_HABIT_ID, 0L)
                    val item = items.firstOrNull { it.habit.id == id } ?: return@work
                    Reminders.reminderFor(item, today)?.let { text ->
                        Notifications.showReminder(context, id, text, offerDone = item.habit.type == HabitType.BUILD)
                    }
                    app.reminders.schedule(item.habit)
                }
                ACTION_NUDGE -> {
                    if (!app.settings.eveningNudge) return@work
                    Reminders.nudgeFor(items, today)?.let { Notifications.showNudge(context, it) }
                    app.reminders.scheduleNudge(true)
                }
            }
        }
    }

    companion object {
        const val ACTION_HABIT = "com.janreins.habitude.REMIND_HABIT"
        const val ACTION_NUDGE = "com.janreins.habitude.STREAK_NUDGE"
    }
}

/** The "Done" button on a reminder: ticks the habit off for today. */
class DoneReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val app = context.applicationContext as HabitudeApplication
        val id = intent.getLongExtra(EXTRA_HABIT_ID, 0L)
        if (id == 0L) return
        work {
            app.repository.setEntry(id, LocalDate.now(), present = true)
            Notifications.cancel(context, id.toInt())
        }
    }
}

/** Alarms don't survive a restart, so set them all again after boot or an app update. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED && intent.action != Intent.ACTION_MY_PACKAGE_REPLACED) return
        val app = context.applicationContext as HabitudeApplication
        work { app.rescheduleReminders() }
    }
}
