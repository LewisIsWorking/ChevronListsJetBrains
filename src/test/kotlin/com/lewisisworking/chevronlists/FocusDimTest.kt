/**
 * FocusDimTest.kt
 * Plain JUnit 4 tests for dimmedRanges.
 */
package com.lewisisworking.chevronlists

import org.junit.Test
import org.junit.Assert.*

class FocusDimTest {

    private val lines = listOf(
        "> First",
        ">> - One",
        ">> - Two",
        "> Second",
        ">> - Three",
        "> Third",
        ">> - Four"
    )

    @Test fun `caret outside any section dims nothing`() {
        assertTrue(dimmedRanges(listOf("No headers here"), 0).isEmpty())
    }

    @Test fun `out-of-range caret dims nothing`() {
        assertTrue(dimmedRanges(lines, 99).isEmpty())
    }

    @Test fun `caret in first section dims the rest`() {
        assertEquals(listOf(3..4, 5..6), dimmedRanges(lines, 1))
    }

    @Test fun `caret on a header keeps its own section lit`() {
        assertEquals(listOf(0..2, 5..6), dimmedRanges(lines, 3))
    }

    @Test fun `caret in last section dims everything above`() {
        assertEquals(listOf(0..2, 3..4), dimmedRanges(lines, 6))
    }

    @Test fun `single section dims nothing`() {
        val single = listOf("> Only", ">> - One")
        assertTrue(dimmedRanges(single, 1).isEmpty())
    }

    @Test fun `blank lines belong to the surrounding section`() {
        val spaced = listOf("> First", "", ">> - One", "", "> Second", ">> - Two")
        assertEquals(listOf(4..5), dimmedRanges(spaced, 2))
    }
}
