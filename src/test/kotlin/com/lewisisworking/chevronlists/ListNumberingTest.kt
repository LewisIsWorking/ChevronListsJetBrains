/**
 * ListNumberingTest.kt
 * Plain JUnit 4 tests for rebasing, offsetting and restarting numbered lists,
 * and for wrapping an item's text.
 */
package com.lewisisworking.chevronlists

import org.junit.Test
import org.junit.Assert.*

class ListNumberingTest {

    @Test fun `rebase continues from the number above the caret`() {
        val edit = rebaseListFrom(listOf("> A", ">> 5. a", ">> 9. b", ">> 2. c"), 2)!!
        assertEquals(listOf("> A", ">> 5. a", ">> 6. b", ">> 7. c"), edit.lines)
        assertEquals(2, edit.changed)
    }

    @Test fun `rebase leaves items above the caret alone`() {
        val edit = rebaseListFrom(listOf("> A", ">> 8. a", ">> 3. b"), 2)!!
        assertEquals(">> 8. a", edit.lines[1])
        assertEquals(">> 9. b", edit.lines[2])
    }

    @Test fun `rebase starts a list with no number above at 1`() {
        assertEquals(">> 1. a", rebaseListFrom(listOf("> A", ">> 4. a"), 1)!!.lines[1])
    }

    @Test fun `rebase counts the children of different parents separately`() {
        val lines = listOf("> A", ">> 1. a", ">>> 1. a1", ">> 2. b", ">>> 7. b1")
        assertEquals(">>> 1. b1", rebaseListFrom(lines, 3)!!.lines[4])
    }

    @Test fun `rebase stops at the next section`() {
        val edit = rebaseListFrom(listOf("> A", ">> 3. a", "> B", ">> 9. b"), 1)!!
        assertEquals(listOf("> A", ">> 1. a", "> B", ">> 9. b"), edit.lines)
    }

    @Test fun `rebase reports nothing changed when in sequence`() {
        assertEquals(0, rebaseListFrom(listOf("> A", ">> 1. a", ">> 2. b"), 1)!!.changed)
    }

    @Test fun `rebase needs a numbered item in a section`() {
        assertNull(rebaseListFrom(listOf("> A", ">> - a"), 1))
        assertNull(rebaseListFrom(listOf(">> 1. a"), 0))
    }

    @Test fun `offset moves every number in the section only`() {
        val edit = offsetListNumbers(listOf("> A", ">> 1. a", ">>> 2. b", ">> - c", "> B", ">> 1. d"), 1, 10)!!
        assertEquals(listOf("> A", ">> 11. a", ">>> 12. b", ">> - c", "> B", ">> 1. d"), edit.lines)
        assertEquals(2, edit.changed)
        assertEquals(0, edit.tooLow)
    }

    @Test fun `offset is all or nothing when an item would go below 1`() {
        val lines = listOf("> A", ">> 1. a", ">> 2. b", ">> 10. c")
        val edit = offsetListNumbers(lines, 1, -5)!!
        assertEquals(lines, edit.lines)
        assertEquals(0, edit.changed)
        assertEquals(2, edit.tooLow)
    }

    @Test fun `offset may land exactly on 1`() {
        assertEquals(">> 1. a", offsetListNumbers(listOf("> A", ">> 6. a"), 1, -5)!!.lines[1])
    }

    @Test fun `offset needs a section`() = assertNull(offsetListNumbers(listOf(">> 1. a"), 0, 1))

    @Test fun `set start renumbers the rest of the caret item's list`() {
        val lines = listOf("> A", ">> 1. a", ">> 2. b", ">> 3. c")
        assertEquals(listOf("> A", ">> 1. a", ">> 69. b", ">> 70. c"), setListStart(lines, 2, 69))
    }

    @Test fun `set start leaves nested and later lists alone`() {
        val lines = listOf("> A", ">> 1. a", ">>> 1. n", ">> 2. b", "text", ">> - x", "> B", ">> 1. c")
        assertEquals(
            listOf("> A", ">> 5. a", ">>> 1. n", ">> 6. b", "text", ">> - x", "> B", ">> 1. c"),
            setListStart(lines, 1, 5),
        )
    }

    @Test fun `set start works before any header`() {
        assertEquals(listOf(">> 3. a", ">> 4. b"), setListStart(listOf(">> 1. a", ">> 2. b"), 0, 3))
    }

    @Test fun `set start needs a numbered item`() = assertNull(setListStart(listOf(">> - a"), 0, 3))

    @Test fun `wrap splits a bullet at the column`() {
        val wrap = wrapItemAt(">> - hello world", 11, "-")!!
        assertEquals(">> - hello", wrap.first)
        assertEquals(">>   world", wrap.second)
        assertEquals(5, wrap.column)
    }

    @Test fun `wrap keeps a number and the depth`() {
        val wrap = wrapItemAt(">>> 12. one two", 12, "-")!!
        assertEquals(">>> 12. one", wrap.first)
        assertEquals(">>>   two", wrap.second)
    }

    @Test fun `wrap refuses the ends of the words and non-items`() {
        assertNull(wrapItemAt(">> - hello", 5, "-"))
        assertNull(wrapItemAt(">> - hello", 10, "-"))
        assertNull(wrapItemAt(">> - hello", 2, "-"))
        assertNull(wrapItemAt("> A heading", 4, "-"))
    }
}
