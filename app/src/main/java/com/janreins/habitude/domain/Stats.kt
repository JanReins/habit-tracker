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
                // Any day can count towards a times-a-week goal, so no single day is "missed".
                habit.isWeekly || !habit.isDue(day) -> DayMark.REST
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
        if (item.habit.isWeekly) return weeklyRate(item, from, to, today)
        var hits = 0
        var total = 0
        var day = from
        while (!day.isAfter(to)) {
            val scheduled = item.habit.isDue(day)
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

    /**
     * For a times-a-week habit: the share of the goal met across the weeks touching [from]..[to],
     * counting each week's done days up to the goal. This week only counts once it's met.
     */
    private fun weeklyRate(item: HabitWithEntries, from: LocalDate, to: LocalDate, today: LocalDate): Float? {
        val target = item.habit.weeklyTarget ?: return null
        val firstWeek = weekStart(item.habit.createdOn)
        var hits = 0
        var total = 0
        var week = weekStart(from)
        while (!week.isAfter(to) && !week.isAfter(today)) {
            if (!week.isBefore(firstWeek)) {
                val done = Streaks.doneInWeek(item.entries, week, today).coerceAtMost(target)
                val finished = week.plusDays(6).isBefore(today)
                if (finished || done >= target) {
                    hits += done
                    total += target
                }
            }
            week = week.plusWeeks(1)
        }
        return if (total == 0) null else hits.toFloat() / total
    }

    /** The last [weeks] weeks, oldest first, ending with the week containing [today]. */
    fun weekly(item: HabitWithEntries, today: LocalDate, weeks: Int): List<WeekStat> {
        val lastWeek = weekStart(today)
        return (weeks - 1 downTo 0).map { back ->
            val start = lastWeek.minusWeeks(back.toLong())
            val end = start.plusDays(6)
            val target = item.habit.weeklyTarget
            val value = when {
                item.habit.isWeekly && target != null ->
                    if (end.isBefore(item.habit.createdOn) || start.isAfter(today)) null
                    else Streaks.doneInWeek(item.entries, start, today).coerceAtMost(target).toFloat() / target
                item.habit.type == HabitType.BUILD -> completionRate(item, start, end, today)
                else ->
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
            HabitType.BUILD -> if (habit.isWeekly) weeklyRuns(item, today) else buildRuns(item, today)
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

    /** Runs of weeks that met a times-a-week goal. Lengths are in weeks. */
    private fun weeklyRuns(item: HabitWithEntries, today: LocalDate): List<Run> {
        val target = item.habit.weeklyTarget ?: return emptyList()
        val thisWeek = weekStart(today)
        val result = mutableListOf<Run>()
        var week = weekStart(minOf(item.habit.createdOn, item.entries.minOrNull() ?: item.habit.createdOn))
        var length = 0
        var lastEnd = today
        while (!week.isAfter(thisWeek)) {
            if (Streaks.doneInWeek(item.entries, week, today) >= target) {
                length++
                lastEnd = if (week == thisWeek) today else week.plusDays(6)
            } else if (week != thisWeek) {
                if (length > 0) result += Run(lastEnd, length, ongoing = false)
                length = 0
            }
            week = week.plusWeeks(1)
        }
        if (length > 0) result += Run(lastEnd, length, ongoing = true)
        return result
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
