/**
 * PasteItemsTest.kt
 * Plain JUnit 4 tests for itemsFromPaste: several pasted lines become separate items.
 */
package com.lewisisworking.chevronlists

import org.junit.Test
import org.junit.Assert.*

class PasteItemsTest {

    private fun paste(line: String, clip: String, at: Int = line.length, prefix: String = "-") =
        itemsFromPaste(line, at, clip, prefix)

    // Bullets
    @Test fun `each pasted line becomes a bullet at the same depth`() =
        assertEquals("one\n>> - two\n>> - three", paste(">> - ", "one\ntwo\nthree"))

    @Test fun `deeper items keep their depth`() =
        assertEquals("a\n>>> - b", paste(">>> - ", "a\nb"))

    @Test fun `the configured prefix is used`() =
        assertEquals("a\n>> * b", paste(">> * ", "a\nb", prefix = "*"))

    @Test fun `first line joins the text already in the item`() =
        assertEquals("a\n>> - b", paste(">> - start ", "a\nb"))

    // Numbered
    @Test fun `numbered items keep counting`() =
        assertEquals("a\n>> 4. b\n>> 5. c", paste(">> 3. ", "a\nb\nc"))

    // Cleaning the clipboard
    @Test fun `blank lines are dropped and lines are trimmed`() =
        assertEquals("a\n>> - b", paste(">> - ", "  a  \n\n\t\n   b"))

    @Test fun `windows line endings are handled`() =
        assertEquals("a\n>> - b", paste(">> - ", "a\r\nb\r\n"))

    @Test fun `lines that are already bullets keep only their text`() =
        assertEquals("a\n>> - b", paste(">> - ", ">> - a\n>>> - b"))

    @Test fun `lines that are already numbered keep only their text`() =
        assertEquals("a\n>> 2. b", paste(">> 1. ", ">> 7. a\n>> 8. b"))

    // Not a paste this handles
    @Test fun `a single line is left to the ordinary paste`() =
        assertNull(paste(">> - ", "just one"))

    @Test fun `one line plus blank lines is still a single line`() =
        assertNull(paste(">> - ", "\n  only  \n\n"))

    @Test fun `a line that is not an item is left alone`() =
        assertNull(paste("plain text", "a\nb"))

    @Test fun `a header line is left alone`() =
        assertNull(paste("> Section", "a\nb"))

    @Test fun `pasting before the item's content is left alone`() =
        assertNull(paste(">> - item", "a\nb", at = 1))

    @Test fun `pasting at the start of the content is handled`() =
        assertEquals("a\n>> - b", paste(">> - item", "a\nb", at = 5))

    @Test fun `a bullet with a different prefix is not an item`() =
        assertNull(paste(">> * ", "a\nb", prefix = "-"))
}
