/**
 * ItemEditsTest.kt
 * Plain JUnit 4 tests for merging and splitting items, sorting sections and text styles.
 */
package com.lewisisworking.chevronlists

import org.junit.Test
import org.junit.Assert.*

class ItemEditsTest {

    @Test fun `merge joins two items`() =
        assertEquals(listOf("> A", ">> - a - b"), mergeItemWithNext(listOf("> A", ">> - a", ">> - b"), 1, "-"))

    @Test fun `merge closes up a numbered list`() =
        assertEquals(
            listOf(">> 1. a - b", ">>> 1. kid", ">> 2. c"),
            mergeItemWithNext(listOf(">> 1. a", ">> 2. b", ">>> 1. kid", ">> 3. c"), 0, "-")
        )

    @Test fun `merge keeps the first markers and the second vote and comment`() =
        assertEquals(
            listOf(">> - [ ] a - b +3 // x"),
            mergeItemWithNext(listOf(">> - [ ] a", ">> - [x] b +3 // x"), 0, "-")
        )

    @Test fun `merge keeps both comments`() =
        assertEquals(listOf(">> - a - b // x; y"), mergeItemWithNext(listOf(">> - a // x", ">> - b // y"), 0, "-"))

    @Test fun `merge needs two items`() {
        assertNull(mergeItemWithNext(listOf(">> - a", "note"), 0, "-"))
        assertNull(mergeItemWithNext(listOf(">> - a"), 0, "-"))
    }

    @Test fun `split makes two items`() =
        assertEquals(ItemSplit(listOf(">> - one", ">> - two"), 5), splitItemAt(listOf(">> - one two"), 0, 9, "-"))

    @Test fun `split opens a gap in a numbered list`() =
        assertEquals(
            listOf(">> 1. one", ">> 2. two", ">>> 1. kid", ">> 3. c"),
            splitItemAt(listOf(">> 1. one two", ">>> 1. kid", ">> 2. c"), 0, 10, "-")!!.lines
        )

    @Test fun `split needs the caret inside the words`() {
        assertNull(splitItemAt(listOf(">> - one"), 0, 5, "-"))
        assertNull(splitItemAt(listOf(">> - one"), 0, 8, "-"))
        assertNull(splitItemAt(listOf("> A"), 0, 2, "-"))
    }

    @Test fun `sort moves whole sections and keeps the gaps`() =
        assertEquals(
            listOf("intro", "> a", ">> - 1", "", "> B", ">> - 2", ""),
            sortAllSections(listOf("intro", "> B", ">> - 2", "", "> a", ">> - 1", ""), true)
        )

    @Test fun `sort adds no blank line at the end`() =
        assertEquals(
            listOf("> A", "", "> B"),
            sortAllSections(listOf("> B", "", "> A"), true)
        )

    @Test fun `sort descending`() =
        assertEquals(listOf("> B", "> A"), sortAllSections(listOf("> A", "> B"), false))

    @Test fun `sort needs two sections`() =
        assertNull(sortAllSections(listOf("> A", ">> - a"), true))

    @Test fun `wrap toggles`() {
        assertEquals("**a**", toggleWrap("a", "**"))
        assertEquals("a", toggleWrap("**a**", "**"))
        assertEquals("****", toggleWrap("", "**"))
    }

    @Test fun `underline toggles and keeps an emoji whole`() {
        assertEquals("a̲b̲", toggleUnderline("ab"))
        assertEquals("ab", toggleUnderline("a̲b̲"))
        assertEquals("😀̲", toggleUnderline("😀"))
    }

    @Test fun `word at the caret`() {
        assertEquals(3..5, wordRangeAt("ab cde", 4))
        assertEquals(3..5, wordRangeAt("ab cde", 6))
        assertNull(wordRangeAt("ab  cd", 3))
    }
}
