package com.janreins.habitude.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.janreins.habitude.HabitudeApplication
import com.janreins.habitude.data.HabitRepository
import com.janreins.habitude.domain.HabitWithEntries
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

/** Every habit with its full history, for the charts. Null until the first load. */
class HabitsViewModel(repository: HabitRepository) : ViewModel() {

    val habits: StateFlow<List<HabitWithEntries>?> =
        repository.habits.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    companion object {
        val Factory = viewModelFactory {
            initializer {
                HabitsViewModel((this[APPLICATION_KEY] as HabitudeApplication).repository)
            }
        }
    }
}
