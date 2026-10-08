package com.janreins.habitude.domain

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters

/**
 * Streak maths, kept free of Android so it can be unit tested.
 *
 * Build habits: a streak counts the days the habit was done, walking back from today.
 * A scheduled day that was missed ends the streak; an unscheduled day (a rest day) is
 * skipped, and still counts if the habit was done anyway. Today never breaks a streak,
 * because the day isn't over yet.
 *
 * Break habits: "days clean" is the number of days since the last slip, or since the
 * habit was started if there hasn't been one.
 *
 * Times-a-week habits: a streak counts weeks (Monday to Sunday) in a row that met the goal.
 * This week only adds once the goal is met, and never breaks the streak while it's going.
 */
object Streaks {

    /** The current streak for any habit: days, weeks for a times-a-week habit, or days clean. */
    fun current(item: HabitWithEntries, today: LocalDate): Int {
        val habit = item.habit
        return when {
            habit.type == HabitType.BREAK -> daysClean(item.entries, habit.createdOn, today)
            habit.isWeekly -> currentWeeklyStreak(item.entries, habit.weeklyTarget!!, habit.createdOn, today)
            else -> currentBuildStreak(item.entries, habit::isDue, today)
        }
    }

    /** The best streak for any habit, in the same unit as [current]. */
    fun best(item: HabitWithEntries, today: LocalDate): Int {
        val habit = item.habit
        return when {
            habit.type == HabitType.BREAK -> bestCleanRun(item.entries, habit.createdOn, today)
            habit.isWeekly -> bestWeeklyStreak(item.entries, habit.weeklyTarget!!, habit.createdOn, today)
            else -> bestBuildStreak(item.entries, habit::isDue, today)
        }
    }

    /** Days done in the Monday-to-Sunday week that contains [day], up to [today]. */
    fun doneInWeek(done: Set<LocalDate>, day: LocalDate, today: LocalDate): Int {
        val start = weekStart(day)
        val end = minOf(start.plusDays(6), today)
        return done.count { !it.isBefore(start) && !it.isAfter(end) }
    }

    fun currentWeeklyStreak(done: Set<LocalDate>, target: Int, startedOn: LocalDate, today: LocalDate): Int {
        val first = weekStart(minOf(startedOn, done.minOrNull() ?: startedOn))
        var week = weekStart(today)
        var streak = if (doneInWeek(done, week, today) >= target) 1 else 0
        week = week.minusWeeks(1)
        while (!week.isBefore(first) && doneInWeek(done, week, today) >= target) {
            streak++
            week = week.minusWeeks(1)
        }
        return streak
    }

    fun bestWeeklyStreak(done: Set<LocalDate>, target: Int, startedOn: LocalDate, today: LocalDate): Int {
        val thisWeek = weekStart(today)
        var week = weekStart(minOf(startedOn, done.minOrNull() ?: startedOn))
        var run = 0
        var best = 0
        while (!week.isAfter(thisWeek)) {
            if (doneInWeek(done, week, today) >= target) {
                run++
                best = maxOf(best, run)
            } else if (week != thisWeek) {
                run = 0
            }
            week = week.plusWeeks(1)
        }
        return best
    }

    private fun weekStart(day: LocalDate): LocalDate = day.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

    fun currentBuildStreak(done: Set<LocalDate>, schedule: Set<DayOfWeek>, today: LocalDate): Int =
        currentBuildStreak(done, fixed(schedule), today)

    fun bestBuildStreak(done: Set<LocalDate>, schedule: Set<DayOfWeek>, today: LocalDate): Int =
        bestBuildStreak(done, fixed(schedule), today)

    /** [isDue] says whether a day was scheduled, e.g. [Habit.isDue]. */
    fun currentBuildStreak(done: Set<LocalDate>, isDue: (LocalDate) -> Boolean, today: LocalDate): Int {
        val earliest = done.minOrNull() ?: return 0
        var day = today
        var streak = 0
        while (!day.isBefore(earliest)) {
            when {
                day in done -> streak++
                day == today -> Unit
                isDue(day) -> return streak
            }
            day = day.minusDays(1)
        }
        return streak
    }

    fun bestBuildStreak(done: Set<LocalDate>, isDue: (LocalDate) -> Boolean, today: LocalDate): Int {
        val earliest = done.minOrNull() ?: return 0
        var day = earliest
        var run = 0
        var best = 0
        while (!day.isAfter(today)) {
            when {
                day in done -> {
                    run++
                    best = maxOf(best, run)
                }
                day == today -> Unit
                isDue(day) -> run = 0
            }
            day = day.plusDays(1)
        }
        return best
    }

    private fun fixed(schedule: Set<DayOfWeek>): (LocalDate) -> Boolean {
        val days = schedule.ifEmpty { DayOfWeek.entries.toSet() }
        return { it.dayOfWeek in days }
    }

    fun daysClean(slips: Set<LocalDate>, startedOn: LocalDate, today: LocalDate): Int {
        val lastSlip = slips.filter { !it.isAfter(today) }.maxOrNull()
        val from = maxOf(lastSlip ?: startedOn, startedOn)
        return ChronoUnit.DAYS.between(from, today).toInt().coerceAtLeast(0)
    }

    /** The longest clean run reached so far, including the one in progress. */
    fun bestCleanRun(slips: Set<LocalDate>, startedOn: LocalDate, today: LocalDate): Int {
        val sorted = slips.filter { !it.isAfter(today) && !it.isBefore(startedOn) }.sorted()
        var best = daysClean(slips, startedOn, today)
        var from = startedOn
        for (slip in sorted) {
            // The day before a slip is the last day the counter was still climbing.
            best = maxOf(best, ChronoUnit.DAYS.between(from, slip).toInt() - 1)
            from = slip
        }
        return best.coerceAtLeast(0)
    }
}
