/**
 * HeaderItemConvertTest.kt
 * Plain JUnit 4 tests for itemAsHeader and headerAsItem.
 */
package com.lewisisworking.chevronlists

import org.junit.Test
import org.junit.Assert.*

class HeaderItemConvertTest {

    private val doc = listOf(
        "> A",            // 0
        ">> - a1",        // 1
        ">> 2. a2",       // 2
        "note",           // 3
        "> B",            // 4
        ">> - b1",        // 5
    )

    @Test fun `a bullet item becomes a header`() =
        assertEquals("> a1", itemAsHeader(doc, 1, "-"))

    @Test fun `a numbered item becomes a header`() =
        assertEquals("> a2", itemAsHeader(doc, 2, "-"))

    @Test fun `only an item can be promoted`() {
        assertNull(itemAsHeader(doc, 0, "-"))
        assertNull(itemAsHeader(doc, 3, "-"))
    }

    @Test fun `a header becomes an item of the section above`() =
        assertEquals(4 to ">> - B", headerAsItem(doc, 5, "-"))

    @Test fun `it works from the header line and uses the configured bullet`() =
        assertEquals(4 to ">> * B", headerAsItem(doc, 4, "*"))

    @Test fun `the first section has nowhere to go`() =
        assertNull(headerAsItem(doc, 1, "-"))

    @Test fun `outside a section there is no header`() =
        assertNull(headerAsItem(listOf("intro", "> A"), 0, "-"))
}
