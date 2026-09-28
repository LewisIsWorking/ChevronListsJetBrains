/**
 * SectionEntriesTest.kt
 * Plain JUnit 4 tests for sectionEntries and SectionEntry.label.
 */
package com.lewisisworking.chevronlists

import org.junit.Test
import org.junit.Assert.*

class SectionEntriesTest {

    private fun entries(vararg lines: String) = sectionEntries(lines.toList(), "-")

    @Test fun `every section is listed with its line and item count`() =
        assertEquals(listOf(SectionEntry(1, "One", 2), SectionEntry(4, "Two", 1)),
            entries("intro", "> One", ">> - a", ">>> 1. a1", "> Two", ">> - b"))

    @Test fun `empty sections are listed too`() =
        assertEquals(listOf(SectionEntry(0, "Empty", 0), SectionEntry(1, "Full", 1)),
            entries("> Empty", "> Full", ">> - x"))

    @Test fun `notes and blank lines are not items`() =
        assertEquals(listOf(SectionEntry(0, "One", 1)), entries("> One", "", "a note", ">> - a"))

    @Test fun `titles are trimmed`() =
        assertEquals("Spaced", entries(">   Spaced  ")[0].title)

    @Test fun `no headers means no sections`() =
        assertEquals(emptyList<SectionEntry>(), entries(">> - a", "plain"))

    @Test fun `labels show the item count`() {
        assertEquals("Empty", SectionEntry(0, "Empty", 0).label)
        assertEquals("One  (1 item)", SectionEntry(0, "One", 1).label)
        assertEquals("Many  (5 items)", SectionEntry(0, "Many", 5).label)
    }
}
