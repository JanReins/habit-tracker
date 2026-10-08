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
    val createdOn: LocalDate,
)

/**
 * A habit with every day it has an entry for. For a [HabitType.BUILD] habit an entry
 * means "done that day"; for a [HabitType.BREAK] habit it means "slipped that day".
 */
data class HabitWithEntries(
    val habit: Habit,
    val entries: Set<LocalDate>,
)

object Schedule {
    fun toMask(days: Set<DayOfWeek>): Int = days.fold(0) { mask, day -> mask or (1 shl (day.value - 1)) }

    fun fromMask(mask: Int): Set<DayOfWeek> =
        DayOfWeek.entries.filter { mask and (1 shl (it.value - 1)) != 0 }.toSet()
}
