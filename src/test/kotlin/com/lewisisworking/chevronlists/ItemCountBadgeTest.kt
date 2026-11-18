/**
 * ItemCountBadgeTest.kt
 * Plain JUnit 4 tests for the badge count in ItemCountBadge.kt.
 */
package com.lewisisworking.chevronlists

import org.junit.Test
import org.junit.Assert.*

class ItemCountBadgeTest {

    private val lines = listOf(
        "> Shopping",          // 0
        ">> - milk",           // 1
        ">> - bread",          // 2
        ">>> - nested counts", // 3
        ">> 1. eggs",          // 4
        ">> > Note",           // 5
        "",                   // 6
        "> Empty",             // 7
        "> Notes",             // 8
        "just prose"           // 9
    )

    @Test
    fun `counts bullets numbered and nested lines but not notes`() {
        assertEquals(listOf(1, 2, 3, 4), badgeItemLines(lines, 0, "-"))
    }

    @Test
    fun `badge text wraps the count`() {
        assertEquals("  (4)", badgeText(lines, 0, "-"))
    }

    @Test
    fun `header only section has a zero badge`() {
        assertEquals("  (0)", badgeText(lines, 7, "-"))
    }

    @Test
    fun `stops at the next header`() {
        assertEquals("  (0)", badgeText(lines, 8, "-"))
    }

    @Test
    fun `respects the list prefix`() {
        assertEquals("  (2)", badgeText(listOf("> S", ">> * milk", ">> 1. eggs"), 0, "*"))
        assertEquals("  (1)", badgeText(listOf("> S", ">> * milk", ">> 1. eggs"), 0, "-"))
    }
}
