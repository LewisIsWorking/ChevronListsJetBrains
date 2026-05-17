/**
 * EnterContinuationTest.kt
 * Plain JUnit 4 tests for computeEnterAction.
 */
package com.lewisisworking.chevronlists

import org.junit.Test
import org.junit.Assert.*

class EnterContinuationTest {
    private fun action(line: String, prefix: String = "-", defaultType: String = "unordered") =
        computeEnterAction(line, prefix, defaultType)

    // Header line -> start a new list
    @Test fun `header line starts an unordered list by default`() {
        val a = action("> My Section") as EnterAction.Continue
        assertEquals(">> - ", a.insert)
    }
    @Test fun `header line starts an ordered list when configured`() {
        val a = action("> My Section", defaultType = "ordered") as EnterAction.Continue
        assertEquals(">> 1. ", a.insert)
    }
    @Test fun `header line respects custom bullet prefix`() {
        val a = action("> My Section", prefix = "*") as EnterAction.Continue
        assertEquals(">> * ", a.insert)
    }

    // Bullet line -> continue with another bullet
    @Test fun `bullet with content continues with another bullet`() {
        val a = action(">> - First task") as EnterAction.Continue
        assertEquals(">> - ", a.insert)
    }
    @Test fun `bullet at depth 3 continues at depth 3`() {
        val a = action(">>> - Nested task") as EnterAction.Continue
        assertEquals(">>> - ", a.insert)
    }
    @Test fun `bullet at depth 4 continues at depth 4`() {
        val a = action(">>>> - Deep") as EnterAction.Continue
        assertEquals(">>>> - ", a.insert)
    }
    @Test fun `empty bullet ends the list`() {
        assertEquals(EnterAction.EndList, action(">> - "))
    }
    @Test fun `empty bullet with trailing spaces still ends the list`() {
        assertEquals(EnterAction.EndList, action(">> -    "))
    }

    // Numbered line -> continue with the next number
    @Test fun `numbered item with content increments`() {
        val a = action(">> 1. First") as EnterAction.Continue
        assertEquals(">> 2. ", a.insert)
    }
    @Test fun `numbered item handles multi-digit numbers`() {
        val a = action(">> 42. Many") as EnterAction.Continue
        assertEquals(">> 43. ", a.insert)
    }
    @Test fun `nested numbered item continues at correct depth`() {
        val a = action(">>> 5. Nested") as EnterAction.Continue
        assertEquals(">>> 6. ", a.insert)
    }
    @Test fun `empty numbered item ends the list`() {
        assertEquals(EnterAction.EndList, action(">> 1. "))
    }
    @Test fun `empty numbered item with trailing spaces ends the list`() {
        assertEquals(EnterAction.EndList, action(">> 5.   "))
    }

    // Fall-through cases
    @Test fun `plain text returns Default`() {
        assertEquals(EnterAction.Default, action("Just some plain markdown."))
    }
    @Test fun `empty line returns Default`() {
        assertEquals(EnterAction.Default, action(""))
    }
    @Test fun `markdown subheading returns Default`() {
        assertEquals(EnterAction.Default, action("## Subheading"))
    }
    @Test fun `bullet with wrong prefix returns Default`() {
        // listPrefix is "-" but line uses "*"
        assertEquals(EnterAction.Default, action(">> * item"))
    }
    @Test fun `chevron with no space after returns Default`() {
        assertEquals(EnterAction.Default, action(">>nospace"))
    }
}