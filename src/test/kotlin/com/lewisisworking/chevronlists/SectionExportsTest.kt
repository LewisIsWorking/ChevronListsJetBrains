/**
 * SectionExportsTest.kt
 * Plain JUnit 4 tests for copying a section as HTML, JSON and a CSV row.
 */
package com.lewisisworking.chevronlists

import org.junit.Test
import org.junit.Assert.*

class SectionExportsTest {

    @Test fun `html escapes the section name and the words`() {
        val html = sectionAsHtml(listOf("> A & <B>", ">> - x < y"), 0, "-")!!
        assertTrue(html, html.contains(">A &amp; &lt;B&gt;</h3>"))
        assertTrue(html, html.contains("x &lt; y</li>"))
    }

    @Test fun `html shows checkboxes, priority, numbering and depth`() {
        val html = sectionAsHtml(listOf("> A", ">> - [x] a", ">> 1. !! b", ">>> - c"), 1, "-")!!
        assertTrue(html, html.contains("""<input type="checkbox" checked disabled> a</li>"""))
        assertTrue(html, html.contains("""list-style:decimal;margin:.2em 0">"""))
        assertTrue(html, html.contains("🟠 "))
        assertTrue(html, html.contains("&nbsp;&nbsp;&nbsp;&nbsp;c</li>"))
    }

    @Test fun `html stops at the next section`() =
        assertFalse(sectionAsHtml(listOf("> A", ">> - a", "> B", ">> - zzz"), 0, "-")!!.contains("zzz"))

    @Test fun `csv row quotes what needs it and strips metadata`() =
        assertEquals("a,\"b, c\",\"say \"\"hi\"\"\"",
            sectionAsCsvRow(listOf("> A", ">> - [x] a +2", ">> - b, c #t", ">> - say \"hi\""), 0, "-"))

    @Test fun `csv needs items`() =
        assertNull(sectionAsCsvRow(listOf("> A", "note"), 0, "-"))

    @Test fun `outside a section there is nothing`() {
        assertNull(sectionAsHtml(listOf(">> - a"), 0, "-"))
        assertNull(sectionAsJson(listOf(">> - a"), 0, "-"))
        assertNull(sectionAsCsvRow(listOf(">> - a"), 0, "-"))
    }

    @Test fun `json has every field`() = assertEquals(
        """
        {
          "section": "A",
          "items": [
            {
              "depth": 1,
              "type": "numbered",
              "num": 2,
              "content": "[x] !! * {red} a @2026-01-02 ~1h30m ★4 #T #t \"q\" +3 // why",
              "done": true,
              "priority": 2,
              "dueDate": "2026-01-02",
              "estimate": "1h30m",
              "votes": 3,
              "starred": true,
              "colour": "red",
              "flagged": false,
              "comment": "why",
              "rating": 4,
              "tags": [
                "t"
              ]
            }
          ]
        }
        """.trimIndent(),
        sectionAsJson(listOf("> A", ">>> 2. [x] !! * {red} a @2026-01-02 ~1h30m ★4 #T #t \"q\" +3 // why"), 0, "-")
    )

    @Test fun `json with plain and no items`() {
        assertEquals("{\n  \"section\": \"A\",\n  \"items\": []\n}", sectionAsJson(listOf("> A"), 0, "-"))
        val json = sectionAsJson(listOf("> A", ">> - a"), 0, "-")!!
        assertTrue(json, json.contains("\"type\": \"bullet\""))
        assertTrue(json, json.contains("\"num\": null"))
        assertTrue(json, json.contains("\"tags\": []"))
    }

    @Test fun `struck text does not hide an estimate`() {
        val json = sectionAsJson(listOf("> A", ">> - ~~old~~ ~2h"), 0, "-")!!
        assertTrue(json, json.contains("\"estimate\": \"2h\""))
    }
}
