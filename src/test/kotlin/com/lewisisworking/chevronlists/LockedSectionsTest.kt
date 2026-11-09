/**
 * LockedSectionsTest.kt
 * Pure logic of section locks and of which locked sections a save has changed.
 */
package com.lewisisworking.chevronlists

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LockedSectionsTest {

    @Test fun `locking writes the marker below the header`() {
        assertEquals(listOf("> A", ">> [locked]", ">> - a"), withSectionLocked(listOf("> A", ">> - a"), 0))
    }

    @Test fun `a locked section cannot be locked again`() {
        assertNull(withSectionLocked(listOf("> A", ">> [locked]"), 0))
        assertNull(withSectionLocked(listOf("> A", ">> - a", ">> [locked]"), 0))
    }

    @Test fun `unlocking removes the marker`() {
        assertEquals(listOf("> A", ">> - a"), withSectionUnlocked(listOf("> A", ">> [locked]", ">> - a"), 0))
    }

    @Test fun `unlocking finds a marker lower in the section`() {
        assertEquals(listOf("> A", ">> - a"), withSectionUnlocked(listOf("> A", ">> - a", ">> [locked]"), 0))
    }

    @Test fun `unlocking only looks in its own section`() {
        assertNull(withSectionUnlocked(listOf("> A", ">> - a", "> B", ">> [locked]"), 0))
    }

    @Test fun `an edit to a locked section is reported`() {
        val before = listOf("> A", ">> [locked]", ">> - a")
        assertEquals(listOf("A"), changedLockedSections(before, listOf("> A", ">> [locked]", ">> - b")))
    }

    @Test fun `a locked section nothing touched is not reported`() {
        val before = listOf("> A", ">> [locked]", ">> - a", "> B", ">> - b")
        assertEquals(emptyList<String>(), changedLockedSections(before, listOf("> A", ">> [locked]", ">> - a", "> B", ">> - c")))
    }

    @Test fun `only blank lines added at the end is not an edit`() {
        val before = listOf("> A", ">> [locked]", ">> - a")
        assertEquals(emptyList<String>(), changedLockedSections(before, before + ""))
    }

    @Test fun `locking a section is not an edit to a locked section`() {
        val before = listOf("> A", ">> - a")
        assertEquals(emptyList<String>(), changedLockedSections(before, listOf("> A", ">> [locked]", ">> - a")))
    }

    @Test fun `unlocking a section is not an edit to a locked section`() {
        val before = listOf("> A", ">> [locked]", ">> - a")
        assertEquals(emptyList<String>(), changedLockedSections(before, listOf("> A", ">> - a")))
    }

    @Test fun `a locked section new to the file is not reported`() {
        assertEquals(emptyList<String>(), changedLockedSections(listOf("> B"), listOf("> B", "> A", ">> [locked]")))
    }
}
