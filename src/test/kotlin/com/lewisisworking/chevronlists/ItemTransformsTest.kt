/**
 * ItemTransformsTest.kt
 * Plain JUnit 4 tests for computePromote, computeDemote, computeCycleListType.
 */
package com.lewisisworking.chevronlists

import org.junit.Test
import org.junit.Assert.*

class ItemTransformsTest {

    // computePromote
    @Test fun `promote reduces bullet depth from 3 to 2`() {
        assertEquals(">> - Task", computePromote(">>> - Task", "-"))
    }
    @Test fun `promote reduces bullet depth from 4 to 3`() {
        assertEquals(">>> - Deep", computePromote(">>>> - Deep", "-"))
    }
    @Test fun `promote reduces numbered depth and preserves number`() {
        assertEquals(">> 5. Task", computePromote(">>> 5. Task", "-"))
    }
    @Test fun `promote returns null at depth 2 (cannot go shallower)`() {
        assertNull(computePromote(">> - Task", "-"))
        assertNull(computePromote(">> 1. Task", "-"))
    }
    @Test fun `promote returns null for a header line`() {
        assertNull(computePromote("> Section", "-"))
    }
    @Test fun `promote returns null for plain text`() {
        assertNull(computePromote("Just text", "-"))
    }
    @Test fun `promote preserves content with markers`() {
        assertEquals(">> - ⭐ Important", computePromote(">>> - ⭐ Important", "-"))
    }

    // computeDemote
    @Test fun `demote increases bullet depth from 2 to 3`() {
        assertEquals(">>> - Task", computeDemote(">> - Task", "-"))
    }
    @Test fun `demote increases bullet depth from 3 to 4`() {
        assertEquals(">>>> - Deep", computeDemote(">>> - Deep", "-"))
    }
    @Test fun `demote increases numbered depth and preserves number`() {
        assertEquals(">>> 5. Task", computeDemote(">> 5. Task", "-"))
    }
    @Test fun `demote has no upper bound`() {
        assertEquals(">>>>>>>>> - Deep", computeDemote(">>>>>>>> - Deep", "-"))
    }
    @Test fun `demote returns null for a header line`() {
        assertNull(computeDemote("> Section", "-"))
    }
    @Test fun `demote returns null for plain text`() {
        assertNull(computeDemote("Just text", "-"))
    }
    @Test fun `demote preserves content with markers and tags`() {
        assertEquals(">>> - ⭐ #urgent Task", computeDemote(">> - ⭐ #urgent Task", "-"))
    }

    // computeCycleListType
    @Test fun `cycle bullet to numbered uses number 1`() {
        assertEquals(">> 1. Task", computeCycleListType(">> - Task", "-"))
    }
    @Test fun `cycle numbered to bullet uses configured prefix`() {
        assertEquals(">> - Task", computeCycleListType(">> 5. Task", "-"))
    }
    @Test fun `cycle respects custom bullet prefix`() {
        assertEquals(">> * Task", computeCycleListType(">> 3. Task", "*"))
    }
    @Test fun `cycle preserves chevron depth`() {
        assertEquals(">>>> 1. Deep", computeCycleListType(">>>> - Deep", "-"))
    }
    @Test fun `cycle preserves item content`() {
        assertEquals(">> 1. ⭐ #urgent Fix bug @2026-04-22",
                     computeCycleListType(">> - ⭐ #urgent Fix bug @2026-04-22", "-"))
    }
    @Test fun `cycle returns null for a header line`() {
        assertNull(computeCycleListType("> Section", "-"))
    }
    @Test fun `cycle returns null for plain text`() {
        assertNull(computeCycleListType("Just text", "-"))
    }
    @Test fun `cycle returns null for a subheading`() {
        assertNull(computeCycleListType("## Heading", "-"))
    }

    // childItemLines
    @Test fun `childItemLines returns deeper items until same depth`() {
        val lines = listOf(
            ">> - Parent",
            ">>> - Child A",
            ">>>> - Grandchild",
            ">>> - Child B",
            ">> - Sibling"
        )
        assertEquals(listOf(1, 2, 3), childItemLines(lines, 0, "-"))
    }

    @Test fun `childItemLines stops at header or plain text`() {
        val lines = listOf(
            ">> - Parent",
            ">>> - Child",
            "> Section",
            ">>> - Not a child"
        )
        assertEquals(listOf(1), childItemLines(lines, 0, "-"))
    }

    @Test fun `childItemLines returns empty when no children or invalid line`() {
        val lines = listOf(
            ">> - Parent",
            ">> - Sibling",
            "> Header"
        )
        assertEquals(emptyList<Int>(), childItemLines(lines, 0, "-"))
        assertEquals(emptyList<Int>(), childItemLines(lines, 2, "-"))
        assertEquals(emptyList<Int>(), childItemLines(lines, 99, "-"))
    }

    @Test fun `childItemLines works for numbered items`() {
        val lines = listOf(
            ">> 1. Parent",
            ">>> 1. Child",
            ">> 2. Sibling"
        )
        assertEquals(listOf(1), childItemLines(lines, 0, "-"))
    }
}