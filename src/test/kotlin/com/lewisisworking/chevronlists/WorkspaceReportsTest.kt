/**
 * WorkspaceReportsTest.kt
 * The reports that look across every markdown file of a project.
 */
package com.lewisisworking.chevronlists

import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

class WorkspaceReportsTest {
    private val one = ScannedFile("a/one.md", listOf(
        "> S",
        ">> - [ ] late @2026-01-01 #t",
        ">> - [x] done @2026-01-01",
        ">> - due @2026-01-10 @bob",
        ">> - later @2026-02-01",
        ">> - c // @2026-01-01",
        "see [[Nope]] and [[s]] and [[file:two.md]] [[file:gone.md#x]]",
    ))
    private val two = ScannedFile("two.md", listOf("> T", ">> - {red} r #t"))
    private val files = listOf(one, two)

    private fun where(entries: List<WorkspaceEntry>?) = entries!!.map { it.path to it.line }

    @Test fun `a file name is the end of its path`() = assertEquals("one.md", one.name)

    @Test fun `today lists open items due by today, latest first`() {
        assertEquals(
            listOf(
                WorkspaceEntry("a/one.md", 1, "9d overdue  late #t  (one.md › S)"),
                WorkspaceEntry("a/one.md", 3, "Today  due @bob  (one.md › S)"),
            ),
            todayEntries(files, "-", LocalDate.of(2026, 1, 10)),
        )
    }

    @Test fun `today skips an impossible date`() {
        assertEquals(emptyList<WorkspaceEntry>(), todayEntries(listOf(ScannedFile("a.md", listOf(">> - a @2026-13-45"))), "-", LocalDate.of(2026, 1, 10)))
    }

    @Test fun `dead links are the ones with no target`() {
        assertEquals(
            listOf(
                WorkspaceEntry("a/one.md", 6, "[[Nope]] (section not found)  one.md:7"),
                WorkspaceEntry("a/one.md", 6, "[[file:gone.md]] (file not found)  one.md:7"),
            ),
            deadLinks(files),
        )
    }

    @Test fun `a section link only counts sections of its own file`() {
        assertEquals(1, deadLinks(listOf(ScannedFile("a.md", listOf("[[T]]")), two)).size)
    }

    @Test fun `items name their file and section`() {
        val items = workspaceItems(files, "-")
        assertEquals(6, items.size)
        assertEquals(WorkspaceEntry("two.md", 1, "{red} r #t  (two.md › T)"), items.last())
    }

    @Test fun `sections name their file`() {
        assertEquals(listOf(WorkspaceEntry("a/one.md", 0, "S  (one.md)"), WorkspaceEntry("two.md", 0, "T  (two.md)")), workspaceSections(files))
    }

    @Test fun `tags gather across files`() {
        val tags = workspaceTags(files, "-")
        assertEquals(setOf("t"), tags.keys)
        assertEquals(listOf("a/one.md" to 1, "two.md" to 1), where(tags["t"]))
    }

    @Test fun `mentions gather across files`() = assertEquals(listOf("a/one.md" to 3), where(workspaceMentions(files, "-")["bob"]))

    @Test fun `colours list only those in use`() {
        val colours = workspaceColours(files, "-")
        assertEquals(setOf("red"), colours.keys)
        assertEquals(listOf("two.md" to 1), where(colours["red"]))
    }

    @Test fun `the tag report counts per file`() {
        assertEquals(
            "# Tag Report - Workspace\n\n1 unique tag across 2 files\n\n## #t (2 total)\n\n  - a/one.md: 1 item\n  - two.md: 1 item",
            workspaceTagReport(files, "-"),
        )
    }

    @Test fun `the tag report needs a tag`() = assertNull(workspaceTagReport(listOf(ScannedFile("a.md", listOf("> S"))), "-"))

    @Test fun `statistics total the files`() {
        val report = workspaceStatistics(files, "-")!!
        assertTrue(report, report.contains("| Files | 2 |"))
        assertTrue(report, report.contains("| Sections | 2 |"))
        assertTrue(report, report.contains("| Items | 6 |"))
        assertTrue(report, report.contains("| Tags | 2 |"))
        assertTrue(report, report.contains("| Completion | 1/2 (50%) |"))
    }

    @Test fun `statistics without checkboxes have no percentage`() {
        assertTrue(workspaceStatistics(listOf(two), "-")!!.contains("| Completion | 0/0 (n/a) |"))
    }

    @Test fun `statistics need a file`() = assertNull(workspaceStatistics(emptyList(), "-"))
}
