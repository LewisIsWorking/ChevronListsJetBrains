/**
 * HeaderIsNotAnItemTest.kt
 * A single ">" is a section header, never an item. The item patterns accepted
 * one chevron, so a header such as "> - Notes" or "> 1. Plan" was also read as
 * an item: pasting lines into it made new headers, and Toggle Done and the
 * marker toggles rewrote the header. Items now need two or more chevrons, as in
 * the VS Code extension.
 */
package com.lewisisworking.chevronlists

import org.junit.Test
import org.junit.Assert.*

class HeaderIsNotAnItemTest {

    @Test fun `a header shaped like a bullet is not a bullet`() =
        assertNull(parseBullet("> - Notes", "-"))

    @Test fun `a header shaped like a numbered item is not numbered`() =
        assertNull(parseNumbered("> 1. Plan"))

    @Test fun `pasting several lines into a header does not create headers`() =
        assertNull(itemsFromPaste("> 1. Plan", 9, "a\nb", "-"))

    @Test fun `toggle done leaves a header alone`() =
        assertNull(computeToggleDone("> - Notes", "-"))

    @Test fun `a star is not toggled on a header`() =
        assertNull(computeToggleMarker("> 1. Plan", "-", "⭐"))

    // Two chevrons are still the shallowest item
    @Test fun `two chevrons are still an item`() {
        assertNotNull(parseBullet(">> - a", "-"))
        assertEquals(1, parseNumbered(">> 1. a")?.num)
    }
}
