/**
 * SectionRenameTest.kt
 * Plain JUnit 4 tests for sectionNameAt and renameSection.
 */
package com.lewisisworking.chevronlists

import org.junit.Test
import org.junit.Assert.*

class SectionRenameTest {

    private val doc = listOf(
        "intro [[Shopping]]",          // 0
        "> Shopping",                  // 1
        ">> - milk",                   // 2
        "> Plans",                     // 3
        ">> - see [[Shopping]] and [[Shopping]]", // 4
        ">> - not [[Shopping list]]",  // 5
    )

    @Test fun `the name is the caret section's header text`() {
        assertEquals(1 to "Shopping", sectionNameAt(doc, 2))
        assertEquals(1 to "Shopping", sectionNameAt(doc, 1))
    }

    @Test fun `there is no name outside a section`() =
        assertNull(sectionNameAt(doc, 0))

    @Test fun `renaming rewrites the header and every link to it`() {
        val result = renameSection(doc, 1, "Groceries")
        assertEquals(listOf(
            "intro [[Groceries]]",
            "> Groceries",
            ">> - milk",
            "> Plans",
            ">> - see [[Groceries]] and [[Groceries]]",
            ">> - not [[Shopping list]]",
        ), result.lines)
        assertEquals(2, result.linkLines)
    }

    @Test fun `the new name is trimmed`() =
        assertEquals("> Groceries", renameSection(doc, 1, "  Groceries ").lines[1])

    @Test fun `a name with regex characters is matched literally`() {
        val result = renameSection(listOf("> a.b (x)", "[[a.b (x)]] [[aXb (x)]]"), 0, "c")
        assertEquals(listOf("> c", "[[c]] [[aXb (x)]]"), result.lines)
        assertEquals(1, result.linkLines)
    }
}
