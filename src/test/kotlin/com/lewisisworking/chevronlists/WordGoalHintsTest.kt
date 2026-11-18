/**
 * WordGoalHintsTest.kt
 * Plain JUnit 4 tests for sectionWordGoalHint.
 */
package com.lewisisworking.chevronlists

import org.junit.Test
import org.junit.Assert.*

class WordGoalHintsTest {

    @Test fun `hint is null for a non-header line`() {
        val lines = listOf("> Section ==10", ">> - words here")
        assertNull(sectionWordGoalHint(lines, 1, "-"))
    }

    @Test fun `hint is null when the header has no goal`() {
        val lines = listOf("> Section", ">> - words here")
        assertNull(sectionWordGoalHint(lines, 0, "-"))
    }

    @Test fun `hint shows words against the goal`() {
        val lines = listOf("> Novel ==10", ">> - four score words here", ">> - two more")
        assertEquals("(▓▓▓▓▓▓░░░░ 6/10)", sectionWordGoalHint(lines, 0, "-"))
    }

    @Test fun `hint clamps an over-goal section to a full bar`() {
        val lines = listOf("> Novel ==2", ">> - four score words here")
        assertEquals("(▓▓▓▓▓▓▓▓▓▓ 4/2)", sectionWordGoalHint(lines, 0, "-"))
    }

    @Test fun `hint stops at the next header`() {
        val lines = listOf("> First ==10", ">> - one two", "> Second ==10", ">> - three four five")
        assertEquals("(▓▓░░░░░░░░ 2/10)", sectionWordGoalHint(lines, 0, "-"))
        assertEquals("(▓▓▓░░░░░░░ 3/10)", sectionWordGoalHint(lines, 2, "-"))
    }

    @Test fun `hint counts numbered items too`() {
        val lines = listOf("> Plan ==10", ">> 1. one two three")
        assertEquals("(▓▓▓░░░░░░░ 3/10)", sectionWordGoalHint(lines, 0, "-"))
    }

    @Test fun `hint counts a mid-line vote as a word like the VS Code extension`() {
        val lines = listOf("> Tasks ==10", ">> - [x] !!! done #tag +1 @created:2026-01-01")
        assertEquals("(▓▓░░░░░░░░ 2/10)", sectionWordGoalHint(lines, 0, "-"))
    }
}
