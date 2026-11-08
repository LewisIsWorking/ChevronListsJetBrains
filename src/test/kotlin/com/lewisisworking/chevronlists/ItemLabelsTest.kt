/**
 * ItemLabelsTest.kt
 * Plain JUnit 4 tests for ratings, priorities, URLs and cloning into a section.
 */
package com.lewisisworking.chevronlists

import org.junit.Test
import org.junit.Assert.*

class ItemLabelsTest {

    @Test fun `a rating goes among the words after the markers`() {
        assertEquals("[x] ! ★3 a // n", setItemRating("[x] ! a // n", 3))
    }

    @Test fun `a new rating replaces the old one`() = assertEquals("★5 a", setItemRating("★2 a", 5))

    @Test fun `rating zero removes the rating`() = assertEquals("a b +1", setItemRating("a ★2 b +1", 0))

    @Test fun `a star in a comment is left alone`() {
        assertEquals("★1 a // was ★2", setItemRating("a // was ★2", 1))
    }

    @Test fun `a priority goes after the checkbox and replaces the old one`() {
        assertEquals("[x] !!! a", setItemPriority("[x] ! a", 3))
        assertEquals("!! a", setItemPriority("a", 2))
    }

    @Test fun `urls are listed once without the punctuation around them`() {
        assertEquals(
            listOf("https://a.example/x", "http://b.example"),
            sectionUrls(listOf("> A", ">> - see https://a.example/x, and (http://b.example).", ">> 1. https://a.example/x", "https://not.an.item", "> B", ">> - https://c.example"), 1, "-"),
        )
    }

    @Test fun `urls are empty with none and null outside a section`() {
        assertEquals(emptyList<String>(), sectionUrls(listOf("> A", ">> - a"), 1, "-"))
        assertNull(sectionUrls(listOf("text"), 0, "-"))
    }

    @Test fun `a section is found by name in any case`() {
        assertEquals(2, findSectionNamed(listOf("text", "> A", "> Long name"), " long NAME "))
        assertNull(findSectionNamed(listOf("> A"), "B"))
    }

    @Test fun `a bullet is cloned to the end of the chosen section`() {
        assertEquals(
            DuplicateEdit(4, listOf(">> - a"), emptyMap()),
            cloneItemToSection(listOf("> A", ">> - a", "> B", ">> - b", ">> - c", "", "> C"), 1, "-", 2),
        )
    }

    @Test fun `a numbered clone continues the list in the chosen section`() {
        assertEquals(
            DuplicateEdit(3, listOf(">> 8. a"), emptyMap()),
            cloneItemToSection(listOf("> A", ">> 2. a", "> B", ">> 7. b"), 1, "-", 2),
        )
    }

    @Test fun `only items can be cloned`() = assertNull(cloneItemToSection(listOf("> A", "text", "> B"), 1, "-", 2))
}
