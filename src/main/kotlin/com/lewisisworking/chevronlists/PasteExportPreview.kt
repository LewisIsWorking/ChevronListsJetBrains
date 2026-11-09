/**
 * PasteExportPreview.kt
 * Pure logic for Smart Paste, Copy Section As, Export to Obsidian and Preview
 * Item. No IntelliJ Platform imports - fully testable with plain JUnit. Ported
 * from the VS Code extension.
 */
package com.lewisisworking.chevronlists

import java.time.LocalDate

private val CLIP_LINE_BREAK = Regex("""\r?\n""")
private val NUMBERED_CLIP   = Regex("""^\d+[.)]\s""")
private val OBSIDIAN_CHECK  = Regex("""^\[(x| ?)]\s*""", RegexOption.IGNORE_CASE)
private val OBSIDIAN_RANK   = Regex("""^(!{1,3}) +""")
private val OBSIDIAN_EXPIRY = Regex("""@expires:(\d{4}-\d{2}-\d{2})""")
private val OBSIDIAN_DUE    = Regex("""@(\d{4}-\d{2}-\d{2})""")
private val OBSIDIAN_FILE   = Regex("""\[\[file:([^]]+)]]""")
private val OBSIDIAN_SIGNS  = listOf("🟡", "🟠", "🔴")
private val PREVIEW_RANKS   = mapOf("!!!" to "🔴 !!! Critical", "!!" to "🟡 !! Medium", "!" to "🔵 ! Low")

/** The formats Copy Section As offers */
val COPY_FORMATS = listOf("Markdown", "Plain Text", "JSON", "CSV", "HTML")

/** Pure: clipboard text as items: numbered if every line is numbered, otherwise bullets */
fun smartPasted(clip: String, lines: List<String>, caretLine: Int, listPrefix: String): List<String> {
    val text = CLIP_LINE_BREAK.replace(clip, "\n")
    val raw  = text.split("\n").map { it.trim() }.filter { it.isNotEmpty() }
    if (raw.isEmpty()) return emptyList()
    return if (raw.all { NUMBERED_CLIP.containsMatchIn(it) }) pastedNumbered(text, lines, caretLine) else pastedBullets(text, listPrefix)
}

/** Pure: the section at [caretLine] as index, depth and plain words; null outside a section */
fun sectionItemsCsv(lines: List<String>, caretLine: Int, listPrefix: String): String? {
    val header = headerAtOrAbove(lines, caretLine) ?: return null
    val rows = (header + 1..sectionLastLine(lines, header)).mapNotNull { i ->
        val chevrons = parseBullet(lines[i], listPrefix)?.chevrons ?: parseNumbered(lines[i])?.chevrons
        val content  = itemContent(lines[i], listPrefix)
        if (chevrons == null || content == null) null else (chevrons.length - 2) to stripItemMetadata(content)
    }
    return (listOf("index,depth,content") + rows.mapIndexed { i, (depth, plain) ->
        "${i + 1},$depth,\"${plain.replace("\"", "\"\"")}\""
    }).joinToString("\n")
}

/** Pure: the section at [caretLine] in [format], one of [COPY_FORMATS]; null when there is nothing to copy */
fun sectionCopiedAs(format: String, lines: List<String>, caretLine: Int, listPrefix: String): String? = when (format) {
    "Markdown"   -> sectionAsMarkdown(lines, caretLine, listPrefix)
    "Plain Text" -> sectionAsPlainText(lines, caretLine, listPrefix)
    "JSON"       -> sectionAsJson(lines, caretLine, listPrefix)
    "CSV"        -> sectionItemsCsv(lines, caretLine, listPrefix)
    "HTML"       -> sectionAsHtml(lines, caretLine, listPrefix)
    else         -> null
}

/** One item as an Obsidian list line. A checkbox and priority count only at the start, in that order. */
private fun obsidianItem(depth: Int, num: Int?, content: String): String {
    var rest  = content
    var marks = ""
    OBSIDIAN_CHECK.find(rest)?.let {
        marks += if (it.groupValues[1].equals("x", ignoreCase = true)) "[x] " else "[ ] "
        rest = rest.substring(it.value.length)
    }
    OBSIDIAN_RANK.find(rest)?.let {
        marks += OBSIDIAN_SIGNS[it.groupValues[1].length - 1] + " "
        rest = rest.substring(it.value.length)
    }
    rest = OBSIDIAN_EXPIRY.replace(rest, "⏰ \$1")
    rest = OBSIDIAN_DUE.replace(rest, "📅 \$1")
    rest = OBSIDIAN_FILE.replace(rest, "[[\$1]]").trim()
    return ("  ".repeat(depth) + (if (num != null) "$num." else "-") + " $marks$rest").trimEnd()
}

/** Pure: the file as Obsidian markdown, frontmatter first with every tag it uses */
fun fileAsObsidian(lines: List<String>, listPrefix: String, today: LocalDate): String {
    val body = mutableListOf<String>()
    val tags = linkedSetOf<String>()
    for ((i, line) in lines.withIndex()) {
        if (isHeader(line)) {
            if (body.isNotEmpty()) body += ""
            body += "## ${plainSectionName(lines, i)}"
            continue
        }
        val numbered = parseNumbered(line)
        val chevrons = parseBullet(line, listPrefix)?.chevrons ?: numbered?.chevrons
        val content  = itemContent(line, listPrefix)
        if (chevrons == null || content == null) {
            if (line.isNotBlank()) body += line
            continue
        }
        body += obsidianItem(chevrons.length - 2, numbered?.num, content)
        tags += tagFields(content)
    }
    val tagLines = if (tags.isEmpty()) listOf("tags: []") else listOf("tags:") + tags.map { "  - $it" }
    return (listOf("---", "created: $today") + tagLines + listOf("---", "") + body).joinToString("\n")
}

/** Pure: a heading line for the item on [line] and one line per marker it has; null if it is not an item */
fun itemPreview(line: String, listPrefix: String, today: LocalDate): List<String>? {
    val bullet   = parseBullet(line, listPrefix)
    val numbered = parseNumbered(line)
    val content  = bullet?.content ?: numbered?.content ?: return null
    val depth    = (bullet?.chevrons ?: numbered!!.chevrons).length - 2
    val parts    = splitItem(content)
    val due      = ITEM_DUE_DATE.find(parts.body)?.groupValues?.get(1)
    val overdue  = due?.let { runCatching { LocalDate.parse(it).isBefore(today) }.getOrDefault(false) } ?: false
    val tags     = tagFields(content)
    return listOfNotNull(
        "${if (numbered != null) "#${numbered.num}" else "•"} [depth $depth] ${parts.body}",
        parts.check.takeIf { it.isNotEmpty() }?.let { if (it.contains('x', ignoreCase = true)) "✅ Done" else "⬜ Todo" },
        "⭐ Starred".takeIf { parts.marker == "*" },
        "❓ Flagged".takeIf { parts.marker == "?" },
        PREVIEW_RANKS[parts.priority],
        parts.colour.takeIf { it.isNotEmpty() }?.let { "🎨 ${it.trim('{', '}')}" },
        due?.let { "📅 $it" + if (overdue) " ⚠️ OVERDUE" else "" },
        estimateOf(content)?.let { "⏱ ~$it" },
        recurrenceOf(parts)?.let { "🔁 @$it" },
        parts.vote?.let { "👍 +$it" },
        tags.takeIf { it.isNotEmpty() }?.let { names -> "🏷 " + names.joinToString(" ") { "#$it" } },
        parts.comment?.let { "💬 $it" },
    )
}
