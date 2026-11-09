/**
 * ItemLifecycleTest.kt
 * The pure age, goal, complexity, recurrence, grouping and archive logic.
 */
package com.lewisisworking.chevronlists

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class ItemLifecycleTest {

    private val today = LocalDate.parse("2026-03-31")

    @Test fun `age report lists the oldest first without the stamp`() {
        val lines = listOf("> A", ">> - new @created:2026-03-30", ">> - old @created:2026-01-01 // note", ">> - plain")
        assertEquals(
            listOf(ReportEntry(2, "89 days - old  (A)"), ReportEntry(1, "1 day - new  (A)")),
            itemAgeReport(lines, "-", today)
        )
        assertEquals("Item Age Report (oldest 2 of 2)", itemAgeReportTitle(lines, "-", today))
    }

    @Test fun `age report stops at fifty but says how many there are`() {
        val lines = List(60) { ">> - a @created:2026-01-01" }
        assertEquals(50, itemAgeReport(lines, "-", today).size)
        assertEquals("Item Age Report (oldest 50 of 60)", itemAgeReportTitle(lines, "-", today))
    }

    @Test fun `age stats cover only the caret section`() {
        val lines = listOf("> A", ">> - old @created:2026-03-01", ">> - new @created:2026-03-29", "> B", ">> - other @created:2020-01-01")
        assertEquals(
            "CL: \"A\" - 2 stamped items  Oldest: 2026-03-01 (30d) old  Newest: 2026-03-29 (2d) new  Average age: 16 days",
            ageStatsMessage(lines, 1, "-", today)
        )
    }

    @Test fun `age stats say when nothing is stamped`() {
        assertEquals(
            "CL: \"A\" - no @created: stamps found. Use CL: Stamp Item to add them.",
            ageStatsMessage(listOf("> A", ">> - a"), 0, "-", today)
        )
        assertNull(ageStatsMessage(listOf("text"), 0, "-", today))
    }

    @Test fun `word goals list the furthest from its goal first`() {
        val lines = listOf("> Done ==1", ">> - one two", "> Short ==10", ">> - one two")
        assertEquals(
            listOf(
                ReportEntry(2, "○ Short  ▓▓░░░░░░░░ 2/10 (20%) - 8 words to go"),
                ReportEntry(0, "✅ Done  ▓▓▓▓▓▓▓▓▓▓ 2/1 (100%) - 1 over goal"),
            ),
            wordCountGoals(lines, "-")
        )
    }

    @Test fun `a goal of zero is already met`() {
        assertEquals(
            listOf(ReportEntry(0, "✅ A  ▓▓▓▓▓▓▓▓▓▓ 0/0 (100%) - 0 over goal")),
            wordCountGoals(listOf("> A ==0"), "-")
        )
    }

    @Test fun `complexity counts each marker in the words`() {
        assertEquals(
            "CL: Complexity ▓▓▓▓▓▓▓▓▓░ 86%  Priority: 2  Tags: 1  Estimate: 1  Due: 1  Expiry: 0  Vote: 1  Label: 0",
            itemComplexityMessage(">> - !! a #x ~1h @2026-01-01 +3", "-")
        )
    }

    @Test fun `complexity ignores dates in a comment`() {
        assertEquals(
            "CL: Complexity ░░░░░░░░░░ 0%  Priority: 0  Tags: 0  Estimate: 0  Due: 0  Expiry: 0  Vote: 0  Label: 0",
            itemComplexityMessage(">> - a // @2026-01-01 @expires:2026-01-01", "-")
        )
    }

    @Test fun `complexity needs an item`() = assertNull(itemComplexityMessage("text", "-"))

    @Test fun `tag lists accept hashes commas and capitals`() {
        assertEquals(listOf("a", "b"), parseTagList(" #A, b  #a "))
    }

    @Test fun `multiple tags can match all or any`() {
        val lines = listOf("> S", ">> - one #a #b", ">> - two #a")
        assertEquals(listOf(ReportEntry(1, "one #a #b  (S)")), itemsWithTags(lines, "-", listOf("a", "b"), true))
        assertEquals(listOf(1, 2), itemsWithTags(lines, "-", listOf("a", "b"), false).map { it.line })
        assertEquals("1 item matching #a & #b", tagFilterTitle(1, listOf("a", "b"), true))
        assertEquals("2 items matching #a | #b", tagFilterTitle(2, listOf("a", "b"), false))
    }

    @Test fun `next occurrence moves the date on`() {
        assertEquals(">> - a @weekly @2026-01-08", nextOccurrenceLine(">> - a @weekly @2026-01-01", "-", today))
        assertEquals(">> - a @daily @2026-04-01", nextOccurrenceLine(">> - a @daily", "-", today))
    }

    @Test fun `a monthly item on the last day does not skip a month`() {
        assertEquals(">> - a @Monthly @2026-02-28", nextOccurrenceLine(">> - a @Monthly @2026-01-31", "-", today))
    }

    @Test fun `next occurrence stays numbered and continues the list`() {
        assertEquals(">>> 4. a @daily @2026-01-02", nextOccurrenceLine(">>> 3. a @daily @2026-01-01", "-", today))
    }

    @Test fun `next occurrence of a done item is open`() {
        assertEquals(">> - [ ] !! a @daily @2026-01-02", nextOccurrenceLine(">> - [x] !! a @daily @2026-01-01", "-", today))
    }

    @Test fun `next occurrence keeps the date before a vote and comment`() {
        assertEquals(">> - a @daily @2026-01-02 +2 // note", nextOccurrenceLine(">> - a @daily @2026-01-01 +2 // note", "-", today))
    }

    @Test fun `a recurrence must be a whole word in the words`() {
        assertNull(nextOccurrenceLine(">> - see @dailybrief", "-", today))
        assertNull(nextOccurrenceLine(">> - a // @daily", "-", today))
    }

    @Test fun `group by mention puts each person under a divider`() {
        val lines = listOf("> S", ">> - a @Sam", ">> - b", ">> - c @Kim", ">> - d @Sam", "> T", ">> - e @Sam")
        val result = groupItemsByMention(lines, 0, "-")!!
        assertEquals(3, result.count)
        assertEquals(
            listOf("> S", ">> -- Sam", ">> - a @Sam", ">> - d @Sam", ">> -- Kim", ">> - c @Kim", ">> -- Unassigned", ">> - b", "> T", ">> - e @Sam"),
            result.lines
        )
    }

    @Test fun `group by mention keeps other lines and replaces old dividers`() {
        val lines = listOf("> S", ">> -- Old", "a note", ">> - a @Sam", "")
        assertEquals(listOf("> S", ">> -- Sam", ">> - a @Sam", "a note", ""), groupItemsByMention(lines, 0, "-")!!.lines)
    }

    @Test fun `one person is still a group`() {
        assertEquals(1, groupItemsByMention(listOf("> S", ">> - a @Sam"), 0, "-")!!.count)
    }

    @Test fun `group by mention needs a mention and a section`() {
        assertNull(groupItemsByMention(listOf("> S", ">> - a"), 0, "-"))
        assertNull(groupItemsByMention(listOf(">> - a @Sam"), 0, "-"))
    }

    @Test fun `collect copies tagged items to a new section at the end`() {
        val result = collectItemsByTag(listOf("> S", ">> - a #x", ">> - b", ">> 1. c #x", ""), "-", "x")!!
        assertEquals(2, result.count)
        assertEquals(listOf("> S", ">> - a #x", ">> - b", ">> 1. c #x", "", "> #x Results", ">> - a #x", ">> 1. c #x", ""), result.lines)
    }

    @Test fun `collecting again leaves out the earlier results`() {
        val once  = collectItemsByTag(listOf("> S", ">> - a #x"), "-", "x")!!.lines
        val twice = collectItemsByTag(once, "-", "x")!!
        assertEquals(1, twice.count)
    }

    @Test fun `collect finds nothing for an unused tag`() = assertNull(collectItemsByTag(listOf(">> - a"), "-", "x"))

    @Test fun `remove old items keeps the young and the unstamped`() {
        val lines  = listOf("> S", ">> - a @created:2026-03-01", ">> - b @created:2026-03-30", ">> - c")
        val result = removeOldItems(lines, "-", 30, today)!!
        assertEquals(1, result.count)
        assertEquals(listOf("> S", ">> - b @created:2026-03-30", ">> - c"), result.lines)
        assertNull(removeOldItems(lines, "-", 31, today))
    }

    @Test fun `archive old done moves only old done items with their children`() {
        val lines = listOf(
            "> S", ">> - [x] a @created:2026-01-01", ">>> - child", ">> - [ ] b @created:2026-01-01",
            ">> - [x] c @created:2026-03-30", ">> - !! [x] d @created:2026-01-01"
        )
        val result = archiveOldDoneItems(lines, "-", 30, today)!!
        assertEquals(2, result.archived)
        assertEquals(
            listOf(
                "> S", ">> - [ ] b @created:2026-01-01", ">> - [x] c @created:2026-03-30", "",
                "> Archive", ">> - [x] a @created:2026-01-01", ">>> - child", ">> - !! [x] d @created:2026-01-01"
            ),
            result.lines
        )
    }

    @Test fun `archive old done leaves the archive alone`() {
        assertNull(archiveOldDoneItems(listOf("> Archive", ">> - [x] a @created:2026-01-01"), "-", 30, today))
    }

    @Test fun `archive done finds a checkbox after a priority`() {
        val result = computeArchiveDone(listOf("> S", ">> - !! [x] a", ">> - b"), 0, "-")!!
        assertEquals(1, result.archived)
        assertEquals(listOf("> S", ">> - b", "", "> Archive", ">> - !! [x] a"), result.lines)
    }
}
