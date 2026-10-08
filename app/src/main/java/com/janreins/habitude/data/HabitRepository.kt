package com.janreins.habitude.data

import com.janreins.habitude.domain.Habit
import com.janreins.habitude.domain.HabitType
import com.janreins.habitude.domain.HabitWithEntries
import com.janreins.habitude.domain.Schedule
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.LocalDate

class HabitRepository(private val dao: HabitDao) {

    val habits: Flow<List<HabitWithEntries>> =
        combine(dao.observeHabits(), dao.observeEntries()) { habits, entries ->
            val byHabit = entries.groupBy({ it.habitId }, { LocalDate.ofEpochDay(it.epochDay) })
            habits.map { HabitWithEntries(it.toDomain(), byHabit[it.id].orEmpty().toSet()) }
        }

    suspend fun getHabit(id: Long): Habit? = dao.getHabit(id)?.toDomain()

    suspend fun save(habit: Habit): Long =
        if (habit.id == 0L) dao.insertHabit(habit.toEntity())
        else habit.id.also { dao.updateHabit(habit.toEntity()) }

    suspend fun delete(id: Long) = dao.deleteHabit(id)

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
)

private fun Habit.toEntity() = HabitEntity(
    id = id,
    name = name,
    emoji = emoji,
    type = type.name,
    scheduleMask = Schedule.toMask(schedule),
    createdOnEpochDay = createdOn.toEpochDay(),
)
