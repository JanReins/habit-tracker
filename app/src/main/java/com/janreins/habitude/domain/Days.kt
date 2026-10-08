package com.janreins.habitude.domain

import java.time.Duration
import java.time.ZonedDateTime

object Days {
    /** Milliseconds from [now] until the next midnight, plus a second so the date has surely changed. */
    fun millisUntilTomorrow(now: ZonedDateTime): Long =
        Duration.between(now, now.toLocalDate().plusDays(1).atStartOfDay(now.zone)).toMillis() + 1_000
}
