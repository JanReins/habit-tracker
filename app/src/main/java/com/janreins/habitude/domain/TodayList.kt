package com.janreins.habitude.domain

import java.time.LocalDate

/** How far through today's build habits you are. */
data class DayProgress(val done: Int, val due: Int)

/** What the Today tab shows, and in what order. */
object TodayList {

    /** Build habits due today (or done anyway), and how many of those are done. Archived ones don't count. */
    fun progress(items: List<HabitWithEntries>, today: LocalDate): DayProgress {
        val builds = items.filter { !it.habit.archived && it.habit.type == HabitType.BUILD }
        val due = builds.filter { isDueToday(it, today) || today in it.entries }
        return DayProgress(done = due.count { today in it.entries }, due = due.size)
    }

    /**
     * Build habits still to do today first, then the ones done, then rest days, each group
     * keeping its usual order. Break habits keep their order.
     */
    fun order(items: List<HabitWithEntries>, today: LocalDate): List<HabitWithEntries> =
        items.sortedBy { item ->
            when {
                item.habit.type == HabitType.BREAK -> 0
                today in item.entries -> 1
                isDueToday(item, today) -> 0
                else -> 2
            }
        }

    /**
     * Whether a build habit still wants doing today: due on its set days, or for a
     * times-a-week habit, this week's goal isn't met yet.
     */
    fun isDueToday(item: HabitWithEntries, today: LocalDate): Boolean {
        val target = item.habit.weeklyTarget
        return if (item.habit.isWeekly && target != null) {
            Streaks.doneInWeek(item.entries, today, today) < target
        } else {
            item.habit.isDue(today)
        }
    }
}
