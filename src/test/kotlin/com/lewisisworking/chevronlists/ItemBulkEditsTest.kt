/**
 * ItemBulkEditsTest.kt
 * Plain JUnit 4 tests for stamps, bulk tags, expiry dates and pasted lists.
 */
package com.lewisisworking.chevronlists

import org.junit.Test
import org.junit.Assert.*

class ItemBulkEditsTest {

    @Test fun `a stamp goes before the vote and comment`() {
        assertEquals("a @created:2026-01-02 +2 // n", stampCreated("a +2 // n", "2026-01-02"))
    }

    @Test fun `a stamp in a comment does not count`() {
        assertFalse(hasCreatedStamp("a // see @created:2020-01-01"))
        assertTrue(hasCreatedStamp("[x] a @created:2020-01-01"))
    }

    @Test fun `a bulk tag goes before the vote and comment`() {
        assertEquals("a #x +2 // n", addItemTag("a +2 // n", "x"))
    }

    @Test fun `a bulk tag is skipped when present in any case`() {
        assertEquals("a #X", addItemTag("a #X", "x"))
    }

    @Test fun `a longer tag or one in a comment is not the tag`() {
        assertEquals("a #xy #x", addItemTag("a #xy", "x"))
        assertEquals("a #x // on #x", addItemTag("a // on #x", "x"))
    }

    @Test fun `an expiry goes before the vote and comment`() {
        assertEquals("a @expires:2026-02-02 +2 // n", setItemExpiry("a +2 // n", "2026-02-02"))
    }

    @Test fun `an expiry replaces one among the words only`() {
        assertEquals(
            "@expires:2026-02-02 a // was @expires:2025-01-01",
            setItemExpiry("@expires:2026-01-01 a // was @expires:2025-01-01", "2026-02-02"),
        )
    }

    @Test fun `pasted lines drop blanks and copied markers`() {
        assertEquals(
            listOf("milk", "eggs", "ham", "old", "plain"),
            pastedWords("- milk\r\n* eggs\n\n1. ham\n>> - old\n  plain  "),
        )
    }

    @Test fun `pasted bullets use the prefix`() {
        assertEquals(listOf(">> * a", ">> * b"), pastedBullets("a\n- b", "*"))
    }

    @Test fun `pasted numbers continue the section's highest top level number`() {
        val lines = listOf("> A", ">> 4. a", ">>> 9. n", "> B", ">> 20. b")
        assertEquals(listOf(">> 5. x", ">> 6. y"), pastedNumbered("x\n2. y", lines, 1))
    }

    @Test fun `pasted numbers start at 1 outside a section`() {
        assertEquals(listOf(">> 1. x"), pastedNumbered("x", listOf("text"), 0))
    }

    @Test fun `a pasted section takes the first line as its name and keeps numbers`() {
        assertEquals(
            listOf("> Title", ">> - milk", ">> 3. eggs", ">> - ham"),
            pastedSection("## Title\n- milk\n3) eggs\n>> ham", "-"),
        )
        assertEquals(listOf("> Title"), pastedSection("> Title", "-"))
    }

    @Test fun `nothing pasted from an empty clipboard`() {
        assertTrue(pastedWords(" \n ").isEmpty())
        assertNull(pastedSection(" \n ", "-"))
    }

    @Test fun `a section block goes after the caret section's content`() {
        val lines = listOf("> A", ">> - a", "", "> B")
        assertEquals(listOf("> A", ">> - a", "", "> N", ">> - x", "", "> B"), insertSectionBlock(lines, 1, listOf("> N", ">> - x")))
    }

    @Test fun `a section block on a header goes above it with a blank line`() {
        assertEquals(listOf("> N", "", "> A"), insertSectionBlock(listOf("> A"), 0, listOf("> N")))
    }

    @Test fun `a section block on a blank line outside a section adds no extra blank`() {
        assertEquals(listOf("> N", "", "> A"), insertSectionBlock(listOf("", "> A"), 0, listOf("> N")))
    }
}
