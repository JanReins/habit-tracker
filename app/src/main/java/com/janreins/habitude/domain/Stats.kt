package com.janreins.habitude.domain

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters

/** What happened on one day for one habit, as drawn on the calendar heatmap. */
enum class DayMark {
    /** Build habit, done. */
    DONE,

    /** Build habit, scheduled but not done. */
    MISSED,

    /** Build habit, not scheduled that day. */
    REST,

    /** Build habit, scheduled today and not done yet. */
    PENDING,

    /** Break habit, no slip. */
    CLEAN,

    /** Break habit, slipped. */
    SLIP,

    /** Before the habit started, or in the future. */
    NONE,
}

/** One week of a habit. For build habits [value] is the share of scheduled days done (0..1); for break habits it's the number of slips. */
data class WeekStat(val weekStart: LocalDate, val value: Float?)

/** A streak (build) or clean run (break) that has ended, or is still going if [ongoing]. */
data class Run(val end: LocalDate, val length: Int, val ongoing: Boolean)

object Stats {

    fun weekStart(day: LocalDate): LocalDate = day.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

    fun dayMark(item: HabitWithEntries, day: LocalDate, today: LocalDate): DayMark {
        val habit = item.habit
        if (day.isBefore(habit.createdOn) || day.isAfter(today)) return DayMark.NONE
        val logged = day in item.entries
        return when (habit.type) {
            HabitType.BREAK -> if (logged) DayMark.SLIP else DayMark.CLEAN
            HabitType.BUILD -> when {
                logged -> DayMark.DONE
                day.dayOfWeek !in habit.schedule -> DayMark.REST
                day == today -> DayMark.PENDING
                else -> DayMark.MISSED
            }
        }
    }

    /**
     * Share of scheduled days done between [from] and [to] (inclusive) for a build habit,
     * or share of clean days for a break habit. Null when there's nothing to measure yet.
     */
    fun completionRate(item: HabitWithEntries, from: LocalDate, to: LocalDate, today: LocalDate): Float? {
        var hits = 0
        var total = 0
        var day = from
        while (!day.isAfter(to)) {
            val scheduled = day.dayOfWeek in item.habit.schedule
            when (dayMark(item, day, today)) {
                DayMark.DONE -> if (scheduled) { hits++; total++ }
                DayMark.MISSED -> total++
                DayMark.CLEAN -> { hits++; total++ }
                DayMark.SLIP -> total++
                else -> Unit
            }
            day = day.plusDays(1)
        }
        return if (total == 0) null else hits.toFloat() / total
    }

    /** The last [weeks] weeks, oldest first, ending with the week containing [today]. */
    fun weekly(item: HabitWithEntries, today: LocalDate, weeks: Int): List<WeekStat> {
        val lastWeek = weekStart(today)
        return (weeks - 1 downTo 0).map { back ->
            val start = lastWeek.minusWeeks(back.toLong())
            val end = start.plusDays(6)
            val value = when (item.habit.type) {
                HabitType.BUILD -> completionRate(item, start, end, today)
                HabitType.BREAK ->
                    if (end.isBefore(item.habit.createdOn) || start.isAfter(today)) null
                    else item.entries.count { !it.isBefore(start) && !it.isAfter(end) }.toFloat()
            }
            WeekStat(start, value)
        }
    }

    /**
     * Every streak (build) or clean run (break) so far, oldest first. Lengths follow the same
     * rules as [Streaks], so the longest run always equals the "best" figure on the card.
     */
    fun runs(item: HabitWithEntries, today: LocalDate): List<Run> {
        val habit = item.habit
        return when (habit.type) {
            HabitType.BUILD -> buildRuns(item, today)
            HabitType.BREAK -> {
                val slips = item.entries.filter { !it.isAfter(today) && !it.isBefore(habit.createdOn) }.sorted()
                val result = mutableListOf<Run>()
                var from = habit.createdOn
                for (slip in slips) {
                    val length = ChronoUnit.DAYS.between(from, slip).toInt() - 1
                    if (length > 0) result += Run(slip.minusDays(1), length, ongoing = false)
                    from = slip
                }
                val current = Streaks.daysClean(item.entries, habit.createdOn, today)
                if (current > 0) result += Run(today, current, ongoing = true)
                result
            }
        }
    }

    private fun buildRuns(item: HabitWithEntries, today: LocalDate): List<Run> {
        val result = mutableListOf<Run>()
        var day = item.habit.createdOn
        var length = 0
        var lastDone: LocalDate? = null
        while (!day.isAfter(today)) {
            when (dayMark(item, day, today)) {
                DayMark.DONE -> { length++; lastDone = day }
                DayMark.MISSED -> {
                    if (length > 0) result += Run(lastDone!!, length, ongoing = false)
                    length = 0
                }
                else -> Unit
            }
            day = day.plusDays(1)
        }
        if (length > 0) result += Run(lastDone!!, length, ongoing = true)
        return result
    }

    /**
     * For each day from [from] to [to], the share of build habits done out of those scheduled
     * (or done anyway) that day. Null for days with nothing scheduled or in the future.
     */
    fun dailyOverall(items: List<HabitWithEntries>, from: LocalDate, to: LocalDate, today: LocalDate): Map<LocalDate, Float?> {
        val builds = items.filter { it.habit.type == HabitType.BUILD }
        val result = LinkedHashMap<LocalDate, Float?>()
        var day = from
        while (!day.isAfter(to)) {
            var done = 0
            var due = 0
            for (item in builds) {
                when (dayMark(item, day, today)) {
                    DayMark.DONE -> { done++; due++ }
                    DayMark.MISSED, DayMark.PENDING -> due++
                    else -> Unit
                }
            }
            result[day] = if (due == 0) null else done.toFloat() / due
            day = day.plusDays(1)
        }
        return result
    }
}
