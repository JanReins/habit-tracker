package com.janreins.habitude.ui.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.janreins.habitude.HabitudeApplication
import com.janreins.habitude.data.HabitRepository
import com.janreins.habitude.domain.DayProgress
import com.janreins.habitude.domain.HabitType
import com.janreins.habitude.domain.HabitWithEntries
import com.janreins.habitude.domain.Streaks
import com.janreins.habitude.domain.TodayList
import com.janreins.habitude.ui.DayClock
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
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
    /** Current streak for a build habit (weeks for a weekly goal), days clean for a break habit. */
    val current: Int,
    val best: Int,
    /** For a times-a-week habit: the goal, and how many days are done this week. */
    val weeklyTarget: Int? = null,
    val doneThisWeek: Int = 0,
    /** For a habit counted through the day: the goal, and today's count so far. */
    val dailyTarget: Int? = null,
    val countToday: Int = 0,
)

data class TodayUiState(
    val loading: Boolean = true,
    /** The day the cards are for. */
    val day: LocalDate? = null,
    /** Build habits done out of those due today. */
    val progress: DayProgress = DayProgress(0, 0),
    val building: List<HabitCardState> = emptyList(),
    val breaking: List<HabitCardState> = emptyList(),
) {
    val isEmpty: Boolean get() = building.isEmpty() && breaking.isEmpty()
}

class TodayViewModel(
    private val repository: HabitRepository,
    today: Flow<LocalDate> = DayClock.today,
) : ViewModel() {

    val state: StateFlow<TodayUiState> = combine(repository.habits, today) { habits, day ->
        val active = habits.filter { !it.habit.archived }
        val cards = TodayList.order(active, day).map { it.toCard(day) }
        TodayUiState(
            loading = false,
            day = day,
            progress = TodayList.progress(active, day),
            building = cards.filter { it.type == HabitType.BUILD },
            breaking = cards.filter { it.type == HabitType.BREAK },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TodayUiState())

    /** Logs the day the cards are showing, so a tap always changes what you see. */
    fun setLoggedToday(habitId: Long, logged: Boolean) {
        val day = state.value.day ?: return
        viewModelScope.launch { repository.setEntry(habitId, day, logged) }
    }

    /** Logs a slip on the day shown, with an optional note on what led to it. */
    fun logSlip(habitId: Long, note: String) {
        val day = state.value.day ?: return
        viewModelScope.launch {
            repository.setEntry(habitId, day, true)
            if (note.isNotBlank()) repository.setNote(habitId, day, note)
        }
    }

    /** Counts a counted habit up (or down, with a negative [delta]) on the day shown. */
    fun addCount(habitId: Long, delta: Int) {
        val day = state.value.day ?: return
        viewModelScope.launch { repository.addCount(habitId, day, delta) }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                TodayViewModel((this[APPLICATION_KEY] as HabitudeApplication).repository)
            }
        }
    }
}

internal fun HabitWithEntries.toCard(today: LocalDate): HabitCardState =
    HabitCardState(
        id = habit.id,
        name = habit.name,
        emoji = habit.emoji,
        type = habit.type,
        loggedToday = today in entries,
        scheduledToday = TodayList.isDueToday(this, today),
        current = Streaks.current(this, today),
        best = Streaks.best(this, today),
        weeklyTarget = habit.weeklyTarget.takeIf { habit.isWeekly },
        doneThisWeek = Streaks.doneInWeek(entries, today, today),
        dailyTarget = habit.dailyTarget.takeIf { habit.isCount },
        countToday = countOn(today),
    )
