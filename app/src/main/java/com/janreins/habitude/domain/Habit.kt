package com.janreins.habitude.domain

import java.time.DayOfWeek
import java.time.LocalDate

enum class HabitType {
    /** A habit to keep: tick it off on the days it's scheduled. */
    BUILD,

    /** A habit to quit: log a slip when it happens, and the clean run resets. */
    BREAK,
}

data class Habit(
    val id: Long = 0,
    val name: String,
    val emoji: String,
    val type: HabitType,
    val schedule: Set<DayOfWeek> = DayOfWeek.entries.toSet(),
    /** The day the habit started. For a break habit this is "clean since", and can be in the past. */
    val createdOn: LocalDate,
    /** Daily reminder time in minutes after midnight, or null for no reminder. */
    val reminderMinutes: Int? = null,
    /** Earlier schedules, so changing the days doesn't rewrite the past. Oldest first. */
    val pastSchedules: List<PastSchedule> = emptyList(),
    /** Archived habits keep their history but leave Today and send no reminders. */
    val archived: Boolean = false,
    /**
     * For a build habit done "X times a week" on any days: X (1 to 6). Null means it's due on
     * the days in [schedule] instead.
     */
    val weeklyTarget: Int? = null,
    /**
     * For a build habit counted through the day, like 8 glasses of water: how many make the
     * day done (2 to 99). Null means a single tick does it.
     */
    val dailyTarget: Int? = null,
) {
    /** A build habit with a times-a-week goal rather than set days. */
    val isWeekly: Boolean get() = type == HabitType.BUILD && weeklyTarget != null

    /** A build habit counted up through the day rather than ticked once. */
    val isCount: Boolean get() = type == HabitType.BUILD && dailyTarget != null

    /** Whether the habit was due on [day], using the schedule that applied then. */
    fun isDue(day: LocalDate): Boolean {
        val days = pastSchedules.firstOrNull { !day.isAfter(it.until) }?.days ?: schedule
        return day.dayOfWeek in days.ifEmpty { DayOfWeek.entries.toSet() }
    }

    /**
     * This habit with its days changed to [newDays] from [today] on. Days before [today]
     * keep the schedule they had. Changing it twice in one day keeps the first old schedule.
     */
    fun withSchedule(newDays: Set<DayOfWeek>, today: LocalDate): Habit {
        if (newDays == schedule) return this
        val yesterday = today.minusDays(1)
        val alreadyChangedToday = pastSchedules.lastOrNull()?.until?.let { !it.isBefore(yesterday) } == true
        if (yesterday.isBefore(createdOn) || alreadyChangedToday) return copy(schedule = newDays)
        return copy(schedule = newDays, pastSchedules = pastSchedules + PastSchedule(yesterday, schedule))
    }
}

/** A schedule a habit used to have, up to and including [until]. */
data class PastSchedule(val until: LocalDate, val days: Set<DayOfWeek>)

/**
 * A habit with every day it has an entry for. For a [HabitType.BUILD] habit an entry
 * means "done that day"; for a [HabitType.BREAK] habit it means "slipped that day".
 * A count habit also has its running count for each day it was counted.
 */
data class HabitWithEntries(
    val habit: Habit,
    val entries: Set<LocalDate>,
    val counts: Map<LocalDate, Int> = emptyMap(),
) {
    /** How many were counted on [day]. A day ticked done without a count counts as the goal. */
    fun countOn(day: LocalDate): Int = Counts.current(counts[day], day in entries, habit.dailyTarget ?: 1)
}

object Schedule {
    fun toMask(days: Set<DayOfWeek>): Int = days.fold(0) { mask, day -> mask or (1 shl (day.value - 1)) }

    fun fromMask(mask: Int): Set<DayOfWeek> =
        DayOfWeek.entries.filter { mask and (1 shl (it.value - 1)) != 0 }.toSet()
}
