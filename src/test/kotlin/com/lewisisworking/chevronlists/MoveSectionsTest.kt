/**
 * MoveSectionsTest.kt
 * Plain JUnit 4 tests for computeMoveSection: swapping the section at the caret
 * with the one above or below it.
 */
package com.lewisisworking.chevronlists

import org.junit.Test
import org.junit.Assert.*

class MoveSectionsTest {

    private val doc = listOf(
        "intro",        // 0
        "> A",          // 1
        ">> - a1",      // 2
        ">> - a2",      // 3
        "",             // 4
        "> B",          // 5
        ">> - b1",      // 6
        "",             // 7
        "",             // 8
        "> C",          // 9
        ">> - c1",      // 10
    )

    @Test fun `a section moves up past the one above, keeping the gap between them`() {
        val r = computeMoveSection(doc, 6, up = true)!!
        assertEquals(listOf("intro", "> B", ">> - b1", "", "> A", ">> - a1", ">> - a2", "", "", "> C", ">> - c1"), r.lines)
        // The caret stays on the same line of the section it moved
        assertEquals(2, r.newIndex)
    }

    @Test fun `a section moves down past the one below`() {
        val r = computeMoveSection(doc, 1, up = false)!!
        assertEquals(listOf("intro", "> B", ">> - b1", "", "> A", ">> - a1", ">> - a2", "", "", "> C", ">> - c1"), r.lines)
        assertEquals(4, r.newIndex)
    }

    @Test fun `the last section moves up at the end of the file`() {
        val r = computeMoveSection(doc, 10, up = true)!!
        assertEquals(listOf("intro", "> A", ">> - a1", ">> - a2", "", "> C", ">> - c1", "", "", "> B", ">> - b1"), r.lines)
        assertEquals(6, r.newIndex)
    }

    @Test fun `notes under a section travel with it`() {
        // VS Code's move stops a section at its last item, so a paragraph under
        // the items was left behind and ended up under the other section
        val lines = listOf("> A", ">> - a1", "Some notes", "", "> B", ">> - b1")
        val r = computeMoveSection(lines, 5, up = true)!!
        assertEquals(listOf("> B", ">> - b1", "", "> A", ">> - a1", "Some notes"), r.lines)
    }

    @Test fun `a caret on a blank line after the items stays in the section`() {
        val r = computeMoveSection(doc, 4, up = false)!!
        assertEquals(6, r.newIndex)
    }

    @Test fun `nowhere to go`() {
        assertNull(computeMoveSection(doc, 2, up = true))     // first section
        assertNull(computeMoveSection(doc, 10, up = false))   // last section
        assertNull(computeMoveSection(doc, 0, up = false))    // above every header
        assertNull(computeMoveSection(listOf("> Only", ">> - x"), 1, up = true))
    }

    @Test fun `the line count never changes`() {
        for (line in doc.indices) for (up in listOf(true, false)) {
            computeMoveSection(doc, line, up)?.let { assertEquals(doc.size, it.lines.size) }
        }
    }
}
