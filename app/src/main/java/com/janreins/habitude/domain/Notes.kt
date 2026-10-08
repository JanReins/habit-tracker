package com.janreins.habitude.domain

/** Short notes on a day, such as what led to a slip. */
object Notes {
    const val MAX_LENGTH = 500

    /** A note as it's stored: trimmed and capped. Empty means no note. */
    fun clean(text: String): String = text.trim().take(MAX_LENGTH).trim()

    /** The newest [limit] notes, newest first. */
    fun recent(item: HabitWithEntries, limit: Int = 10): List<Pair<java.time.LocalDate, String>> =
        item.notes.entries.sortedByDescending { it.key }.take(limit).map { it.key to it.value }
}
