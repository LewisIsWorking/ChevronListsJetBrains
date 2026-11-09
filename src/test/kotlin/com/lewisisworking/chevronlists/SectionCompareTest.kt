/**
 * SectionCompareTest.kt
 * Word frequency, the word cloud, heatmaps and section comparisons.
 */
package com.lewisisworking.chevronlists

import org.junit.Assert.*
import org.junit.Test

class SectionCompareTest {
    private val doc = listOf(
        "> A", ">> - [x] milk and milk #t", ">> - bread // milk", "",
        "> B", ">> - [ ] milk", ">> - [x] bread #t #u", ">> - jam",
    )

    @Test fun `word frequency leaves out metadata, comments and small words`() {
        assertEquals(listOf("milk" to 2, "bread" to 1), sectionWordFrequency(doc, 0, "-"))
    }

    @Test fun `word frequency stays in its section`() {
        assertEquals(3, sectionWordFrequency(doc, 4, "-").size)
    }

    @Test fun `the message lists the top words`() {
        assertEquals("CL: \"A\" top words: \"milk\" ×2  ·  \"bread\" ×1", sectionWordsMessage(doc, 0, "-"))
    }

    @Test fun `the message says when there are none`() {
        assertEquals("CL: \"S\" - no words found", sectionWordsMessage(listOf("> S"), 0, "-"))
    }

    @Test fun `the cloud scales bars to the top word`() {
        assertEquals(
            listOf(ReportEntry(0, "██████████ milk  2"), ReportEntry(0, "█████░░░░░ bread  1")),
            wordCloudEntries(doc, 0, "-"),
        )
    }

    @Test fun `the tag heatmap ranks by tags`() {
        assertEquals(listOf(ReportEntry(4, "B: 2 tags"), ReportEntry(0, "A: 1 tag")), tagHeatmap(doc, "-"))
    }

    @Test fun `the completion heatmap ranks by percent done`() {
        assertEquals(listOf(ReportEntry(0, "A: 100% done"), ReportEntry(4, "B: 50% done")), completionHeatmap(doc, "-"))
    }

    @Test fun `a section without boxes says so`() {
        assertEquals(listOf(ReportEntry(0, "S: no checkboxes")), completionHeatmap(listOf("> S"), "-"))
    }

    @Test fun `side by side pads the shorter section and keeps pipes in cells`() {
        val lines = listOf("> A|1", ">> - x|y", "> B", ">> - p", ">> - q")
        assertEquals(
            "# A|1 vs B\n\n| A\\|1 | B |\n|---|---|\n| x\\|y | p |\n|  | q |\n",
            sectionsSideBySide(lines, 0, 2, "-"),
        )
    }

    @Test fun `the diff marks each side`() {
        assertEquals(
            "# Diff: \"A\" vs \"B\"\n\n```diff\n- milk and milk\n+ milk\n  bread\n+ jam\n```",
            sectionsDiff(doc, 0, 4, "-"),
        )
    }

    @Test fun `the clipboard reads copied chevron lines as items`() {
        val lines = listOf("> A", ">> - same", ">> - mine")
        assertEquals(
            "**Section vs Clipboard Diff**\n\n= same\n- mine  *(only in section)*\n+ yours  *(only in clipboard)*",
            sectionAgainstClipboard(lines, 0, "-", ">> - same\r\n\nyours"),
        )
    }

    @Test fun `an empty clipboard is nothing to compare`() {
        assertNull(sectionAgainstClipboard(listOf("> A"), 0, "-", " \n "))
    }
}
