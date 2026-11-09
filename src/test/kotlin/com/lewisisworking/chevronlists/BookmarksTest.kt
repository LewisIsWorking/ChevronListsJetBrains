/**
 * BookmarksTest.kt
 * Pure logic of the bookmark markers.
 */
package com.lewisisworking.chevronlists

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BookmarksTest {

    @Test fun `a marker gives its name`() {
        assertEquals("Chapter 3", parseBookmark(">> [bookmark:Chapter 3]"))
    }

    @Test fun `a marker may be indented and spaced`() {
        assertEquals("Key", parseBookmark("  >>  [bookmark: Key ]"))
    }

    @Test fun `other lines are not bookmarks`() {
        assertNull(parseBookmark(">> - [bookmark:Item]"))
        assertNull(parseBookmark("text [bookmark:Prose]"))
        assertNull(parseBookmark(">> [bookmark:]"))
        assertNull(parseBookmark(">> [bookmark:   ]"))
    }

    @Test fun `entries list each bookmark by line`() {
        val lines = listOf("> A", ">> [bookmark:One]", ">> - item", ">> [bookmark:Two]")
        assertEquals(listOf(ReportEntry(1, "One"), ReportEntry(3, "Two")), bookmarkEntries(lines))
    }

    @Test fun `no bookmarks gives no entries`() {
        assertEquals(emptyList<ReportEntry>(), bookmarkEntries(listOf("> A", ">> - item")))
    }

    @Test fun `a new marker trims its name`() {
        assertEquals(">> [bookmark:Key point]", bookmarkMarker("  Key point "))
    }

    @Test fun `a closing bracket cannot cut the name short`() {
        val marker = bookmarkMarker("a]b]")
        assertEquals(">> [bookmark:ab]", marker)
        assertEquals("ab", parseBookmark(marker!!))
    }

    @Test fun `a name with nothing left gives no marker`() {
        assertNull(bookmarkMarker("   "))
        assertNull(bookmarkMarker(" ] "))
    }
}
