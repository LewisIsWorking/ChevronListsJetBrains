/**
 * SectionInsightsTest.kt
 * Section rankings and checks, pinning, =expressions, and a checkbox after other markers.
 */
package com.lewisisworking.chevronlists

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SectionInsightsTest {

    @Test fun `weights rank by items priorities votes and tags`() {
        val lines = listOf("> Light", ">> - a", "> Heavy ==9", ">> - !! b #x +4", ">> - c")
        assertEquals(
            listOf(ReportEntry(2, "Heavy  weight: 13"), ReportEntry(0, "Light  weight: 3")),
            sectionWeights(lines, "-")
        )
    }

    @Test fun `a completed section needs a box and every box done`() {
        val lines = listOf("> Plain", ">> - a", "> Open", ">> - [x] a", ">> - [ ] b", "> Done", ">> - !! [x] a", ">> - note", ">> - [X] b")
        assertEquals(listOf(ReportEntry(5, "✅ Done  2 items - all done")), completedSections(lines, "-"))
    }

    @Test fun `health check finds empty long and repeated items`() {
        val long  = "x".repeat(201)
        val lines = listOf("> S", ">> - #only", ">> - $long", ">> - Same", ">> - !! same", "> T", ">> - Same")
        assertEquals(
            listOf(
                ReportEntry(1, "Line 2 - Item has no content after stripping metadata"),
                ReportEntry(2, "Line 3 - Item is very long (201 chars) - consider splitting"),
                ReportEntry(4, "Line 5 - Duplicate item content (same as line 4)"),
            ),
            sectionHealthIssues(lines, 0, "-")
        )
    }

    @Test fun `health check is empty for a clean section and null outside one`() {
        assertEquals(emptyList<ReportEntry>(), sectionHealthIssues(listOf("> S", ">> - a"), 0, "-"))
        assertNull(sectionHealthIssues(listOf("text"), 0, "-"))
    }

    @Test fun `pin moves the section above the first one`() {
        assertEquals(
            listOf("intro", "> C", ">> - c", "", "> A", "", "> B", ""),
            pinSectionToTop(listOf("intro", "> A", "", "> C", ">> - c", "", "> B", ""), 3)
        )
    }

    @Test fun `pin of the last section keeps one blank line between sections`() {
        assertEquals(listOf("> B", "", "> A"), pinSectionToTop(listOf("> A", "", "> B"), 2))
    }

    @Test fun `pin does nothing for the first section or outside one`() {
        assertNull(pinSectionToTop(listOf("> A", "> B"), 0))
        assertNull(pinSectionToTop(listOf("text", "> A"), 0))
    }

    @Test fun `similar sections are close but not equal`() {
        val lines = listOf("> Groceries", "> Grocerys", "> Holiday", "> holiday")
        assertEquals(listOf(ReportEntry(0, "\"Groceries\" ↔ \"Grocerys\"  78% similar")), similarSections(lines))
    }

    @Test fun `progress report lists what each section has`() {
        val lines = listOf(
            "> A ==10", ">> - [x] one @2020-01-01", ">> - [ ] two @2020-01-01", ">> - ? three // @2020-01-01", "> B"
        )
        assertEquals(
            listOf(
                ReportEntry(0, "A: 3 items · 1/2 done (50%) · 1 flagged · ⚠ 1 overdue · 3/10 words (30%)"),
                ReportEntry(4, "B: 0 items"),
            ),
            progressReport(lines, "-", "2026-01-01")
        )
    }

    private fun evaluated(content: String) = evaluateItemExpression(">> - $content", "-")?.line?.removePrefix(">> - ")

    @Test fun `expressions follow arithmetic order`() {
        assertEquals("total =14 more", evaluated("total =2+3*4 more"))
        assertEquals("=20", evaluated("=(2+3)*4"))
        assertEquals("=-1", evaluated("=1 - 2"))
        assertEquals("=8", evaluated("=2**3"))
        assertEquals("=512", evaluated("=2**3**2"))
    }

    @Test fun `results are kept to four places without trailing zeros`() {
        assertEquals("=0.3333", evaluated("=1/3"))
        assertEquals("=2.5", evaluated("=5/2"))
    }

    @Test fun `only the first expression changes and markers stay`() {
        assertEquals("[x] !! a =2 b =1+1 +3 // =9+9", evaluated("[x] !! a =1+1 b =1+1 +3 // =9+9"))
    }

    @Test fun `a bad expression or none is left alone`() {
        assertNull(evaluated("=1/0"))
        assertNull(evaluated("=2+"))
        assertNull(evaluated("=(1"))
        assertNull(evaluated("=5 5"))
        assertNull(evaluated("plain"))
        assertNull(evaluated("a // =1+1"))
        assertNull(evaluateItemExpression("text =1+1", "-"))
    }

    @Test fun `evaluation reports what it worked out`() {
        val result = evaluateItemExpression(">> - =1+1 more", "-")!!
        assertEquals("1+1", result.expression)
        assertEquals("2", result.result)
    }

    @Test fun `kanban sees a done box after a priority and a star after a box`() {
        val board = kanbanBoard(listOf("> S", ">> - !! [x] a", ">> - [ ] * b"), "-")
        assertEquals(listOf("!! a"), board.done.map { it.content })
        assertEquals(listOf("b"), board.inProgress.map { it.content })
    }

    @Test fun `statistics see a done box after a priority and ignore a date in a comment`() {
        val stats = sectionStats(listOf("> S", ">> - !! [x] a", ">> - b // @2020-01-01"), 0, "-", "2026-01-01")
        assertEquals(1, stats.done)
        assertEquals(0, stats.overdue)
    }

    @Test fun `section json reads the due date from the words`() {
        assertTrue(sectionAsJson(listOf("> S", ">> - a // @2020-01-01"), 0, "-")!!.contains("\"dueDate\": null"))
    }
}
