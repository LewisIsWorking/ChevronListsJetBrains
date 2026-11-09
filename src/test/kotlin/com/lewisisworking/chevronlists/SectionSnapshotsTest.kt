package com.lewisisworking.chevronlists

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SectionSnapshotsTest {

    @Test fun `a section moves to a new archive at the end`() {
        assertEquals(
            listOf("> B", ">> - b", "", "> Archive", "> A", ">> - a"),
            archiveSection(listOf("> A", ">> - a", "", "> B", ">> - b"), 1),
        )
    }

    @Test fun `a section moves straight after the items of an archive below it`() {
        assertEquals(
            listOf("> Archive", ">> - old", "> A", ">> - a", "", "> Z"),
            archiveSection(listOf("> A", ">> - a", "", "> Archive", ">> - old", "", "> Z"), 0),
        )
    }

    @Test fun `a section moves up into an archive above it`() {
        assertEquals(
            listOf("> Archive", ">> - old", "> B", ">> - b", "", "> Z", ">> - z", ""),
            archiveSection(listOf("> Archive", ">> - old", "", "> Z", ">> - z", "", "> B", ">> - b"), 6),
        )
    }

    @Test fun `the last section keeps the file's final newline`() {
        assertEquals(
            listOf("> A", "", "> Archive", "> B", ">> - b", ""),
            archiveSection(listOf("> A", "", "> B", ">> - b", ""), 3),
        )
    }

    @Test fun `the archive itself cannot be archived`() {
        assertNull(archiveSection(listOf("> Archive", ">> - old"), 1))
    }

    @Test fun `a section sitting directly after the archive is already archived`() {
        assertNull(archiveSection(listOf("> Archive", ">> - old", "", "> Done", ">> - d"), 4))
    }

    @Test fun `outside a section nothing is archived`() {
        assertNull(archiveSection(listOf("prose", "> A"), 0))
    }

    @Test fun `archive entries are indented and point at their lines`() {
        assertEquals(
            listOf(ReportEntry(1, "one"), ReportEntry(2, "  two"), ReportEntry(3, "three")),
            archiveEntries(listOf("> Archive", ">> - one", ">>> - two", ">> 1. three", "> Next", ">> - no"), "-"),
        )
    }

    @Test fun `no archive gives no entries`() {
        assertNull(archiveEntries(listOf("> A", ">> - a"), "-"))
    }

    @Test fun `a snapshot round trips through the section it came from`() {
        val lines = listOf("> A", ">> - a", "", "> B", ">> - b")
        val text  = sectionSnapshotText(lines, 1)!!
        assertEquals("> A\n>> - a\n", text)
        val edited = listOf("> A", ">> - changed", ">> - more", "", "> B", ">> - b")
        assertEquals(lines, restoreSnapshotText(edited, 2, text))
    }

    @Test fun `a snapshot replaces the section at the caret and no other`() {
        assertEquals(
            listOf("> A", ">> - a", "> S", ">> - s"),
            restoreSnapshotText(listOf("> A", ">> - a", "> B", ">> - b"), 3, "> S\n>> - s"),
        )
    }

    @Test fun `outside a section there is no snapshot`() {
        assertNull(sectionSnapshotText(listOf("prose"), 0))
        assertNull(restoreSnapshotText(listOf("prose"), 0, "> S"))
    }

    @Test fun `the listing names each snapshot with its time`() {
        assertEquals("# Snapshots\n\n- One (2026-01-02 03:04)\n", snapshotListing(listOf("One" to "2026-01-02 03:04")))
    }

    @Test fun `the item diff lists removed then added words`() {
        assertEquals(
            "# Item Snapshot Diff\n\n## Before\n\nbuy red milk\n\n## After\n\nbuy blue milk\n\n## Changes\n\n- red\n+ blue",
            itemSnapshotDiff("buy red milk", "buy blue milk"),
        )
    }

    @Test fun `an unchanged item says so`() {
        assertEquals(true, itemSnapshotDiff("same", "same").endsWith("_No changes detected_"))
    }
}
