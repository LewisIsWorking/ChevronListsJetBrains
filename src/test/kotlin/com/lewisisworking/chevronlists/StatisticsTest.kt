/**
 * StatisticsTest.kt
 * Plain JUnit 4 tests for Statistics.kt.
 */
package com.lewisisworking.chevronlists

import org.junit.Test
import org.junit.Assert.*

class StatisticsTest {

    private val doc = listOf(
        "# Notes",                          // 0
        "> Groceries",                      // 1
        ">> - [x] milk #dairy",             // 2
        ">> - [ ] two eggs @2026-01-01",    // 3
        ">>> - free range {green}",         // 4
        ">> - ",                            // 5: empty, not counted
        "",
        "> Empty",                          // 7
        "## Later",                         // 8
        "> Ideas",                          // 9
        ">> 1. one big idea @2099-01-01",   // 10
    )
    private val today = "2026-10-14"

    @Test fun `word count leaves out checkboxes, tags, dates and labels`() {
        assertEquals(1, itemWordCount("[x] milk #dairy"))
        assertEquals(2, itemWordCount("!! two eggs @2026-01-01 {red}"))
        assertEquals(2, itemWordCount("~~struck text~~"))
        assertEquals(0, itemWordCount("[ ] #only-a-tag"))
    }

    @Test fun `a section counts its items at every depth, stopping at the next heading`() {
        assertEquals(SectionStats("Groceries", 1, 3, 5, 1, 2, 1, 1, 1), sectionStats(doc, 1, "-", today))
        assertEquals(SectionStats("Empty", 7, 0, 0, 0, 0, 0, 0, 0), sectionStats(doc, 7, "-", today))
        assertEquals(0, sectionStats(doc, 9, "-", today).overdue)
    }

    @Test fun `file totals add up the sections`() {
        val f = fileStats(doc, "-", today)
        assertEquals(listOf(1, 7, 9), f.sections.map { it.line })
        assertEquals(4, f.totalItems)
        assertEquals(1, f.totalDone)
        assertEquals("Groceries", f.mostPopulated!!.name)
        assertEquals("Empty", f.leastPopulated!!.name)
        assertEquals(4.0 / 3, f.avgItems, 1e-9)
    }

    @Test fun `the section at a line is found above it, not past a heading`() {
        assertEquals(1, sectionHeaderAt(doc, 4))
        assertEquals(1, sectionHeaderAt(doc, 1))
        assertEquals(-1, sectionHeaderAt(doc, 0))
        assertEquals(-1, sectionHeaderAt(doc, 8))
        assertEquals(9, sectionHeaderAt(doc, 10))
    }

    @Test fun `quick stats reads as one line`() {
        assertEquals("\"Groceries\" - 3 items, 1/3 done (33%), 5 words, 1 tag, 1 overdue",
            quickStatsText(sectionStats(doc, 1, "-", today)))
        assertEquals("\"Empty\" - 0 items, 0/0 done (-), 0 words", quickStatsText(sectionStats(doc, 7, "-", today)))
    }

    @Test fun `the report lists totals then each section`() {
        val lines = fileStatsReport(fileStats(doc, "-", today)).lines()
        assertEquals("3 sections, 4 items, 8 words", lines[0])
        assertEquals("Done: 1 of 2 checkboxes", lines[1])
        assertEquals("Average: 1.3 items per section", lines[3])
        assertEquals("Groceries: 3 items, 5 words, 1/2 done", lines[7])
        assertEquals("Ideas: 1 item, 3 words", lines.last())
    }
}
