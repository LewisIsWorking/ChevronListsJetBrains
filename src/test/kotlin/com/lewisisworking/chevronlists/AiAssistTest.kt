/**
 * AiAssistTest.kt
 * Plain JUnit 4 tests for the pure AI assist logic in AiAssist.kt.
 */
package com.lewisisworking.chevronlists

import org.junit.Test
import org.junit.Assert.*

class AiAssistTest {

    private val doc = listOf(
        "intro",               // 0
        "> Shopping",          // 1
        ">> - milk",           // 2
        ">> 2. bread",         // 3
        ">>> - wholemeal",     // 4
        ">> - ",               // 5: empty
        "",                    // 6
        "",                    // 7
        "> Empty",             // 8
        "",                    // 9
    )

    @Test fun `section collects items, numbers kept, empty ones skipped`() =
        assertEquals(AiSection("Shopping", listOf("milk", "2. bread", "wholemeal"), 1, 5), aiSection(doc, 3, "-"))

    @Test fun `section content end skips trailing blank lines`() =
        assertEquals(8, aiSection(doc, 9, "-")!!.contentEnd)

    @Test fun `no section above the first header`() {
        assertNull(aiSection(doc, 0, "-"))
        assertNull(aiSection(doc, 99, "-"))
    }

    @Test fun `suggest message lists items, or says none yet`() {
        assertEquals("Section: \"Shopping\"\nExisting items:\n- milk\n- 2. bread\n- wholemeal",
            suggestMessage(aiSection(doc, 2, "-")!!))
        assertEquals("Section: \"Empty\"\nExisting items:\n(none yet)", suggestMessage(aiSection(doc, 8, "-")!!))
    }

    @Test fun `summarise message lists items`() =
        assertEquals("Section: \"Shopping\"\nItems:\n- milk\n- 2. bread\n- wholemeal", summariseMessage(aiSection(doc, 1, "-")!!))

    @Test fun `item at the caret, bullet or numbered`() {
        assertEquals(AiItem("wholemeal", ">>>", "Shopping"), aiItem(doc, 4, "-"))
        assertEquals(AiItem("bread", ">>", "Shopping"), aiItem(doc, 3, "-"))
        assertEquals("Section: \"Shopping\"\nItem to expand: \"bread\"", expandMessage(aiItem(doc, 3, "-")!!))
    }

    @Test fun `no item on a header, an empty item or out of range`() {
        assertNull(aiItem(doc, 1, "-"))
        assertNull(aiItem(doc, 5, "-"))
        assertNull(aiItem(doc, -1, "-"))
    }

    @Test fun `item above every header has no section`() =
        assertEquals(AiItem("loose", ">>", ""), aiItem(listOf(">> - loose"), 0, "-"))

    @Test fun `reply lines lose list markers and blanks`() =
        assertEquals(listOf("eggs", "butter", "jam", "tea", "cheese", "Plain line"),
            replyItems("- eggs\n\n* butter\n1. jam\n2) tea\n>> - cheese\n  Plain line  \n"))

    @Test fun `summary line takes the first non-blank line, in italics`() {
        assertEquals(">> - _Weekly food shop._", summaryLine("\n Weekly food shop. \nextra", "-"))
        assertNull(summaryLine("  \n", "-"))
    }

    @Test fun `a rejected key gets a plain message, not the JSON body`() {
        // Live test: a bad key showed {"type":"error","error":{...}} in the dialog
        val raw = """401: {"type":"error","error":{"type":"authentication_error","message":"invalid x-api-key"},"request_id":"req_1"}"""
        assertEquals("Anthropic rejected the API key. Check it in Settings, Tools, Chevron Lists.", aiErrorMessage(401, raw))
    }

    @Test fun `other errors show the API's own message`() {
        val raw = """400: {"type":"error","error":{"type":"invalid_request_error","message":"Your credit balance is too low"}}"""
        assertEquals("Claude request failed (400): Your credit balance is too low", aiErrorMessage(400, raw))
        assertEquals("This API key is not allowed to make the request: no \"access\"",
            aiErrorMessage(403, """{"message":"no \"access\""}"""))
        assertEquals("Claude request failed (418): teapot", aiErrorMessage(418, "teapot"))
    }

    @Test fun `busy, rate limited and unreachable`() {
        assertEquals("Anthropic is rate limiting this key. Try again in a minute.", aiErrorMessage(429, ""))
        assertEquals("Claude is busy or unavailable. Try again shortly.", aiErrorMessage(529, ""))
        assertEquals("Could not reach Claude: timeout", aiErrorMessage(null, "timeout"))
    }

    @Test fun `item lines use the depth and prefix`() =
        assertEquals(listOf(">>> * a", ">>> * b"), itemLines(listOf("a", "b"), ">>>", "*"))
}
