/**
 * ItemFiltersTest.kt
 * Tests for the item filters, leaderboards and the markers they rely on.
 */
package com.lewisisworking.chevronlists

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ItemFiltersTest {

    private val star = "⭐"
    private val flag = "🚩"

    @Test fun `priority filter shows every priority at level zero`() {
        val lines = listOf("> A", ">> - ! low", ">> - !!! top", ">> - plain")
        assertEquals(listOf(ReportEntry(1, "! low  (A)"), ReportEntry(2, "!!! top  (A)")), itemsWithPriority(lines, "-", 0))
    }

    @Test fun `priority filter keeps one level`() {
        val lines = listOf("> A", ">> - ! low", ">> - [ ] !!! top")
        assertEquals(listOf(ReportEntry(2, "!!! top  (A)")), itemsWithPriority(lines, "-", 3))
    }

    @Test fun `starred items read both markers after a checkbox`() {
        val lines = listOf("> A ==9", ">> - [ ] * a", ">> - $star b", ">> - c")
        assertEquals(listOf(ReportEntry(1, "$star a  (A)"), ReportEntry(2, "$star b  (A)")), starredItems(lines, "-"))
    }

    @Test fun `flagged items read both markers`() {
        val lines = listOf("> A", ">> - ? a", ">> - b $flag x", ">> - c")
        assertEquals(listOf(ReportEntry(1, "$flag a  (A)"), ReportEntry(2, "$flag b x  (A)")), flaggedItems(lines, "-"))
    }

    @Test fun `expired items are before today and in the words`() {
        val lines = listOf("> A", ">> - a @expires:2026-01-01 #x", ">> - b @expires:2026-01-02", ">> - c // @expires:2020-01-01")
        assertEquals(listOf(ReportEntry(1, "a #x  (A) - expired 2026-01-01")), expiredItems(lines, "-", "2026-01-02"))
    }

    @Test fun `expired items need no section`() =
        assertEquals(
            listOf(ReportEntry(0, "a - expired 2020-01-01")),
            expiredItems(listOf(">> - a @expires:2020-01-01"), "-", "2026-01-02")
        )

    @Test fun `rated items are at least the minimum and highest first`() {
        val lines = listOf(">> - ★2 a", ">> - ★5 b", ">> - ★3 c", ">> - d")
        assertEquals(
            listOf(ReportEntry(1, "★★★★★ b"), ReportEntry(2, "★★★ c")),
            ratedItems(lines, "-", 3)
        )
    }

    @Test fun `colour counts put the most used first`() {
        val lines = listOf("> A", ">> - {blue} b", ">> - {red} a", ">> - {red} c", ">> - d")
        assertEquals(listOf("red" to 2, "blue" to 1), colourCounts(lines, "-"))
    }

    @Test fun `items with colour keep one label`() {
        val lines = listOf("> A", ">> - {red} a", ">> - {blue} b", ">> - [x] {red} c")
        assertEquals(listOf(ReportEntry(1, "{red} a  (A)"), ReportEntry(3, "{red} c  (A)")), itemsWithColour(lines, "-", "red"))
    }

    @Test fun `vote leaderboard ranks positive votes`() {
        val lines = listOf("> A", ">> - a +2", ">> - b +5", ">> - c +0", ">> - d")
        assertEquals(listOf(ReportEntry(2, "+5 b  (A)"), ReportEntry(1, "+2 a  (A)")), voteLeaderboard(lines, "-"))
    }

    @Test fun `section vote leaderboard stays in the section`() {
        val lines = listOf("> A", ">> - a +2", ">> - b +5", "> B", ">> - c +9")
        assertEquals(listOf(ReportEntry(2, "#1  +5 b"), ReportEntry(1, "#2  +2 a")), sectionVoteLeaderboard(lines, 1, "-"))
    }

    @Test fun `section vote leaderboard is null outside a section`() =
        assertNull(sectionVoteLeaderboard(listOf("text"), 0, "-"))

    @Test fun `duplicates match plain words in any case`() {
        val lines = listOf("> A", ">> - Same", ">> - other", "> B", ">> - [x] same")
        assertEquals(listOf(ReportEntry(1, "same  (A, line 2)"), ReportEntry(4, "  duplicate  (B, line 5)")), duplicateItems(lines, "-"))
    }

    @Test fun `duplicates need more than one`() =
        assertEquals(emptyList<ReportEntry>(), duplicateItems(listOf(">> - a", ">> - b"), "-"))

    @Test fun `json reports an emoji star and flag`() {
        val json = sectionAsJson(listOf("> A", ">> - $star $flag a"), 0, "-")!!
        assertTrue(json, json.contains("\"starred\": true"))
        assertTrue(json, json.contains("\"flagged\": true"))
    }

    @Test fun `toggle done finds a checkbox after a priority`() =
        assertEquals(">> - [x] !! a", computeToggleDone(">> - !! [ ] a", "-"))

    @Test fun `toggle done finds a done box after a star`() =
        assertEquals("[ ] * a", toggleCheckbox("* [x] a"))
}
