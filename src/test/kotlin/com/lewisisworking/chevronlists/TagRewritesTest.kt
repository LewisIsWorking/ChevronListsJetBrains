/**
 * TagRewritesTest.kt
 * Plain JUnit 4 tests for renaming tags, changing bullets, grouping by tag and
 * setting a word goal.
 */
package com.lewisisworking.chevronlists

import org.junit.Test
import org.junit.Assert.*

class TagRewritesTest {

    private fun assertEdit(lines: List<String>, changed: Int, edit: ListEdit?) {
        assertEquals(lines, edit!!.lines)
        assertEquals(changed, edit.changed)
    }

    @Test fun `file tags come from items only, once each, sorted`() {
        assertEquals(listOf("a", "b"), itemTags(listOf("# Top #z", "> S", ">> - x #b #A", ">> 1. y #a"), "-"))
    }

    @Test fun `renaming a tag changes items in any case and leaves longer tags`() {
        assertEdit(
            listOf("# Top #x", "see #x", ">> - a #y #x-long", ">> 1. #y b"), 2,
            renameTagInLines(listOf("# Top #x", "see #x", ">> - a #X #x-long", ">> 1. #x b"), "-", "x", "#y"),
        )
    }

    @Test fun `renaming a tag nothing holds changes nothing`() {
        assertEquals(0, renameTagInLines(listOf(">> - a"), "-", "x", "y").changed)
    }

    @Test fun `renaming in a section stays in that section`() {
        assertEdit(
            listOf("> A", ">> - #y", "> B", ">> - #x"), 1,
            renameTagInSection(listOf("> A", ">> - #x", "> B", ">> - #x"), 1, "-", "x", "y"),
        )
    }

    @Test fun `tags can be listed for one section`() {
        assertEquals(listOf("x"), sectionTags(listOf("> A", ">> - #x", "> B", ">> - #z"), 1, "-"))
        assertNull(sectionTags(listOf("text"), 0, "-"))
    }

    @Test fun `a bullet prefix changes and can be changed back`() {
        val starred = changeSectionPrefix(listOf("> A", ">> - a", ">> 1. b", "> B", ">> - c"), 1, "-", "*")!!
        assertEdit(listOf("> A", ">> * a", ">> 1. b", "> B", ">> - c"), 1, starred)
        assertEdit(listOf("> A", ">> - a", ">> 1. b", "> B", ">> - c"), 1, changeSectionPrefix(starred.lines, 1, "-", "-"))
    }

    @Test fun `a prefix already in use changes nothing`() {
        assertEquals(0, changeSectionPrefix(listOf("> A", ">> - a"), 1, "-", "-")!!.changed)
        assertNull(changeSectionPrefix(listOf("text"), 0, "-", "*"))
    }

    @Test fun `grouping clusters items under tag headings, then the untagged`() {
        assertEquals(
            listOf("> S", "// #x", ">> - a #x", ">> - c #X", "// #y", ">> - b #y", ">> - d"),
            groupSectionByTag(listOf("> S", ">> - a #x", ">> - b #y", ">> - c #X", ">> - d"), 1, "-"),
        )
    }

    @Test fun `grouping twice does not pile up headings`() {
        val once = groupSectionByTag(listOf("> S", ">> - a #x", ">> - b #y"), 1, "-")!!
        assertEquals(once, groupSectionByTag(once + ">> - c #x", 1, "-")!!.filterNot { it == ">> - c #x" })
    }

    @Test fun `grouping renumbers`() {
        val grouped = groupSectionByTag(listOf("> S", ">> 1. a #x", ">> 2. b #y", ">> 3. c #x"), 1, "-")!!
        assertEquals(listOf(">> 1. a #x", ">> 2. c #x", ">> 3. b #y"), grouped.filter { it.startsWith(">>") })
    }

    @Test fun `grouping needs two tags`() {
        assertNull(groupSectionByTag(listOf("> S", ">> - a #x", ">> - b"), 1, "-"))
    }

    @Test fun `a word goal is read and replaced and removed`() {
        assertEquals(500, sectionGoal("> A ==500"))
        assertNull(sectionGoal("> A"))
        assertEquals("> A ==9", withSectionGoal("> A ==500", 9))
        assertEquals("> A ==9", withSectionGoal("> A", 9))
        assertEquals("> A", withSectionGoal("> A ==500", null))
    }
}
