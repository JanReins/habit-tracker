package com.janreins.habitude.ui.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.janreins.habitude.domain.DayMark
import com.janreins.habitude.domain.HabitType
import com.janreins.habitude.domain.HabitWithEntries
import com.janreins.habitude.domain.Stats
import com.janreins.habitude.domain.Streaks
import com.janreins.habitude.ui.HabitsViewModel
import com.janreins.habitude.ui.charts.CalendarHeatmap
import com.janreins.habitude.ui.charts.ChartCard
import com.janreins.habitude.ui.charts.HeatCell
import com.janreins.habitude.ui.charts.Legend
import com.janreins.habitude.ui.charts.LegendItem
import com.janreins.habitude.ui.charts.StatTile
import com.janreins.habitude.ui.charts.StreakHistory
import com.janreins.habitude.ui.charts.WeeklyBars
import com.janreins.habitude.ui.charts.chartPalette
import com.janreins.habitude.ui.dayCaption
import com.janreins.habitude.ui.percent
import com.janreins.habitude.ui.plural
import com.janreins.habitude.ui.weekLabel
import java.time.LocalDate
import kotlin.math.ceil

private const val HEATMAP_WEEKS = 20
private const val BAR_WEEKS = 12

@Composable
fun HabitDetailScreen(
    habitId: Long,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    viewModel: HabitsViewModel = viewModel(factory = HabitsViewModel.Factory),
) {
    val habits by viewModel.habits.collectAsStateWithLifecycle()
    val item = habits?.firstOrNull { it.habit.id == habitId }

    if (habits != null && item == null) {
        // The habit was deleted while we were looking at it.
        LaunchedEffect(Unit) { onBack() }
        return
    }
    if (item == null) return

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
            }
            Text(
                "${item.habit.emoji}  ${item.habit.name}",
                style = MaterialTheme.typography.headlineSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = onEdit) {
                Icon(Icons.Outlined.Edit, contentDescription = "Edit habit")
            }
        }
        HabitStats(item)
    }
}

@Composable
private fun HabitStats(item: HabitWithEntries) {
    val today = remember { LocalDate.now() }
    val palette = chartPalette()
    val habit = item.habit
    val isBuild = habit.type == HabitType.BUILD
    val color = if (isBuild) palette.build else palette.breakHabit
    val monthAgo = today.minusDays(29)

    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        if (isBuild) {
            StatTile(
                "${Streaks.currentBuildStreak(item.entries, habit.schedule, today)}",
                "Streak 🔥",
                Modifier.weight(1f),
            )
            StatTile(
                "${Streaks.bestBuildStreak(item.entries, habit.schedule, today)}",
                "Best",
                Modifier.weight(1f),
            )
            StatTile(percent(Stats.completionRate(item, monthAgo, today, today)), "Last 30 days", Modifier.weight(1f))
        } else {
            StatTile("${Streaks.daysClean(item.entries, habit.createdOn, today)}", "Days clean", Modifier.weight(1f))
            StatTile("${Streaks.bestCleanRun(item.entries, habit.createdOn, today)}", "Best run", Modifier.weight(1f))
            StatTile(
                "${item.entries.count { !it.isBefore(monthAgo) && !it.isAfter(today) }}",
                "Slips, 30 days",
                Modifier.weight(1f),
            )
        }
    }

    ChartCard("Last $HEATMAP_WEEKS weeks") {
        CalendarHeatmap(
            today = today,
            weeks = HEATMAP_WEEKS,
            cell = { day ->
                when (Stats.dayMark(item, day, today)) {
                    DayMark.DONE, DayMark.CLEAN -> HeatCell(palette.build)
                    DayMark.SLIP -> HeatCell(palette.breakHabit, dot = true)
                    DayMark.MISSED -> HeatCell(palette.missed)
                    DayMark.PENDING -> HeatCell(palette.missed, outlined = true)
                    DayMark.REST -> HeatCell(palette.faint)
                    DayMark.NONE -> HeatCell(null)
                }
            },
            describe = { day -> dayCaption(day, Stats.dayMark(item, day, today)) },
        )
        Legend(
            if (isBuild) {
                listOf(
                    LegendItem("Done", palette.build),
                    LegendItem("Missed", palette.missed),
                    LegendItem("Rest day", palette.faint),
                )
            } else {
                listOf(
                    LegendItem("Clean", palette.build),
                    LegendItem("Slipped", palette.breakHabit, dot = true),
                )
            },
        )
    }

    val weeks = remember(item, today) { Stats.weekly(item, today, BAR_WEEKS) }
    if (isBuild) {
        ChartCard("Weekly completion") {
            WeeklyBars(
                weeks = weeks,
                color = color,
                maxValue = 1f,
                ticks = listOf(0f, 0.5f, 1f),
                tickLabel = { percent(it) },
                describe = { "${weekLabel(it.weekStart)} · ${percent(it.value)} of planned days" },
            )
        }
    } else {
        val most = ceil(weeks.maxOf { it.value ?: 0f }).coerceAtLeast(2f)
        ChartCard("Slips per week") {
            WeeklyBars(
                weeks = weeks,
                color = color,
                maxValue = most,
                ticks = listOf(0f, most),
                tickLabel = { "${it.toInt()}" },
                describe = { "${weekLabel(it.weekStart)} · ${plural(it.value?.toInt() ?: 0, "slip")}" },
            )
        }
    }

    val runs = remember(item, today) { Stats.runs(item, today) }
    ChartCard(if (isBuild) "Streak history" else "Clean runs") {
        if (runs.isEmpty()) {
            Text(
                if (isBuild) "Your first streak starts with one tick." else "Your first clean day is on its way.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            StreakHistory(runs, color)
        }
    }
}
