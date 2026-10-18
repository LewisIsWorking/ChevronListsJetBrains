/**
 * AiAssist.kt
 * Pure logic for the AI assist actions (Suggest Items, Summarise Section,
 * Expand Item): the prompts, the message sent to Claude, reading the reply into
 * items and building the lines to insert. No IntelliJ Platform imports; JSON
 * goes through Gson, which the IDE bundles. Mirrors aiCommands.ts in the VS Code
 * extension.
 */
package com.lewisisworking.chevronlists

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser

const val AI_SYSTEM_SUGGEST = """You are a helpful writing assistant for a bullet list tool.
Given a section header and existing list items, suggest 3-5 additional relevant items.
Return ONLY the items, one per line, with no numbering, bullets, or explanation.
Keep each item concise (under 10 words)."""

const val AI_SYSTEM_SUMMARISE = """You are a helpful writing assistant.
Given a section header and its list items, write a single concise summary sentence (max 15 words).
Return ONLY the summary sentence, nothing else."""

const val AI_SYSTEM_EXPAND = """You are a helpful writing assistant for a bullet list tool.
Given a section header and a single item, expand it into 3-5 sub-items.
Return ONLY the sub-items, one per line, with no numbering, bullets, or explanation.
Keep each sub-item concise (under 10 words)."""

/** The section at the caret: its title, its items, its header line and its last non-blank line */
data class AiSection(val header: String, val items: List<String>, val headerLine: Int, val contentEnd: Int)

/** The item at the caret, for Expand Item */
data class AiItem(val content: String, val chevrons: String, val section: String)

/** Pure: the section holding [caretLine], or null when the caret is above every header */
fun aiSection(lines: List<String>, caretLine: Int, listPrefix: String): AiSection? {
    if (caretLine !in lines.indices) return null
    val headerLine = (caretLine downTo 0).firstOrNull { isHeader(lines[it]) } ?: return null
    var end = (headerLine + 1 until lines.size).firstOrNull { isHeader(lines[it]) }?.minus(1) ?: (lines.size - 1)
    while (end > headerLine && lines[end].isBlank()) end--
    val items = (headerLine + 1..end).mapNotNull { i ->
        val b = parseBullet(lines[i], listPrefix)
        val n = parseNumbered(lines[i])
        when {
            b != null && b.content.isNotBlank() -> b.content
            n != null && n.content.isNotBlank() -> "${n.num}. ${n.content}"
            else -> null
        }
    }
    return AiSection(parseHeader(lines[headerLine])!!.content, items, headerLine, end)
}

/** Pure: the chevron item on [caretLine], or null when it is not one */
fun aiItem(lines: List<String>, caretLine: Int, listPrefix: String): AiItem? {
    if (caretLine !in lines.indices) return null
    val text = lines[caretLine]
    val b = parseBullet(text, listPrefix)
    val n = parseNumbered(text)
    val content  = (b?.content ?: n?.content)?.trim()
    if (content.isNullOrEmpty()) return null
    val section = aiSection(lines, caretLine, listPrefix)?.header ?: ""
    return AiItem(content, b?.chevrons ?: n!!.chevrons, section)
}

/** Pure: the user message for Suggest Items */
fun suggestMessage(s: AiSection): String =
    "Section: \"${s.header}\"\nExisting items:\n" +
        (s.items.joinToString("\n") { "- $it" }.ifEmpty { "(none yet)" })

/** Pure: the user message for Summarise Section */
fun summariseMessage(s: AiSection): String =
    "Section: \"${s.header}\"\nItems:\n" + s.items.joinToString("\n") { "- $it" }

/** Pure: the user message for Expand Item */
fun expandMessage(item: AiItem): String =
    "Section: \"${item.section}\"\nItem to expand: \"${item.content}\""

// A bullet, star, number or chevron marker Claude sometimes adds despite the prompt
private val REPLY_MARKER = Regex("""^(>+\s*)?([-*•]|\d+[.)])\s+""")

/** Pure: Claude's reply as items, one per non-blank line, with any list marker removed */
fun replyItems(reply: String): List<String> =
    reply.lines().map { REPLY_MARKER.replaceFirst(it.trim(), "").trim() }.filter { it.isNotEmpty() }

/** Pure: the summary line inserted under the header, in italics */
fun summaryLine(reply: String, listPrefix: String): String? {
    val summary = reply.trim().lines().firstOrNull { it.isNotBlank() }?.trim() ?: return null
    return ">> $listPrefix _${summary}_"
}

const val AI_MODEL = "claude-opus-5-5"

/**
 * Pure: the Messages API request body. Low effort keeps short list edits quick
 * and cheap; "fallbacks" lets the server retry on its default fallback model if
 * the model declines (needs the server-side-fallback beta header).
 */
fun aiRequestBody(system: String, user: String): String {
    val body = JsonObject()
    body.addProperty("model", AI_MODEL)
    body.addProperty("max_tokens", 16000)
    body.addProperty("system", system)
    body.add("messages", JsonArray().apply {
        add(JsonObject().apply { addProperty("role", "user"); addProperty("content", user) })
    })
    body.add("output_config", JsonObject().apply { addProperty("effort", "low") })
    body.addProperty("fallbacks", "default")
    return body.toString()
}

/**
 * Pure: the text of a Messages API response, every text block joined (thinking
 * blocks skipped). Null when Claude declined the request.
 */
fun aiReplyText(response: String): String? {
    val json = JsonParser.parseString(response).asJsonObject
    if (json.get("stop_reason")?.takeIf { it.isJsonPrimitive }?.asString == "refusal") return null
    return json.getAsJsonArray("content")
        ?.map { it.asJsonObject }
        ?.filter { it.get("type")?.asString == "text" }
        ?.joinToString("") { it.get("text")?.asString ?: "" }
        ?: ""
}

private val API_ERROR_MESSAGE = Regex(""""message"\s*:\s*"((?:[^"\\]|\\.)*)"""")

/**
 * Pure: a readable message for a failed request, from its HTTP status (null when
 * no response came back) and the raw error text, which for an API error holds the
 * JSON body. It used to show that JSON as it was.
 */
fun aiErrorMessage(status: Int?, raw: String): String {
    val detail = API_ERROR_MESSAGE.find(raw)?.groupValues?.get(1)?.replace("\\\"", "\"") ?: raw
    return when (status) {
        null -> "Could not reach Claude: $raw"
        401  -> "Anthropic rejected the API key. Check it in Settings, Tools, Chevron Lists."
        403  -> "This API key is not allowed to make the request: $detail"
        429  -> "Anthropic is rate limiting this key. Try again in a minute."
        500, 503, 529 -> "Claude is busy or unavailable. Try again shortly."
        else -> "Claude request failed ($status): $detail"
    }
}

/** Pure: [items] as bullet lines at [chevrons] depth */
fun itemLines(items: List<String>, chevrons: String, listPrefix: String): List<String> =
    items.map { "$chevrons $listPrefix $it" }
