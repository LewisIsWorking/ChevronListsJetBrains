/**
 * SectionCopyTest.kt
 * Plain JUnit 4 tests for sectionAsMarkdown and sectionAsPlainText.
 */
package com.lewisisworking.chevronlists

import org.junit.Test
import org.junit.Assert.*

class SectionCopyTest {

    private val doc = listOf(
        "intro",               // 0
        "> Shopping",          // 1
        ">> - [ ] milk",       // 2
        ">>> - semi-skimmed",  // 3
        "a note",              // 4
        ">> - bread",          // 5
        "",                    // 6
        "> Next",              // 7
        ">> - other",          // 8
    )

    @Test fun `a section becomes a heading and a markdown list`() =
        assertEquals("## Shopping\n\n- [ ] milk\n  - semi-skimmed\n- bread", sectionAsMarkdown(doc, 3, "-"))

    @Test fun `children of a numbered item are indented to its text`() {
        // Two spaces under "1. " is not enough in CommonMark: the child left the list
        val lines = listOf("> Steps", ">> 1. mix", ">>> - flour", ">>>> 10. sift", ">>>>> - twice", ">> 2. bake")
        assertEquals("## Steps\n\n1. mix\n   - flour\n     10. sift\n         - twice\n2. bake",
            sectionAsMarkdown(lines, 0, "-"))
    }

    @Test fun `an item deeper than any parent keeps the nearest parent's indent`() =
        assertEquals("## H\n\n- a\n  - deep", sectionAsMarkdown(listOf("> H", ">> - a", ">>>> - deep"), 0, "-"))

    @Test fun `an item with no parent is indented two spaces a level`() =
        assertEquals("## H\n\n  - deep\n    1. deeper", sectionAsMarkdown(listOf("> H", ">>> - deep", ">>>> 1. deeper"), 0, "-"))

    @Test fun `plain text is the header and item texts`() =
        assertEquals("Shopping\n\n[ ] milk\nsemi-skimmed\nbread", sectionAsPlainText(doc, 2, "-"))

    @Test fun `a markdown heading ends the section`() =
        assertEquals("## A\n\n- x", sectionAsMarkdown(listOf("> A", ">> - x", "## Later", ">> - y"), 1, "-"))

    @Test fun `outside a section there is nothing to copy`() {
        assertNull(sectionAsMarkdown(doc, 0, "-"))
        assertNull(sectionAsPlainText(doc, 0, "-"))
    }

    @Test fun `only the configured bullet counts`() =
        assertEquals("## H\n\n- b", sectionAsMarkdown(listOf("> H", ">> * a", ">> - b"), 0, "-"))
}
