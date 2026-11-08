/**
 * ItemContentEdits.kt
 * Pure logic for editing an item's words, quick notes, section links, replacing
 * text in a section and shifting its due dates. No IntelliJ Platform imports -
 * fully testable with plain JUnit. Ported from the VS Code extension.
 */
package com.lewisisworking.chevronlists

import java.time.LocalDate
import java.time.format.DateTimeParseException

private val INLINE_METADATA = Regex(
    """(?<!\S)(?:#\w[\w-]*|@\d{4}-\d{2}-\d{2}|@created:\d{4}-\d{2}-\d{2}|@(?:daily|weekly|monthly)""" +
        """|~\d+h(?:\d+m)?|~\d+m|\{(?:red|green|blue|yellow|orange|purple)\}|\[\[[^]]+]])(?!\S)"""
)
private val STRUCK_WORDS = Regex("""~~.+~~""")
private val SPACE_RUN    = Regex("""\s{2,}""")
private val ITEM_DATE    = Regex("""@(\d{4}-\d{2}-\d{2})""")

/** Pure: [line] with its words replaced by [words], keeping every marker and the metadata among the old words */
fun replaceItemWords(line: String, listPrefix: String, words: String): String? =
    transformItemLine(line, listPrefix) { content ->
        val parts = splitItem(content)
        val kept = INLINE_METADATA.findAll(parts.body).map { it.value }.toList()
        val oldWords = SPACE_RUN.replace(INLINE_METADATA.replace(parts.body, ""), " ").trim()
        val newWords = if (STRUCK_WORDS.matches(oldWords)) "~~${words.trim()}~~" else words.trim()
        joinItem(parts.copy(body = (listOf(newWords) + kept).joinToString(" ")))
    }

/** Pure: [line] with [note] as its // comment, replacing any comment it had */
fun addItemNote(line: String, listPrefix: String, note: String): String? =
    transformItemLine(line, listPrefix) { joinItem(splitItem(it).copy(comment = note.trim())) }

/** Pure: the item at [index] as a [[link]] to the section sharing its name, or null when there is none */
fun itemAsSectionLink(lines: List<String>, index: Int, listPrefix: String): String? {
    val content = lines.getOrNull(index)?.let { itemContent(it, listPrefix) } ?: return null
    val name = stripItemMetadata(content)
    val section = lines.firstNotNullOfOrNull { line ->
        parseHeader(line)?.content?.trim()?.takeIf { it.equals(name, ignoreCase = true) }
    } ?: return null
    return transformItemLine(lines[index], listPrefix) { "[[$section]]" }
}

private fun mapSectionItemLines(
    lines: List<String>, caretLine: Int, change: (String) -> Pair<String, Int>,
): ListEdit? {
    if (caretLine !in lines.indices) return null
    val header = headerAtOrAbove(lines, caretLine) ?: return null
    val out = lines.toMutableList()
    var changed = 0
    for (i in header + 1..sectionLastLine(lines, header)) {
        val (line, count) = change(lines[i])
        out[i] = line
        changed += count
    }
    return ListEdit(out, changed)
}

/**
 * Pure: replaces [find] in the words of the section's items. The list markers
 * are never searched. Counts occurrences. Null outside a section.
 */
fun replaceInSection(lines: List<String>, caretLine: Int, listPrefix: String, find: String, replacement: String): ListEdit? =
    mapSectionItemLines(lines, caretLine) { line ->
        val content = itemContent(line, listPrefix)
        val count = content?.split(find)?.size?.minus(1) ?: 0
        if (count == 0) line to 0
        else line.dropLast(content!!.length) + content.replace(find, replacement) to count
    }

private fun shifted(date: String, days: Long): String? = try {
    LocalDate.parse(date).plusDays(days).toString()
} catch (_: DateTimeParseException) {
    null
}

/**
 * Pure: moves each @YYYY-MM-DD date on the section's items by [days], leaving
 * dates the calendar does not have. Counts dates. Null outside a section.
 */
fun shiftSectionDueDates(lines: List<String>, caretLine: Int, listPrefix: String, days: Long): ListEdit? =
    mapSectionItemLines(lines, caretLine) { line ->
        if (itemContent(line, listPrefix) == null) return@mapSectionItemLines line to 0
        var count = 0
        val moved = ITEM_DATE.replace(line) { match ->
            val date = shifted(match.groupValues[1], days) ?: return@replace match.value
            count++
            "@$date"
        }
        moved to count
    }
