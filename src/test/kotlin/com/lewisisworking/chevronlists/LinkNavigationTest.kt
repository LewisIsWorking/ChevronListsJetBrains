package com.lewisisworking.chevronlists

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LinkNavigationTest {

    @Test fun `the section link under the cursor is found`() {
        val line = "see [[One]] and [[ Two ]]"
        assertEquals("One", sectionLinkAt(line, 6))
        assertEquals("Two", sectionLinkAt(line, 20))
        assertNull(sectionLinkAt(line, 13))
    }

    @Test fun `a link is found with the cursor at either end of it`() {
        assertEquals("One", sectionLinkAt("[[One]]", 0))
        assertEquals("One", sectionLinkAt("[[One]]", 7))
    }

    @Test fun `a file link is not a section link`() {
        assertNull(sectionLinkAt("[[file:a.md]]", 4))
    }

    @Test fun `a file link splits into file and section`() {
        assertEquals(FileLinkTarget("a.md", "Plans"), fileLinkAt("x [[file:a.md#Plans]]", 5))
        assertEquals(FileLinkTarget("a.md", null), fileLinkAt("[[file: a.md ]]", 5))
    }

    @Test fun `a section link is not a file link`() {
        assertNull(fileLinkAt("[[One]]", 3))
    }

    @Test fun `a file section link is written from its parts`() {
        assertEquals("[[file:a.md#Plans]]", fileSectionLink("a.md", "Plans"))
    }

    @Test fun `items are searched without regard to case and say their section`() {
        val lines = listOf(">> - loose milk", "> Shop", ">> - Buy MILK", ">> - eggs", "> Other", ">>> 1. milk run")
        assertEquals(
            listOf(ReportEntry(0, "loose milk"), ReportEntry(2, "Buy MILK  (Shop)"), ReportEntry(5, "milk run  (Other)")),
            itemsMatching(lines, "-", "milk"),
        )
    }

    @Test fun `dependencies are listed with their sections`() {
        val lines = listOf(">>depends:Early", "> Build", ">>depends:Design", ">> - item", "> Ship", ">>depends: Build ")
        assertEquals(
            listOf(ReportEntry(2, "Build depends on Design"), ReportEntry(5, "Ship depends on Build")),
            sectionDependencies(lines),
        )
    }

    @Test fun `every marker is shown in readable form`() {
        assertEquals(
            "✓ 🔴 ⭐ buy #a (due: 2026-01-02) (~2h) (+3) [note: why]",
            richItemText(">> - [x] !!! * buy #a @2026-01-02 ~2h +3 // why", "-"),
        )
    }

    @Test fun `an open check and a flag are shown`() {
        assertEquals("☐ ❓ a", richItemText(">> - [ ] ? a", "-"))
    }

    @Test fun `struck text and colours are made readable`() {
        assertEquals("(old) new", richItemText(">> - {red} ~~old~~ new", "-"))
    }

    @Test fun `a link in the text is not mistaken for a comment`() {
        assertEquals("see https://x.com", richItemText(">> - see https://x.com", "-"))
    }

    @Test fun `words that only look like markers stay as they are`() {
        assertEquals("C++11 ~approx", richItemText(">> - C++11 ~approx", "-"))
    }

    @Test fun `nested and numbered items are indented by depth`() {
        assertEquals("  a", richItemText(">>> - a", "-"))
        assertEquals("    b", richItemText(">>>> 1. b", "-"))
    }

    @Test fun `a line that is not an item has no rich text`() {
        assertNull(richItemText("> Header", "-"))
    }
}
