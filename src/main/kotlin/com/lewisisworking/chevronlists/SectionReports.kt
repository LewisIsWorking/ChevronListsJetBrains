/**
 * SectionReports.kt
 * Pure logic for the table of contents, reading time, section path, an item as
 * markdown and a section as a table. No IntelliJ Platform imports - fully
 * testable with plain JUnit. Ported from the VS Code extension.
 */
package com.lewisisworking.chevronlists

import kotlin.math.roundToInt

private const val CONTENTS_NAME    = "Table of Contents"
private const val WORDS_PER_MINUTE = 200

private val WORD_GOAL     = Regex("""==\d+""")
private val WHITESPACE    = Regex("""\s+""")
private val INLINE_COLOUR = Regex("""\{(?:red|green|blue|yellow|orange|purple)\}\s*""")
private val TAG           = Regex("""(?<!\S)#(\w[\w-]*)""")

private fun sectionTitle(header: String) = WORD_GOAL.replace(parseHeader(header)!!.content, "").trim()

private fun plural(count: Int, noun: String) = if (count == 1) "$count $noun" else "$count ${noun}s"

private fun itemContents(lines: List<String>, range: IntRange, listPrefix: String) =
    range.mapNotNull { itemContent(lines[it], listPrefix) }

private fun plainWordCount(content: String): Int {
    val plain = stripItemMetadata(content)
    return if (plain.isEmpty()) 0 else WHITESPACE.split(plain).size
}

/** Pure: a Table of Contents section linking every other section; null when there are none */
fun tableOfContents(lines: List<String>): List<String>? {
    val names = lines.mapNotNull { parseHeader(it)?.content?.trim() }.filter { it != CONTENTS_NAME }
    if (names.isEmpty()) return null
    return listOf("> $CONTENTS_NAME") + names.map { ">> - [[$it]]" }
}

private fun readingTime(words: Int): String {
    val seconds = (words * 60.0 / WORDS_PER_MINUTE).roundToInt()
    return if (seconds < 60) plural(seconds, "second") else plural((seconds / 60.0).roundToInt(), "minute")
}

/** Pure: the words and reading time of the caret's section, or of the whole file outside one */
fun readingTimeMessage(lines: List<String>, caretLine: Int, listPrefix: String): String {
    val header = headerAtOrAbove(lines, caretLine)
    val range  = if (header == null) lines.indices else header + 1..sectionLastLine(lines, header)
    val words  = itemContents(lines, range, listPrefix).sumOf(::plainWordCount)
    val place  = if (header == null) "Whole file" else "\"${sectionTitle(lines[header])}\""
    return "CL: $place - ${plural(words, "word")}, ~${readingTime(words)} to read"
}

/** Pure: where the caret is in its section and how much the section holds; null outside a section */
fun sectionPathMessage(lines: List<String>, caretLine: Int, listPrefix: String): String? {
    val header = headerAtOrAbove(lines, caretLine) ?: return null
    val last   = sectionLastLine(lines, header)
    val items  = itemContents(lines, header + 1..last, listPrefix)
    return "📍 ${sectionTitle(lines[header])}  ·  line ${caretLine - header} of ${last - header}" +
        "  ·  ${plural(items.size, "item")}  ·  ${plural(items.sumOf(::plainWordCount), "word")}"
}

private fun priorityEmoji(priority: String) = when (priority.length) {
    3    -> "🔴 "
    2    -> "🟠 "
    1    -> "🟡 "
    else -> ""
}

/** Pure: the item on [line] as a standard markdown list line; null unless it is an item */
fun itemAsMarkdown(line: String, listPrefix: String): String? {
    val content  = itemContent(line, listPrefix) ?: return null
    val numbered = parseNumbered(line)
    val chevrons = numbered?.chevrons ?: parseBullet(line, listPrefix)!!.chevrons
    val parts    = splitItem(content)
    val check    = when {
        parts.check.equals("[x]", ignoreCase = true) -> "[x] "
        parts.check.isNotEmpty()                     -> "[ ] "
        else                                         -> ""
    }
    val words  = TAG.replace(INLINE_COLOUR.replace(listOf(parts.marker, parts.body).filter { it.isNotEmpty() }.joinToString(" "), "")) { "**#${it.groupValues[1]}**" }
    val bullet = numbered?.let { "${it.num}." } ?: "-"
    return "  ".repeat(chevrons.length - 2) + "$bullet $check${priorityEmoji(parts.priority)}$words".trim()
}

/** Pure: the caret's section as a markdown document holding a numbered table; null with no items */
fun sectionAsMarkdownTable(lines: List<String>, caretLine: Int, listPrefix: String): String? {
    val header = headerAtOrAbove(lines, caretLine) ?: return null
    val items  = itemContents(lines, header + 1..sectionLastLine(lines, header), listPrefix)
    if (items.isEmpty()) return null
    val rows = items.mapIndexed { i, content -> "| ${i + 1} | ${stripItemMetadata(content).replace("|", "\\|")} |" }
    return (listOf("# ${sectionTitle(lines[header])}", "", "| # | Content |", "|---|---------|") + rows).joinToString("\n") + "\n"
}
