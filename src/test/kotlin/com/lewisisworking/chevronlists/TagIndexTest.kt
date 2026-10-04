/**
 * TagIndexTest.kt
 * Plain JUnit 4 tests for tagHits, tagCounts and TagHit.label.
 */
package com.lewisisworking.chevronlists

import org.junit.Test
import org.junit.Assert.*

class TagIndexTest {

    private val doc = listOf(
        "> Work", ">> 1. email Sam #urgent", ">> 2. ship #release #URGENT",
        "> Home", ">> - bins #chores", "a note #urgent", ">>> - deep #chores #chores",
    )

    @Test fun `items are indexed with their section`() =
        assertEquals(
            listOf(
                TagHit("urgent", 1, "email Sam #urgent", "Work"),
                TagHit("release", 2, "ship #release #URGENT", "Work"),
                TagHit("urgent", 2, "ship #release #URGENT", "Work"),
                TagHit("chores", 4, "bins #chores", "Home"),
                TagHit("chores", 6, "deep #chores #chores", "Home"),
            ),
            tagHits(doc, "-"))

    @Test fun `counts are per item, case-insensitive, alphabetical`() =
        assertEquals(listOf("chores" to 2, "release" to 1, "urgent" to 2), tagCounts(tagHits(doc, "-")))

    @Test fun `notes and headers are not indexed`() =
        assertEquals(emptyList<TagHit>(), tagHits(listOf("> Plans #big", "just text #idea"), "-"))

    @Test fun `this plugin's tag rule applies, a letter first`() =
        assertEquals(listOf("v2"), tagHits(listOf(">> - fix #123 and #v2"), "-").map { it.tag })

    @Test fun `a fragment inside a word is not a tag`() =
        assertEquals(emptyList<TagHit>(), tagHits(listOf(">> - see page#setup"), "-"))

    @Test fun `items before any header have no section`() {
        val hit = tagHits(listOf(">> - loose #misc"), "-").single()
        assertEquals("", hit.section)
        assertEquals("loose #misc", hit.label)
    }

    @Test fun `the label names the section`() =
        assertEquals("bins #chores  (Home)", TagHit("chores", 4, "bins #chores", "Home").label)
}
