/**
 * SectionSummariesTest.kt
 * Tests for the section reports and the all-caret item edits.
 */
package com.lewisisworking.chevronlists

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SectionSummariesTest {

    @Test fun `word count lists every section and a met goal`() {
        val lines = listOf("> A ==2", ">> - one two", ">> - three", "> B ==9", ">> - one", "> C")
        assertEquals(
            listOf(ReportEntry(0, "A: 3 / 2 words ✅"), ReportEntry(3, "B: 1 word"), ReportEntry(5, "C: 0 words")),
            wordCountReport(lines, "-")
        )
    }

    @Test fun `word count is empty without sections`() =
        assertEquals(emptyList<ReportEntry>(), wordCountReport(listOf("text"), "-"))

    @Test fun `nesting summary counts each depth`() {
        val lines = listOf("> A ==9", ">> - a", ">>> - b", ">>> 1. c")
        assertEquals("CL: \"A\" - Depth 0: 1 item, Depth 1: 2 items", nestingSummary(lines, 1, "-"))
    }

    @Test fun `nesting summary reports an empty section`() =
        assertEquals("CL: \"A\" has no items", nestingSummary(listOf("> A"), 0, "-"))

    @Test fun `nesting summary is null outside a section`() =
        assertNull(nestingSummary(listOf("text"), 0, "-"))

    @Test fun `priority report puts the most urgent first`() {
        val lines = listOf("> A", ">> - ! low", ">> - !!! top", ">> - plain", "> B ==9", ">> - !! mid")
        assertEquals(
            listOf(
                ReportEntry(2, "🔴 Urgent (!!!)  top  (A)"),
                ReportEntry(5, "🟠 High (!!)  mid  (B)"),
                ReportEntry(1, "🟡 Normal (!)  low  (A)"),
            ),
            priorityReport(lines, "-")
        )
    }

    @Test fun `section estimate adds up the words of each item`() {
        val lines = listOf("> A ==9", ">> - ~1h a", ">> - b ~30m", ">> - ~~x~~ ~15m", ">> - none // ~5m")
        assertEquals("CL: \"A\" - 3 estimates totalling 1h 45m", sectionEstimateMessage(lines, 1, "-"))
    }

    @Test fun `section estimate says one estimate`() =
        assertEquals("CL: \"A\" - 1 estimate totalling 20m", sectionEstimateMessage(listOf("> A", ">> - ~20m"), 0, "-"))

    @Test fun `section estimate reports none`() =
        assertEquals("CL: \"A\" - no ~estimate markers found", sectionEstimateMessage(listOf("> A", ">> - a"), 0, "-"))

    @Test fun `an estimate in a comment is not exported`() {
        val json = sectionAsJson(listOf("> A", ">> - a // ~5m"), 0, "-")!!
        assertFalse(json, json.contains("\"estimate\": \"5m\""))
    }

    @Test fun `word frequency ranks words without stop words`() {
        val lines = listOf("> A", ">> - Apple pear the apple", ">> - [x] Pear apple", ">> - an")
        assertEquals("CL: \"A\" top words: \"apple\" ×3  ·  \"pear\" ×2", wordFrequencyMessage(lines, 1, "-"))
    }

    @Test fun `word frequency keeps accented words whole`() =
        assertEquals("CL: \"A\" top words: \"café\" ×1", wordFrequencyMessage(listOf("> A", ">> - café"), 0, "-"))

    @Test fun `word frequency shows ten words at most`() {
        val words = ('a'..'l').joinToString(" ") { "${it}xx" }
        val message = wordFrequencyMessage(listOf("> A", ">> - $words"), 0, "-")!!
        assertEquals(10, message.count { it == '×' })
        assertTrue(message, message.contains("axx"))
    }

    @Test fun `word frequency reports no words`() =
        assertEquals("CL: \"A\" - no words found", wordFrequencyMessage(listOf("> A", ">> - an"), 0, "-"))

    @Test fun `toggle done changes each caret line once`() =
        assertEquals(
            listOf(">> - [x] a", "text", ">> 1. [ ] b", ">> - c"),
            toggleDoneAtLines(listOf(">> - a", "text", ">> 1. [x] b", ">> - c"), listOf(0, 0, 1, 2), "-")
        )

    @Test fun `set priority changes every caret item`() =
        assertEquals(
            listOf(">> 1. [x] !!! a", "text", ">> - !!! b"),
            setPriorityAtLines(listOf(">> 1. [x] !! a", "text", ">> - b"), listOf(0, 1, 2), "-", 3)
        )

    @Test fun `set priority zero removes it`() =
        assertEquals(listOf(">> - a"), setPriorityAtLines(listOf(">> - !! a"), listOf(0), "-", 0))
}
