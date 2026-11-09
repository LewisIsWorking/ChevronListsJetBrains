/**
 * SectionToolsTest.kt
 * Section colours, the growth chart, search lists, section comparison and the
 * statistics exports.
 */
package com.lewisisworking.chevronlists

import org.junit.Assert.*
import org.junit.Test

class SectionToolsTest {
    private val doc = listOf("> A ==100 [colour:red]", ">> - [x] one #t", ">> - two", "", "> B", ">> - three")

    @Test fun `reads a section colour`() {
        assertEquals(ColourLabel.RED, sectionColourOf("> A [colour:RED]"))
        assertNull(sectionColourOf("> A [colour:pink]"))
        assertNull(sectionColourOf("> A"))
    }

    @Test fun `sets a section colour`() = assertEquals("> A [colour:blue]", withSectionColour("> A", ColourLabel.BLUE))

    @Test fun `replaces a section colour`() = assertEquals("> A [colour:blue]", withSectionColour("> A [colour:red]", ColourLabel.BLUE))

    @Test fun `clears a section colour`() = assertEquals("> A", withSectionColour("> A [colour:pink]", null))

    @Test fun `a section name has no goal or colour`() = assertEquals(listOf("A", "B"), sectionNames(doc))

    @Test fun `finds a section by name ignoring case`() {
        assertEquals(4, sectionNamed(doc, " b "))
        assertNull(sectionNamed(doc, "C"))
    }

    @Test fun `growth is fullest first with bars`() {
        assertEquals(
            listOf(ReportEntry(0, "██████████ 2  A"), ReportEntry(4, "█████░░░░░ 1  B")),
            sectionGrowth(doc, "-"),
        )
    }

    @Test fun `growth of an empty section has an empty bar`() {
        assertEquals(listOf(ReportEntry(0, "░░░░░░░░░░ 0  A")), sectionGrowth(listOf("> A"), "-"))
    }

    @Test fun `item search names the section`() {
        assertEquals(ReportEntry(5, "three  (B)"), itemSearchEntries(doc, "-").last())
        assertEquals(3, itemSearchEntries(doc, "-").size)
    }

    @Test fun `section search lists every section`() {
        assertEquals(listOf(ReportEntry(0, "A"), ReportEntry(4, "B")), sectionSearchEntries(doc))
    }

    @Test fun `compares two sections`() {
        assertEquals(
            listOf(
                "| Metric | A | B |", "|--------|--------|--------|",
                "| Items | 2 | 1 |", "| Words | 2 | 1 |", "| Done | 1/1 | 0/0 |", "| Tags | 1 | 0 |",
            ).joinToString("\n"),
            compareSections(doc, 0, 4, "-"),
        )
    }

    @Test fun `a pipe in a name stays in its cell`() {
        val lines = listOf("> A|B", "> C")
        assertTrue(compareSections(lines, 0, 1, "-").startsWith("| Metric | A\\|B | C |"))
    }

    @Test fun `statistics csv doubles a quote and adds a total`() {
        assertEquals(
            "Section,Items,Words\n\"a \"\"b\"\", c\",1,2\nTOTAL,1,2",
            fileStatsAsCsv(fileStats(listOf("> a \"b\", c", ">> - one two"), "-", "")),
        )
    }

    @Test fun `statistics json has sections and totals`() {
        val json = fileStatsAsJson(fileStats(listOf("> A", ">> - [x] one"), "-", ""))
        assertTrue(json, json.contains("\"totalSections\": 1"))
        assertTrue(json, json.contains("\"name\": \"A\""))
        assertTrue(json, json.contains("\"itemCount\": 1"))
        assertTrue(json, json.contains("\"done\": 1"))
        assertTrue(json, json.contains("\"mostPopulated\": \"A\""))
    }

    @Test fun `statistics json of no sections has no most populated`() {
        assertTrue(fileStatsAsJson(fileStats(listOf("text"), "-", "")).contains("\"mostPopulated\": null"))
    }

    @Test fun `a done item is not overdue`() {
        val lines = listOf("> A", ">> - [x] a @2020-01-01", ">> - !! [X] b @2020-01-01", ">> - c @2020-01-01")
        assertEquals(1, fileStats(lines, "-", "2026-01-01").totalOverdue)
    }
}
