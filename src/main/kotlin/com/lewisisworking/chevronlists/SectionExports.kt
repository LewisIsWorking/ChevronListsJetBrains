/**
 * SectionExports.kt
 * Pure logic for copying the caret section as HTML, JSON or a CSV row. No
 * IntelliJ Platform imports - fully testable with plain JUnit. Ported from the
 * VS Code extension.
 */
package com.lewisisworking.chevronlists

private val DUE_DATE_FIELD = Regex("""@(\d{4}-\d{2}-\d{2})""")
private val ESTIMATE_FIELD = Regex("""~(?:(\d+)h)?(?:(\d+)m)?""")
private val RATING_FIELD   = Regex("""★([1-5])""")
private val TAG_FIELD      = Regex("""(?<!\S)#(\w[\w-]*)""")

private class ExportItem(val depth: Int, val num: Int?, val content: String)

private fun exportItems(lines: List<String>, header: Int, listPrefix: String): List<ExportItem> =
    (header + 1 until lines.size)
        .asSequence()
        .takeWhile { !isHeader(lines[it]) && parseSubheading(lines[it]) == null }
        .mapNotNull { i ->
            val content = itemContent(lines[i], listPrefix) ?: return@mapNotNull null
            val numbered = parseNumbered(lines[i])
            val chevrons = numbered?.chevrons ?: parseBullet(lines[i], listPrefix)!!.chevrons
            ExportItem(chevrons.length - 2, numbered?.num, content)
        }
        .toList()

private fun headerAt(lines: List<String>, caretLine: Int): Int? = sectionHeaderAt(lines, caretLine).takeIf { it >= 0 }

private fun sectionName(lines: List<String>, header: Int) = parseHeader(lines[header])!!.content.trim()

internal fun escapeHtml(text: String) = text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")

private fun priorityBadge(level: Int) = when (level) {
    3    -> """<span style="color:#E06C75;font-weight:bold">🔴 </span>"""
    2    -> """<span style="color:#E5C07B;font-weight:bold">🟠 </span>"""
    1    -> """<span style="color:#E5C07B">🟡 </span>"""
    else -> ""
}

/** Pure: the section at [caretLine] as an HTML snippet; null outside a section */
fun sectionAsHtml(lines: List<String>, caretLine: Int, listPrefix: String): String? {
    val header = headerAt(lines, caretLine) ?: return null
    val rows = exportItems(lines, header, listPrefix).joinToString("") { item ->
        val indent = "&nbsp;".repeat(item.depth * 4)
        val parts  = splitItem(item.content)
        val badge  = priorityBadge(parts.priority.length)
        val plain  = escapeHtml(stripItemMetadata(item.content))
        when {
            parts.check.isNotEmpty() -> {
                val checked = if (parts.check.equals("[x]", ignoreCase = true)) " checked" else ""
                """<li style="margin:.2em 0">$indent<input type="checkbox"$checked disabled> $badge$plain</li>"""
            }
            item.num != null -> """<li style="list-style:decimal;margin:.2em 0">$indent$badge$plain</li>"""
            else             -> """<li style="list-style:disc;margin:.2em 0">$indent$badge$plain</li>"""
        }
    }
    val name = escapeHtml(sectionName(lines, header))
    return """<section><h3 style="margin:0 0 .5em">$name</h3><ul style="padding-left:1.2em;margin:0">$rows</ul></section>"""
}

internal fun csvEscape(value: String) =
    if (value.any { it == ',' || it == '"' || it == '\n' }) "\"${value.replace("\"", "\"\"")}\"" else value

/** Pure: the plain words of each item in the section at [caretLine] as one CSV row; null with no items */
fun sectionAsCsvRow(lines: List<String>, caretLine: Int, listPrefix: String): String? {
    val header = headerAt(lines, caretLine) ?: return null
    val items = exportItems(lines, header, listPrefix)
    if (items.isEmpty()) return null
    return items.joinToString(",") { csvEscape(stripItemMetadata(it.content)) }
}

private fun estimateParts(content: String): Pair<Int, Int>? {
    val match = ESTIMATE_FIELD.findAll(splitItem(content).body).firstOrNull { it.value.length > 1 } ?: return null
    val hours = match.groupValues[1].toIntOrNull() ?: 0
    val minutes = match.groupValues[2].toIntOrNull() ?: 0
    return (hours to minutes).takeIf { hours > 0 || minutes > 0 }
}

