package com.janreins.habitude.domain

import java.time.LocalDate

/** How far through today's build habits you are. */
data class DayProgress(val done: Int, val due: Int)

/** What the Today tab shows, and in what order. */
object TodayList {

    /** Build habits due today (or done anyway), and how many of those are done. Archived ones don't count. */
    fun progress(items: List<HabitWithEntries>, today: LocalDate): DayProgress {
        val builds = items.filter { !it.habit.archived && it.habit.type == HabitType.BUILD }
        val due = builds.filter { it.habit.isDue(today) || today in it.entries }
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
                item.habit.isDue(today) -> 0
                else -> 2
            }
        }
}
