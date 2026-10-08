package com.janreins.habitude.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [HabitEntity::class, EntryEntity::class, PastScheduleEntity::class], version = 3, exportSchema = false)
abstract class HabitudeDatabase : RoomDatabase() {
    abstract fun habitDao(): HabitDao

    companion object {
        /** Version 2 adds an optional daily reminder time to each habit. */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE habits ADD COLUMN reminderMinutes INTEGER")
            }
        }

        /** Version 3 keeps old schedules, so changing a habit's days doesn't rewrite its past. */
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `past_schedules` (`habitId` INTEGER NOT NULL, " +
                        "`untilEpochDay` INTEGER NOT NULL, `scheduleMask` INTEGER NOT NULL, " +
                        "PRIMARY KEY(`habitId`, `untilEpochDay`), FOREIGN KEY(`habitId`) REFERENCES `habits`(`id`) " +
                        "ON UPDATE NO ACTION ON DELETE CASCADE )",
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_past_schedules_habitId` ON `past_schedules` (`habitId`)")
            }
        }

        fun create(context: Context): HabitudeDatabase =
            Room.databaseBuilder(context, HabitudeDatabase::class.java, "habitude.db")
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                .build()
    }
}
