/**
 * DailyNotesTest.kt
 * Plain JUnit 4 tests for the daily note logic in DailyNotes.kt.
 */
package com.lewisisworking.chevronlists

import org.junit.Test
import org.junit.Assert.*
import java.nio.file.Paths
import java.time.LocalDate

class DailyNotesTest {

    private val day = LocalDate.of(2026, 10, 8) // a Thursday

    @Test fun `file name is the ISO date`() =
        assertEquals("2026-10-08.md", dailyNoteFileName(day))

    @Test fun `template placeholders are filled, every occurrence`() =
        assertEquals("> Thursday 8 (2026-10-08)\n>> - 2026-10-08",
            fillDateTemplate("> {{weekday}} {{day}} ({{date}})\n>> - {{date}}", day))

    @Test fun `a blank template gives a dated header and one empty item`() =
        assertEquals("> 2026-10-08\n>> * \n", newDailyNote("  ", "*", day))

    @Test fun `a template is filled and ends with a newline`() =
        assertEquals("# Thursday\n", newDailyNote("# {{weekday}}", "-", day))

    @Test fun `blank folder setting means the project folder`() =
        assertEquals(Paths.get("/work"), dailyNotesFolder(" ", "/work"))

    @Test fun `relative folder sits under the project folder`() =
        assertEquals(Paths.get("/work", "notes/daily"), dailyNotesFolder("notes/daily", "/work"))

    @Test fun `absolute folder is used as it is, project or not`() {
        val abs = Paths.get("").toAbsolutePath().resolve("daily").toString()
        assertEquals(Paths.get(abs), dailyNotesFolder(abs, "/work"))
        assertEquals(Paths.get(abs), dailyNotesFolder(abs, null))
    }

    @Test fun `relative or blank folder with no project has nowhere to go`() {
        assertNull(dailyNotesFolder("daily", null))
        assertNull(dailyNotesFolder("", null))
    }

    @Test fun `caret goes to the first empty item`() =
        assertEquals(2, firstBlankItemLine(listOf("> Day", ">> - done", ">> - ", ">> - "), "-"))

    @Test fun `an empty item with another marker is found too`() =
        assertEquals(1, firstBlankItemLine(listOf("> Day", ">> * "), "-"))

    @Test fun `no empty item means no caret line`() =
        assertNull(firstBlankItemLine(listOf("> Day", ">> - a"), "-"))

    @Test fun `item content comes from bullets and numbered items`() {
        assertEquals("buy milk", itemContent(">> - buy milk", "-"))
        assertEquals("call Sam", itemContent(">>> 3. call Sam", "-"))
    }

    @Test fun `headers, plain text and empty items have no content`() {
        assertNull(itemContent("> Groceries", "-"))
        assertNull(itemContent("just text", "-"))
        assertNull(itemContent(">> - ", "-"))
    }

    @Test fun `a missing note is created with a title and an inbox`() =
        assertEquals("# 2026-10-08\n\n> Inbox\n>> - a\n", sendToInbox(null, "a", "-", day))

    @Test fun `the item goes first under an existing inbox, matched case-insensitively`() =
        assertEquals("> Work\n>> - w\n> inbox \n>> - new\n>> - old\n",
            sendToInbox("> Work\n>> - w\n> inbox \n>> - old\n", "new", "-", day))

    @Test fun `a note with no inbox gets one at the end after a blank line`() =
        assertEquals("> Work\n>> - w\n\n> Inbox\n>> - new\n",
            sendToInbox("> Work\n>> - w\n", "new", "-", day))

    @Test fun `a note with no trailing newline still gets a blank line before the inbox`() =
        assertEquals("> Work\n\n> Inbox\n>> - new\n", sendToInbox("> Work", "new", "-", day))

    @Test fun `an empty note gets just the inbox`() =
        assertEquals("> Inbox\n>> - new\n", sendToInbox("", "new", "-", day))
}
