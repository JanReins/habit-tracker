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
    /** Current streak for a build habit, days clean for a break habit. */
    val current: Int,
    val best: Int,
)

data class TodayUiState(
    val loading: Boolean = true,
    /** The day the cards are for. */
    val day: LocalDate? = null,
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
        val cards = habits.map { it.toCard(day) }
        TodayUiState(
            loading = false,
            day = day,
            building = cards.filter { it.type == HabitType.BUILD },
            breaking = cards.filter { it.type == HabitType.BREAK },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TodayUiState())

    /** Logs the day the cards are showing, so a tap always changes what you see. */
    fun setLoggedToday(habitId: Long, logged: Boolean) {
        val day = state.value.day ?: return
        viewModelScope.launch { repository.setEntry(habitId, day, logged) }
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
            Streaks.currentBuildStreak(entries, habit::isDue, today) to
                Streaks.bestBuildStreak(entries, habit::isDue, today)
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
        scheduledToday = habit.isDue(today),
        current = current,
        best = best,
    )
}
