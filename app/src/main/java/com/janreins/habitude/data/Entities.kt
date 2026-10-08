package com.janreins.habitude.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "habits")
data class HabitEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val emoji: String,
    /** [com.janreins.habitude.domain.HabitType] name. */
    val type: String,
    /** Bit 0 = Monday … bit 6 = Sunday. */
    val scheduleMask: Int,
    /** [java.time.LocalDate.toEpochDay]. */
    val createdOnEpochDay: Long,
    /** Minutes after midnight for the daily reminder; null means no reminder. */
    val reminderMinutes: Int? = null,
    @ColumnInfo(defaultValue = "0") val archived: Boolean = false,
)

/** One row per habit per day: "done" for build habits, "slipped" for break habits. */
@Entity(
    tableName = "entries",
    primaryKeys = ["habitId", "epochDay"],
    foreignKeys = [
        ForeignKey(
            entity = HabitEntity::class,
            parentColumns = ["id"],
            childColumns = ["habitId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("habitId")],
)
data class EntryEntity(
    val habitId: Long,
    val epochDay: Long,
)

/** A schedule a build habit used to have, up to and including [untilEpochDay]. */
@Entity(
    tableName = "past_schedules",
    primaryKeys = ["habitId", "untilEpochDay"],
    foreignKeys = [
        ForeignKey(
            entity = HabitEntity::class,
            parentColumns = ["id"],
            childColumns = ["habitId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("habitId")],
)
data class PastScheduleEntity(
    val habitId: Long,
    val untilEpochDay: Long,
    /** Bit 0 = Monday … bit 6 = Sunday. */
    val scheduleMask: Int,
)
