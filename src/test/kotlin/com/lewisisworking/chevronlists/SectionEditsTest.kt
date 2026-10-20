/**
 * SectionEditsTest.kt
 * Plain JUnit 4 tests for deleteSectionRange and duplicateSection.
 */
package com.lewisisworking.chevronlists

import org.junit.Test
import org.junit.Assert.*

class SectionEditsTest {

    private val doc = listOf(
        "intro",        // 0
        "> A",          // 1
        ">> - a1",      // 2
        "a note",       // 3
        "",             // 4
        "> B",          // 5
        ">> - b1",      // 6
        "",             // 7
    )

    @Test fun `delete takes the section and the blank lines before the next header`() =
        assertEquals(1..4, deleteSectionRange(doc, 2))

    @Test fun `delete works from the header line`() =
        assertEquals(1..4, deleteSectionRange(doc, 1))

    @Test fun `deleting the last section keeps the file's trailing blank line`() =
        assertEquals(5..6, deleteSectionRange(doc, 7))

    @Test fun `delete outside a section does nothing`() =
        assertNull(deleteSectionRange(doc, 0))

    @Test fun `duplicate inserts a copy after the notes with the same spacing`() {
        val insert = duplicateSection(doc, 2)!!
        assertEquals(3, insert.afterLine)
        assertEquals(listOf("", "> A", ">> - a1", "a note"), insert.lines)
    }

    @Test fun `duplicating the last section uses its trailing blank lines as spacing`() {
        val insert = duplicateSection(doc, 5)!!
        assertEquals(6, insert.afterLine)
        assertEquals(listOf("", "> B", ">> - b1"), insert.lines)
    }

    @Test fun `duplicate with nothing between sections adds the copy directly below`() {
        val insert = duplicateSection(listOf("> A", ">> - a", "> B"), 0)!!
        assertEquals(1, insert.afterLine)
        assertEquals(listOf("> A", ">> - a"), insert.lines)
    }

    @Test fun `duplicate outside a section does nothing`() =
        assertNull(duplicateSection(doc, 0))
}
