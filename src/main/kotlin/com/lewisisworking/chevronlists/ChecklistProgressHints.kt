/**
 * ChecklistProgressHints.kt
 * Pure logic for the checklist progress inlay hints: an end-of-line progress
 * bar on every `> Header` whose section holds checkboxes, plus a mini bar on
 * any `>> N.` line whose deeper children hold checkboxes. No IntelliJ
 * Platform imports - fully testable with plain JUnit.
 *
 * The JB-native counterpart of the VS Code extension's checklist progress
 * decorations (CL: Toggle Checklist Progress Bar): VS Code paints them as
 * `after` decorations in red/amber/green, while IntelliJ draws them as inlay
 * hints, which survive soft wrap. Toggled in Settings, Editor, Inlay Hints,
 * like every other provider.
 */
package com.lewisisworking.chevronlists

private const val BAR_FULL  = "▓"
private const val BAR_EMPTY = "░"

/** Pure: a block bar of [length] cells with [done] of [total] filled. */
fun checklistBar(done: Int, total: Int, length: Int): String {
    val filled = Math.round(done.toDouble() / total * length).toInt()
    return BAR_FULL.repeat(filled) + BAR_EMPTY.repeat(length - filled)
}

private fun isDone(content: String): Boolean? {
    val check = splitItem(content).check
    if (check.isEmpty()) return null
    return check.equals("[x]", ignoreCase = true)
}

/**
 * Pure: counts done/total checkboxes among the item lines in [range].
 * Only lines at exactly [depth] chevrons count; scanning stops at a line at
 * [stopDepth] chevrons or shallower (a sibling or an outdent).
 */
private fun countChecks(
    lines: List<String>,
    range: IntRange,
    prefix: String,
    depth: Int?,
    stopDepth: Int
): Pair<Int, Int> {
    var done = 0; var total = 0
    for (k in range) {
        val b = parseBullet(lines[k], prefix)
        val n = parseNumbered(lines[k])
        val chevs   = b?.chevrons ?: n?.chevrons
        val content = b?.content  ?: n?.content
        if (chevs != null && chevs.length <= stopDepth) break
        if (content.isNullOrBlank()) continue
        if (depth != null && chevs != null && chevs.length != depth) continue
        if (depth != null && chevs == null) continue
        val d = isDone(content) ?: continue
        total++
        if (d) done++
    }
    return done to total
}

/**
 * Pure: the progress hint for the section headed at [header], or null when
 * [header] is not a section header or the section holds no checkboxes.
 * Counts every checkbox in the section, like the VS Code extension.
 */
fun sectionChecklistHint(lines: List<String>, header: Int, listPrefix: String): String? {
    if (header !in lines.indices || !isHeader(lines[header])) return null
    var end = lines.size - 1
    for (i in header + 1 until lines.size) {
        if (parseHeader(lines[i]) != null || parseSubheading(lines[i]) != null) { end = i - 1; break }
    }
    if (end <= header) return null
    val (done, total) = countChecks(lines, header + 1..end, listPrefix, null, 1)
    if (total == 0) return null
    return "(${checklistBar(done, total, 10)} $done/$total)"
}

/**
 * Pure: the mini progress hint for the numbered item at [index], or null when
 * the line is not a plain (checkbox-free) numbered item whose deeper children
 * hold checkboxes. Counts only children at exactly one depth deeper.
 */
fun subgroupChecklistHint(lines: List<String>, index: Int, listPrefix: String): String? {
    if (index !in lines.indices) return null
    val numbered = parseNumbered(lines[index]) ?: return null
    if (isDone(numbered.content) != null) return null
    var end = lines.size - 1
    for (i in index + 1 until lines.size) {
        if (parseHeader(lines[i]) != null || parseSubheading(lines[i]) != null) { end = i - 1; break }
    }
    // Peek at the next non-blank line: it must sit exactly one depth deeper.
    var firstChild = -1
    for (k in index + 1..end) {
        if (lines[k].isBlank()) continue
        val b = parseBullet(lines[k], listPrefix)
        val n = parseNumbered(lines[k])
        val chevs = b?.chevrons ?: n?.chevrons
        if (chevs == numbered.chevrons + ">") firstChild = k
        break
    }
    if (firstChild < 0) return null
    val childDepth = numbered.chevrons.length + 1
    val (done, total) = countChecks(lines, index + 1..end, listPrefix, childDepth, numbered.chevrons.length)
    if (total == 0) return null
    return "(${checklistBar(done, total, 6)} $done/$total)"
}
