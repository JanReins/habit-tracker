package com.janreins.habitude.domain

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * The backup file: plain, readable JSON holding every habit and every day logged against it.
 * Bump [Backup.FORMAT_VERSION] if the shape ever changes, and keep reading older versions.
 */
@Serializable
data class BackupFile(
    val app: String = Backup.APP_NAME,
    @SerialName("format") val formatVersion: Int = Backup.FORMAT_VERSION,
    val exportedAt: String,
    val habits: List<BackupHabit>,
    val eveningNudge: Boolean? = null,
)

@Serializable
data class BackupHabit(
    val id: Long,
    val name: String,
    val emoji: String,
    /** "BUILD" or "BREAK". */
    val type: String,
    /** ISO day numbers, 1 = Monday … 7 = Sunday. */
    val days: List<Int>,
    /** yyyy-MM-dd */
    val createdOn: String,
    val reminderMinutes: Int? = null,
    /** yyyy-MM-dd for every day done (build) or slipped (break). */
    val entries: List<String>,
    /** Earlier schedules, oldest first. Missing in backups from before schedule history. */
    val pastSchedules: List<BackupPastSchedule> = emptyList(),
    val archived: Boolean = false,
)

@Serializable
data class BackupPastSchedule(
    /** yyyy-MM-dd, the last day this schedule applied. */
    val until: String,
    /** ISO day numbers, 1 = Monday … 7 = Sunday. */
    val days: List<Int>,
)

data class BackupContents(
    val habits: List<HabitWithEntries>,
    val eveningNudge: Boolean?,
    val exportedAt: String,
)

class BackupException(message: String) : Exception(message)

object Backup {
    const val APP_NAME = "Habitude"
    const val FORMAT_VERSION = 1

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    fun export(items: List<HabitWithEntries>, eveningNudge: Boolean, now: LocalDateTime): String =
        json.encodeToString(
            BackupFile.serializer(),
            BackupFile(
                exportedAt = now.withNano(0).toString(),
                eveningNudge = eveningNudge,
                habits = items.map { (habit, entries) ->
                    BackupHabit(
                        id = habit.id,
                        name = habit.name,
                        emoji = habit.emoji,
                        type = habit.type.name,
                        days = habit.schedule.map { it.value }.sorted(),
                        createdOn = habit.createdOn.toString(),
                        reminderMinutes = habit.reminderMinutes,
                        entries = entries.sorted().map { it.toString() },
                        pastSchedules = habit.pastSchedules.map { past ->
                            BackupPastSchedule(past.until.toString(), past.days.map { it.value }.sorted())
                        },
                        archived = habit.archived,
                    )
                },
            ),
        )

    /** Reads a backup, or throws [BackupException] with a message fit to show the user. */
    fun parse(text: String): BackupContents {
        val file = try {
            json.decodeFromString(BackupFile.serializer(), text)
        } catch (e: SerializationException) {
            throw BackupException("This doesn't look like a Habitude backup.")
        } catch (e: IllegalArgumentException) {
            throw BackupException("This doesn't look like a Habitude backup.")
        }
        if (file.app != APP_NAME) throw BackupException("This doesn't look like a Habitude backup.")
        if (file.formatVersion > FORMAT_VERSION) {
            throw BackupException("This backup was made by a newer version of Habitude. Update the app, then try again.")
        }
        if (file.habits.map { it.id }.toSet().size != file.habits.size) {
            throw BackupException("This backup is damaged: two habits share the same id.")
        }
        val habits = file.habits.map { h ->
            try {
                val type = HabitType.valueOf(h.type)
                val days = h.days.map { DayOfWeek.of(it) }.toSet()
                HabitWithEntries(
                    habit = Habit(
                        id = h.id,
                        name = h.name.trim().take(40).ifEmpty { "Habit" },
                        emoji = h.emoji,
                        type = type,
                        schedule = days.ifEmpty { DayOfWeek.entries.toSet() },
                        createdOn = LocalDate.parse(h.createdOn),
                        reminderMinutes = h.reminderMinutes?.takeIf { it in 0 until 24 * 60 },
                        pastSchedules = h.pastSchedules
                            .map { past ->
                                PastSchedule(
                                    LocalDate.parse(past.until),
                                    past.days.map { DayOfWeek.of(it) }.toSet().ifEmpty { DayOfWeek.entries.toSet() },
                                )
                            }
                            .distinctBy { it.until }
                            .sortedBy { it.until },
                        archived = h.archived,
                    ),
                    entries = h.entries.map { LocalDate.parse(it) }.toSet(),
                )
            } catch (e: IllegalArgumentException) {
                throw BackupException("This backup is damaged: \"${h.name}\" couldn't be read.")
            } catch (e: java.time.DateTimeException) {
                throw BackupException("This backup is damaged: \"${h.name}\" has a bad date.")
            }
        }
        if (habits.any { it.habit.id <= 0 }) throw BackupException("This backup is damaged: a habit has no id.")
        return BackupContents(habits, file.eveningNudge, file.exportedAt)
    }
}
