package com.lewisisworking.chevronlists

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FileExportsTest {

    private val doc = listOf(
        ">> - before any section",
        "> Work",
        ">> - [x] !! ship it @2026-05-01 #Release",
        ">>> 1. nested, with a comma",
        "",
        "> Home",
        ">> - plain",
    )

    @Test fun `json separates each item's fields and skips items above the first section`() {
        val json = fileAsJson(doc, "-")
        assertFalse(json.contains("before any section"))
        assertTrue(json.contains("\"name\": \"Work\""))
        assertTrue(json.contains("\"content\": \"ship it #Release\""))
        assertTrue(json.contains("\"priority\": 2"))
        assertTrue(json.contains("\"dueDate\": \"2026-05-01\""))
        assertTrue(json.contains("\"done\": true"))
        assertTrue(json.contains("\"release\""))
        assertTrue(json.contains("\"numbered\": true"))
    }

    @Test fun `an item with no checkbox has no done value in json`() {
        assertTrue(fileAsJson(listOf("> A", ">> - x"), "-").contains("\"done\": null"))
    }

    @Test fun `csv has one row per item and quotes a comma`() {
        val rows = fileAsCsv(doc, "-").split("\n")
        assertEquals("Section,Depth,Numbered,Number,Content,Tags,Priority,DueDate,Done", rows[0])
        assertEquals("Work,0,false,,ship it #Release,release,2,2026-05-01,true", rows[1])
        assertEquals("Work,1,true,1,\"nested, with a comma\",,,,", rows[2])
        assertEquals("Home,0,false,,plain,,,,", rows[3])
        assertEquals(4, rows.size)
    }

    @Test fun `html gives each section a block and marks done items`() {
        val html = fileAsHtml(doc, "-", "notes")
        assertTrue(html.contains("<title>notes</title>"))
        assertTrue(html.contains("""<details id="section-work" open>"""))
        assertTrue(html.contains("""<li class="cl-item cl-done">"""))
        assertTrue(html.contains("""<span class="cl-num">1.</span>"""))
        assertTrue(html.contains("""style="margin-left:1.25rem""""))
        assertFalse(html.contains("before any section"))
    }

    @Test fun `html escapes markup and makes badges and links`() {
        val html = fileAsHtml(listOf("> A & B", ">> - <b> see [[Other Part]] #Tag x#y"), "-", "a<b")
        assertTrue(html.contains("<title>a&lt;b</title>"))
        assertTrue(html.contains("A &amp; B"))
        assertTrue(html.contains("&lt;b&gt;"))
        assertTrue(html.contains("""<a class="cl-link" href="#section-other-part">Other Part</a>"""))
        assertTrue(html.contains("""<span class="cl-tag">#Tag</span>"""))
        assertTrue(html.contains("x#y"))
    }

    @Test fun `html keeps dollar text and template words as they are`() {
        val html = fileAsHtml(listOf("> A", ">> - costs $1 and {{BODY}}"), "-", "{{BODY}}")
        assertTrue(html.contains("costs $1 and {{BODY}}"))
        assertTrue(html.contains("<title>{{BODY}}</title>"))
    }

    @Test fun `markdown document uses headings and nested lists`() {
        assertEquals(
            "- before any section\n\n## Work\n\n- [x] !! ship it @2026-05-01 #Release\n  1. nested, with a comma\n\n## Home\n\n- plain\n",
            fileAsMarkdown(doc, "-"),
        )
    }

    @Test fun `all sections json keeps the raw item and every field under the file name`() {
        val json = allSectionsAsJson(listOf("> A", ">> - * {red} task ~1h30m ★4 +2 // why"), "-", "notes")
        assertTrue(json.contains("\"file\": \"notes\""))
        assertTrue(json.contains("\"section\": \"A\""))
        assertTrue(json.contains("\"starred\": true"))
        assertTrue(json.contains("\"colour\": \"red\""))
        assertTrue(json.contains("\"estimate\": \"1h30m\""))
        assertTrue(json.contains("\"votes\": 2"))
        assertTrue(json.contains("\"rating\": 4"))
        assertTrue(json.contains("\"comment\": \"why\""))
        assertTrue(json.contains("\"type\": \"bullet\""))
    }

    @Test fun `items under a subheading still belong to the section`() {
        val rows = fileAsCsv(listOf("> A", ">> - one", ">> # Sub", ">> - two"), "-").split("\n")
        assertEquals(listOf("A", "A"), rows.drop(1).map { it.substringBefore(',') })
    }
}
