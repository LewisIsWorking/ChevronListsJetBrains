/**
 * ArchiveDoneTest.kt
 * Plain JUnit 4 tests for computeArchiveDone and minimalReplacement.
 */
package com.lewisisworking.chevronlists

import org.junit.Test
import org.junit.Assert.*

class ArchiveDoneTest {

    private fun archive(lines: List<String>, caret: Int = 1) = computeArchiveDone(lines, caret, "-")

    @Test fun `done items move to a new archive at the end`() {
        val r = archive(listOf("> Tasks", ">> - [x] a", ">> - [ ] b", ">> - [x] c"))!!
        assertEquals(listOf("> Tasks", ">> - [ ] b", "", "> Archive", ">> - [x] a", ">> - [x] c"), r.lines)
        assertEquals(2, r.archived)
    }

    @Test fun `a done item takes its children with it`() =
        assertEquals(listOf("> Tasks", ">> - [ ] b", "", "> Archive", ">> - [x] a", ">>> - a1"),
            archive(listOf("> Tasks", ">> - [x] a", ">>> - a1", ">> - [ ] b"))!!.lines)

    @Test fun `a done child moves on its own when its parent is not done`() =
        assertEquals(listOf("> Tasks", ">> - [ ] p", ">>> - [ ] keep", "", "> Archive", ">>> - [x] gone"),
            archive(listOf("> Tasks", ">> - [ ] p", ">>> - [x] gone", ">>> - [ ] keep"))!!.lines)

    @Test fun `a done child inside a done parent is counted once`() =
        assertEquals(1, archive(listOf("> T", ">> - [x] p", ">>> - [x] c"))!!.archived)

    @Test fun `items go to the end of an existing archive, before its trailing blank`() =
        assertEquals(listOf("> Tasks", ">> - [ ] b", "", "> Archive", ">> - [x] old", ">> - [x] a", "", "> Later"),
            archive(listOf("> Tasks", ">> - [x] a", ">> - [ ] b", "", "> Archive", ">> - [x] old", "", "> Later"))!!.lines)

    @Test fun `an archive above the section works too`() =
        assertEquals(listOf("> archive", ">> - [x] old", ">> - [X] a", "", "> Tasks", ">> - [ ] b"),
            archive(listOf("> archive", ">> - [x] old", "", "> Tasks", ">> - [X] a", ">> - [ ] b"), caret = 4)!!.lines)

    @Test fun `a file ending with a newline keeps it`() =
        assertEquals(listOf("> T", "", "> Archive", ">> - [x] a", ""),
            archive(listOf("> T", ">> - [x] a", ""))!!.lines)

    @Test fun `only the caret's section is archived`() =
        assertEquals(listOf("> One", ">> - [x] a", "> Two", "", "> Archive", ">> - [x] b"),
            archive(listOf("> One", ">> - [x] a", "> Two", ">> - [x] b"), caret = 3)!!.lines)

    @Test fun `nothing done means nothing to do`() =
        assertNull(archive(listOf("> T", ">> - [ ] a", ">> - b")))

    @Test fun `the archive itself is never archived`() =
        assertNull(archive(listOf("> Archive", ">> - [x] a"), caret = 1))

    @Test fun `a box that is not at the start does not count`() =
        assertNull(archive(listOf("> T", ">> - see [x] later")))

    // minimalReplacement
    private fun applied(before: String, after: String): String {
        val r = minimalReplacement(before, after) ?: return before
        return before.substring(0, r.start) + r.text + before.substring(r.end)
    }

    @Test fun `the replacement turns before into after`() {
        val cases = listOf(
            "a\nb\nc" to "a\nc", "a\nc" to "a\nb\nc", "" to "x", "x" to "", "abc" to "abc\n\n> Archive\nabc",
            "same\nsame" to "same\nsame\nsame", "a\nb" to "b\na",
        )
        for ((before, after) in cases) assertEquals(after, applied(before, after))
    }

    @Test fun `the replacement is small`() {
        val r = minimalReplacement("line one\nline two\nline three", "line one\nline 2\nline three")!!
        assertEquals("2", r.text)
    }

    @Test fun `no replacement for equal text`() = assertNull(minimalReplacement("a\nb", "a\nb"))
}
