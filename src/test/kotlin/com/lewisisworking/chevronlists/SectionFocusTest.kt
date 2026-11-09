/**
 * SectionFocusTest.kt
 * Pure logic of hidden section markers.
 */
package com.lewisisworking.chevronlists

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SectionFocusTest {

    @Test fun `a section is hidden by its marker`() {
        assertTrue(isSectionHidden(listOf("> A", ">> [hidden]", ">> - a"), 0))
    }

    @Test fun `a marker lower in the section still counts`() {
        assertTrue(isSectionHidden(listOf("> A", ">> - a", ">> [hidden]"), 0))
    }

    @Test fun `a marker in another section does not count`() {
        assertFalse(isSectionHidden(listOf("> A", ">> - a", "> B", ">> [hidden]"), 0))
    }

    @Test fun `marker lines are found in order`() {
        assertEquals(listOf(1, 4), hiddenMarkerLines(listOf("> A", ">> [hidden]", ">> - a", "> B", ">> [hidden]")))
    }

    @Test fun `no markers gives no lines`() {
        assertEquals(emptyList<Int>(), hiddenMarkerLines(listOf("> A", ">> - [hidden]")))
    }

    @Test fun `a folded range is hidden when it holds the marker`() {
        assertTrue(foldHoldsHiddenMarker("> A\n>> [hidden]\n>> - a"))
        assertFalse(foldHoldsHiddenMarker("> A\n>> - [hidden]"))
    }
}
