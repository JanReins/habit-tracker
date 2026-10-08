package com.janreins.habitude.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PinTest {
    @Test
    fun onlySixDigitsAreValid() {
        assertTrue(Pin.isValid("123456"))
        assertFalse(Pin.isValid("12345"))
        assertFalse(Pin.isValid("1234567"))
        assertFalse(Pin.isValid("12a456"))
    }

    @Test
    fun hashMatchesOnlyTheRightPin() {
        val salt = Pin.newSalt()
        val hash = Pin.hash("482913", salt)
        assertTrue(Pin.matches("482913", salt, hash))
        assertFalse(Pin.matches("482914", salt, hash))
        assertFalse(Pin.matches("48291", salt, hash))
    }

    @Test
    fun saltMakesHashesDifferent() {
        assertNotEquals(Pin.hash("000000", Pin.newSalt()), Pin.hash("000000", Pin.newSalt()))
    }

    @Test
    fun lockoutGrowsAfterFiveWrongTries() {
        assertEquals(0, Pin.lockoutSeconds(4))
        assertEquals(30, Pin.lockoutSeconds(5))
        assertEquals(60, Pin.lockoutSeconds(6))
        assertEquals(480, Pin.lockoutSeconds(9))
        assertEquals(480, Pin.lockoutSeconds(20))
    }
}
