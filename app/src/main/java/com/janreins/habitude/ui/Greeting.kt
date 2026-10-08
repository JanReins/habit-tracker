package com.janreins.habitude.ui

/** A friendly header line that changes with the time of day. */
fun greetingFor(hourOfDay: Int): String = when (hourOfDay) {
    in 5..11 -> "Good morning"
    in 12..16 -> "Good afternoon"
    in 17..21 -> "Good evening"
    else -> "Burning the midnight oil"
}
