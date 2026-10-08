package com.janreins.habitude.domain

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.ChronoUnit

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
 */
object Streaks {

    fun currentBuildStreak(done: Set<LocalDate>, schedule: Set<DayOfWeek>, today: LocalDate): Int {
        val earliest = done.minOrNull() ?: return 0
        val days = schedule.ifEmpty { DayOfWeek.entries.toSet() }
        var day = today
        var streak = 0
        while (!day.isBefore(earliest)) {
            when {
                day in done -> streak++
                day == today -> Unit
                day.dayOfWeek in days -> return streak
            }
            day = day.minusDays(1)
        }
        return streak
    }

    fun bestBuildStreak(done: Set<LocalDate>, schedule: Set<DayOfWeek>, today: LocalDate): Int {
        val earliest = done.minOrNull() ?: return 0
        val days = schedule.ifEmpty { DayOfWeek.entries.toSet() }
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
                day.dayOfWeek in days -> run = 0
            }
            day = day.plusDays(1)
        }
        return best
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
