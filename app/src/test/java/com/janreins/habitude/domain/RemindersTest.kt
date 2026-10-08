package com.janreins.habitude.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.DayOfWeek
import java.time.DayOfWeek.MONDAY
import java.time.LocalDate
import java.time.LocalDateTime

class RemindersTest {
    // 2026-10-05 is a Monday
    private val mon = LocalDate.of(2026, 10, 5)
    private fun day(offset: Long) = mon.plusDays(offset)

    private fun build(id: Long, name: String, done: Set<LocalDate>, schedule: Set<DayOfWeek> = DayOfWeek.entries.toSet()) =
        HabitWithEntries(Habit(id, name, "📚", HabitType.BUILD, schedule, mon), done)

    private fun quit(slips: Set<LocalDate>) =
        HabitWithEntries(Habit(9, "Scrolling", "📱", HabitType.BREAK, DayOfWeek.entries.toSet(), mon), slips)

    @Test
    fun nextTriggerIsLaterTodayOrTomorrow() {
        val morning = LocalDateTime.of(2026, 10, 5, 7, 0)
        assertEquals(LocalDateTime.of(2026, 10, 5, 8, 30), Reminders.nextTrigger(morning, 8 * 60 + 30))
        assertEquals(LocalDateTime.of(2026, 10, 6, 6, 0), Reminders.nextTrigger(morning, 6 * 60))
        assertEquals(LocalDateTime.of(2026, 10, 6, 7, 0), Reminders.nextTrigger(morning, 7 * 60))
    }

    @Test
    fun noReminderWhenDoneOrRestDay() {
        assertNull(Reminders.reminderFor(build(1, "Read", setOf(day(2))), day(2)))
        assertNull(Reminders.reminderFor(build(1, "Read", emptySet(), schedule = setOf(MONDAY)), day(1)))
    }

    @Test
    fun reminderMentionsStreak() {
        val text = Reminders.reminderFor(build(1, "Read", setOf(day(0), day(1))), day(2))
        assertNotNull(text)
        assertEquals("📚 Read", text!!.title)
        assertEquals("Keep your 2-day streak going 🔥", text.body)
    }

    @Test
    fun breakReminderCountsCleanDaysAndSkipsSlipDays() {
        assertEquals("3 days clean. You've got this.", Reminders.reminderFor(quit(emptySet()), day(3))!!.body)
        assertNull(Reminders.reminderFor(quit(setOf(day(3))), day(3)))
    }

    @Test
    fun nudgeOnlyForStreaksWorthSaving() {
        val short = build(1, "Read", setOf(day(1)))
        val long = build(2, "Run", setOf(day(0), day(1)))
        val doneToday = build(3, "Water", setOf(day(0), day(1), day(2)))
        val items = listOf(short, long, doneToday, quit(emptySet()))
        assertEquals(listOf(2L), Reminders.atRisk(items, day(2)).map { it.first.habit.id })
        assertEquals("Your 2-day streak is at risk", Reminders.nudgeFor(items, day(2))!!.title)
        assertNull(Reminders.nudgeFor(listOf(short, doneToday), day(2)))
    }

    @Test
    fun nudgeGroupsSeveralStreaks() {
        val a = build(1, "Read", setOf(day(0), day(1)))
        val b = build(2, "Run", setOf(day(0), day(1)))
        val text = Reminders.nudgeFor(listOf(a, b), day(2))!!
        assertEquals("2 streaks need you tonight", text.title)
    }
}
