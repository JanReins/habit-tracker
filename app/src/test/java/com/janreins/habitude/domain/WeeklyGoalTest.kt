package com.janreins.habitude.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

class WeeklyGoalTest {
    // 2026-10-05 is a Monday
    private val mon = LocalDate.of(2026, 10, 5)
    private fun day(offset: Long) = mon.plusDays(offset)

    private fun gym(done: Set<LocalDate>, start: LocalDate = mon.minusWeeks(4), target: Int = 3) =
        HabitWithEntries(Habit(1, "Gym", "🏃", HabitType.BUILD, createdOn = start, weeklyTarget = target), done)

    /** Three sessions in each of the [weeksBack] weeks before this one. */
    private fun pastWeeks(weeksBack: Int) =
        (1..weeksBack).flatMap { w -> listOf(0L, 2L, 4L).map { mon.minusWeeks(w.toLong()).plusDays(it) } }.toSet()

    @Test
    fun streakCountsWeeksThatMetTheGoal() {
        val item = gym(pastWeeks(3))
        assertEquals(3, Streaks.current(item, day(2))) // this week in progress doesn't break it
        val thisWeekDone = gym(pastWeeks(3) + setOf(day(0), day(1), day(2)))
        assertEquals(4, Streaks.current(thisWeekDone, day(2)))
        assertEquals(4, Streaks.best(thisWeekDone, day(2)))
    }

    @Test
    fun aShortWeekEndsTheStreak() {
        val done = pastWeeks(3) - mon.minusWeeks(2) // two weeks ago only had two sessions
        val item = gym(done)
        assertEquals(1, Streaks.current(item, day(2)))
        assertEquals(1, Streaks.best(item, day(2)))
    }

    @Test
    fun runsAreInWeeks() {
        val done = pastWeeks(4) - mon.minusWeeks(2)
        val runs = Stats.runs(gym(done), day(2))
        assertEquals(listOf(2, 1), runs.map { it.length })
        assertTrue(runs.last().ongoing)
        assertEquals(Streaks.best(gym(done), day(2)), runs.maxOf { it.length })
    }

    @Test
    fun noDayIsMissedButTheWeekCounts() {
        val item = gym(setOf(day(0)), start = mon)
        assertEquals(DayMark.REST, Stats.dayMark(item, day(1), day(3)))
        assertEquals(DayMark.DONE, Stats.dayMark(item, day(0), day(3)))
        // This week isn't over and isn't met yet, so it doesn't count against the rate.
        assertNull(Stats.completionRate(item, mon, day(3), day(3)))
        assertEquals(1f / 3, Stats.weekly(item, day(3), 1).single().value!!, 0.001f)
    }

    @Test
    fun finishedWeeksCountTowardsTheRate() {
        val item = gym(setOf(mon.minusWeeks(1), mon.minusWeeks(1).plusDays(1)), start = mon.minusWeeks(1))
        assertEquals(2f / 3, Stats.completionRate(item, mon.minusWeeks(1), day(2), day(2))!!, 0.001f)
    }

    @Test
    fun dueTodayUntilTheGoalIsMet() {
        assertTrue(TodayList.isDueToday(gym(setOf(day(0), day(1))), day(2)))
        assertFalse(TodayList.isDueToday(gym(setOf(day(0), day(1), day(2))), day(3)))
        assertEquals(DayProgress(done = 0, due = 0), TodayList.progress(listOf(gym(setOf(day(0), day(1), day(2)))), day(3)))
    }

    @Test
    fun reminderSaysHowManyAreLeft() {
        assertEquals("1 of 3 this week. Today's a good day for one more.", Reminders.reminderFor(gym(setOf(day(0))), day(2))!!.body)
        assertNull(Reminders.reminderFor(gym(setOf(day(0), day(1), day(2))), day(3)))
    }

    @Test
    fun nudgeOnlyWhenEveryDayLeftIsNeeded() {
        // Friday, one done this week, two needed and three days left: still time.
        assertEquals(emptyList<Any>(), Reminders.atRisk(listOf(gym(pastWeeks(2) + day(0))), day(4)))
        // Saturday, one done, two needed, two days left: tonight matters.
        val risky = gym(pastWeeks(2) + day(0))
        assertEquals("Your 2-week streak is at risk", Reminders.nudgeFor(listOf(risky), day(5))!!.title)
    }

    @Test
    fun backupKeepsTheGoal() {
        val items = listOf(gym(pastWeeks(1)))
        val back = Backup.parse(Backup.export(items, true, LocalDateTime.of(2026, 10, 8, 9, 0)))
        assertEquals(items, back.habits)
    }
}
