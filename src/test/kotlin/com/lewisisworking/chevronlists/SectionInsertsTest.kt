/**
 * SectionInsertsTest.kt
 * Plain JUnit 4 tests for newSectionInsert and cloneSectionInsert.
 */
package com.lewisisworking.chevronlists

import org.junit.Test
import org.junit.Assert.*

class SectionInsertsTest {

    private val doc = listOf(
        "intro",        // 0
        "> A",          // 1
        ">> - a1",      // 2
        "note",         // 3
        "",             // 4
        "> B",          // 5
        ">> - b1",      // 6
    )

    @Test fun `inside a section the new one goes after its content`() =
        assertEquals(SectionBlockInsert(4, listOf("", "> New", ">> - "), 5), newSectionInsert(doc, 2, "New", "-"))

    @Test fun `on a header the new one goes above it with a blank line after`() =
        assertEquals(SectionBlockInsert(5, listOf("> New", ">> - ", ""), 5), newSectionInsert(doc, 5, "New", "-"))

    @Test fun `outside any section it goes at the caret`() =
        assertEquals(SectionBlockInsert(0, listOf("> New", ">> - ", ""), 0), newSectionInsert(doc, 0, "New", "-"))

    @Test fun `on a blank line outside a section no extra blank is added`() =
        assertEquals(SectionBlockInsert(0, listOf("> New", ">> - "), 0), newSectionInsert(listOf("", "> A"), 0, "New", "-"))

    @Test fun `in the last section it goes at the end of the file`() =
        assertEquals(SectionBlockInsert(2, listOf("", "> New", ">> * "), 3),
            newSectionInsert(listOf("> A", ">> * a"), 1, "New", "*"))

    @Test fun `a clone copies the content under a copy header after a blank line`() =
        assertEquals(SectionBlockInsert(4, listOf("", "> A (copy)", ">> - a1", "note"), 5), cloneSectionInsert(doc, 2))

    @Test fun `a clone works from the header line`() =
        assertEquals(SectionBlockInsert(7, listOf("", "> B (copy)", ">> - b1"), 8), cloneSectionInsert(doc, 5))

    @Test fun `there is nothing to clone outside a section`() =
        assertNull(cloneSectionInsert(doc, 0))
}
