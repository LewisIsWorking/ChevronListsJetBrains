/**
 * SectionSummaries.kt
 * Pure reports on sections and priorities, and the item edits that act at every caret.
 * No IntelliJ imports.
 */
package com.lewisisworking.chevronlists

/** One line of a report; choosing it goes to [line] */
data class ReportEntry(val line: Int, val label: String)

private const val TOP_WORDS = 10
private const val SHORTEST_WORD = 3

private val NOT_A_WORD = Regex("""[^\p{L}\p{N}_]+""")
private val STOP_WORDS = setOf(
    "a", "an", "the", "and", "or", "but", "in", "on", "at", "to", "for", "of", "with",
    "is", "it", "its", "this", "that", "be", "as", "by", "from", "was", "are", "have",
    "has", "had", "not", "i", "my", "we", "our", "you", "your", "they", "their",
)
private val PRIORITY_BUCKETS = listOf(
    "!!!" to "🔴 Urgent (!!!)",
    "!!"  to "🟠 High (!!)",
    "!"   to "🟡 Normal (!)",
)

internal fun plural(n: Int, noun: String) = "$n $noun${if (n == 1) "" else "s"}"

internal fun headerName(lines: List<String>, header: Int) =
    withSectionGoal(parseHeader(lines[header])!!.content.trim(), null)

private fun sectionItems(lines: List<String>, header: Int, listPrefix: String) =
    (header + 1..sectionLastLine(lines, header)).mapNotNull { itemContent(lines[it], listPrefix) }

internal fun formatTotal(minutes: Int): String {
    val h = minutes / 60
    val m = minutes % 60
    return when {
        h == 0 -> "${m}m"
        m == 0 -> "${h}h"
        else   -> "${h}h ${m}m"
    }
}

/** Pure: one entry per section with its word count, and its goal once that is met */
fun wordCountReport(lines: List<String>, listPrefix: String): List<ReportEntry> =
    lines.indices.filter { parseHeader(lines[it]) != null }.map { header ->
        val words = sectionItems(lines, header, listPrefix).sumOf { itemWordCount(it) }
        val goal  = sectionGoal(parseHeader(lines[header])!!.content)
        val count = if (goal != null && words >= goal) "$words / $goal words ✅" else plural(words, "word")
        ReportEntry(header, "${headerName(lines, header)}: $count")
    }

/** Pure: how many items the caret's section has at each depth; null outside a section */
fun nestingSummary(lines: List<String>, caretLine: Int, listPrefix: String): String? {
    val header = headerAtOrAbove(lines, caretLine) ?: return null
    val name   = headerName(lines, header)
    val depths = (header + 1..sectionLastLine(lines, header)).mapNotNull { i ->
        (parseBullet(lines[i], listPrefix)?.chevrons ?: parseNumbered(lines[i])?.chevrons)?.let { it.length - 2 }
    }
    if (depths.isEmpty()) return "CL: \"$name\" has no items"
    val counts = depths.groupingBy { it }.eachCount().toSortedMap()
    return "CL: \"$name\" - " + counts.entries.joinToString(", ") { (depth, n) -> "Depth $depth: ${plural(n, "item")}" }
}

/** Pure: every item with a priority in the file, most urgent first */
fun priorityReport(lines: List<String>, listPrefix: String): List<ReportEntry> =
    PRIORITY_BUCKETS.flatMap { (priority, bucket) ->
        lines.indices.mapNotNull { i ->
            val content = itemContent(lines[i], listPrefix) ?: return@mapNotNull null
            if (splitItem(content).priority != priority) return@mapNotNull null
            val section = headerAtOrAbove(lines, i)?.let { "  (${headerName(lines, it)})" } ?: ""
            ReportEntry(i, "$bucket  ${stripItemMetadata(content)}$section")
        }
    }

/** Pure: how much estimated time the caret's section adds up to; null outside a section */
fun sectionEstimateMessage(lines: List<String>, caretLine: Int, listPrefix: String): String? {
    val header    = headerAtOrAbove(lines, caretLine) ?: return null
    val name      = headerName(lines, header)
    val estimates = sectionItems(lines, header, listPrefix).mapNotNull { itemEstimateMinutes(it) }
    if (estimates.isEmpty()) return "CL: \"$name\" - no ~estimate markers found"
    return "CL: \"$name\" - ${plural(estimates.size, "estimate")} totalling ${formatTotal(estimates.sum())}"
}

/** Pure: the most used words in the caret's section; null outside a section */
fun wordFrequencyMessage(lines: List<String>, caretLine: Int, listPrefix: String): String? {
    val header = headerAtOrAbove(lines, caretLine) ?: return null
    val name   = headerName(lines, header)
    val top = sectionItems(lines, header, listPrefix)
        .flatMap { stripItemMetadata(it).lowercase().split(NOT_A_WORD) }
        .filter { it.length >= SHORTEST_WORD && it !in STOP_WORDS }
        .groupingBy { it }.eachCount().entries
        .sortedByDescending { it.value }
        .take(TOP_WORDS)
    if (top.isEmpty()) return "CL: \"$name\" - no words found"
    return "CL: \"$name\" top words: " + top.joinToString("  ·  ") { (word, n) -> "\"$word\" ×$n" }
}

private fun editItemsAt(
    lines: List<String>,
    caretLines: Collection<Int>,
    listPrefix: String,
    change: (String) -> String?,
): List<String> {
    val after = lines.toMutableList()
    for (i in caretLines.distinct()) {
        if (itemContent(lines[i], listPrefix) != null) change(lines[i])?.let { after[i] = it }
    }
    return after
}

/** Pure: [lines] with the done state of the item on each caret line toggled once */
fun toggleDoneAtLines(lines: List<String>, caretLines: Collection<Int>, listPrefix: String): List<String> =
    editItemsAt(lines, caretLines, listPrefix) { computeToggleDone(it, listPrefix) }

/** Pure: [lines] with priority [level] on the item on each caret line; 0 removes it */
fun setPriorityAtLines(lines: List<String>, caretLines: Collection<Int>, listPrefix: String, level: Int): List<String> =
    editItemsAt(lines, caretLines, listPrefix) { line ->
        transformItemLine(line, listPrefix) { setItemPriority(it, level) }
    }
