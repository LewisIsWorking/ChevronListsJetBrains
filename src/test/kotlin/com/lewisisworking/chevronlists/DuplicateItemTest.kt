/**
 * DuplicateItemTest.kt
 * Plain JUnit 4 tests for computeDuplicateItem / applyDuplicate.
 */
package com.lewisisworking.chevronlists

import org.junit.Test
import org.junit.Assert.*

class DuplicateItemTest {

    private fun dup(lines: List<String>, index: Int, prefix: String = "-"): List<String>? =
        computeDuplicateItem(lines, index, prefix)?.let { applyDuplicate(lines, it) }

    @Test fun `a bullet is copied below itself`() =
        assertEquals(listOf("> H", ">> - a", ">> - a", ">> - b"), dup(listOf("> H", ">> - a", ">> - b"), 1))

    @Test fun `the copy goes after the children, which are copied too`() =
        assertEquals(listOf("> H", ">> - a", ">>> - a1", ">> - a", ">>> - a1", ">> - b"),
            dup(listOf("> H", ">> - a", ">>> - a1", ">> - b"), 1))

    @Test fun `a numbered copy takes the next number and later items move up`() =
        assertEquals(listOf("> H", ">> 1. a", ">> 2. a", ">> 3. b", ">> 4. c"),
            dup(listOf("> H", ">> 1. a", ">> 2. b", ">> 3. c"), 1))

    @Test fun `children of later items keep their own numbers`() =
        assertEquals(listOf(">> 1. a", ">> 2. a", ">> 3. b", ">>> 1. b1"),
            dup(listOf(">> 1. a", ">> 2. b", ">>> 1. b1"), 0))

    @Test fun `renumbering stops at a shallower line`() =
        assertEquals(listOf(">> - p", ">>> 1. x", ">>> 2. x", ">> - q", ">>> 1. y"),
            dup(listOf(">> - p", ">>> 1. x", ">> - q", ">>> 1. y"), 1))

    @Test fun `renumbering stops at a header`() =
        assertEquals(listOf("> A", ">> 1. a", ">> 2. a", "> B", ">> 2. z"),
            dup(listOf("> A", ">> 1. a", "> B", ">> 2. z"), 1))

    @Test fun `a same-depth bullet does not end the numbered list`() =
        assertEquals(listOf(">> 1. a", ">> 2. a", ">> - note", ">> 3. b"),
            dup(listOf(">> 1. a", ">> - note", ">> 2. b"), 0))

    // The block of line 1 ends at its child on line 2, so the copy starts on line 3
    @Test fun `the copy starts right after the original block`() =
        assertEquals(2, computeDuplicateItem(listOf("> H", ">> - a", ">>> - a1"), 1, "-")!!.afterLine)

    // "> - x" is a header even though it also matches the bullet pattern
    @Test fun `a header that looks like a bullet is not duplicated`() =
        assertNull(dup(listOf("> - x", ">> - a"), 0))

    @Test fun `the last line of the document can be duplicated`() =
        assertEquals(listOf(">> - a", ">> - a"), dup(listOf(">> - a"), 0))

    @Test fun `lines that are not items are not duplicated`() {
        assertNull(dup(listOf("> H", "plain"), 1))
        assertNull(dup(listOf("> H"), 0))
        assertNull(dup(listOf(">> * a"), 0))
    }

    @Test fun `an index outside the document is ignored`() {
        assertNull(dup(listOf(">> - a"), 3))
        assertNull(dup(listOf(">> - a"), -1))
    }
}
