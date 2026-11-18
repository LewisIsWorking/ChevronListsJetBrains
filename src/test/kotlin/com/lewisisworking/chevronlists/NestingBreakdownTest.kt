/**
 * NestingBreakdownTest.kt
 * Plain JUnit 4 tests for the breakdown in NestingBreakdown.kt.
 */
package com.lewisisworking.chevronlists

import org.junit.Test
import org.junit.Assert.*

class NestingBreakdownTest {

    private val lines = listOf(
        "> Shopping",          // 0
        ">> - milk",           // 1 depth 1
        ">>> - oat",           // 2 depth 2
        ">>> - almond",        // 3 depth 2
        ">> 1. eggs",          // 4 depth 1
        ">> > Note",           // 5 not an item
        "",                   // 6
        "> Empty",             // 7
        "## Sub",              // 8 subheading ends the section
        ">> - under sub"       // 9
    )

    @Test
    fun `counts items by depth and skips notes`() {
        assertEquals(mapOf(1 to 2, 2 to 2), nestingBreakdown(lines, 0, "-"))
    }

    @Test
    fun `empty section has an empty breakdown`() {
        assertEquals(emptyMap<Int, Int>(), nestingBreakdown(lines, 7, "-"))
    }

    @Test
    fun `stops at the next section boundary`() {
        assertEquals(mapOf(1 to 1), nestingBreakdown(lines, 8, "-"))
        assertEquals(mapOf(1 to 2), nestingBreakdown(listOf("> S", ">> - a", "> T", ">> - b", ">> - c"), 2, "-"))
    }

    @Test
    fun `dialog text lists one line per depth`() {
        val text = nestingBreakdownText("Shopping", mapOf(1 to 2, 2 to 1))
        assertEquals("CL: \"Shopping\" nesting breakdown\n› Depth 1: 2 items\n›› Depth 2: 1 item", text)
    }
}
