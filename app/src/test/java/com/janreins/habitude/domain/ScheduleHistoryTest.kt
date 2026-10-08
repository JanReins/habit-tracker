package com.janreins.habitude.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.DayOfWeek.FRIDAY
import java.time.DayOfWeek.MONDAY
import java.time.DayOfWeek.THURSDAY
import java.time.DayOfWeek.TUESDAY
import java.time.DayOfWeek.WEDNESDAY
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime

class ScheduleHistoryTest {
    // 2026-10-05 is a Monday
    private val mon = LocalDate.of(2026, 10, 5)
    private fun day(offset: Long) = mon.plusDays(offset)
    private val weekdays = setOf(MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY)
    private val everyDay = DayOfWeek.entries.toSet()

    private fun habit(start: LocalDate, schedule: Set<DayOfWeek> = weekdays) =
        Habit(1, "Run", "🏃", HabitType.BUILD, schedule, start)

    @Test
    fun changingDaysKeepsPastStreaks() {
        val start = mon.minusWeeks(4)
        val today = day(3) // Thursday
        // Done every weekday for four weeks, up to yesterday.
        val done = (1..31L).map { today.minusDays(it) }.filter { !it.isBefore(start) && it.dayOfWeek in weekdays }.toSet()
        val before = habit(start)
        val after = before.withSchedule(everyDay, today)

        assertEquals(Streaks.currentBuildStreak(done, before::isDue, today), Streaks.currentBuildStreak(done, after::isDue, today))
        assertEquals(Streaks.bestBuildStreak(done, before::isDue, today), Streaks.bestBuildStreak(done, after::isDue, today))
        // From today on the new days count: missing Saturday now ends the streak.
        assertEquals(0, Streaks.currentBuildStreak(done, after::isDue, day(6)))
    }

    @Test
    fun newScheduleAppliesFromToday() {
        val h = habit(mon.minusWeeks(2)).withSchedule(everyDay, day(3))
        assertEquals(listOf(PastSchedule(day(2), weekdays)), h.pastSchedules)
        assertFalse(h.isDue(day(-1))) // last Sunday, old schedule
        assertTrue(h.isDue(day(5))) // this Saturday, new schedule
    }

    @Test
    fun twoChangesInOneDayKeepTheFirstOldSchedule() {
        val h = habit(mon.minusWeeks(2))
            .withSchedule(everyDay, day(3))
            .withSchedule(setOf(MONDAY), day(3))
        assertEquals(listOf(PastSchedule(day(2), weekdays)), h.pastSchedules)
        assertEquals(setOf(MONDAY), h.schedule)
    }

    @Test
    fun changesOnDifferentDaysStack() {
        val h = habit(mon.minusWeeks(2))
            .withSchedule(everyDay, day(1))
            .withSchedule(setOf(MONDAY), day(3))
        assertEquals(listOf(PastSchedule(day(0), weekdays), PastSchedule(day(2), everyDay)), h.pastSchedules)
        assertFalse(h.isDue(day(-1)))
        assertTrue(h.isDue(day(2)))
        assertFalse(h.isDue(day(4)))
    }

    @Test
    fun changingOnTheFirstDayKeepsNoHistory() {
        val h = habit(day(3)).withSchedule(everyDay, day(3))
        assertEquals(emptyList<PastSchedule>(), h.pastSchedules)
    }

    @Test
    fun statsUseTheScheduleOfTheDay() {
        val h = habit(mon.minusWeeks(1)).withSchedule(everyDay, mon)
        val item = HabitWithEntries(h, emptySet())
        assertEquals(DayMark.REST, Stats.dayMark(item, day(-1), day(6)))
        assertEquals(DayMark.MISSED, Stats.dayMark(item, day(5), day(6)))
    }

    @Test
    fun backupKeepsScheduleHistory() {
        val h = habit(mon.minusWeeks(2)).withSchedule(everyDay, day(1)).withSchedule(setOf(MONDAY), day(3))
        val items = listOf(HabitWithEntries(h, setOf(day(0))))
        val back = Backup.parse(Backup.export(items, true, LocalDateTime.of(2026, 10, 8, 9, 0)))
        assertEquals(items, back.habits)
    }

    @Test
    fun midnightCountdown() {
        val zone = ZoneId.of("Europe/Berlin")
        assertEquals(60_000L + 1_000, Days.millisUntilTomorrow(ZonedDateTime.of(2026, 10, 8, 23, 59, 0, 0, zone)))
        // The night the clocks go back has 25 hours.
        assertEquals(25 * 3_600_000L + 1_000, Days.millisUntilTomorrow(ZonedDateTime.of(2026, 10, 25, 0, 0, 0, 0, zone)))
    }
}
