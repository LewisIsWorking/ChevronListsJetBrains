/**
 * MoveItemsTest.kt
 * Plain JUnit 4 tests for computeMoveItem: moving an item up or down among its
 * siblings, carrying anything nested under it.
 */
package com.lewisisworking.chevronlists

import org.junit.Test
import org.junit.Assert.*

class MoveItemsTest {

    private fun move(lines: List<String>, index: Int, up: Boolean, prefix: String = "-") =
        computeMoveItem(lines, index, up, prefix)

    private val flat = listOf("> H", ">> - a", ">> - b", ">> - c")
    private val nested = listOf("> H", ">> - a", ">>> - a1", ">>> - a2", ">> - b")

    // Up
    @Test fun `an item swaps with the one above it`() {
        val r = move(flat, 2, true)!!
        assertEquals(listOf("> H", ">> - b", ">> - a", ">> - c"), r.lines)
        assertEquals(1, r.newIndex)
    }

    @Test fun `the first item has nowhere to go`() = assertNull(move(flat, 1, true))

    @Test fun `children travel with their parent`() {
        val r = move(nested, 4, true)!!
        assertEquals(listOf("> H", ">> - b", ">> - a", ">>> - a1", ">>> - a2"), r.lines)
        assertEquals(1, r.newIndex)
    }

    @Test fun `a child cannot move above its parent`() = assertNull(move(nested, 2, true))

    @Test fun `a child swaps with its sibling child`() {
        val r = move(nested, 3, true)!!
        assertEquals(listOf("> H", ">> - a", ">>> - a2", ">>> - a1", ">> - b"), r.lines)
        assertEquals(2, r.newIndex)
    }

    // Down
    @Test fun `an item swaps with the one below it`() {
        val r = move(flat, 1, false)!!
        assertEquals(listOf("> H", ">> - b", ">> - a", ">> - c"), r.lines)
        assertEquals(2, r.newIndex)
    }

    @Test fun `the last item has nowhere to go`() = assertNull(move(flat, 3, false))

    // A parent moving down must land below its sibling, not below its own child
    @Test fun `a parent moves past the whole of its next sibling`() {
        val lines = listOf("> H", ">> - a", ">> - b", ">>> - b1")
        val r = move(lines, 1, false)!!
        assertEquals(listOf("> H", ">> - b", ">>> - b1", ">> - a"), r.lines)
        assertEquals(3, r.newIndex)
    }

    @Test fun `a parent carries its children downwards`() {
        val r = move(nested, 1, false)!!
        assertEquals(listOf("> H", ">> - b", ">> - a", ">>> - a1", ">>> - a2"), r.lines)
        assertEquals(2, r.newIndex)
    }

    // Sections and other lines
    @Test fun `an item never crosses into another section`() {
        val lines = listOf("> One", ">> - a", "> Two", ">> - b")
        assertNull(move(lines, 3, true))
        assertNull(move(lines, 1, false))
    }

    @Test fun `numbered items move too and keep their numbers`() {
        val lines = listOf("> H", ">> 1. a", ">> 2. b")
        val r = move(lines, 2, true)!!
        assertEquals(listOf("> H", ">> 2. b", ">> 1. a"), r.lines)
    }

    @Test fun `items in a document with no header still move`() {
        val lines = listOf(">> - a", ">> - b")
        val r = move(lines, 1, true)!!
        assertEquals(listOf(">> - b", ">> - a"), r.lines)
        assertEquals(0, r.newIndex)
    }

    // Lines that are not items
    @Test fun `a line that is not an item does not move`() {
        assertNull(move(listOf("> H", "plain", ">> - a"), 1, false))
    }

    @Test fun `a bullet with another prefix is not an item`() =
        assertNull(move(listOf("> H", ">> * a", ">> * b"), 2, true))

    @Test fun `an index outside the document is ignored`() {
        assertNull(move(flat, 99, true))
        assertNull(move(flat, -1, false))
    }

    // changedSpan: the lines the editor has to rewrite
    @Test fun `changedSpan covers only the lines that moved`() {
        val after = move(flat, 2, true)!!.lines
        assertEquals(1..2, changedSpan(flat, after))
    }

    @Test fun `changedSpan is null for identical documents`() =
        assertNull(changedSpan(flat, flat))

    @Test fun `changedSpan refuses documents of different lengths`() =
        assertNull(changedSpan(flat, flat.dropLast(1)))

    @Test fun `changedSpan covers a whole moved block`() {
        val after = move(nested, 4, true)!!.lines
        assertEquals(1..4, changedSpan(nested, after))
    }
}
