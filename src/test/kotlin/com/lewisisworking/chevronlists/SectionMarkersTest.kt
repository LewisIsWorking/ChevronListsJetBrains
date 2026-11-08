/**
 * SectionMarkersTest.kt
 * Plain JUnit 4 tests for the section-wide marker commands and votes.
 */
package com.lewisisworking.chevronlists

import org.junit.Test
import org.junit.Assert.*

class SectionMarkersTest {

    @Test fun `mark done ticks a todo, adds a box to a plain item and leaves a done item`() {
        assertEquals("[x] a", markItemDone("[ ] a"))
        assertEquals("[x] a", markItemDone("a"))
        assertEquals("[x] a", markItemDone("[x] a"))
    }

    @Test fun `mark done finds a checkbox after a priority`() =
        assertEquals("[x] !! a", markItemDone("!! [ ] a"))

    @Test fun `mark undone only resets done items`() {
        assertEquals("[ ] a", markItemUndone("[x] a"))
        assertEquals("[ ] a", markItemUndone("[X] a"))
        assertEquals("a", markItemUndone("a"))
    }

    @Test fun `remove checkbox keeps the other markers`() =
        assertEquals("!! a +2", removeItemCheckbox("[x] !! a +2"))

    @Test fun `clear priority only removes the priority marker`() {
        assertEquals("[ ] a", clearItemPriority("[ ] !! a"))
        assertEquals("Wow! great", clearItemPriority("Wow! great"))
        assertEquals("!important", clearItemPriority("!important"))
    }

    @Test fun `clear due dates leaves creation dates`() {
        assertEquals("a b +1", clearItemDueDates("a @2026-01-01 b +1"))
        assertEquals("a @created:2026-01-01", clearItemDueDates("a @created:2026-01-01"))
    }

    @Test fun `strip comment leaves a url`() {
        assertEquals("a +2", stripItemComment("a +2 // why"))
        assertEquals("see https://x.y", stripItemComment("see https://x.y"))
    }

    @Test fun `votes go up, come down, stop at none and stay before a comment`() {
        assertEquals("a +1", changeItemVotes("a", 1))
        assertEquals("a +6 // why", changeItemVotes("a +5 // why", 1))
        assertEquals("a", changeItemVotes("a +1", -1))
        assertEquals("a", changeItemVotes("a", -1))
    }

    @Test fun `an item the command does not touch keeps its exact text`() =
        assertEquals("!! [ ] a", removeItemVoteless("!! [ ] a"))

    private fun removeItemVoteless(content: String) = stripItemComment(content)

    @Test fun `only item lines change`() =
        assertEquals(
            listOf(">> - [x] a", "note", ">> 1. [x] b"),
            mapSectionItems(listOf(">> - a", "note", ">> 1. b"), "-", ::markItemDone)
        )

    @Test fun `strip metadata finds a checkbox after a priority`() =
        assertEquals("a", stripItemMetadata("!! [ ] a"))
}
