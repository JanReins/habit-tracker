package com.janreins.habitude.domain

import java.time.LocalDate

/** A milestone reached by one habit, ready to celebrate. */
data class Celebration(val habitId: Long, val emoji: String, val title: String, val body: String)

/**
 * Small celebrations when a streak or clean run reaches 7, 30, 100 or 365 days (or 4, 12, 26
 * or 52 weeks for a times-a-week goal). Each one is celebrated once per run: if the run
 * breaks and starts again, its milestones can be celebrated again.
 */
object Milestones {
    val DAYS = listOf(7, 30, 100, 365)
    val WEEKS = listOf(4, 12, 26, 52)

    fun stepsFor(habit: Habit): List<Int> = if (habit.isWeekly) WEEKS else DAYS

    /** The highest milestone a run of [length] has reached, or 0 for none yet. */
    fun reached(steps: List<Int>, length: Int): Int = steps.lastOrNull { it <= length } ?: 0

    /**
     * Compares the current run with the milestone already celebrated in it. Returns the
     * milestone to celebrate now (null for none) and the value to remember from here on.
     * A run that dropped back below what was celebrated has broken, so that resets.
     */
    fun check(steps: List<Int>, current: Int, celebrated: Int): Pair<Int?, Int> {
        val reached = reached(steps, current)
        return if (reached > celebrated) reached to reached else null to minOf(celebrated, reached)
    }

    /**
     * Checks every active habit against what was already celebrated ([celebrated] by habit id).
     * Returns what to celebrate now and the new value to remember for each habit. When
     * [quiet], it only takes note and celebrates nothing.
     */
    fun review(
        items: List<HabitWithEntries>,
        today: LocalDate,
        celebrated: (Long) -> Int,
        quiet: Boolean = false,
    ): Pair<List<Celebration>, Map<Long, Int>> {
        val toCelebrate = mutableListOf<Celebration>()
        val remember = mutableMapOf<Long, Int>()
        for (item in items) {
            val habit = item.habit
            if (habit.archived) continue
            val (now, keep) = check(stepsFor(habit), Streaks.current(item, today), celebrated(habit.id))
            remember[habit.id] = keep
            if (now != null && !quiet) toCelebrate += celebrationFor(habit, now)
        }
        return toCelebrate to remember
    }

    fun celebrationFor(habit: Habit, milestone: Int): Celebration {
        val title = when {
            habit.type == HabitType.BREAK -> "$milestone days clean!"
            habit.isWeekly -> "$milestone weeks in a row!"
            else -> "A $milestone-day streak!"
        }
        val body = if (habit.isWeekly) {
            when (milestone) {
                4 -> "A whole month of hitting your goal for ${habit.name}."
                12 -> "Three months of ${habit.name}. This is part of your week now."
                26 -> "Half a year of ${habit.name}, week after week."
                else -> "A full year of ${habit.name}. Take a moment to be proud."
            }
        } else {
            when (milestone) {
                7 -> "A whole week of ${habit.name}. That's how habits start."
                30 -> "A month of ${habit.name}. It's becoming part of you."
                100 -> "100 days of ${habit.name}. Seriously impressive."
                else -> "A full year of ${habit.name}. Take a moment to be proud."
            }
        }
        return Celebration(habit.id, habit.emoji, title, body)
    }
}
