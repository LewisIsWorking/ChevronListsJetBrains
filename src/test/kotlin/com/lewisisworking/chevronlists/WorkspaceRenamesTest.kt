package com.lewisisworking.chevronlists

import org.junit.Assert.assertEquals
import org.junit.Test

class WorkspaceRenamesTest {

    private val files = listOf(
        ScannedFile("a.md", listOf("> Plans", ">> - one #Work", "see #work in prose")),
        ScannedFile("b.md", listOf("> Other", ">> - two #work and #work-more", ">> - none")),
        ScannedFile("c.md", listOf("> Quiet", ">> - nothing")),
    )

    @Test fun `a tag is renamed on items in every file that has it`() {
        val changes = workspaceTagRenames(files, "-", "work", "job")
        assertEquals(listOf("a.md", "b.md"), changes.map { it.path })
        assertEquals(listOf("> Plans", ">> - one #job", "see #work in prose"), changes[0].after)
        assertEquals(">> - two #job and #work-more", changes[1].after[1])
        assertEquals(listOf(1, 1), changes.map { it.changed })
    }

    @Test fun `a tag no item has changes nothing`() {
        assertEquals(emptyList<FileChange>(), workspaceTagRenames(files, "-", "absent", "job"))
    }

    private val linked = listOf(
        ScannedFile("a.md", listOf("> Plans", ">> - see [[Plans]]", ">> - see [[Plans B]]")),
        ScannedFile("b.md", listOf("> Plans", ">> - [[Plans]] and [[Plans]]")),
        ScannedFile("c.md", listOf("> Quiet")),
    )

    @Test fun `a section is renamed in its own file and its links everywhere`() {
        val changes = workspaceSectionRenames(linked, "a.md", 0, "Goals")
        assertEquals(listOf("a.md", "b.md"), changes.map { it.path })
        assertEquals(listOf("> Goals", ">> - see [[Goals]]", ">> - see [[Plans B]]"), changes[0].after)
    }

    @Test fun `a section of the same name in another file keeps its header`() {
        val changes = workspaceSectionRenames(linked, "a.md", 0, "Goals")
        assertEquals(listOf("> Plans", ">> - [[Goals]] and [[Goals]]"), changes[1].after)
        assertEquals(1, changes[1].changed)
    }

    @Test fun `the new section name is trimmed`() {
        assertEquals("> Goals", workspaceSectionRenames(linked, "a.md", 0, "  Goals ")[0].after[0])
    }
}
