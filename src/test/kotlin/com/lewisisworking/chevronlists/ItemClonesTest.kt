/**
 * ItemClonesTest.kt
 * Plain JUnit 4 tests for the item clone variants.
 */
package com.lewisisworking.chevronlists

import org.junit.Test
import org.junit.Assert.*

class ItemClonesTest {

    private fun below(item: String, change: (String) -> String) =
        applyDuplicate(listOf(item), itemCopyBelow(listOf(item), 0, "-", change)!!)[1]

    @Test fun `the copy below goes after the children, which stay with the original`() {
        val lines = listOf(">> - a", ">>> - kid", ">> - b")
        assertEquals(
            listOf(">> - a", ">>> - kid", ">> - a!", ">> - b"),
            applyDuplicate(lines, itemCopyBelow(lines, 0, "-") { "$it!" }!!)
        )
    }

    @Test fun `a numbered copy below takes the next number and moves the rest up`() {
        val lines = listOf(">> 1. a", ">> 2. b")
        assertEquals(
            listOf(">> 1. a", ">> 2. a", ">> 3. b"),
            applyDuplicate(lines, itemCopyBelow(lines, 0, "-") { it }!!)
        )
    }

    @Test fun `only an item is copied`() =
        assertNull(itemCopyBelow(listOf("> Header"), 0, "-") { it })

    @Test fun `as done adds a checkbox`() =
        assertEquals(">> - [x] a", below(">> - a", ::markCloneDone))

    @Test fun `as done ticks an open checkbox`() =
        assertEquals(">> - [x] a", below(">> - [ ] a", ::markCloneDone))

    @Test fun `as done does not double a capital checkbox or one after a priority`() {
        assertEquals(">> - [x] a", below(">> - [X] a", ::markCloneDone))
        assertEquals(">> - [x] !! a", below(">> - !! [ ] a", ::markCloneDone))
    }

    @Test fun `stripped removes every marker and keeps the words`() =
        assertEquals(
            "buy milk",
            stripItemMetadata("[x] !! * buy {red} ~~milk~~ #shop @2026-01-01 ~2h [[Plans]] +3 // note")
        )

    @Test fun `stripped leaves ordinary prose alone`() {
        assertEquals("Why? Because", stripItemMetadata("Why? Because"))
        assertEquals("C++11 rocks", stripItemMetadata("C++11 rocks"))
        assertEquals("5 * 3 = 15", stripItemMetadata("5 * 3 = 15"))
    }

    @Test fun `increment bumps the first plain number`() {
        assertEquals("step 10 of 12", incrementFirstNumber("step 9 of 12"))
        assertEquals("v008", incrementFirstNumber("v007"))
    }

    @Test fun `increment skips tags, dates, votes and links`() =
        assertEquals("#1 @2026 [[Part 3]] then 3", incrementFirstNumber("#1 @2026 [[Part 3]] then 2"))

    @Test fun `increment finds nothing in words`() =
        assertNull(incrementFirstNumber("no digits here"))

    private val doc = listOf("> A", ">> - a", ">>> - kid", ">> - b", "", "> B", ">> - c")

    @Test fun `clone goes to the end of the section, before the blank line`() =
        assertEquals(
            listOf("> A", ">> - a", ">>> - kid", ">> - b", ">> - a", "", "> B", ">> - c"),
            applyDuplicate(doc, cloneItemToSectionEnd(doc, 1, "-")!!)
        )

    @Test fun `a numbered clone continues the list`() {
        val lines = listOf("> A", ">> 1. a", ">> 2. b")
        assertEquals(
            listOf("> A", ">> 1. a", ">> 2. b", ">> 3. a"),
            applyDuplicate(lines, cloneItemToSectionEnd(lines, 1, "-")!!)
        )
    }

    @Test fun `clone needs an item in a section`() {
        assertNull(cloneItemToSectionEnd(doc, 0, "-"))
        assertNull(cloneItemToSectionEnd(listOf(">> - a"), 0, "-"))
    }
}
