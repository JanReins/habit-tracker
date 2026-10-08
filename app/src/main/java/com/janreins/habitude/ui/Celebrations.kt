package com.janreins.habitude.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.janreins.habitude.HabitudeApplication
import com.janreins.habitude.data.HabitRepository
import com.janreins.habitude.domain.Celebration
import com.janreins.habitude.domain.Milestones
import com.janreins.habitude.notify.Settings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

/**
 * Watches every habit for a streak or clean run reaching a milestone, wherever it was
 * ticked (the app, the widget or a reminder), and queues a celebration for each.
 */
class CelebrationViewModel(
    repository: HabitRepository,
    private val settings: Settings,
    today: Flow<LocalDate> = DayClock.today,
) : ViewModel() {

    private val queue = MutableStateFlow<List<Celebration>>(emptyList())

    /** The celebration to show now, if any. */
    val current: StateFlow<Celebration?> =
        queue.map { it.firstOrNull() }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    init {
        viewModelScope.launch {
            combine(repository.habits, today) { items, day -> items to day }.collect { (items, day) ->
                val (found, remember) = Milestones.review(
                    items,
                    day,
                    celebrated = settings::celebratedMilestone,
                    quiet = !settings.milestonesStarted,
                )
                remember.forEach { (id, milestone) -> settings.setCelebratedMilestone(id, milestone) }
                settings.milestonesStarted = true
                if (found.isNotEmpty()) {
                    // A newer milestone for the same habit replaces one still waiting.
                    queue.update { waiting -> waiting.filter { old -> found.none { it.habitId == old.habitId } } + found }
                }
            }
        }
    }

    fun dismiss() = queue.update { it.drop(1) }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as HabitudeApplication
                CelebrationViewModel(app.repository, app.settings)
            }
        }
    }
}

/** Shows queued milestone celebrations one at a time. */
@Composable
fun CelebrationHost(viewModel: CelebrationViewModel = viewModel(factory = CelebrationViewModel.Factory)) {
    val celebration by viewModel.current.collectAsStateWithLifecycle()
    celebration?.let { CelebrationDialog(it, onDismiss = viewModel::dismiss) }
}

@Composable
private fun CelebrationDialog(celebration: Celebration, onDismiss: () -> Unit) {
    val pop = remember(celebration) { Animatable(0.4f) }
    LaunchedEffect(celebration) {
        pop.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow))
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Text("🎉 ${celebration.emoji}", fontSize = 40.sp, modifier = Modifier.scale(pop.value))
        },
        title = { Text(celebration.title, textAlign = TextAlign.Center) },
        text = {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Text(celebration.body, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Thanks!") } },
    )
}
