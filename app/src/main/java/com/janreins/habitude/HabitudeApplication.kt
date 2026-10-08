package com.janreins.habitude

import android.app.Application
import com.janreins.habitude.data.HabitRepository
import com.janreins.habitude.data.HabitudeDatabase
import com.janreins.habitude.notify.Notifications
import com.janreins.habitude.notify.ReminderScheduler
import com.janreins.habitude.notify.Settings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class HabitudeApplication : Application() {
    val repository: HabitRepository by lazy {
        HabitRepository(HabitudeDatabase.create(this).habitDao())
    }
    val reminders: ReminderScheduler by lazy { ReminderScheduler(this) }
    val settings: Settings by lazy { Settings(this) }

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        Notifications.createChannels(this)
        appScope.launch { rescheduleReminders() }
    }

    suspend fun rescheduleReminders() {
        reminders.rescheduleAll(repository.snapshot().map { it.habit }, settings.eveningNudge)
    }
}
