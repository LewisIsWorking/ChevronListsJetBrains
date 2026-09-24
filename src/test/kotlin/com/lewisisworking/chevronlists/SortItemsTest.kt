/**
 * SortItemsTest.kt
 * Plain JUnit 4 tests for computeSortSection: siblings are sorted with their
 * nested items attached, and numbers stay with their positions.
 */
package com.lewisisworking.chevronlists

import org.junit.Test
import org.junit.Assert.*

class SortItemsTest {

    private fun sort(lines: List<String>, caret: Int = 1, descending: Boolean = false, prefix: String = "-") =
        computeSortSection(lines, caret, descending, prefix)

    @Test fun `bullets sort A to Z`() =
        assertEquals(listOf("> H", ">> - apple", ">> - Banana", ">> - cherry"),
            sort(listOf("> H", ">> - cherry", ">> - apple", ">> - Banana")))

    @Test fun `bullets sort Z to A`() =
        assertEquals(listOf("> H", ">> - cherry", ">> - Banana", ">> - apple"),
            sort(listOf("> H", ">> - apple", ">> - cherry", ">> - Banana"), descending = true))

    @Test fun `children travel with their parent`() =
        assertEquals(listOf("> H", ">> - a", ">>> - a1", ">> - b", ">>> - b1"),
            sort(listOf("> H", ">> - b", ">>> - b1", ">> - a", ">>> - a1")))

    @Test fun `children are sorted among themselves`() =
        assertEquals(listOf("> H", ">> - p", ">>> - x", ">>> - y", ">>> - z"),
            sort(listOf("> H", ">> - p", ">>> - z", ">>> - x", ">>> - y")))

    @Test fun `numbers stay with their positions`() =
        assertEquals(listOf("> H", ">> 1. apple", ">> 2. banana", ">> 3. cherry"),
            sort(listOf("> H", ">> 1. cherry", ">> 2. apple", ">> 3. banana")))

    @Test fun `a numbered list that starts later keeps its start`() =
        assertEquals(listOf("> H", ">> 5. a", ">> 6. b"),
            sort(listOf("> H", ">> 5. b", ">> 6. a")))

    @Test fun `a blank line splits the lists around it`() =
        assertEquals(listOf("> H", ">> - c", ">> - d", "", ">> - a", ">> - b"),
            sort(listOf("> H", ">> - d", ">> - c", "", ">> - b", ">> - a")))

    @Test fun `only the caret's section is sorted`() {
        val lines = listOf("> One", ">> - b", ">> - a", "> Two", ">> - d", ">> - c")
        assertEquals(listOf("> One", ">> - b", ">> - a", "> Two", ">> - c", ">> - d"), sort(lines, caret = 4))
        assertEquals(listOf("> One", ">> - a", ">> - b", "> Two", ">> - d", ">> - c"), sort(lines, caret = 0))
    }

    @Test fun `equal items keep their order`() =
        assertEquals(listOf("> H", ">> - a", ">> - Same", ">> - same"),
            sort(listOf("> H", ">> - Same", ">> - same", ">> - a")))

    // A tab after the chevrons parses as an item; the sort must not assume a space
    @Test fun `items with a tab after the chevrons sort without crashing`() =
        assertEquals(listOf("> H", ">>\t- a", ">>\t- b"), sort(listOf("> H", ">>\t- b", ">>\t- a")))

    @Test fun `a sorted section is left alone`() =
        assertNull(sort(listOf("> H", ">> - a", ">> - b")))

    @Test fun `a single item is left alone`() =
        assertNull(sort(listOf("> H", ">> - a")))

    @Test fun `bullets with another prefix are not items`() =
        assertNull(sort(listOf("> H", ">> * b", ">> * a")))

    @Test fun `a document with no header is one section`() =
        assertEquals(listOf(">> - a", ">> - b"), sort(listOf(">> - b", ">> - a"), caret = 0))

    @Test fun `a caret outside the document is ignored`() {
        assertNull(sort(listOf("> H", ">> - b", ">> - a"), caret = 9))
        assertNull(sort(listOf("> H", ">> - b", ">> - a"), caret = -1))
    }
}
