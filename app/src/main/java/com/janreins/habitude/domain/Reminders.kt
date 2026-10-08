package com.janreins.habitude.domain

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

data class ReminderText(val title: String, val body: String)

/** What reminders say and when they fire. No Android here, so it can be unit tested. */
object Reminders {

    /** Evening check for streaks that are about to break, in minutes after midnight (8pm). */
    const val NUDGE_MINUTES = 20 * 60

    /** The next moment a daily reminder set for [minutes] after midnight should fire. */
    fun nextTrigger(now: LocalDateTime, minutes: Int): LocalDateTime {
        val today = now.toLocalDate().atTime(LocalTime.of(minutes / 60, minutes % 60))
        return if (today.isAfter(now)) today else today.plusDays(1)
    }

    /** The reminder for one habit today, or null if there's nothing to remind about. */
    fun reminderFor(item: HabitWithEntries, today: LocalDate): ReminderText? {
        val habit = item.habit
        val title = "${habit.emoji} ${habit.name}"
        return when (habit.type) {
            HabitType.BUILD -> {
                if (today in item.entries || today.dayOfWeek !in habit.schedule) return null
                val streak = Streaks.currentBuildStreak(item.entries, habit.schedule, today)
                val body = if (streak > 0) "Keep your ${dayWord(streak)} streak going 🔥" else "A good day to start a streak."
                ReminderText(title, body)
            }
            HabitType.BREAK -> {
                if (today in item.entries) return null
                val clean = Streaks.daysClean(item.entries, habit.createdOn, today)
                val body = if (clean > 0) "${if (clean == 1) "1 day" else "$clean days"} clean. You've got this." else "Today's a clean slate. You've got this."
                ReminderText(title, body)
            }
        }
    }

    /** Build habits due today, not done yet, with a streak of at least [minStreak] that would break tonight. */
    fun atRisk(items: List<HabitWithEntries>, today: LocalDate, minStreak: Int = 2): List<Pair<HabitWithEntries, Int>> =
        items
            .filter { it.habit.type == HabitType.BUILD && today.dayOfWeek in it.habit.schedule && today !in it.entries }
            .map { it to Streaks.currentBuildStreak(it.entries, it.habit.schedule, today) }
            .filter { (_, streak) -> streak >= minStreak }
            .sortedByDescending { (_, streak) -> streak }

    fun nudgeFor(items: List<HabitWithEntries>, today: LocalDate): ReminderText? {
        val risky = atRisk(items, today)
        return when (risky.size) {
            0 -> null
            1 -> {
                val (item, streak) = risky.first()
                ReminderText(
                    "Your ${dayWord(streak)} streak is at risk",
                    "${item.habit.emoji} ${item.habit.name} still needs a tick today.",
                )
            }
            else -> ReminderText(
                "${risky.size} streaks need you tonight",
                risky.joinToString(", ") { (item, streak) -> "${item.habit.emoji} ${item.habit.name} ($streak)" },
            )
        }
    }

    private fun dayWord(n: Int) = if (n == 1) "1-day" else "$n-day"
}
