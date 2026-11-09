/**
 * SectionCompare.kt
 * Pure logic for word frequency, the word cloud, the heatmaps and comparing
 * sections with each other or the clipboard. No IntelliJ Platform imports.
 * Mirrors wordFrequencyCommands.ts, wordCloudCommands.ts, heatmapCommands.ts,
 * sectionCompareTableCommands.ts, diffSectionCommands.ts and compareCommands.ts.
 */
package com.lewisisworking.chevronlists

import kotlin.math.roundToInt

private val STOP_WORDS = setOf(
    "the", "a", "an", "and", "or", "but", "in", "on", "at", "to", "for", "of", "with", "is", "it", "this", "that",
    "was", "be", "as", "by", "are", "from", "have", "has", "had", "not", "they", "we", "you", "i", "he", "she",
)
private val WORD = Regex("""[\p{L}\p{N}_]+""")
private const val CLOUD_WORDS = 40
private const val CLOUD_CELLS = 10

private fun sectionContents(lines: List<String>, header: Int, listPrefix: String): List<String> {
    val last = sectionLastLine(lines, header)
    return foundItems(lines, listPrefix).filter { it.line in (header + 1)..last }.map { it.content }
}

private fun plainItems(lines: List<String>, header: Int, listPrefix: String) =
    sectionContents(lines, header, listPrefix).map { stripItemMetadata(it) }

/** Pure: the words of the section headed at [header] with their counts, most used first */
fun sectionWordFrequency(lines: List<String>, header: Int, listPrefix: String): List<Pair<String, Int>> =
    plainItems(lines, header, listPrefix)
        .flatMap { item -> WORD.findAll(item.lowercase()).map { it.value }.toList() }
        .filter { it.length >= 3 && it !in STOP_WORDS }
        .groupingBy { it }.eachCount().toList()
        .sortedByDescending { it.second }

/** Pure: the section's ten most used words as one line */
fun sectionWordsMessage(lines: List<String>, header: Int, listPrefix: String): String {
    val name = plainSectionName(lines, header)
    val top  = sectionWordFrequency(lines, header, listPrefix).take(10)
    if (top.isEmpty()) return "CL: \"$name\" - no words found"
    return "CL: \"$name\" top words: ${top.joinToString("  ·  ") { (word, n) -> "\"$word\" ×$n" }}"
}

/** Pure: the section's most used words with a bar each, every entry pointing at the header */
fun wordCloudEntries(lines: List<String>, header: Int, listPrefix: String): List<ReportEntry> {
    val top  = sectionWordFrequency(lines, header, listPrefix).take(CLOUD_WORDS)
    val most = top.firstOrNull()?.second ?: 1
    return top.map { (word, n) ->
        val filled = (n * CLOUD_CELLS.toDouble() / most).roundToInt()
        ReportEntry(header, "${"█".repeat(filled)}${"░".repeat(CLOUD_CELLS - filled)} $word  $n")
    }
}

/** Pure: sections ranked by how many tags they hold */
fun tagHeatmap(lines: List<String>, listPrefix: String): List<ReportEntry> =
    fileStats(lines, listPrefix, "").sections.sortedByDescending { it.tags }
        .map { ReportEntry(it.line, "${plainSectionName(lines, it.line)}: ${plural(it.tags, "tag")}") }

private fun percentDone(s: SectionStats) = if (s.checks > 0) (s.done * 100.0 / s.checks).roundToInt() else 0

/** Pure: sections ranked by how much of their checkboxes are done */
fun completionHeatmap(lines: List<String>, listPrefix: String): List<ReportEntry> =
    fileStats(lines, listPrefix, "").sections.sortedByDescending { percentDone(it) }.map {
        ReportEntry(it.line, "${plainSectionName(lines, it.line)}: ${if (it.checks > 0) "${percentDone(it)}% done" else "no checkboxes"}")
    }

private fun cell(text: String) = text.replace("|", "\\|")

/** Pure: two sections' items side by side as a markdown table */
fun sectionsSideBySide(lines: List<String>, first: Int, second: Int, listPrefix: String): String {
    val a = plainItems(lines, first, listPrefix)
    val b = plainItems(lines, second, listPrefix)
    val nameA = plainSectionName(lines, first)
    val nameB = plainSectionName(lines, second)
    val rows  = (0 until maxOf(a.size, b.size)).map { "| ${cell(a.getOrElse(it) { "" })} | ${cell(b.getOrElse(it) { "" })} |" }
    return (listOf("# $nameA vs $nameB", "", "| ${cell(nameA)} | ${cell(nameB)} |", "|---|---|") + rows).joinToString("\n", postfix = "\n")
}

/** Pure: a line-by-line diff of two sections' items, as a markdown code block */
fun sectionsDiff(lines: List<String>, first: Int, second: Int, listPrefix: String): String {
    val a = plainItems(lines, first, listPrefix)
    val b = plainItems(lines, second, listPrefix)
    val diff = (0 until maxOf(a.size, b.size)).flatMap { i ->
        val left  = a.getOrNull(i)
        val right = b.getOrNull(i)
        when {
            left == right -> listOf("  ${left.orEmpty()}")
            left == null  -> listOf("+ $right")
            right == null -> listOf("- $left")
            else          -> listOf("- $left", "+ $right")
        }
    }
    val heading = "# Diff: \"${plainSectionName(lines, first)}\" vs \"${plainSectionName(lines, second)}\""
    return (listOf(heading, "", "```diff") + diff + "```").joinToString("\n")
}

/**
 * Pure: the section's items against the clipboard's lines, one line per distinct item.
 * Copied chevron lines count as their item text. Null with nothing on the clipboard.
 */
fun sectionAgainstClipboard(lines: List<String>, header: Int, listPrefix: String, clipboard: String): String? {
    val pasted = clipboard.split("\n").map { it.trim() }.filter { it.isNotEmpty() }.map { itemContent(it, listPrefix) ?: it }
    if (pasted.isEmpty()) return null
    val current = sectionContents(lines, header, listPrefix)
    val rows = (current + pasted).distinct().map { line ->
        when {
            line in current && line in pasted -> "= $line"
            line in current                   -> "- $line  *(only in section)*"
            else                              -> "+ $line  *(only in clipboard)*"
        }
    }
    return (listOf("**Section vs Clipboard Diff**", "") + rows).joinToString("\n")
}
