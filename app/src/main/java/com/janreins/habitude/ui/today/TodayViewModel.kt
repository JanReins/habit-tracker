package com.janreins.habitude.ui.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.janreins.habitude.HabitudeApplication
import com.janreins.habitude.data.HabitRepository
import com.janreins.habitude.domain.HabitType
import com.janreins.habitude.domain.HabitWithEntries
import com.janreins.habitude.domain.Streaks
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

data class HabitCardState(
    val id: Long,
    val name: String,
    val emoji: String,
    val type: HabitType,
    /** Done today for a build habit, slipped today for a break habit. */
    val loggedToday: Boolean,
    val scheduledToday: Boolean,
    /** Current streak for a build habit, days clean for a break habit. */
    val current: Int,
    val best: Int,
)

data class TodayUiState(
    val loading: Boolean = true,
    val building: List<HabitCardState> = emptyList(),
    val breaking: List<HabitCardState> = emptyList(),
) {
    val isEmpty: Boolean get() = building.isEmpty() && breaking.isEmpty()
}

class TodayViewModel(
    private val repository: HabitRepository,
    private val today: () -> LocalDate = LocalDate::now,
) : ViewModel() {

    val state: StateFlow<TodayUiState> = repository.habits
        .map { habits ->
            val cards = habits.map { it.toCard(today()) }
            TodayUiState(
                loading = false,
                building = cards.filter { it.type == HabitType.BUILD },
                breaking = cards.filter { it.type == HabitType.BREAK },
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TodayUiState())

    fun setLoggedToday(habitId: Long, logged: Boolean) {
        viewModelScope.launch { repository.setEntry(habitId, today(), logged) }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                TodayViewModel((this[APPLICATION_KEY] as HabitudeApplication).repository)
            }
        }
    }
}

internal fun HabitWithEntries.toCard(today: LocalDate): HabitCardState {
    val (current, best) = when (habit.type) {
        HabitType.BUILD ->
            Streaks.currentBuildStreak(entries, habit.schedule, today) to
                Streaks.bestBuildStreak(entries, habit.schedule, today)
        HabitType.BREAK ->
            Streaks.daysClean(entries, habit.createdOn, today) to
                Streaks.bestCleanRun(entries, habit.createdOn, today)
    }
    return HabitCardState(
        id = habit.id,
        name = habit.name,
        emoji = habit.emoji,
        type = habit.type,
        loggedToday = today in entries,
        scheduledToday = today.dayOfWeek in habit.schedule,
        current = current,
        best = best,
    )
}
