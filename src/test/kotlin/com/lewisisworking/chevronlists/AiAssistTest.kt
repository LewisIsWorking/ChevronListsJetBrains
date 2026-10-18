/**
 * AiAssistTest.kt
 * Plain JUnit 4 tests for the pure AI assist logic in AiAssist.kt.
 */
package com.lewisisworking.chevronlists

import com.google.gson.JsonParser
import org.junit.Test
import org.junit.Assert.*
import java.io.File

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

    @Test fun `request body asks Opus 5_5 at low effort with server fallback`() {
        val body = JsonParser.parseString(aiRequestBody("sys \"quoted\"", "line 1\nline 2")).asJsonObject
        assertEquals("claude-opus-5-5", body.get("model").asString)
        assertEquals(16000, body.get("max_tokens").asInt)
        assertEquals("sys \"quoted\"", body.get("system").asString)
        val message = body.getAsJsonArray("messages")[0].asJsonObject
        assertEquals("user", message.get("role").asString)
        assertEquals("line 1\nline 2", message.get("content").asString)
        assertEquals("low", body.getAsJsonObject("output_config").get("effort").asString)
        assertEquals("default", body.get("fallbacks").asString)
    }

    @Test fun `reply text joins text blocks and skips thinking`() {
        val response = """{"content":[{"type":"thinking","thinking":"hmm"},{"type":"text","text":"- eggs\n"},""" +
            """{"type":"text","text":"- jam"}],"stop_reason":"end_turn"}"""
        assertEquals("- eggs\n- jam", aiReplyText(response))
    }

    @Test fun `a refusal gives no reply text`() =
        assertNull(aiReplyText("""{"content":[],"stop_reason":"refusal"}"""))

    @Test fun `the plugin does not bundle an Anthropic SDK`() {
        // 0.18.0 first shipped with the Java SDK, which grew the plugin zip from 0.4 MB to 44 MB
        val gradle = File("build.gradle.kts").readText()
        assertFalse(gradle.contains("com.anthropic"))
    }

    @Test fun `rewrite keeps leading markers and moves metadata after the new words`() {
        val t = rewriteTarget(">>> - [ ] !! ⭐ fix the #urgent bug in login @2026-11-01 ~2h +3 // ask Sam", "-")!!
        assertEquals(RewriteTarget(">>> -", "[ ] !! ⭐", "fix the bug in login", "#urgent @2026-11-01 ~2h +3 // ask Sam"), t)
        assertEquals("Item: fix the bug in login", rewriteMessage(t))
        assertEquals(">>> - [ ] !! ⭐ Fix the login bug #urgent @2026-11-01 ~2h +3 // ask Sam",
            rewrittenLine(t, "\n\"Fix the login bug\"\n"))
    }

    @Test fun `rewrite of a plain numbered item`() {
        val t = rewriteTarget(">> 2. call the bank about it", "-")!!
        assertEquals(">> 2. Call the bank", rewrittenLine(t, "Call the bank"))
    }

    @Test fun `rewrite keeps prose that only looks like a marker`() {
        // Mid-sentence "!!", "?" and C++11 are words, not markers
        assertEquals("Wow!! is C++11 ok?", rewriteTarget(">> - Wow!! is C++11 ok?", "-")!!.plain)
    }

    @Test fun `nothing to rewrite`() {
        assertNull(rewriteTarget("> Header", "-"))
        assertNull(rewriteTarget(">> - [x] #done @2026-01-01", "-"))
        assertNull(rewrittenLine(rewriteTarget(">> - a", "-")!!, "  \n"))
    }

    @Test fun `item lines use the depth and prefix`() =
        assertEquals(listOf(">>> * a", ">>> * b"), itemLines(listOf("a", "b"), ">>>", "*"))
}
