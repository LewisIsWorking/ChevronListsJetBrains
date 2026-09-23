/**
 * SectionNavigationTest.kt
 * Plain JUnit 4 tests for findHeaderBelow / findHeaderAbove.
 */
package com.lewisisworking.chevronlists

import org.junit.Test
import org.junit.Assert.*

class SectionNavigationTest {

    private val doc = listOf("intro", "> One", ">> - a", ">> - b", "> Two", ">> - c")

    // Down
    @Test fun `next header from an item`() = assertEquals(4, findHeaderBelow(doc, 2))
    @Test fun `next header from a header skips that header`() = assertEquals(4, findHeaderBelow(doc, 1))
    @Test fun `next header from above the first header`() = assertEquals(1, findHeaderBelow(doc, 0))
    @Test fun `no header below the last section`() = assertNull(findHeaderBelow(doc, 4))
    @Test fun `no header below the last line`() = assertNull(findHeaderBelow(doc, 5))

    // Up
    @Test fun `previous header from an item is its own section`() = assertEquals(1, findHeaderAbove(doc, 3))
    @Test fun `previous header from a header skips that header`() = assertEquals(1, findHeaderAbove(doc, 4))
    @Test fun `no header above the first header`() = assertNull(findHeaderAbove(doc, 1))
    @Test fun `no header above the intro`() = assertNull(findHeaderAbove(doc, 0))

    // Edges
    @Test fun `a document with no headers has nowhere to go`() {
        val plain = listOf(">> - a", ">> - b")
        assertNull(findHeaderBelow(plain, 0))
        assertNull(findHeaderAbove(plain, 1))
    }

    @Test fun `an empty document has nowhere to go`() {
        assertNull(findHeaderBelow(emptyList(), 0))
        assertNull(findHeaderAbove(emptyList(), 0))
    }

    // A caret past the end (a stale position) must not throw
    @Test fun `a line past the end searches from the last line`() {
        assertNull(findHeaderBelow(doc, 99))
        assertEquals(4, findHeaderAbove(doc, 99))
    }

    @Test fun `a negative line is treated as the start`() {
        assertEquals(1, findHeaderBelow(doc, -5))
        assertNull(findHeaderAbove(doc, -5))
    }

    // "## Markdown" subheadings and ">> items" are not section headers
    @Test fun `only chevron headers count`() {
        val mixed = listOf(">> - a", "## Notes", ">>> - deep", "> Real")
        assertEquals(3, findHeaderBelow(mixed, 0))
    }
}