internal fun estimateOf(content: String): String? {
    val (hours, minutes) = estimateParts(content) ?: return null
    return when {
        hours > 0 && minutes > 0 -> "${hours}h${minutes}m"
        hours > 0                -> "${hours}h"
        else                     -> "${minutes}m"
    }
}

/** Pure: the minutes of the first ~estimate in an item's words; null when there is none */
internal fun itemEstimateMinutes(content: String): Int? =
    estimateParts(content)?.let { (hours, minutes) -> hours * 60 + minutes }

private fun jsonString(text: String): String = buildString {
    append('"')
    for (c in text) when {
        c == '"'      -> append("\\\"")
        c == '\\'     -> append("\\\\")
        c == '\n'     -> append("\\n")
        c == '\r'     -> append("\\r")
        c == '\t'     -> append("\\t")
        c < ' '       -> append("\\u%04x".format(c.code))
        else          -> append(c)
    }
    append('"')
}

internal fun toJson(value: Any?, indent: String = ""): String {
    val inner = "$indent  "
    return when (value) {
        null       -> "null"
        is String  -> jsonString(value)
        is Map<*, *> -> value.entries.joinToString(",\n", "{\n", "\n$indent}") { (k, v) ->
            "$inner${jsonString(k as String)}: ${toJson(v, inner)}"
        }
        is List<*> -> if (value.isEmpty()) "[]" else value.joinToString(",\n", "[\n", "\n$indent]") { inner + toJson(it, inner) }
        else       -> value.toString()
    }
}

/** Pure: a CSV of each section's item and word counts, then the totals */
fun fileStatsAsCsv(stats: FileStats): String =
    (listOf("Section,Items,Words") +
        stats.sections.map { "${csvEscape(it.name)},${it.items},${it.words}" } +
        "TOTAL,${stats.totalItems},${stats.totalWords}").joinToString("\n")

/** Pure: the file's statistics as JSON, with the field names VS Code writes */
fun fileStatsAsJson(stats: FileStats): String = toJson(mapOf(
    "totalSections"  to stats.sections.size,
    "totalItems"     to stats.totalItems,
    "totalWords"     to stats.totalWords,
    "totalDone"      to stats.totalDone,
    "totalChecks"    to stats.totalChecks,
    "totalTags"      to stats.totalTags,
    "totalColoured"  to stats.totalColoured,
    "totalOverdue"   to stats.totalOverdue,
    "avgItems"       to stats.avgItems,
    "mostPopulated"  to stats.mostPopulated?.name,
    "leastPopulated" to stats.leastPopulated?.name,
    "sections"       to stats.sections.map {
        mapOf(
            "name"      to it.name,
            "itemCount" to it.items,
            "wordCount" to it.words,
            "lineIndex" to it.line,
            "done"      to it.done,
            "total"     to it.checks,
            "tagCount"  to it.tags,
            "coloured"  to it.coloured,
            "overdue"   to it.overdue,
        )
    },
))

/** The tags of an item, lower case and without repeats */
internal fun tagFields(content: String): List<String> =
    TAG_FIELD.findAll(content).map { it.groupValues[1].lowercase() }.distinct().toList()

/** One item's fields, with the names VS Code writes */
internal fun itemFields(depth: Int, num: Int?, content: String): Map<String, Any?> {
    val parts = splitItem(content)
    return linkedMapOf(
        "depth"    to depth,
        "type"     to if (num != null) "numbered" else "bullet",
        "num"      to num,
        "content"  to content.trim(),
        "done"     to parts.check.equals("[x]", ignoreCase = true),
        "priority" to parts.priority.length.takeIf { it > 0 },
        "dueDate"  to DUE_DATE_FIELD.find(parts.body)?.groupValues?.get(1),
        "estimate" to estimateOf(content),
        "votes"    to parts.vote,
        "starred"  to isStarred(parts),
        "colour"   to parts.colour.trim('{', '}').ifEmpty { null },
        "flagged"  to isFlagged(parts),
        "comment"  to parts.comment,
        "rating"   to RATING_FIELD.find(content)?.groupValues?.get(1)?.toInt(),
        "tags"     to tagFields(content),
    )
}

/** Pure: the section at [caretLine] and each item's fields as JSON; null outside a section */
fun sectionAsJson(lines: List<String>, caretLine: Int, listPrefix: String): String? {
    val header = headerAt(lines, caretLine) ?: return null
    val items = exportItems(lines, header, listPrefix).map { itemFields(it.depth, it.num, it.content) }
    return toJson(linkedMapOf("section" to sectionName(lines, header), "items" to items))
}
