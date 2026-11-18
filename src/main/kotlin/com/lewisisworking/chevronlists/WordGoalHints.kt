/**
 * WordGoalHints.kt
 * Pure logic for the word-goal progress inlay hints: an end-of-line progress
 * bar on every `> Header` carrying an `==N` goal, showing section words
 * against the goal. No IntelliJ Platform imports - fully testable with plain
 * JUnit.
 *
 * The JB-native counterpart of the VS Code extension's word-goal bar
 * decorations (CL: Toggle Word Goal Bar): VS Code paints them as `after`
 * decorations in red/amber/green, while IntelliJ draws them as inlay hints,
 * which survive soft wrap. Toggled in Settings, Editor, Inlay Hints, like
 * every other provider.
 */
package com.lewisisworking.chevronlists

private const val GOAL_FULL  = "▓"
private const val GOAL_EMPTY = "░"

/**
 * Pure: the progress hint for the section headed at [header], or null when
 * [header] is not a section header or carries no `==N` goal. Words are the
 * section's item words, counted like the word-count-goals report.
 */
fun sectionWordGoalHint(lines: List<String>, header: Int, listPrefix: String): String? {
    if (header !in lines.indices || !isHeader(lines[header])) return null
    val goal = parseHeader(lines[header])?.let { sectionGoal(it.content) } ?: return null
    if (goal <= 0) return null
    var end = lines.size - 1
    for (i in header + 1 until lines.size) {
        if (parseHeader(lines[i]) != null || parseSubheading(lines[i]) != null) { end = i - 1; break }
    }
    var words = 0
    for (j in header + 1..end) {
        val content = parseBullet(lines[j], listPrefix)?.content ?: parseNumbered(lines[j])?.content
        if (!content.isNullOrBlank()) words += itemWordCount(content)
    }
    val pct  = (words.toDouble() / goal).coerceAtMost(1.0)
    val bars = Math.round(pct * 10).toInt()
    return "(${GOAL_FULL.repeat(bars)}${GOAL_EMPTY.repeat(10 - bars)} $words/$goal)"
}
