/**
 * NumberingRunsTest.kt
 * Plain JUnit 4 tests for NumberingRuns and chevronDepth.
 */
package com.lewisisworking.chevronlists

import org.junit.Test
import org.junit.Assert.*

class NumberingRunsTest {

    private fun runsOf(vararg lines: String): List<Int?> {
        val runs = NumberingRuns()
        return lines.map { runs.visit(it) }
    }

    @Test fun `one list at one depth is one run`() =
        assertEquals(listOf(null, 0, 0, 0), runsOf("> H", ">> 1. a", ">> 2. b", ">> 3. c"))

    @Test fun `children of different parents are different runs`() =
        assertEquals(listOf(null, 0, 1, 0, 2), runsOf("> H", ">> 1. a", ">>> 1. a1", ">> 2. b", ">>> 1. b1"))

    @Test fun `deeper items do not break a list`() =
        assertEquals(listOf(0, 1, 0), runsOf(">> 1. a", ">>> 1. a1", ">> 2. b"))

    @Test fun `a header starts new runs`() =
        assertEquals(listOf(null, 0, null, 1), runsOf("> One", ">> 1. a", "> Two", ">> 1. b"))

    @Test fun `a shallower bullet ends a deeper list`() =
        assertEquals(listOf(0, null, 1), runsOf(">>> 1. a", ">> - b", ">>> 1. c"))

    @Test fun `a bullet at the same depth does not end the list`() =
        assertEquals(listOf(0, null, 0), runsOf(">> 1. a", ">> - note", ">> 2. b"))

    @Test fun `plain text lines are ignored`() =
        assertEquals(listOf(0, null, null, 0), runsOf(">> 1. a", "", "plain", ">> 2. b"))

    @Test fun `chevronDepth counts chevrons of chevron lines only`() {
        assertEquals(2, chevronDepth(">> - a"))
        assertEquals(4, chevronDepth(">>>> 1. a"))
        assertNull(chevronDepth("> Header"))
        assertNull(chevronDepth(">>"))
        assertNull(chevronDepth("plain"))
    }
}
