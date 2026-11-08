/**
 * ItemContentEditsTest.kt
 * Plain JUnit 4 tests for editing an item's words, quick notes, section links,
 * replacing text in a section and shifting its due dates.
 */
package com.lewisisworking.chevronlists

import org.junit.Test
import org.junit.Assert.*

class ItemContentEditsTest {

    @Test fun `new words keep the markers and inline metadata`() {
        assertEquals(">> - [x] !! bread #food @2026-01-02 +3 // note",
            replaceItemWords(">> - [x] !! milk #food @2026-01-02 +3 // note", "-", "bread"))
    }

    @Test fun `new words keep a number`() = assertEquals(">> 4. b", replaceItemWords(">> 4. a", "-", "b"))

    @Test fun `new words do not touch a matching tag`() {
        assertEquals(">> - bread #milk", replaceItemWords(">> - #milk milk", "-", "bread"))
    }

    @Test fun `new words stay struck through`() {
        assertEquals(">> - ~~b~~ #t", replaceItemWords(">> - ~~a~~ #t", "-", "b"))
    }

    @Test fun `new words need an item`() = assertNull(replaceItemWords("> A", "-", "b"))

    @Test fun `a quick note is added as a comment`() {
        assertEquals(">> - a +2 // ring first", addItemNote(">> - a +2", "-", " ring first "))
    }

    @Test fun `a quick note replaces the old one and keeps a url`() {
        assertEquals(">> 1. see https://a.example // new", addItemNote(">> 1. see https://a.example // old", "-", "new"))
    }

    @Test fun `a section link takes the section's own spelling`() {
        val lines = listOf("> Launch Plan", "> B", ">> - [x] !! launch plan #t")
        assertEquals(">> - [[Launch Plan]]", itemAsSectionLink(lines, 2, "-"))
    }

    @Test fun `a section link matches a name holding punctuation`() {
        assertEquals(">> - [[Launch!]]", itemAsSectionLink(listOf("> Launch!", ">> - Launch!"), 1, "-"))
    }

    @Test fun `a section link needs a section of that name`() {
        assertNull(itemAsSectionLink(listOf("> A", ">> - other"), 1, "-"))
        assertNull(itemAsSectionLink(listOf("> A", "text"), 1, "-"))
    }

    @Test fun `replace edits only the words of the section's items`() {
        val lines = listOf("> A", ">> - a-b", ">> 1. a1", "prose a-b", "> B", ">> - a-b")
        val edit = replaceInSection(lines, 1, "-", "-", "x")!!
        assertEquals(listOf("> A", ">> - axb", ">> 1. a1", "prose a-b", "> B", ">> - a-b"), edit.lines)
        assertEquals(1, edit.changed)
    }

    @Test fun `replace leaves a number alone and counts occurrences`() {
        val edit = replaceInSection(listOf("> A", ">> 1. a1 b1"), 1, "-", "1", "2")!!
        assertEquals(">> 1. a2 b2", edit.lines[1])
        assertEquals(2, edit.changed)
    }

    @Test fun `replace needs a section`() = assertNull(replaceInSection(listOf(">> - a"), 0, "-", "a", "b"))

    @Test fun `shift moves item dates across a month and counts dates`() {
        val edit = shiftSectionDueDates(listOf("> A", ">> - a @2026-01-31 @2026-02-01"), 1, "-", 1)!!
        assertEquals(">> - a @2026-02-01 @2026-02-02", edit.lines[1])
        assertEquals(2, edit.changed)
    }

    @Test fun `shift moves back and leaves prose and other sections`() {
        val lines = listOf("> A", ">> 1. a @2026-03-01", "prose @2026-03-01", "> B", ">> - b @2026-03-01")
        val edit = shiftSectionDueDates(lines, 1, "-", -1)!!
        assertEquals(listOf("> A", ">> 1. a @2026-02-28", "prose @2026-03-01", "> B", ">> - b @2026-03-01"), edit.lines)
    }

    @Test fun `shift leaves a date the calendar does not have`() {
        val edit = shiftSectionDueDates(listOf("> A", ">> - a @2026-02-30"), 1, "-", 1)!!
        assertEquals(">> - a @2026-02-30", edit.lines[1])
        assertEquals(0, edit.changed)
    }

    @Test fun `shift needs a section`() = assertNull(shiftSectionDueDates(listOf(">> - a"), 0, "-", 1))
}
