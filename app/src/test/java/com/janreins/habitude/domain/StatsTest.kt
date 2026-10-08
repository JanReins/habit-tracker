package com.janreins.habitude.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.DayOfWeek
import java.time.DayOfWeek.MONDAY
import java.time.DayOfWeek.WEDNESDAY
import java.time.LocalDate

class StatsTest {
    // 2026-10-05 is a Monday
    private val mon = LocalDate.of(2026, 10, 5)
    private fun day(offset: Long) = mon.plusDays(offset)

    private fun build(done: Set<LocalDate>, schedule: Set<DayOfWeek> = DayOfWeek.entries.toSet(), start: LocalDate = mon) =
        HabitWithEntries(Habit(1, "Read", "📚", HabitType.BUILD, schedule, start), done)

    private fun quit(slips: Set<LocalDate>, start: LocalDate = mon) =
        HabitWithEntries(Habit(2, "Scrolling", "📱", HabitType.BREAK, DayOfWeek.entries.toSet(), start), slips)

    @Test
    fun dayMarksForBuildHabit() {
        val item = build(setOf(day(0)), schedule = setOf(MONDAY, WEDNESDAY))
        val today = day(2)
        assertEquals(DayMark.NONE, Stats.dayMark(item, day(-1), today))
        assertEquals(DayMark.DONE, Stats.dayMark(item, day(0), today))
        assertEquals(DayMark.REST, Stats.dayMark(item, day(1), today))
        assertEquals(DayMark.PENDING, Stats.dayMark(item, day(2), today))
        assertEquals(DayMark.NONE, Stats.dayMark(item, day(3), today))
        assertEquals(DayMark.MISSED, Stats.dayMark(item, day(2), day(3)))
    }

    @Test
    fun dayMarksForBreakHabit() {
        val item = quit(setOf(day(1)))
        assertEquals(DayMark.CLEAN, Stats.dayMark(item, day(0), day(2)))
        assertEquals(DayMark.SLIP, Stats.dayMark(item, day(1), day(2)))
    }

    @Test
    fun completionRateIgnoresTodayUntilDone() {
        val item = build(setOf(day(0), day(1)))
        assertEquals(1f, Stats.completionRate(item, day(0), day(2), day(2))!!, 0.001f)
        assertEquals(2f / 3, Stats.completionRate(item, day(0), day(3), day(3))!!, 0.001f)
        assertNull(Stats.completionRate(item, day(-7), day(-1), day(3)))
    }

    @Test
    fun cleanRateForBreakHabit() {
        val item = quit(setOf(day(1)))
        assertEquals(3f / 4, Stats.completionRate(item, day(0), day(3), day(3))!!, 0.001f)
    }

    @Test
    fun weeklyStatsEndWithCurrentWeek() {
        val item = build(setOf(day(0), day(1), day(7)), start = mon)
        val weeks = Stats.weekly(item, today = day(8), weeks = 3)
        assertEquals(listOf(mon.minusWeeks(1), mon, mon.plusWeeks(1)), weeks.map { it.weekStart })
        assertNull(weeks[0].value)
        assertEquals(2f / 7, weeks[1].value!!, 0.001f)
        assertEquals(1f, weeks[2].value!!, 0.001f) // Monday done, Tuesday still pending
    }

    @Test
    fun weeklySlipCounts() {
        val item = quit(setOf(day(1), day(3), day(9)))
        val weeks = Stats.weekly(item, today = day(10), weeks = 2)
        assertEquals(listOf(2f, 1f), weeks.map { it.value })
    }

    @Test
    fun buildRunsMatchBestStreak() {
        val done = setOf(day(0), day(1), day(2), day(4), day(5))
        val item = build(done)
        val runs = Stats.runs(item, day(5))
        assertEquals(listOf(3, 2), runs.map { it.length })
        assertEquals(listOf(false, true), runs.map { it.ongoing })
        assertEquals(Streaks.bestBuildStreak(done, item.habit.schedule, day(5)), runs.maxOf { it.length })
    }

    @Test
    fun breakRunsMatchBestCleanRun() {
        val slips = setOf(day(6), day(8))
        val item = quit(slips)
        val runs = Stats.runs(item, day(10))
        assertEquals(listOf(5, 1, 2), runs.map { it.length })
        assertEquals(Streaks.bestCleanRun(slips, mon, day(10)), runs.maxOf { it.length })
    }

    @Test
    fun dailyOverallMixesBuildHabits() {
        val a = build(setOf(day(0), day(1)))
        val b = build(setOf(day(0)), schedule = setOf(MONDAY))
        val overall = Stats.dailyOverall(listOf(a, b, quit(emptySet())), day(0), day(3), today = day(2))
        assertEquals(1f, overall[day(0)]!!, 0.001f)
        assertEquals(1f, overall[day(1)]!!, 0.001f)
        assertEquals(0f, overall[day(2)]!!, 0.001f)
        assertNull(overall[day(3)])
    }
}
