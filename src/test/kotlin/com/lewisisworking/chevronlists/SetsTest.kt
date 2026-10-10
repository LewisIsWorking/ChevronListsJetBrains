/**
 * SetsTest.kt
 * Plain JUnit 4 tests for sets in Sets.kt: finding them, flagging an item in
 * two of a set's lists, and moving an item between lists.
 */
package com.lewisisworking.chevronlists

import org.junit.Test
import org.junit.Assert.*

class SetsTest {

    private val bikes = listOf(
        "# Beer Bikes #set",             // 0
        "",
        "> Paid.",                       // 2
        ">> 1. Lewis.",
        ">> 2. Eimer.",
        "",
        "> Hasn't paid.",                // 6
        ">> 1. Ethan.",
        ">> 2. Luke.",
        ">>> owes for two",
        "",
        "> Not going.",                  // 11
        ">> 1. ethan",
        ">> 2. Jess.",
        "",
        "# Other",                       // 15
        "> Elsewhere",                   // 16
        ">> - Ethan.",
    )

    @Test fun `a set holds the sections up to the next heading of its level`() =
        assertEquals(listOf(ChevronSet(0, listOf(2, 6, 11))), findSets(bikes))

    @Test fun `deeper headings stay inside the set, and the tag must be a whole word`() {
        val doc = listOf("## Trip #set", "> A", "### Day one", "> B", "## Next", "> C", "# Not #settled")
        assertEquals(listOf(ChevronSet(0, listOf(1, 3))), findSets(doc))
    }

    @Test fun `member keys ignore case, checkboxes, spacing and end punctuation`() {
        assertEquals("thomas mcneil", setMemberKey("[x] Thomas  McNeil."))
        assertEquals(setMemberKey("Ethan."), setMemberKey("ethan"))
        assertNotEquals(setMemberKey("Luke"), setMemberKey("Luke Skillen"))
    }

    @Test fun `an item in two lists of a set is flagged on both lines`() {
        val issues = collectSetDuplicates(bikes, "-")
        assertEquals(listOf(7, 12), issues.map { it.line })
        assertEquals("In this set more than once: also in \"Not going.\" (line 13)", issues[0].message)
        assertEquals("In this set more than once: also in \"Hasn't paid.\" (line 8)", issues[1].message)
        assertTrue(issues.all { it.kind == IssueKind.SET_DUPLICATE })
    }

    @Test fun `nested notes and sections outside the set are not members`() {
        val doc = bikes.toMutableList().apply { this[12] = ">> 1. Jo"; this[9] = ">>> - Lewis" }
        assertTrue(collectSetDuplicates(doc, "-").isEmpty())
    }

    @Test fun `collectIssues reports set duplicates`() =
        assertEquals(2, collectIssues(bikes, "-").count { it.kind == IssueKind.SET_DUPLICATE })

    @Test fun `inside a set the targets are its other lists, elsewhere every other section`() {
        assertEquals(listOf(MoveTarget(2, "Paid."), MoveTarget(11, "Not going.")), moveTargetsFor(bikes, 7, "-"))
        assertEquals(listOf(2, 6, 11), moveTargetsFor(bikes, 17, "-").map { it.header })
        assertTrue(moveTargetsFor(bikes, 9, "-").isEmpty())   // a nested note
        assertTrue(moveTargetsFor(bikes, 2, "-").isEmpty())   // a header
    }

    @Test fun `moving down carries nested lines and renumbers both lists`() {
        val r = computeMoveToSection(bikes, 8, 11, "-")!!
        assertEquals(listOf("> Hasn't paid.", ">> 1. Ethan.", "", "> Not going.", ">> 1. ethan", ">> 2. Jess.",
            ">> 3. Luke.", ">>> owes for two", ""), r.subList(6, 15))
        assertEquals(bikes.size, r.size)
    }

    @Test fun `moving up lands after the last item, before the blank line`() {
        val r = computeMoveToSection(bikes, 13, 2, "-")!!
        assertEquals(listOf("> Paid.", ">> 1. Lewis.", ">> 2. Eimer.", ">> 3. Jess.", ""), r.subList(2, 7))
        assertEquals(listOf("> Not going.", ">> 1. ethan", ""), r.subList(12, 15))
    }

    @Test fun `an item takes the list type of its new section`() {
        val r = computeMoveToSection(bikes, 7, 16, "-")!!
        assertEquals(listOf("> Elsewhere", ">> - Ethan.", ">> - Ethan."), r.takeLast(3))
        val back = computeMoveToSection(bikes, 17, 2, "-")!!
        assertEquals(">> 3. Ethan.", back[5])
    }

    @Test fun `an item moves into an empty section right under the header`() {
        val doc = listOf("> A", ">> 1. x", ">> 2. y", "> B", "", "> C")
        assertEquals(listOf("> A", ">> 1. y", "> B", ">> 1. x", "", "> C"), computeMoveToSection(doc, 1, 3, "-"))
    }

    @Test fun `no move to its own section or from a line that is not an item`() {
        assertNull(computeMoveToSection(bikes, 7, 6, "-"))
        assertNull(computeMoveToSection(bikes, 9, 2, "-"))
        assertNull(computeMoveToSection(bikes, 7, 3, "-"))   // line 3 is not a header
    }
}
