package com.janreins.habitude.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface HabitDao {
    @Query("SELECT * FROM habits ORDER BY id")
    fun observeHabits(): Flow<List<HabitEntity>>

    @Query("SELECT * FROM entries")
    fun observeEntries(): Flow<List<EntryEntity>>

    @Query("SELECT * FROM past_schedules ORDER BY untilEpochDay")
    fun observePastSchedules(): Flow<List<PastScheduleEntity>>

    @Query("SELECT * FROM past_schedules WHERE habitId = :habitId ORDER BY untilEpochDay")
    suspend fun getPastSchedules(habitId: Long): List<PastScheduleEntity>

    @Query("DELETE FROM past_schedules WHERE habitId = :habitId")
    suspend fun deletePastSchedules(habitId: Long)

    @Query("DELETE FROM past_schedules")
    suspend fun deleteAllPastSchedules()

    @Insert
    suspend fun insertPastSchedules(schedules: List<PastScheduleEntity>)

    @Query("SELECT * FROM habits WHERE id = :id")
    suspend fun getHabit(id: Long): HabitEntity?

    @Insert
    suspend fun insertHabit(habit: HabitEntity): Long

    @Update
    suspend fun updateHabit(habit: HabitEntity)

    @Query("DELETE FROM habits WHERE id = :id")
    suspend fun deleteHabit(id: Long)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertEntry(entry: EntryEntity)

    @Insert
    suspend fun insertHabits(habits: List<HabitEntity>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertEntries(entries: List<EntryEntity>)

    @Query("DELETE FROM habits")
    suspend fun deleteAllHabits()

    @Query("DELETE FROM entries")
    suspend fun deleteAllEntries()

    @Delete
    suspend fun deleteEntry(entry: EntryEntity)
}
