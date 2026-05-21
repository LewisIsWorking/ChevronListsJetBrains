/**
 * TagCompletionTest.kt
 * Plain JUnit 4 tests for extractAllTags.
 */
package com.lewisisworking.chevronlists

import org.junit.Test
import org.junit.Assert.*

class TagCompletionTest {

    @Test fun `extracts a single tag from one line`() {
        val tags = extractAllTags("Fix the #bug today")
        assertEquals(setOf("#bug"), tags)
    }

    @Test fun `deduplicates the same tag across lines`() {
        val text = "Line one #urgent\nLine two also #urgent\nThird line again #urgent"
        assertEquals(setOf("#urgent"), extractAllTags(text))
    }

    @Test fun `collects multiple distinct tags`() {
        val text = "#urgent #blocked\n#in-progress @2026-04-22\n#done"
        assertEquals(setOf("#urgent", "#blocked", "#in-progress", "#done"), extractAllTags(text))
    }

    @Test fun `returns empty set for a document with no tags`() {
        assertTrue(extractAllTags("Just some text with no tags here.").isEmpty())
    }

    @Test fun `returns empty set for an empty document`() {
        assertTrue(extractAllTags("").isEmpty())
    }

    @Test fun `does not pick up issue numbers like 123`() {
        assertTrue(extractAllTags("Issue #123 is the bug tracker reference").isEmpty())
    }

    @Test fun `tags with hyphens and underscores survive collection`() {
        val tags = extractAllTags("#in-progress #my_tag #foo")
        assertEquals(setOf("#in-progress", "#my_tag", "#foo"), tags)
    }

    @Test fun `tags across chevron items in a real document`() {
        val text = """
            > Today
            >> 1. #urgent Fix the bug
            >> 2. #blocked Waiting on review
            >> 3. #urgent Another urgent thing
            > Tomorrow
            >> - #docs Write documentation
        """.trimIndent()
        assertEquals(setOf("#urgent", "#blocked", "#docs"), extractAllTags(text))
    }
}