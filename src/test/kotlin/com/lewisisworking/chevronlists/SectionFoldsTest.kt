/**
 * SectionFoldsTest.kt
 * Plain JUnit 4 tests for sectionFolds and foldPlaceholder.
 */
package com.lewisisworking.chevronlists

import org.junit.Test
import org.junit.Assert.*

class SectionFoldsTest {

    private fun folds(vararg lines: String) = sectionFolds(lines.toList(), "-")

    @Test fun `each section folds to its last line`() =
        assertEquals(listOf(SectionFold(0, 2, 2), SectionFold(3, 4, 1)),
            folds("> One", ">> - a", ">> - b", "> Two", ">> - c"))

    @Test fun `trailing blank lines stay outside the fold`() =
        assertEquals(listOf(SectionFold(0, 1, 1)), folds("> One", ">> - a", "", "", "> Two"))

    @Test fun `a header with nothing under it does not fold`() =
        assertEquals(emptyList<SectionFold>(), folds("> One", "", "> Two"))

    @Test fun `text before the first header is not folded`() =
        assertEquals(listOf(SectionFold(2, 3, 1)), folds("intro", "", "> One", ">> - a"))

    @Test fun `notes count as content but not as items`() =
        assertEquals(listOf(SectionFold(0, 2, 1)), folds("> One", ">> - a", "a note"))

    @Test fun `nested and numbered items are counted`() =
        assertEquals(listOf(SectionFold(0, 3, 3)), folds("> One", ">> 1. a", ">>> - a1", ">> 2. b"))

    @Test fun `a document with no headers has no folds`() =
        assertEquals(emptyList<SectionFold>(), folds(">> - a", ">> - b"))

    @Test fun `the placeholder counts items`() {
        assertEquals(" (1 item)", foldPlaceholder(SectionFold(0, 1, 1)))
        assertEquals(" (4 items)", foldPlaceholder(SectionFold(0, 5, 4)))
        assertEquals(" ...", foldPlaceholder(SectionFold(0, 1, 0)))
    }
}
