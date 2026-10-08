package com.janreins.habitude.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [HabitEntity::class, EntryEntity::class], version = 2, exportSchema = false)
abstract class HabitudeDatabase : RoomDatabase() {
    abstract fun habitDao(): HabitDao

    companion object {
        /** Version 2 adds an optional daily reminder time to each habit. */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE habits ADD COLUMN reminderMinutes INTEGER")
            }
        }

        fun create(context: Context): HabitudeDatabase =
            Room.databaseBuilder(context, HabitudeDatabase::class.java, "habitude.db")
                .addMigrations(MIGRATION_1_2)
                .build()
    }
}
