/**
 * SectionReportsTest.kt
 * Plain JUnit 4 tests for the table of contents, reading time, section path,
 * item markdown and section table.
 */
package com.lewisisworking.chevronlists

import org.junit.Test
import org.junit.Assert.*

class SectionReportsTest {

    @Test fun `contents list every section except an earlier contents`() {
        assertEquals(
            listOf("> Table of Contents", ">> - [[A]]", ">> - [[B]]"),
            tableOfContents(listOf("> A", ">> - a", "> Table of Contents", ">> - [[A]]", "> B")),
        )
    }

    @Test fun `contents need a section`() = assertNull(tableOfContents(listOf("text")))

    @Test fun `reading time covers the caret section`() {
        val words = (1..400).joinToString(" ") { "w$it" }
        assertEquals("CL: \"A\" - 400 words, ~2 minutes to read",
            readingTimeMessage(listOf("> A ==500", ">> - $words", "> B", ">> - x"), 1, "-"))
    }

    @Test fun `reading time leaves out metadata and reads short text in seconds`() {
        assertEquals("CL: \"A\" - 2 words, ~1 second to read",
            readingTimeMessage(listOf("> A", ">> - [x] one #tag two"), 1, "-"))
    }

    @Test fun `reading time reads the whole file outside a section`() {
        assertEquals("CL: Whole file - 1 word, ~0 seconds to read",
            readingTimeMessage(listOf(">> - one"), 0, "-"))
    }

    @Test fun `section path shows the place and the counts`() {
        assertEquals("📍 A  ·  line 2 of 3  ·  2 items  ·  3 words",
            sectionPathMessage(listOf("> A ==9", ">> - one two", ">> 1. three", "text", "> B"), 2, "-"))
    }

    @Test fun `section path needs a section`() = assertNull(sectionPathMessage(listOf("text"), 0, "-"))

    @Test fun `an item as markdown marks the check and priority and bolds tags`() {
        assertEquals("- [x] 🟠 a **#t**", itemAsMarkdown(">> - [x] !! a #t +3 // note", "-"))
    }

    @Test fun `an item as markdown keeps its number and depth and drops the colour`() {
        assertEquals("  4. a", itemAsMarkdown(">>> 4. {red} a", "-"))
    }

    @Test fun `only items become markdown`() = assertNull(itemAsMarkdown("> A", "-"))

    @Test fun `a section table escapes pipes and numbers its rows`() {
        assertEquals(
            "# A\n\n| # | Content |\n|---|---------|\n| 1 | a \\| b |\n| 2 | c |\n",
            sectionAsMarkdownTable(listOf("> A ==9", ">> - a | b #t", ">> 7. c", "> B", ">> - d"), 1, "-"),
        )
    }

    @Test fun `a section table needs items`() {
        assertNull(sectionAsMarkdownTable(listOf("> A"), 0, "-"))
        assertNull(sectionAsMarkdownTable(listOf("text"), 0, "-"))
    }
}
