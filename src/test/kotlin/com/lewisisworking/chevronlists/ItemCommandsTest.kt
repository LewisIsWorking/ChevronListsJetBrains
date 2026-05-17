/**
 * ItemCommandsTest.kt
 * Plain JUnit 4 tests for toggleCheckbox and computeToggleDone.
 */
package com.lewisisworking.chevronlists

import org.junit.Test
import org.junit.Assert.*

class ItemCommandsTest {

    // toggleCheckbox
    @Test fun `adds checked marker to plain content`() {
        assertEquals("[x] Task", toggleCheckbox("Task"))
    }
    @Test fun `cycles unchecked to checked`() {
        assertEquals("[x] Task", toggleCheckbox("[ ] Task"))
    }
    @Test fun `cycles checked back to unchecked`() {
        assertEquals("[ ] Task", toggleCheckbox("[x] Task"))
    }
    @Test fun `treats uppercase X same as lowercase x`() {
        assertEquals("[ ] Task", toggleCheckbox("[X] Task"))
    }
    @Test fun `treats empty brackets as unchecked and toggles to checked`() {
        assertEquals("[x] Task", toggleCheckbox("[] Task"))
    }
    @Test fun `preserves task content when toggling`() {
        assertEquals("[x] Long task with #tag and !!! priority",
                     toggleCheckbox("Long task with #tag and !!! priority"))
    }
    @Test fun `normalises whitespace around marker`() {
        assertEquals("[x] Task", toggleCheckbox("[ ]   Task"))
    }
    @Test fun `handles empty content`() {
        assertEquals("[x] ", toggleCheckbox(""))
    }

    // computeToggleDone - bullet items
    @Test fun `toggles a bullet item`() {
        assertEquals(">> - [x] Task", computeToggleDone(">> - Task", "-"))
    }
    @Test fun `toggles a checked bullet back to unchecked`() {
        assertEquals(">> - [ ] Task", computeToggleDone(">> - [x] Task", "-"))
    }
    @Test fun `toggles a deeply nested bullet`() {
        assertEquals(">>>> - [x] Deep", computeToggleDone(">>>> - Deep", "-"))
    }
    @Test fun `respects custom bullet prefix`() {
        assertEquals(">> * [x] Task", computeToggleDone(">> * Task", "*"))
    }

    // computeToggleDone - numbered items
    @Test fun `toggles a numbered item`() {
        assertEquals(">> 1. [x] First", computeToggleDone(">> 1. First", "-"))
    }
    @Test fun `preserves the number when toggling`() {
        assertEquals(">> 42. [x] Item", computeToggleDone(">> 42. Item", "-"))
    }
    @Test fun `toggles a checked numbered item back`() {
        assertEquals(">> 1. [ ] Task", computeToggleDone(">> 1. [x] Task", "-"))
    }

    // computeToggleDone - non-items
    @Test fun `returns null for a header line`() {
        assertNull(computeToggleDone("> Section", "-"))
    }
    @Test fun `returns null for plain text`() {
        assertNull(computeToggleDone("Just some text", "-"))
    }
    @Test fun `returns null for a markdown subheading`() {
        assertNull(computeToggleDone("## Subheading", "-"))
    }
    @Test fun `returns null for an empty line`() {
        assertNull(computeToggleDone("", "-"))
    }
    @Test fun `returns null for a bullet with wrong prefix`() {
        // line uses star but configured prefix is dash
        assertNull(computeToggleDone(">> * Task", "-"))
    }
}