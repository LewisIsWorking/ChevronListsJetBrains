/**
 * ListCommandsTest.kt
 * Plain JUnit 4 tests for renumberLines, bulletsToNumbered, numberedToBullets
 * and the section plumbing they share.
 */
package com.lewisisworking.chevronlists

import org.junit.Test
import org.junit.Assert.*

class ListCommandsTest {

    // renumberLines
    @Test fun `renumber restarts a broken list at 1`() =
        assertEquals(listOf(">> 1. a", ">> 2. b", ">> 3. c"), renumberLines(listOf(">> 4. a", ">> 9. b", ">> 2. c")))

    @Test fun `renumber counts the children of each parent separately`() =
        assertEquals(listOf(">> 1. a", ">>> 1. a1", ">>> 2. a2", ">> 2. b", ">>> 1. b1"),
            renumberLines(listOf(">> 3. a", ">>> 5. a1", ">>> 5. a2", ">> 7. b", ">>> 2. b1")))

    @Test fun `renumber leaves bullets and text alone`() =
        assertEquals(listOf(">> - x", "note", ">> 1. a"), renumberLines(listOf(">> - x", "note", ">> 3. a")))

    // bulletsToNumbered
    @Test fun `plain bullets become 1, 2, 3`() =
        assertEquals(listOf(">> 1. a", ">> 2. b", ">> 3. c"), bulletsToNumbered(listOf(">> - a", ">> - b", ">> - c"), "-"))

    @Test fun `bullets continue from the highest number in their list`() =
        assertEquals(listOf(">> 1. a", ">> 2. b", ">> 3. c"), bulletsToNumbered(listOf(">> 1. a", ">> 2. b", ">> - c"), "-"))

    @Test fun `nested bullets start their own list`() =
        assertEquals(listOf(">> 1. a", ">>> 1. a1", ">> 2. b", ">>> 1. b1"),
            bulletsToNumbered(listOf(">> - a", ">>> - a1", ">> - b", ">>> - b1"), "-"))

    @Test fun `only bullets with the configured prefix convert`() =
        assertEquals(listOf(">> * a", ">> 1. b"), bulletsToNumbered(listOf(">> * a", ">> - b"), "-"))

    // numberedToBullets
    @Test fun `numbered items become bullets with the prefix`() =
        assertEquals(listOf(">> * a", ">>> * a1", ">> - x"), numberedToBullets(listOf(">> 1. a", ">>> 1. a1", ">> - x"), "*"))

    // Section plumbing
    private val doc = listOf("> One", ">> - a", ">> - b", "> Two", ">> - c")

    @Test fun `only the caret's section changes`() =
        assertEquals(listOf("> One", ">> - a", ">> - b", "> Two", ">> 1. c"),
            applyToSection(doc, 4) { bulletsToNumbered(it, "-") })

    @Test fun `the caret on a header changes that header's section`() =
        assertEquals(listOf("> One", ">> 1. a", ">> 2. b", "> Two", ">> - c"),
            applyToSection(doc, 0) { bulletsToNumbered(it, "-") })

    @Test fun `nothing to change gives null`() =
        assertNull(applyToSection(doc, 1) { numberedToBullets(it, "-") })

    @Test fun `an empty section gives null`() {
        assertNull(applyToSection(listOf("> One", "> Two"), 0) { bulletsToNumbered(it, "-") })
        assertNull(applyToSection(listOf("> Last"), 0) { bulletsToNumbered(it, "-") })
    }

    @Test fun `a caret outside the document gives null`() {
        assertNull(applyToSection(doc, 42) { bulletsToNumbered(it, "-") })
        assertNull(applyToSection(doc, -1) { bulletsToNumbered(it, "-") })
    }

    @Test fun `sectionBody without a header is the whole document`() =
        assertEquals(0..1, sectionBody(listOf(">> - a", ">> - b"), 1))
}
