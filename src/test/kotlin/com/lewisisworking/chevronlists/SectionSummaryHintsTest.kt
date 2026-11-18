/**
 * SectionSummaryHintsTest.kt
 * Plain JUnit 4 tests for summarySparkline and sectionHeaderHint.
 */
package com.lewisisworking.chevronlists

import org.junit.Test
import org.junit.Assert.*
import java.time.LocalDate

class SectionSummaryHintsTest {

    private val today = LocalDate.of(2026, 11, 18)

    @Test fun `sparkline of zero is four light cells`() {
        assertEquals("░░░░", summarySparkline(0.0))
    }

    @Test fun `sparkline of one is graduated like the VS Code extension`() {
        assertEquals("▃▅▆█", summarySparkline(1.0))
    }

    @Test fun `sparkline of half fills two cells`() {
        assertEquals("▅█░░", summarySparkline(0.5))
    }

    @Test fun `hint is null for a non-header line`() {
        val lines = listOf("> Section", ">> - Item")
        assertNull(sectionHeaderHint(lines, 1, "-", today))
    }

    @Test fun `hint is null for an empty section`() {
        val lines = listOf("> Section", "", "> Next", ">> - Item")
        assertNull(sectionHeaderHint(lines, 0, "-", today))
    }

    @Test fun `hint counts items only`() {
        val lines = listOf("> Shopping", ">> - Milk", ">> - Eggs")
        assertEquals("(2 items)", sectionHeaderHint(lines, 0, "-", today))
    }

    @Test fun `hint shows done progress with sparkline`() {
        val lines = listOf("> Tasks", ">> - [x] Done", ">> - [ ] Open", ">> - Plain")
        assertEquals("(3 items · ▅█░░ 1 done)", sectionHeaderHint(lines, 0, "-", today))
    }

    @Test fun `hint stops at the next header`() {
        val lines = listOf("> First", ">> - One", "> Second", ">> - Two", ">> - Three")
        assertEquals("(1 item)", sectionHeaderHint(lines, 0, "-", today))
        assertEquals("(2 items)", sectionHeaderHint(lines, 2, "-", today))
    }

    @Test fun `hint shows tags urgent and old items`() {
        val lines = listOf(
            "> Project",
            ">> - !!! Urgent #work @created:2026-09-01",
            ">> - Calm #home"
        )
        assertEquals("(2 items · 2 tags · 1 urgent · 1 old)", sectionHeaderHint(lines, 0, "-", today))
    }

    @Test fun `recent created stamp is not old`() {
        val lines = listOf("> Project", ">> - Fresh @created:2026-11-10")
        assertEquals("(1 item)", sectionHeaderHint(lines, 0, "-", today))
    }

    @Test fun `hint works for numbered items`() {
        val lines = listOf("> Plan", ">> 1. First #tag", ">> 2. Second")
        assertEquals("(2 items · 1 tag)", sectionHeaderHint(lines, 0, "-", today))
    }
}
