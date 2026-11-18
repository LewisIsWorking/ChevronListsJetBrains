/**
 * SectionSummaryHints.kt
 * Pure logic for the per-section summary inlay hints: an end-of-line note on
 * every `> Header` with its item count, done progress, tag count, urgent
 * count and old-item count. No IntelliJ Platform imports - fully testable.
 *
 * The JB-native counterpart of the VS Code extension's section summary
 * decorations (CL: Toggle Section Summary): VS Code paints them as `after`
 * decorations, while IntelliJ draws them as inlay hints, which survive soft
 * wrap. Toggled in Settings, Editor, Inlay Hints, like every other provider.
 */
package com.lewisisworking.chevronlists

import java.time.LocalDate
import java.time.temporal.ChronoUnit

private val SUMMARY_BLOCKS = listOf("▁", "▂", "▃", "▄", "▅", "▆", "▇", "█")
private const val SUMMARY_EMPTY = "░"

/**
 * Pure: a 4-cell sparkline for a 0-1 [ratio], using finer blocks for earlier
 * cells like the VS Code extension. A ratio of 0 renders four light cells.
 */
fun summarySparkline(ratio: Double): String {
    val filled = Math.round(ratio * 4).toInt()
    return (0 until 4).joinToString("") { i ->
        if (i >= filled) SUMMARY_EMPTY
        else SUMMARY_BLOCKS[(Math.round((i + 1).toDouble() / filled * 7)).toInt().coerceAtMost(7)]
    }
}

/**
 * Pure: the summary hint for the section headed at [header], or null when
 * [header] is not a section header or the section holds no items. [today]
 * anchors the "old" count (items created 30 or more days ago).
 */
fun sectionHeaderHint(lines: List<String>, header: Int, listPrefix: String, today: LocalDate): String? {
    if (header !in lines.indices || !isHeader(lines[header])) return null
    var items = 0; var done = 0; var checks = 0; var tags = 0; var urgent = 0; var old = 0
    var i = header + 1
    while (i < lines.size && parseHeader(lines[i]) == null && parseSubheading(lines[i]) == null) {
        val content = parseBullet(lines[i], listPrefix)?.content ?: parseNumbered(lines[i])?.content
        i++
        if (content.isNullOrBlank()) continue
        items++
        tags += findTags(content).size
        val parts = splitItem(content)
        if (parts.check.isNotEmpty()) {
            checks++
            if (parts.check.equals("[x]", ignoreCase = true)) done++
        }
        if (parts.priority.length == 3) urgent++
        val created = createdOn(parts)
        if (created != null && ChronoUnit.DAYS.between(created, today) >= 30) old++
    }
    if (items == 0) return null
    val parts = mutableListOf(plural(items, "item"))
    if (checks > 0) parts += "${summarySparkline(done.toDouble() / checks)} $done done"
    if (tags > 0)   parts += plural(tags, "tag")
    if (urgent > 0) parts += "$urgent urgent"
    if (old > 0)    parts += "$old old"
    return "(${parts.joinToString(" · ")})"
}
