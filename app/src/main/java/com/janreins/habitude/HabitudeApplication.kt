package com.janreins.habitude

import android.app.Application
import com.janreins.habitude.data.HabitRepository
import com.janreins.habitude.data.HabitudeDatabase

class HabitudeApplication : Application() {
    val repository: HabitRepository by lazy {
        HabitRepository(HabitudeDatabase.create(this).habitDao())
    }
}
