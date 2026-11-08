/**
 * SortByKeysTest.kt
 * Plain JUnit 4 tests for sorting a section by priority, votes and due date.
 */
package com.lewisisworking.chevronlists

import org.junit.Test
import org.junit.Assert.*

class SortByKeysTest {

    private fun sorted(order: Comparator<String>, vararg lines: String) =
        computeSortSectionBy(lines.toList(), 1, order, "-")

    @Test fun `priority sorts highest first and keeps children with their parent`() =
        assertEquals(
            listOf("> A", ">> - !!! top", ">>> - kid", ">> - ! low", ">> - plain"),
            sorted(BY_PRIORITY, "> A", ">> - plain", ">> - !!! top", ">>> - kid", ">> - ! low")
        )

    @Test fun `priority is read after a checkbox`() =
        assertEquals(
            listOf("> A", ">> - [ ] !! b", ">> - a"),
            sorted(BY_PRIORITY, "> A", ">> - a", ">> - [ ] !! b")
        )

    @Test fun `numbers stay in position`() =
        assertEquals(
            listOf("> A", ">> 1. !! b", ">> 2. a"),
            sorted(BY_PRIORITY, "> A", ">> 1. a", ">> 2. !! b")
        )

    @Test fun `votes sort most first`() =
        assertEquals(
            listOf("> A", ">> - b +5", ">> - a +1", ">> - c"),
            sorted(BY_VOTES, "> A", ">> - a +1", ">> - b +5", ">> - c")
        )

    @Test fun `equal items keep their order`() =
        assertEquals(
            listOf("> A", ">> - c +3", ">> - a +2", ">> - b +2"),
            sorted(BY_VOTES, "> A", ">> - a +2", ">> - b +2", ">> - c +3")
        )

    @Test fun `a note splits the lists around it`() =
        assertNull(sorted(BY_VOTES, "> A", ">> - a +1", "a note", ">> - b +5"))

    @Test fun `due date sorts soonest first with undated last`() =
        assertEquals(
            listOf("> A", ">> - a @2026-01-01", ">> - b @2026-02-01", ">>> - kid", ">> - none"),
            sorted(BY_DUE_DATE, "> A", ">> - none", ">> - b @2026-02-01", ">>> - kid", ">> - a @2026-01-01")
        )

    @Test fun `the next section is untouched`() =
        assertEquals(
            listOf("> A", ">> - !! b", ">> - a", "", "> B", ">> - ! c", ">> - !!! d"),
            sorted(BY_PRIORITY, "> A", ">> - a", ">> - !! b", "", "> B", ">> - ! c", ">> - !!! d")
        )

    @Test fun `already in order is no change`() =
        assertNull(sorted(BY_PRIORITY, "> A", ">> - !! a", ">> - b"))

    @Test fun `A to Z still works`() =
        assertEquals(
            listOf("> A", ">> - a", ">> - B"),
            computeSortSection(listOf("> A", ">> - B", ">> - a"), 1, false, "-")
        )
}
