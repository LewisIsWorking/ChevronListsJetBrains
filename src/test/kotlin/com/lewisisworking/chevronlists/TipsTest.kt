/**
 * TipsTest.kt
 * Plain JUnit 4 tests for the tip rotation and message in Tips.kt.
 */
package com.lewisisworking.chevronlists

import org.junit.Test
import org.junit.Assert.*

class TipsTest {

    @Test
    fun `tips list is not empty and titles are unique`() {
        assertTrue(TIPS.isNotEmpty())
        assertEquals(TIPS.size, TIPS.map { it.title }.toSet().size)
    }

    @Test
    fun `every tip has a body`() {
        assertTrue(TIPS.all { it.body.isNotBlank() })
    }

    @Test
    fun `message appends the keybinding hint when present`() {
        val tip = ChevronTip("Jump between sections", "Jump instantly.", keybinding = "Ctrl+Alt+Down / Ctrl+Alt+Up")
        assertEquals("💡 Jump between sections - Jump instantly.  [Ctrl+Alt+Down / Ctrl+Alt+Up]", tipMessage(tip))
    }

    @Test
    fun `message has no hint without a keybinding`() {
        val tip = ChevronTip("Smart Enter", "Press Enter.")
        assertEquals("💡 Smart Enter - Press Enter.", tipMessage(tip))
    }

    @Test
    fun `next tip wraps around`() {
        val tips = listOf(ChevronTip("A", "a"), ChevronTip("B", "b"))
        assertEquals(1 to tips[1], nextTip(tips, 0))
        assertEquals(0 to tips[0], nextTip(tips, 1))
    }

    @Test
    fun `tip action ids use the ChevronLists prefix`() {
        assertTrue(TIPS.mapNotNull { it.actionId }.all { it.startsWith("ChevronLists.") })
    }
}
