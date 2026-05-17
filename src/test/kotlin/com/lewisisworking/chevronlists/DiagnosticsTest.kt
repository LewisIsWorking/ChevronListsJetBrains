/**
 * DiagnosticsTest.kt
 * Plain JUnit 4 tests for the pure diagnostic functions in Diagnostics.kt.
 */
package com.lewisisworking.chevronlists

import org.junit.Test
import org.junit.Assert.*

class DiagnosticsTest {
    private fun lines(vararg s: String): List<String> = s.toList()

    // collectDuplicateHeaders
    @Test fun `duplicate header is flagged on second occurrence`() {
        val r = collectDuplicateHeaders(lines("> Alpha", ">> - x", "> Alpha"))
        assertEquals(1, r.size); assertEquals(2, r[0].line); assertEquals(IssueKind.DUPLICATE_HEADER, r[0].kind)
    }
    @Test fun `duplicate header check is case-insensitive`() {
        assertEquals(1, collectDuplicateHeaders(lines("> Alpha", "> ALPHA")).size)
    }
    @Test fun `unique headers produce no issues`() {
        assertTrue(collectDuplicateHeaders(lines("> Alpha", "> Beta", "> Gamma")).isEmpty())
    }
    @Test fun `duplicate header message includes first line number`() {
        val r = collectDuplicateHeaders(lines("> Test", "> Test"))
        assertTrue(r[0].message.contains("line 1"))
    }

    // collectDuplicateSubheadings
    @Test fun `duplicate subheading is flagged`() {
        val r = collectDuplicateSubheadings(lines("## Session 1", "## Session 1"))
        assertEquals(1, r.size); assertEquals(IssueKind.DUPLICATE_SUBHEADING, r[0].kind)
    }
    @Test fun `duplicate subheading works for h3 and h4`() {
        assertEquals(1, collectDuplicateSubheadings(lines("### A", "### A")).size)
        assertEquals(1, collectDuplicateSubheadings(lines("#### B", "#### B")).size)
    }
    @Test fun `chevron headers are NOT counted as subheadings`() {
        assertTrue(collectDuplicateSubheadings(lines("> Section", "> Section")).isEmpty())
    }

    // collectBadNumbering
    @Test fun `missing number in sequence is flagged`() {
        val r = collectBadNumbering(lines("> H", ">> 1. a", ">> 3. c"))
        assertEquals(1, r.size); assertEquals(2, r[0].line)
        assertTrue(r[0].message.contains("expected 2"))
    }
    @Test fun `duplicate number is flagged`() {
        val r = collectBadNumbering(lines("> H", ">> 1. a", ">> 2. b", ">> 2. dup"))
        assertEquals(1, r.size); assertEquals(3, r[0].line)
    }
    @Test fun `numbering across sections is independent`() {
        // List2 starting at 1 should NOT be flagged
        assertTrue(collectBadNumbering(lines("> L1", ">> 1. a", ">> 2. b", "> L2", ">> 1. x", ">> 2. y")).isEmpty())
    }
    @Test fun `numbering at different depths is independent`() {
        assertTrue(collectBadNumbering(lines("> H", ">> 1. a", ">>> 1. nested", ">>> 2. nested", ">> 2. b")).isEmpty())
    }
    @Test fun `well-formed sequence produces no issues`() {
        assertTrue(collectBadNumbering(lines("> H", ">> 1. a", ">> 2. b", ">> 3. c")).isEmpty())
    }

    // collectEmptySections
    @Test fun `empty section followed by another section is flagged`() {
        val r = collectEmptySections(lines("> Empty", "> Next", ">> - item"))
        assertEquals(1, r.size); assertEquals(0, r[0].line); assertEquals(IssueKind.EMPTY_SECTION, r[0].kind)
    }
    @Test fun `last empty section is NOT flagged (still being written)`() {
        assertTrue(collectEmptySections(lines("> Last")).isEmpty())
    }
    @Test fun `section with items is not flagged`() {
        assertTrue(collectEmptySections(lines("> Has", ">> - item", "> Next", ">> - item")).isEmpty())
    }
    @Test fun `section followed only by subheading is still empty`() {
        val r = collectEmptySections(lines("> Empty", "## Just a subheading", "> Next"))
        assertEquals(1, r.size); assertEquals(0, r[0].line)
    }

    // collectIssues (integration)
    @Test fun `collectIssues combines all kinds and sorts by line`() {
        val doc = lines("## Dup", "> Empty", "> Next", ">> 1. a", ">> 3. c", "## Dup")
        val r = collectIssues(doc, "-")
        assertEquals(3, r.size)
        // Sorted by line: empty section (line 1), bad numbering (line 4), duplicate subheading (line 5)
        assertEquals(IssueKind.EMPTY_SECTION,        r[0].kind)
        assertEquals(IssueKind.BAD_NUMBERING,        r[1].kind)
        assertEquals(IssueKind.DUPLICATE_SUBHEADING, r[2].kind)
    }
    @Test fun `collectIssues returns empty list for a clean document`() {
        assertTrue(collectIssues(lines("> Alpha", ">> 1. a", ">> 2. b", "> Beta", ">> - x"), "-").isEmpty())
    }
}