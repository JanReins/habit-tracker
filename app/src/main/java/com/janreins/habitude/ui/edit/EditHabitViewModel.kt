package com.janreins.habitude.ui.edit

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.janreins.habitude.HabitudeApplication
import com.janreins.habitude.data.HabitRepository
import com.janreins.habitude.domain.Habit
import com.janreins.habitude.domain.HabitType
import com.janreins.habitude.notify.ReminderScheduler
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate

val HabitEmojis = listOf(
    "💧", "🏃", "📚", "🧘", "🥗", "😴", "✍️", "🌱",
    "🚬", "📱", "🍷", "🍬", "☕", "🎮", "🛒", "💸",
)

data class EditHabitState(
    val isNew: Boolean = true,
    val name: String = "",
    val emoji: String = HabitEmojis.first(),
    val type: HabitType = HabitType.BUILD,
    val schedule: Set<DayOfWeek> = DayOfWeek.entries.toSet(),
    val createdOn: LocalDate = LocalDate.now(),
    /** Minutes after midnight, or null for no reminder. */
    val reminderMinutes: Int? = null,
) {
    val canSave: Boolean
        get() = name.isNotBlank() && (type == HabitType.BREAK || schedule.isNotEmpty())
}

class EditHabitViewModel(
    private val repository: HabitRepository,
    private val reminders: ReminderScheduler,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val habitId: Long = savedStateHandle.get<Long>(ARG_ID) ?: 0L

    var state by mutableStateOf(
        EditHabitState(
            type = savedStateHandle.get<String>(ARG_TYPE)?.let(HabitType::valueOf) ?: HabitType.BUILD,
        ).let { if (it.type == HabitType.BREAK) it.copy(emoji = HabitEmojis[8]) else it },
    )
        private set

    init {
        if (habitId != 0L) {
            viewModelScope.launch {
                repository.getHabit(habitId)?.let { habit ->
                    state = EditHabitState(
                        isNew = false,
                        name = habit.name,
                        emoji = habit.emoji,
                        type = habit.type,
                        schedule = habit.schedule,
                        createdOn = habit.createdOn,
                        reminderMinutes = habit.reminderMinutes,
                    )
                }
            }
        }
    }

    fun setName(name: String) { state = state.copy(name = name.take(40)) }
    fun setEmoji(emoji: String) { state = state.copy(emoji = emoji) }
    fun setType(type: HabitType) { if (state.isNew) state = state.copy(type = type) }

    fun setReminder(minutes: Int?) { state = state.copy(reminderMinutes = minutes) }

    fun toggleDay(day: DayOfWeek) {
        val days = state.schedule
        state = state.copy(schedule = if (day in days) days - day else days + day)
    }

    fun save(onSaved: () -> Unit) {
        val s = state
        if (!s.canSave) return
        viewModelScope.launch {
            val habit = Habit(
                id = habitId,
                name = s.name.trim(),
                emoji = s.emoji,
                type = s.type,
                schedule = if (s.type == HabitType.BUILD) s.schedule else DayOfWeek.entries.toSet(),
                createdOn = s.createdOn,
                reminderMinutes = s.reminderMinutes,
            )
            val id = repository.save(habit)
            reminders.schedule(habit.copy(id = id))
            onSaved()
        }
    }

    fun delete(onDeleted: () -> Unit) {
        viewModelScope.launch {
            repository.delete(habitId)
            reminders.cancel(habitId)
            onDeleted()
        }
    }

    companion object {
        const val ARG_ID = "id"
        const val ARG_TYPE = "type"

        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as HabitudeApplication
                EditHabitViewModel(app.repository, app.reminders, createSavedStateHandle())
            }
        }
    }
}
