package com.janreins.habitude.data

import com.janreins.habitude.domain.Habit
import com.janreins.habitude.domain.HabitType
import com.janreins.habitude.domain.HabitWithEntries
import com.janreins.habitude.domain.Schedule
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import androidx.room.withTransaction
import java.time.LocalDate

class HabitRepository(private val db: HabitudeDatabase) {
    private val dao = db.habitDao()

    val habits: Flow<List<HabitWithEntries>> =
        combine(dao.observeHabits(), dao.observeEntries()) { habits, entries ->
            val byHabit = entries.groupBy({ it.habitId }, { LocalDate.ofEpochDay(it.epochDay) })
            habits.map { HabitWithEntries(it.toDomain(), byHabit[it.id].orEmpty().toSet()) }
        }

    suspend fun getHabit(id: Long): Habit? = dao.getHabit(id)?.toDomain()

    /** Everything as it stands right now, for background work such as reminders. */
    suspend fun snapshot(): List<HabitWithEntries> = habits.first()

    suspend fun save(habit: Habit): Long =
        if (habit.id == 0L) dao.insertHabit(habit.toEntity())
        else habit.id.also { dao.updateHabit(habit.toEntity()) }

    suspend fun delete(id: Long) = dao.deleteHabit(id)

    /** Swaps everything for the contents of a backup, all at once or not at all. */
    suspend fun replaceAll(items: List<HabitWithEntries>) {
        db.withTransaction {
            dao.deleteAllEntries()
            dao.deleteAllHabits()
            dao.insertHabits(items.map { it.habit.toEntity() })
            dao.insertEntries(
                items.flatMap { item -> item.entries.map { EntryEntity(item.habit.id, it.toEpochDay()) } },
            )
        }
    }

    suspend fun setEntry(habitId: Long, day: LocalDate, present: Boolean) {
        val entry = EntryEntity(habitId, day.toEpochDay())
        if (present) dao.insertEntry(entry) else dao.deleteEntry(entry)
    }
}

private fun HabitEntity.toDomain() = Habit(
    id = id,
    name = name,
    emoji = emoji,
    type = HabitType.valueOf(type),
    schedule = Schedule.fromMask(scheduleMask),
    createdOn = LocalDate.ofEpochDay(createdOnEpochDay),
    reminderMinutes = reminderMinutes,
)

private fun Habit.toEntity() = HabitEntity(
    id = id,
    name = name,
    emoji = emoji,
    type = type.name,
    scheduleMask = Schedule.toMask(schedule),
    createdOnEpochDay = createdOn.toEpochDay(),
    reminderMinutes = reminderMinutes,
)
