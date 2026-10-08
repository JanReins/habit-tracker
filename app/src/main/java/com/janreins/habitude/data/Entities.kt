package com.janreins.habitude.data

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
