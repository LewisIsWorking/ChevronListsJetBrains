package com.lewisisworking.chevronlists

import org.junit.Assert.assertEquals
import org.junit.Test

class GroupsSnippetsTest {

    @Test fun `a group gathers the sections under its divider`() {
        val lines = listOf("> Loose", ">> -- Act One", "> A", ">> - a", "> B", ">> -- Act Two", "> C")
        assertEquals(
            listOf(SectionGroup("Act One", listOf("A", "B"), 1), SectionGroup("Act Two", listOf("C"), 5)),
            sectionGroups(lines),
        )
    }

    @Test fun `a divider with no sections is not a group`() {
        assertEquals(listOf("Two"), sectionGroups(listOf(">> -- One", ">> -- Two", "> A")).map { it.name })
        assertEquals(emptyList<SectionGroup>(), sectionGroups(listOf(">> -- One")))
    }

    @Test fun `a list item is not a divider`() {
        assertEquals(emptyList<SectionGroup>(), sectionGroups(listOf(">> - one", "> A")))
    }

    @Test fun `group entries name their sections`() {
        assertEquals(
            listOf(ReportEntry(0, "One  (2 sections: A, B)"), ReportEntry(3, "Two  (1 section: C)")),
            groupEntries(listOf(">> -- One", "> A", "> B", ">> -- Two", "> C")),
        )
    }

    @Test fun `a divider is trimmed`() {
        assertEquals(">> -- Act One", groupDivider("  Act One "))
    }

    @Test fun `placeholders become their defaults and the first is selected`() {
        val snippet = expandItemSnippet("\${2:b} then \${1:first} @\${3:c}")
        assertEquals("b then first @c", snippet.text)
        assertEquals("first", snippet.text.substring(snippet.selectionStart, snippet.selectionEnd))
    }

    @Test fun `a choice placeholder takes its first option`() {
        val snippet = expandItemSnippet("{\${1|red,green,blue|}} \${2:item}")
        assertEquals("{red} item", snippet.text)
        assertEquals("red", snippet.text.substring(snippet.selectionStart, snippet.selectionEnd))
    }

    @Test fun `every snippet expands with no placeholder left`() {
        val snippets = itemSnippets("2026-01-05")
        assertEquals(10, snippets.size)
        for ((name, template) in snippets) assertEquals(name, false, "\$" in expandItemSnippet(template).text)
    }

    @Test fun `dates default to today`() {
        val task = itemSnippets("2026-01-05").first { it.first == "Task" }.second
        assertEquals("[ ] task @2026-01-05 #tag", expandItemSnippet(task).text)
    }

    @Test fun `an urgent task has a real checkbox`() {
        val urgent = itemSnippets("2026-01-05").first { it.first == "Urgent Task" }.second
        val parts = splitItem(expandItemSnippet(urgent).text)
        assertEquals("[ ]", parts.check)
        assertEquals("!!!", parts.priority)
        assertEquals("[ ] !!! task @2026-01-05", expandItemSnippet(urgent).text)
    }
}
