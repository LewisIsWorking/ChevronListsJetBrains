/**
 * PinnedSectionsTest.kt
 * Plain JUnit 4 tests for the pin toggle and filter in PinnedSections.kt.
 */
package com.lewisisworking.chevronlists

import org.junit.Test
import org.junit.Assert.*

class PinnedSectionsTest {

    @Test
    fun `toggle adds an absent name lowercased`() {
        assertEquals(setOf("inbox"), togglePinned(emptySet(), "Inbox"))
    }

    @Test
    fun `toggle removes a present name`() {
        assertEquals(emptySet<String>(), togglePinned(setOf("inbox"), "Inbox"))
    }

    @Test
    fun `toggle matches case-insensitively`() {
        assertEquals(emptySet<String>(), togglePinned(setOf("inbox"), "INBOX"))
        assertEquals(setOf("inbox"), togglePinned(emptySet(), "INBOX"))
    }

    @Test
    fun `pinned sections lists only matching headers`() {
        val lines = listOf("> Inbox", ">> - a", "> Later", ">> - b", "> Inbox")
        assertEquals(
            listOf(PinnedSection(0, "Inbox"), PinnedSection(4, "Inbox")),
            pinnedSections(lines, setOf("inbox"))
        )
    }

    @Test
    fun `pinned sections is empty with no pins or no matches`() {
        val lines = listOf("> Inbox", ">> - a")
        assertTrue(pinnedSections(lines, emptySet()).isEmpty())
        assertTrue(pinnedSections(lines, setOf("later")).isEmpty())
    }

    @Test
    fun `pinned sections ignores non-header lines`() {
        val lines = listOf("inbox", ">> - inbox")
        assertTrue(pinnedSections(lines, setOf("inbox")).isEmpty())
    }
}
