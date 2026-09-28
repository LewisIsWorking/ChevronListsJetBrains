/**
 * SectionCheckboxesTest.kt
 * Plain JUnit 4 tests for setCheckbox and markAll.
 */
package com.lewisisworking.chevronlists

import org.junit.Test
import org.junit.Assert.*

class SectionCheckboxesTest {

    // setCheckbox
    @Test fun `an open box is ticked`() = assertEquals("[x] task", setCheckbox("[ ] task", done = true))
    @Test fun `a ticked box is opened`() = assertEquals("[ ] task", setCheckbox("[x] task", done = false))
    @Test fun `an upper-case X counts as ticked`() = assertEquals("[ ] task", setCheckbox("[X] task", done = false))
    @Test fun `an empty pair of brackets counts as a box`() = assertEquals("[x] task", setCheckbox("[] task", done = true))
    @Test fun `a box already in the target state is unchanged`() = assertEquals("[x] task", setCheckbox("[x] task", done = true))
    @Test fun `content without a box is left alone`() = assertNull(setCheckbox("task [ ] later", done = true))
    @Test fun `a box with no text keeps its space`() = assertEquals("[x] ", setCheckbox("[ ]", done = true))

    // markAll
    private val section = listOf(">> - [ ] a", ">> - b", ">>> 2. [ ] c", "note", ">> - [x] d")

    @Test fun `mark all done ticks every box and leaves the rest`() =
        assertEquals(listOf(">> - [x] a", ">> - b", ">>> 2. [x] c", "note", ">> - [x] d"), markAll(section, true, "-"))

    @Test fun `mark all undone opens every box`() =
        assertEquals(listOf(">> - [ ] a", ">> - b", ">>> 2. [ ] c", "note", ">> - [ ] d"), markAll(section, false, "-"))

    @Test fun `bullets with another prefix are not items`() =
        assertEquals(listOf(">> * [ ] a"), markAll(listOf(">> * [ ] a"), true, "-"))

    @Test fun `in the section at the caret only`() {
        val doc = listOf("> One", ">> - [ ] a", "> Two", ">> - [ ] b")
        assertEquals(listOf("> One", ">> - [x] a", "> Two", ">> - [ ] b"), applyToSection(doc, 1) { markAll(it, true, "-") })
        assertNull(applyToSection(listOf("> One", ">> - [x] a"), 1) { markAll(it, true, "-") })
    }
}
