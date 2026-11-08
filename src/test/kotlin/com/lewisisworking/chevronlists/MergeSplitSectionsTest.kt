/**
 * MergeSplitSectionsTest.kt
 * Plain JUnit 4 tests for mergeSectionBelowRange and splitSectionName.
 */
package com.lewisisworking.chevronlists

import org.junit.Test
import org.junit.Assert.*

class MergeSplitSectionsTest {

    private val doc = listOf(
        "> A",          // 0
        ">> - a1",      // 1
        "a note",       // 2
        "",             // 3
        "",             // 4
        "> B",          // 5
        ">> - b1",      // 6
        "",             // 7
        "> C",          // 8
    )

    @Test fun `merge deletes the blank lines and the next header`() =
        assertEquals(3..5, mergeSectionBelowRange(doc, 1))

    @Test fun `merge works from the header line`() =
        assertEquals(3..5, mergeSectionBelowRange(doc, 0))

    @Test fun `merge with no gap deletes only the header`() =
        assertEquals(1..1, mergeSectionBelowRange(listOf("> A", "> B", ">> - b"), 0))

    @Test fun `there is nothing to merge into the last section`() =
        assertNull(mergeSectionBelowRange(doc, 8))

    @Test fun `merge outside a section does nothing`() =
        assertNull(mergeSectionBelowRange(listOf("intro", "> A"), 0))

    @Test fun `split offers the section name continued`() =
        assertEquals("B (continued)", splitSectionName(doc, 6))

    @Test fun `split needs an item line`() {
        assertNull(splitSectionName(doc, 2))
        assertNull(splitSectionName(doc, 5))
    }

    @Test fun `split needs a section`() =
        assertNull(splitSectionName(listOf(">> - stray", "> A"), 0))
}
