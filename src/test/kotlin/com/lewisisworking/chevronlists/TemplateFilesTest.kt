/**
 * TemplateFilesTest.kt
 * Plain JUnit 4 tests for template import and export in TemplateFiles.kt.
 */
package com.lewisisworking.chevronlists

import org.junit.Test
import org.junit.Assert.*

class TemplateFilesTest {

    @Test fun `every section imports as a template, items at every depth`() {
        val t = importTemplates(listOf("intro", "> Trip", ">> - tickets", ">>> * seat", "prose", ">> 2. hotel", "", "> Empty"), "plans.md")
        assertEquals(listOf("Trip", "Empty"), t.map { it.name })
        assertEquals(listOf("Imported from plans.md", "Imported from plans.md"), t.map { it.description })
        assertEquals("> \${1:Trip}\n>> - \${2:tickets}\n>>> * \${3:seat}\n>> 2. \${4:hotel}\n\$0", t[0].body)
        assertEquals("> \${1:Empty}\n\$0", t[1].body)
    }

    @Test fun `imported text is escaped and empty items get a placeholder`() =
        assertEquals("> \${1:Cost \\\$5}\n>> - \${2:item}\n>> - \${3:a\\}b}\n\$0",
            importTemplates(listOf("> Cost \$5", ">> - ", ">> - a}b"), "f.md").single().body)

    @Test fun `a file with no sections imports nothing`() =
        assertTrue(importTemplates(listOf(">> - stray", "text"), "f.md").isEmpty())

    @Test fun `snippet text fills every field with its default`() =
        assertEquals("> Head\n>> - a}b\n>> - \n", snippetToText("> \${1:Head}\n>> - \${2:a\\}b}\n>> - \$3\n\$0"))

    @Test fun `export writes each template as its section, a blank line apart`() =
        assertEquals("> Trip\n>> - tickets\n\n> Groceries\n>> - milk\n",
            exportTemplates(listOf(
                ChevronTemplate("Trip", "", "> \${1:Trip}\n>> - \${2:tickets}\n\$0"),
                ChevronTemplate("Groceries", "", ">> - \${1:milk}\$0"))))

    @Test fun `export then import gives the same templates back`() {
        val original = importTemplates(listOf("> A", ">> - x \$1", "> B", ">>> 3. y"), "first.md")
        val again    = importTemplates(exportTemplates(original).split("\n"), "first.md")
        assertEquals(original, again)
    }
}
