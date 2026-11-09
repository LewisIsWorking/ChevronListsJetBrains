package com.lewisisworking.chevronlists

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class PasteExportPreviewTest {

    private val today = LocalDate.of(2026, 1, 5)

    @Test fun `a list that is all numbered stays numbered and continues the section`() {
        val lines = listOf("> A", ">> 2. old")
        assertEquals(listOf(">> 3. x", ">> 4. y"), smartPasted("1. x\n2) y", lines, 1, "-"))
    }

    @Test fun `anything else becomes bullets with its markers gone`() {
        assertEquals(listOf(">> - x", ">> - y", ">> - z"), smartPasted("- x\n\n* y\nz\n", listOf("> A"), 0, "-"))
    }

    @Test fun `a mixed list is not numbered`() {
        assertEquals(listOf(">> - x", ">> - y"), smartPasted("1. x\ny", listOf("> A"), 0, "-"))
    }

    @Test fun `windows line endings make no extra items`() {
        assertEquals(listOf(">> 1. x", ">> 2. y"), smartPasted("1. x\r\n2. y\r\n", listOf("> A"), 0, "-"))
    }

    @Test fun `an empty clipboard pastes nothing`() {
        assertEquals(emptyList<String>(), smartPasted(" \n", listOf("> A"), 0, "-"))
    }

    @Test fun `csv has an index, a depth and quoted plain words`() {
        assertEquals(
            "index,depth,content\n1,0,\"say \"\"hi\"\"\"\n2,1,\"b\"",
            sectionItemsCsv(listOf("> A", ">> - say \"hi\" #t", ">>> 1. b", "> B", ">> - c"), 1, "-"),
        )
    }

    @Test fun `csv needs a section`() {
        assertNull(sectionItemsCsv(listOf("prose"), 0, "-"))
    }

    @Test fun `every format of a section is available`() {
        val lines = listOf("> A", ">> - a")
        for (format in COPY_FORMATS) assert(sectionCopiedAs(format, lines, 0, "-") != null) { format }
        assertNull(sectionCopiedAs("Nonsense", lines, 0, "-"))
    }

    @Test fun `obsidian export has frontmatter, headings and converted markers`() {
        val lines = listOf("> Plans ==5 [colour:red]", ">> - [x] !!! do #Home @2026-01-02", ">>> 2. see [[file:a.md]] @expires:2026-02-03", "plain", "> Next")
        assertEquals(
            listOf(
                "---", "created: 2026-01-05", "tags:", "  - home", "---", "",
                "## Plans", "- [x] 🔴 do #Home 📅 2026-01-02", "  2. see [[a.md]] ⏰ 2026-02-03", "plain", "", "## Next",
            ).joinToString("\n"),
            fileAsObsidian(lines, "-", today),
        )
    }

    @Test fun `a marker inside the words is left alone in obsidian export`() {
        assertEquals("- array[x] wow!!! great", fileAsObsidian(listOf(">> - array[x] wow!!! great"), "-", today).split("\n").last())
    }

    @Test fun `a file with no tags says so`() {
        assertEquals(listOf("---", "created: 2026-01-05", "tags: []", "---", "", "- a"), fileAsObsidian(listOf(">> - a"), "-", today).split("\n"))
    }

    @Test fun `a preview lists every part of an item`() {
        assertEquals(
            listOf(
                "• [depth 0] buy #Home @2026-01-02 ~2h @daily", "✅ Done", "⭐ Starred", "🔴 !!! Critical",
                "📅 2026-01-02 ⚠️ OVERDUE", "⏱ ~2h", "🔁 @daily", "👍 +3", "🏷 #home", "💬 why",
            ),
            itemPreview(">> - [x] !!! * buy #Home @2026-01-02 ~2h @daily +3 // why", "-", today),
        )
    }

    @Test fun `a numbered open item with a colour and a later date`() {
        assertEquals(
            listOf("#3 [depth 1] a @2026-02-01", "⬜ Todo", "❓ Flagged", "🟡 !! Medium", "🎨 red", "📅 2026-02-01"),
            itemPreview(">>> 3. [ ] !! ? {red} a @2026-02-01", "-", today),
        )
    }

    @Test fun `a line that is not an item has no preview`() {
        assertNull(itemPreview("> Header", "-", today))
    }
}
