package com.janreins.habitude.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [HabitEntity::class, EntryEntity::class], version = 1, exportSchema = false)
abstract class HabitudeDatabase : RoomDatabase() {
    abstract fun habitDao(): HabitDao

    companion object {
        fun create(context: Context): HabitudeDatabase =
            Room.databaseBuilder(context, HabitudeDatabase::class.java, "habitude.db").build()
    }
}
