package com.janreins.habitude.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.DayOfWeek
import java.time.DayOfWeek.MONDAY
import java.time.DayOfWeek.TUESDAY
import java.time.LocalDate
import java.time.LocalDateTime

class TodayListTest {
    // 2026-10-05 is a Monday
    private val mon = LocalDate.of(2026, 10, 5)

    private fun build(id: Long, done: Set<LocalDate> = emptySet(), schedule: Set<DayOfWeek> = DayOfWeek.entries.toSet(), archived: Boolean = false) =
        HabitWithEntries(Habit(id, "Habit $id", "📚", HabitType.BUILD, schedule, mon.minusWeeks(1), archived = archived), done)

    private fun quit(id: Long, slips: Set<LocalDate> = emptySet()) =
        HabitWithEntries(Habit(id, "Quit $id", "📱", HabitType.BREAK, createdOn = mon.minusWeeks(1)), slips)

    @Test
    fun toDoFirstThenDoneThenRestDays() {
        val items = listOf(
            build(1, done = setOf(mon)),
            build(2, schedule = setOf(TUESDAY)),
            build(3),
            quit(4),
            build(5),
        )
        assertEquals(listOf(3L, 4L, 5L, 1L, 2L), TodayList.order(items, mon).map { it.habit.id })
    }

    @Test
    fun progressCountsDueAndDoneButNotArchivedOrBreak() {
        val items = listOf(
            build(1, done = setOf(mon)),
            build(2, schedule = setOf(TUESDAY)),
            build(3),
            build(4, done = setOf(mon), schedule = setOf(TUESDAY)), // done on a rest day anyway
            build(5, archived = true),
            quit(6),
        )
        assertEquals(DayProgress(done = 2, due = 3), TodayList.progress(items, mon))
    }

    @Test
    fun archivedHabitsSendNoReminders() {
        val streak = setOf(mon.minusDays(2), mon.minusDays(1))
        val archived = build(1, done = streak, schedule = setOf(MONDAY) + DayOfWeek.entries, archived = true)
        assertNull(Reminders.reminderFor(archived, mon))
        assertNull(Reminders.nudgeFor(listOf(archived), mon))
    }

    @Test
    fun cleanSinceInThePastCountsFromThatDay() {
        val item = HabitWithEntries(Habit(1, "Smoking", "🚬", HabitType.BREAK, createdOn = mon.minusDays(90)), emptySet())
        assertEquals(90, Streaks.daysClean(item.entries, item.habit.createdOn, mon))
    }

    @Test
    fun backupKeepsArchived() {
        val items = listOf(build(1, done = setOf(mon), archived = true), build(2))
        val back = Backup.parse(Backup.export(items, true, LocalDateTime.of(2026, 10, 8, 9, 0)))
        assertEquals(items, back.habits)
    }
}
