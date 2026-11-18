/**
 * ChecklistProgressHintsTest.kt
 * Plain JUnit 4 tests for checklistBar, sectionChecklistHint and
 * subgroupChecklistHint.
 */
package com.lewisisworking.chevronlists

import org.junit.Test
import org.junit.Assert.*

class ChecklistProgressHintsTest {

    @Test fun `full bar when all done`() {
        assertEquals("▓▓▓▓▓▓▓▓▓▓", checklistBar(3, 3, 10))
    }

    @Test fun `empty bar when none done`() {
        assertEquals("░░░░░░", checklistBar(0, 4, 6))
    }

    @Test fun `half bar rounds to five cells`() {
        assertEquals("▓▓▓▓▓░░░░░", checklistBar(1, 2, 10))
    }

    @Test fun `section hint is null for a non-header line`() {
        val lines = listOf("> Section", ">> - [x] Done")
        assertNull(sectionChecklistHint(lines, 1, "-"))
    }

    @Test fun `section hint is null when there are no checkboxes`() {
        val lines = listOf("> Section", ">> - Plain", "", "> Next", ">> - [x] Done")
        assertNull(sectionChecklistHint(lines, 0, "-"))
    }

    @Test fun `section hint counts plain items out`() {
        val lines = listOf("> Tasks", ">> - [x] Done", ">> - [ ] Open", ">> - Plain")
        assertEquals("(▓▓▓▓▓░░░░░ 1/2)", sectionChecklistHint(lines, 0, "-"))
    }

    @Test fun `section hint stops at the next header`() {
        val lines = listOf("> First", ">> - [x] One", "> Second", ">> - [ ] Two")
        assertEquals("(▓▓▓▓▓▓▓▓▓▓ 1/1)", sectionChecklistHint(lines, 0, "-"))
        assertEquals("(░░░░░░░░░░ 0/1)", sectionChecklistHint(lines, 2, "-"))
    }

    @Test fun `subgroup hint is null for a checkbox line`() {
        val lines = listOf("> S", ">> 1. [ ] Group", ">>> - [x] Child")
        assertNull(subgroupChecklistHint(lines, 1, "-"))
    }

    @Test fun `subgroup hint is null without deeper children`() {
        val lines = listOf("> S", ">> 1. Group", ">> - [x] Sibling")
        assertNull(subgroupChecklistHint(lines, 1, "-"))
    }

    @Test fun `subgroup hint counts only children one depth deeper`() {
        val lines = listOf(
            "> S",
            ">> 1. Group",
            ">>> - [x] Done",
            ">>> - [ ] Open",
            ">>>> - [ ] Grandchild",
            ">> - [x] Sibling"
        )
        assertEquals("(▓▓▓░░░ 1/2)", subgroupChecklistHint(lines, 1, "-"))
    }

    @Test fun `subgroup hint stops at a sibling`() {
        val lines = listOf(
            "> S",
            ">> 1. First",
            ">>> - [x] Done",
            ">> 2. Second",
            ">>> - [ ] Open"
        )
        assertEquals("(▓▓▓▓▓▓ 1/1)", subgroupChecklistHint(lines, 1, "-"))
        assertEquals("(░░░░░░ 0/1)", subgroupChecklistHint(lines, 3, "-"))
    }
}
