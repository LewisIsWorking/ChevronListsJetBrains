/**
 * TemplatesTest.kt
 * Plain JUnit 4 tests for the template logic in Templates.kt.
 */
package com.lewisisworking.chevronlists

import com.lewisisworking.chevronlists.SnippetPart.End
import com.lewisisworking.chevronlists.SnippetPart.Stop
import com.lewisisworking.chevronlists.SnippetPart.Text
import org.junit.Test
import org.junit.Assert.*

class TemplatesTest {

    @Test fun `the five VS Code templates are built in`() =
        assertEquals(listOf("Bullet List", "Numbered List", "Nested List", "Session Notes", "Character Sheet"),
            BUILT_IN_TEMPLATES.map { it.name })

    @Test fun `a snippet splits into text, stops and the end`() =
        assertEquals(listOf(Text("> "), Stop(1, "Header"), Text("\n>> - "), Stop(2, ""), Text(" "), End),
            parseSnippet("> \${1:Header}\n>> - \$2 \$0"))

    @Test fun `escaped characters are literal, in text and in defaults`() =
        assertEquals(listOf(Text("cost \$5 }"), Stop(1, "a}b\$c\\d")),
            parseSnippet("cost \\\$5 \\}\${1:a\\}b\\\$c\\\\d}"))

    @Test fun `every built-in template parses with its stops in order and ends with the caret`() {
        for (t in BUILT_IN_TEMPLATES) {
            val parts = parseSnippet(t.body)
            val stops = parts.filterIsInstance<Stop>().map { it.index }
            assertEquals(t.name, (1..stops.size).toList(), stops)
            assertEquals(t.name, End, parts.last())
        }
    }

    @Test fun `live template text uses named variables, END and doubled dollars`() =
        assertEquals(LiveTemplateText("> \$V1\$ costs \$\$5\n\$END\$", listOf("V1" to "Header")),
            liveTemplateText(listOf(Text("> "), Stop(1, "Header"), Text(" costs \$5\n"), End)))

    @Test fun `variables follow the stop numbers, not the text order, and a repeat mirrors`() =
        assertEquals(listOf("V1" to "one", "V2" to "two"),
            liveTemplateText(listOf(Stop(2, "two"), Stop(1, "one"), Stop(2, "ignored"))).variables)

    @Test fun `the header line is found from anywhere in the section`() {
        val lines = listOf("intro", "> A", ">> - a", "", "> B", ">> - b")
        assertEquals(1, sectionHeaderLine(lines, 2))
        assertEquals(1, sectionHeaderLine(lines, 1))
        assertEquals(4, sectionHeaderLine(lines, 5))
        assertNull(sectionHeaderLine(lines, 0))
        assertNull(sectionHeaderLine(lines, 99))
    }

    @Test fun `a section becomes a snippet with a stop per item`() =
        assertEquals("> \${1:Plans}\n>> - \${2:a}\n>>> 3. \${3:b}\n\$0",
            sectionToSnippetBody(listOf("> Plans", ">> - a", "prose", ">>> 3. b", "> Next", ">> - n"), 0, "-"))

    @Test fun `empty items get a placeholder and special characters are escaped`() =
        assertEquals("> \${1:Cost \\\$5}\n>> - \${2:item}\n>> - \${3:a\\}b}\n\$0",
            sectionToSnippetBody(listOf("> Cost \$5", ">> - ", ">> - a}b"), 0, "-"))

    @Test fun `a header with nothing under it saves as just the header`() =
        assertEquals("> \${1:Last}\n\$0", sectionToSnippetBody(listOf(">> - x", "> Last"), 1, "-"))

    @Test fun `a saved section round-trips to the text it came from`() {
        val lines = listOf("> Cost \$5", ">> - a}b", ">> 2. c\\d")
        val live  = liveTemplateText(parseSnippet(sectionToSnippetBody(lines, 0, "-")))
        val filled = live.variables.fold(live.text.replace("\$END\$", "")) { text, (name, default) ->
            text.replace("\$$name\$", default) }.replace("\$\$", "\$")
        assertEquals(lines.joinToString("\n") + "\n", filled)
    }
}
