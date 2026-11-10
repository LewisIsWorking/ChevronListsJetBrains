/**
 * TimersTest.kt
 * Pure logic of the timers: how elapsed time is written and stamped on an item.
 */
package com.lewisisworking.chevronlists

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TimersTest {

    @Test fun `short times are seconds`() {
        assertEquals("0s", elapsedStamp(0))
        assertEquals("59s", elapsedStamp(59_999))
    }

    @Test fun `times over a minute drop the seconds`() {
        assertEquals("1m", elapsedStamp(60_000))
        assertEquals("59m", elapsedStamp(3_599_000))
    }

    @Test fun `times over an hour show hours and minutes`() {
        assertEquals("1h 5m", elapsedStamp(3_900_000))
    }

    @Test fun `a clock time pads the seconds`() {
        assertEquals("0:00", clockTime(0))
        assertEquals("1:05", clockTime(65_000))
        assertEquals("25:00", clockTime(1_500_000))
    }

    @Test fun `a short label is unchanged`() {
        assertEquals("work", timerLabel("work"))
    }

    @Test fun `a long label is cut to thirty characters`() {
        assertEquals("a".repeat(30) + "...", timerLabel("a".repeat(31)))
    }

    @Test fun `a bullet item takes the stamp at its end`() {
        assertEquals(">> - work ~5m", withElapsedStamp(">> - work", "-", "5m"))
    }

    @Test fun `a numbered item keeps its number`() {
        assertEquals(">> 3. work ~5m", withElapsedStamp(">> 3. work", "-", "5m"))
    }

    @Test fun `the list prefix of the item is kept`() {
        assertEquals(">> * work ~5m", withElapsedStamp(">> * work", "*", "5m"))
    }

    @Test fun `an existing estimate is replaced`() {
        assertEquals(">> - work ~5m", withElapsedStamp(">> - work ~2h", "-", "5m"))
    }

    @Test fun `a stamp with hours and minutes is replaced whole`() {
        assertEquals(">> - work ~10m", withElapsedStamp(">> - work ~1h 5m", "-", "10m"))
    }

    @Test fun `a tilde word is not an estimate`() {
        assertEquals(">> - see ~notes ~5m", withElapsedStamp(">> - see ~notes", "-", "5m"))
    }

    @Test fun `a line that is not an item gives null`() {
        assertNull(withElapsedStamp("> A", "-", "5m"))
        assertNull(withElapsedStamp("prose", "-", "5m"))
    }
}
