package com.janreins.habitude

import com.janreins.habitude.ui.greetingFor
import org.junit.Assert.assertEquals
import org.junit.Test

class GreetingTest {
    @Test
    fun greetingChangesThroughTheDay() {
        assertEquals("Good morning", greetingFor(7))
        assertEquals("Good afternoon", greetingFor(13))
        assertEquals("Good evening", greetingFor(19))
        assertEquals("Burning the midnight oil", greetingFor(2))
    }
}
