/**
 * ItemDatesTest.kt
 * Plain JUnit 4 tests for natural dates, due dates, Fix Numbering and section
 * item selection.
 */
package com.lewisisworking.chevronlists

import org.junit.Test
import org.junit.Assert.*
import java.time.LocalDate

class ItemDatesTest {

    // A Wednesday
    private val today = LocalDate.of(2026, 1, 28)

    @Test fun `iso dates pass through`() =
        assertEquals("2026-12-31", parseNaturalDate("2026-12-31", today))

    @Test fun `an impossible iso date is refused`() =
        assertNull(parseNaturalDate("2026-13-45", today))

    @Test fun `shorthands`() {
        assertEquals("2026-01-28", parseNaturalDate("today", today))
        assertEquals("2026-01-29", parseNaturalDate(" Tomorrow ", today))
        assertEquals("2026-02-04", parseNaturalDate("next week", today))
        assertEquals("2026-02-28", parseNaturalDate("next month", today))
    }

    @Test fun `relative days`() {
        assertEquals("2026-02-04", parseNaturalDate("+7", today))
        assertNull(parseNaturalDate("+99999999999999999999", today))
    }

    @Test fun `a weekday is the next one, a week on when it is today`() {
        assertEquals("2026-01-30", parseNaturalDate("friday", today))
        assertEquals("2026-01-30", parseNaturalDate("fri", today))
        assertEquals("2026-02-04", parseNaturalDate("wednesday", today))
    }

    @Test fun `nonsense is refused`() =
        assertNull(parseNaturalDate("whenever", today))

    @Test fun `a due date goes after the words, before the vote and comment`() =
        assertEquals("[ ] idea @2026-12-31 +5 // why", setItemDueDate("[ ] idea +5 // why", "2026-12-31"))

    @Test fun `a due date replaces the old one`() =
        assertEquals("a @2026-12-31", setItemDueDate("a @2026-01-01", "2026-12-31"))

    @Test fun `fix numbering counts each list separately`() {
        val lines = listOf("> A", ">> 1. a", ">>> 1. a1", ">> 2. b", ">>> 1. b1")
        assertEquals(lines, fixNumbering(lines))
    }

    @Test fun `fix numbering repairs each section and skips anything before the first`() =
        assertEquals(
            listOf(">> 5. x", "> A", ">> 1. a", ">> 2. b", "> B", ">> 1. c"),
            fixNumbering(listOf(">> 5. x", "> A", ">> 3. a", ">> 7. b", "> B", ">> 2. c"))
        )

    @Test fun `fix numbering leaves a document with no sections alone`() {
        val lines = listOf(">> 5. x")
        assertEquals(lines, fixNumbering(lines))
    }

    private val doc = listOf("> A", ">> - a", "note", ">>> 1. b", "", "> B", ">> - c")

    @Test fun `section items are the item lines of the caret section`() {
        assertEquals(listOf(1, 3), sectionItemLines(doc, 0, "-"))
        assertEquals(listOf(1, 3), sectionItemLines(doc, 2, "-"))
        assertEquals(listOf(6), sectionItemLines(doc, 6, "-"))
    }

    @Test fun `outside a section there are no items`() =
        assertEquals(emptyList<Int>(), sectionItemLines(listOf(">> - a"), 0, "-"))

    @Test fun `a nested clone starts a new list when a shallower item ended the old one`() {
        val lines = listOf("> A", ">> 1. a", ">>> 5. x", ">> 2. b")
        assertEquals(
            lines + ">>> 1. x",
            applyDuplicate(lines, cloneItemToSectionEnd(lines, 2, "-")!!)
        )
    }
}
