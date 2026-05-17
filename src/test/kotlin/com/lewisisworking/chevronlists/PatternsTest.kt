/**
 * PatternsTest.kt
 * Plain JUnit 4 tests for the pure parsing functions in Patterns.kt.
 * No IntelliJ Platform fixtures - runs fast and isolated.
 */
package com.lewisisworking.chevronlists

import org.junit.Test
import org.junit.Assert.*

class PatternsTest {
    // isHeader
    @Test fun `isHeader matches single chevron header`() = assertTrue(isHeader("> Header"))
    @Test fun `isHeader does not match double chevron`() = assertFalse(isHeader(">> Item"))
    @Test fun `isHeader does not match plain text`() = assertFalse(isHeader("Header"))
    @Test fun `isHeader does not match empty string`() = assertFalse(isHeader(""))

    // parseHeader
    @Test fun `parseHeader extracts content`() = assertEquals("My Section", parseHeader("> My Section")?.content)
    @Test fun `parseHeader returns null for non-header`() = assertNull(parseHeader(">> Item"))

    // parseBullet
    @Test fun `parseBullet matches a dash bullet at depth 2`() {
        val b = parseBullet(">> - Task", "-")
        assertEquals(">>", b?.chevrons); assertEquals("Task", b?.content)
    }
    @Test fun `parseBullet returns null when prefix does not match`() = assertNull(parseBullet(">> * Task", "-"))
    @Test fun `parseBullet matches custom prefix`() = assertEquals("Task", parseBullet(">> * Task", "*")?.content)
    @Test fun `parseBullet works at depth 3`() = assertEquals(">>>", parseBullet(">>> - Nested", "-")?.chevrons)

    // parseNumbered
    @Test fun `parseNumbered extracts number and content`() {
        val n = parseNumbered(">> 5. Task")
        assertEquals(5, n?.num); assertEquals("Task", n?.content)
    }
    @Test fun `parseNumbered returns null for bullet`() = assertNull(parseNumbered(">> - Task"))
    @Test fun `parseNumbered handles multi-digit numbers`() = assertEquals(42, parseNumbered(">> 42. Item")?.num)
    @Test fun `parseNumbered works at depth 3`() = assertEquals(">>>", parseNumbered(">>> 1. Nested")?.chevrons)

    // parseSubheading
    @Test fun `parseSubheading matches level 2`() {
        val s = parseSubheading("## Session 277")
        assertEquals(2, s?.level); assertEquals("Session 277", s?.content)
    }
    @Test fun `parseSubheading matches level 4`() = assertEquals(4, parseSubheading("#### Deep")?.level)
    @Test fun `parseSubheading does not match chevron header`() = assertNull(parseSubheading("> Section"))
    @Test fun `parseSubheading trims content`() = assertEquals("Title", parseSubheading("##   Title   ")?.content)
    @Test fun `parseSubheading returns null for plain text`() = assertNull(parseSubheading("Just text"))
    @Test fun `parseSubheading returns null for 7 hashes`() = assertNull(parseSubheading("####### Too deep"))
}