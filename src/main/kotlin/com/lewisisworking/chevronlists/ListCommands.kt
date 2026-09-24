/**
 * ListCommands.kt
 * Pure section-wide list commands: renumber, and convert between bullets and
 * numbered items. No IntelliJ Platform imports. Each takes the lines of one
 * section body (no header) and returns them changed. Mirrors the VS Code
 * extension's sortCommands.ts.
 */
package com.lewisisworking.chevronlists

/** Pure: numbers every list from 1, each list counted separately (see NumberingRuns) */
fun renumberLines(lines: List<String>): List<String> {
    val counters = HashMap<Int, Int>()
    val runs     = NumberingRuns()
    return lines.map { text ->
        val run = runs.visit(text) ?: return@map text
        val n   = parseNumbered(text)!!
        val next = (counters[run] ?: 0) + 1
        counters[run] = next
        "${n.chevrons} $next. ${n.content}"
    }
}

/**
 * Pure: turns every bullet into a numbered item. Each bullet joins the list it
 * sits in and continues from the highest number already in that list, so a
 * section of plain bullets becomes 1, 2, 3.
 */
fun bulletsToNumbered(lines: List<String>, listPrefix: String): List<String> {
    // First pass: which list each bullet joins, and the highest number in each list
    val runs   = NumberingRuns()
    val runOf  = HashMap<Int, Int>()
    val maxNum = HashMap<Int, Int>()
    for ((i, text) in lines.withIndex()) {
        val run = runs.visit(text, asNumbered = parseBullet(text, listPrefix) != null) ?: continue
        runOf[i] = run
        parseNumbered(text)?.let { maxNum[run] = maxOf(maxNum[run] ?: 0, it.num) }
    }
    return lines.mapIndexed { i, text ->
        val b    = parseBullet(text, listPrefix) ?: return@mapIndexed text
        val run  = runOf[i] ?: return@mapIndexed text
        val next = (maxNum[run] ?: 0) + 1
        maxNum[run] = next
        "${b.chevrons} $next. ${b.content}"
    }
}

/** Pure: turns every numbered item into a bullet with [listPrefix]; only the marker changes */
fun numberedToBullets(lines: List<String>, listPrefix: String): List<String> =
    lines.map { text ->
        val n = parseNumbered(text) ?: return@map text
        "${n.chevrons} $listPrefix ${n.content}"
    }

/** The body of the section holding [caretLine]: after its header, up to the next header */
fun sectionBody(lines: List<String>, caretLine: Int): IntRange? {
    if (caretLine !in lines.indices) return null
    var start = 0
    for (i in caretLine downTo 0) if (isHeader(lines[i])) { start = i + 1; break }
    var end = lines.size - 1
    for (i in start until lines.size) if (isHeader(lines[i])) { end = i - 1; break }
    return start..end
}

/**
 * Pure: applies [change] to the body of the section at [caretLine] and returns
 * the whole document, or null when nothing changes.
 */
fun applyToSection(lines: List<String>, caretLine: Int, change: (List<String>) -> List<String>): List<String>? {
    val body = sectionBody(lines, caretLine) ?: return null
    if (body.isEmpty()) return null
    val result = lines.subList(0, body.first) + change(lines.subList(body.first, body.last + 1)) +
        lines.subList(body.last + 1, lines.size)
    return if (result == lines) null else result
}
