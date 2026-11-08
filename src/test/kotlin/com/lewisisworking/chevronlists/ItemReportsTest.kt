/**
 * ItemReportsTest.kt
 * Tests for the date, recurrence, estimate, tag and mention reports.
 */
package com.lewisisworking.chevronlists

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class ItemReportsTest {

    @Test fun `due dates are soonest first and marked overdue`() {
        val lines = listOf("> A", ">> - b @2026-03-01", ">> - a @2026-01-01 x", ">> - c // @2020-01-01", ">> - d")
        assertEquals(
            listOf(ReportEntry(2, "2026-01-01 ⚠️ OVERDUE - a x  (A)"), ReportEntry(1, "2026-03-01 - b  (A)")),
            upcomingDueDates(lines, "-", LocalDate.of(2026, 2, 1))
        )
    }

    @Test fun `due dates skip impossible dates`() =
        assertEquals(emptyList<ReportEntry>(), upcomingDueDates(listOf(">> - a @2026-13-45"), "-", LocalDate.of(2026, 2, 1)))

    @Test fun `recurring items need the whole word in the words`() {
        val lines = listOf("> A", ">> - a @Weekly x", ">> - @dailybrief", ">> - b // @daily")
        assertEquals(listOf(ReportEntry(1, "@weekly - a x  (A)")), recurringItems(lines, "-"))
    }

    @Test fun `old items are thirty days or more and oldest first`() {
        val lines = listOf(
            "> A", ">> - a @created:2026-01-01", ">> - b @created:2026-02-20",
            ">> - c @created:2025-12-01", ">> - d @created:2026-13-45",
        )
        assertEquals(
            listOf(
                ReportEntry(3, "c  (A) - 90 days old (2025-12-01)"),
                ReportEntry(1, "a  (A) - 59 days old (2026-01-01)"),
            ),
            oldItems(lines, "-", LocalDate.of(2026, 3, 1))
        )
    }

    @Test fun `time estimates are longest first`() {
        val lines = listOf("> A", ">> - short ~30m", ">> - long ~1h30m x", ">> - none")
        assertEquals(
            listOf(ReportEntry(2, "~1h30m - long x  (A)"), ReportEntry(1, "~30m - short  (A)")),
            timeEstimates(lines, "-")
        )
        assertEquals("2 estimated items - total: 2h", timeEstimatesTitle(lines, "-"))
    }

    @Test fun `section summary counts items words done and tags`() {
        val lines = listOf("> A ==9", ">> - [x] one two #a", ">> - [ ] three #a #b", ">> - four", "> B", ">> - x #c")
        assertEquals("CL: \"A\" - 3 items, 4 words  Done: 1/3  Tags: #a×2, #b×1", sectionSummary(lines, 1, "-"))
    }

    @Test fun `section summary leaves out what is not there`() =
        assertEquals("CL: \"A\" - 1 item, 1 word", sectionSummary(listOf("> A", ">> - one"), 0, "-"))

    @Test fun `section summary is null outside a section`() =
        assertNull(sectionSummary(listOf("text"), 0, "-"))

    @Test fun `item counts put the most used tag first`() {
        val lines = listOf("> A", ">> - a #x", ">> - b #y #x", ">> - c #y", ">> - d #y")
        assertEquals(listOf(ReportEntry(2, "#y  3 items"), ReportEntry(1, "#x  2 items")), itemCountsByTag(lines, "-"))
    }

    @Test fun `tag stats count a done box after a priority`() {
        val lines = listOf("> A", ">> - [x] a #x", ">> - b #x", ">> - !! [x] c #y")
        assertEquals(
            listOf(ReportEntry(1, "#x  1/2 done  (50%)"), ReportEntry(3, "#y  1/1 done  (100%)")),
            tagStats(lines, "-")
        )
    }

    @Test fun `mention counts read each person once per item in the words`() {
        val lines = listOf(">> - a @Sam @Sam", ">> - @Al b", ">> - c // @Zed", ">> - @lower")
        assertEquals(listOf("Al" to 1, "Sam" to 1), mentionCounts(lines, "-"))
    }

    @Test fun `items mentioning keep one person`() {
        val lines = listOf("> A", ">> - a @Sam", ">> - b @Al")
        assertEquals(listOf(ReportEntry(1, "a @Sam  (A)")), itemsMentioning(lines, "-", "Sam"))
    }

    @Test fun `mention stats show done work`() {
        val lines = listOf("> A", ">> - [x] @Sam a", ">> - @Sam b")
        assertEquals(listOf(ReportEntry(1, "@Sam  1/2 done  (50%)")), mentionStats(lines, "-"))
    }
}
