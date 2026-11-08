/**
 * MoveItemToEdgeTest.kt
 * Plain JUnit 4 tests for computeMoveItemToEdge: moving an item to the first or
 * last place among its siblings, carrying anything nested under it.
 */
package com.lewisisworking.chevronlists

import org.junit.Test
import org.junit.Assert.*

class MoveItemToEdgeTest {

    private val doc = listOf(
        "> H",          // 0
        ">> - a",       // 1
        ">>> - a1",     // 2
        ">> - b",       // 3
        ">> - c",       // 4
        ">>> - c1",     // 5
        ">>> - c2",     // 6
        "",             // 7
        "> Next",       // 8
        ">> - x",       // 9
    )

    @Test fun `to top carries the children to the first place`() {
        val r = computeMoveItemToEdge(doc, 4, top = true, "-")!!
        assertEquals(listOf("> H", ">> - c", ">>> - c1", ">>> - c2", ">> - a", ">>> - a1", ">> - b",
            "", "> Next", ">> - x"), r.lines)
        assertEquals(1, r.newIndex)
    }

    @Test fun `to bottom goes after the last sibling's children, before the gap`() {
        val r = computeMoveItemToEdge(doc, 1, top = false, "-")!!
        assertEquals(listOf("> H", ">> - b", ">> - c", ">>> - c1", ">>> - c2", ">> - a", ">>> - a1",
            "", "> Next", ">> - x"), r.lines)
        assertEquals(5, r.newIndex)
    }

    @Test fun `a nested item stays under its parent`() {
        val r = computeMoveItemToEdge(doc, 6, top = true, "-")!!
        assertEquals(listOf(">>> - c2", ">>> - c1"), r.lines.subList(5, 7))
        assertEquals(5, r.newIndex)
    }

    @Test fun `already first or last does nothing`() {
        assertNull(computeMoveItemToEdge(doc, 1, top = true, "-"))
        assertNull(computeMoveItemToEdge(doc, 4, top = false, "-"))
    }

    @Test fun `a non-item line does nothing`() =
        assertNull(computeMoveItemToEdge(doc, 0, top = false, "-"))
}
