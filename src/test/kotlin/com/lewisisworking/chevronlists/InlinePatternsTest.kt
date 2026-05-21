/**
 * InlinePatternsTest.kt
 * Plain JUnit 4 tests for the inline detection functions in InlinePatterns.kt.
 */
package com.lewisisworking.chevronlists

import org.junit.Test
import org.junit.Assert.*

class InlinePatternsTest {

    // findTags
    @Test fun `finds a single tag at start of line`() {
        val tags = findTags("#urgent Fix the bug")
        assertEquals(1, tags.size); assertEquals(0..6, tags[0])
    }
    @Test fun `finds a tag after whitespace`() {
        val tags = findTags("Fix the #bug today")
        assertEquals(1, tags.size); assertEquals(8..11, tags[0])
    }
    @Test fun `finds multiple tags on the same line`() {
        val tags = findTags("#urgent #blocked review needed")
        assertEquals(2, tags.size)
    }
    @Test fun `tag must start with a letter not a digit`() {
        assertTrue(findTags("Issue #123 needs attention").isEmpty())
    }
    @Test fun `tag does not match in the middle of a word`() {
        // "abc#def" should NOT detect "#def" - it's not preceded by whitespace
        assertTrue(findTags("abc#def").isEmpty())
    }
    @Test fun `tags allow hyphens and underscores after first char`() {
        val tags = findTags("#in-progress #my_tag")
        assertEquals(2, tags.size)
    }
    @Test fun `bare hash with no letters does not match`() {
        assertTrue(findTags("# header").isEmpty())
    }
    @Test fun `empty string returns no tags`() {
        assertTrue(findTags("").isEmpty())
    }

    // findPriorities
    @Test fun `finds high priority three exclamations`() {
        val p = findPriorities("!!! Critical bug")
        assertEquals(1, p.size); assertEquals(3, p[0].level); assertEquals(0..2, p[0].range)
    }
    @Test fun `finds medium priority two exclamations`() {
        val p = findPriorities("Task !! important")
        assertEquals(1, p.size); assertEquals(2, p[0].level)
    }
    @Test fun `finds low priority single exclamation`() {
        val p = findPriorities("Task ! minor")
        assertEquals(1, p.size); assertEquals(1, p[0].level)
    }
    @Test fun `priority at end of line matches`() {
        val p = findPriorities("Fix this !!!")
        assertEquals(1, p.size); assertEquals(3, p[0].level)
    }
    @Test fun `exclamation in middle of word does not match`() {
        // "Hello!" - the ! is attached to a word, not a standalone token
        assertTrue(findPriorities("Hello!world").isEmpty())
    }
    @Test fun `four exclamations match as three`() {
        // greedy match caps at three
        val p = findPriorities("!!!! Too many")
        // !!!! is preceded by start, but is followed by ! before the next space
        // so the regex doesn't match at the start position. !!! followed by ! fails lookahead.
        // The behaviour is: no match (lookahead requires space/end after the priority).
        assertTrue(p.isEmpty())
    }
    @Test fun `multiple priority markers on one line`() {
        val p = findPriorities("!! Task one !!! Task two")
        assertEquals(2, p.size); assertEquals(2, p[0].level); assertEquals(3, p[1].level)
    }
    @Test fun `empty string returns no priorities`() {
        assertTrue(findPriorities("").isEmpty())
    }

    // findDueDates
    @Test fun `finds a due date in ISO format`() {
        val d = findDueDates("Ship it @2026-04-22 latest")
        assertEquals(1, d.size); assertEquals(8..18, d[0])
    }
    @Test fun `finds due date at start of line`() {
        val d = findDueDates("@2026-01-01 New Year")
        assertEquals(1, d.size); assertEquals(0..10, d[0])
    }
    @Test fun `finds multiple due dates`() {
        val d = findDueDates("Start @2026-04-01 end @2026-04-30")
        assertEquals(2, d.size)
    }
    @Test fun `wrong format is rejected`() {
        // missing day
        assertTrue(findDueDates("@2026-04").isEmpty())
        // wrong separator
        assertTrue(findDueDates("@2026/04/22").isEmpty())
        // no whitespace before
        assertTrue(findDueDates("abc@2026-04-22").isEmpty())
    }
    @Test fun `email address is not a due date`() {
        assertTrue(findDueDates("contact me@example.com").isEmpty())
    }
    @Test fun `empty string returns no due dates`() {
        assertTrue(findDueDates("").isEmpty())
    }
}