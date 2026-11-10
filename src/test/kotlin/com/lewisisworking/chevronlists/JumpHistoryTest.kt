/**
 * JumpHistoryTest.kt
 * Pure logic of the jump history and recent section reports.
 */
package com.lewisisworking.chevronlists

import org.junit.Assert.assertEquals
import org.junit.Test

class JumpHistoryTest {

    private val lines = listOf("> A", ">> - a", "> B", ">> - b")

    @Test fun `recent sections come newest first`() {
        assertEquals(
            listOf(ReportEntry(2, "B  (line 3)"), ReportEntry(0, "A  (line 1)")),
            recentSectionEntries(lines, listOf(3, 1)),
        )
    }

    @Test fun `a section visited twice appears once`() {
        assertEquals(listOf(0, 2), recentSectionEntries(lines, listOf(1, 0, 3)).map { it.line })
    }

    @Test fun `a jump from above every section is left out`() {
        assertEquals(listOf(1), recentSectionEntries(listOf("prose", "> A"), listOf(0, 1)).map { it.line })
    }

    @Test fun `no jumps give no sections`() {
        assertEquals(emptyList<ReportEntry>(), recentSectionEntries(lines, emptyList()))
    }

    @Test fun `jump entries number the steps back`() {
        assertEquals(
            listOf(
                ReportEntry(4, "Line 5, Col 3  (most recent)"),
                ReportEntry(0, "Line 1, Col 1  (2 steps back)"),
                ReportEntry(9, "Line 10, Col 2  (3 steps back)"),
            ),
            jumpHistoryEntries(listOf(4 to 2, 0 to 0, 9 to 1)),
        )
    }
}
