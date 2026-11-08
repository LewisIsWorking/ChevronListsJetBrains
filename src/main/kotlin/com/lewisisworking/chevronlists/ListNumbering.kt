/**
 * ListNumbering.kt
 * Pure logic for rebasing, offsetting and restarting numbered lists, and for
 * wrapping an item's text. No IntelliJ Platform imports - fully testable with
 * plain JUnit. Ported from the VS Code extension.
 */
package com.lewisisworking.chevronlists

/** The lines after a numbering change, and how many items it changed */
class ListEdit(val lines: List<String>, val changed: Int)

/** An offset's lines, and how many items it would have taken below 1 (then nothing changes) */
class ListOffset(val lines: List<String>, val changed: Int, val tooLow: Int)

/** The two lines of a wrapped item, and the caret column on the second */
class ItemWrap(val first: String, val second: String, val column: Int)

private fun headerAtOrAbove(lines: List<String>, line: Int): Int? = (line downTo 0).firstOrNull { isHeader(lines[it]) }

private fun sectionLastLine(lines: List<String>, header: Int): Int = (findHeaderBelow(lines, header) ?: lines.size) - 1

private fun renumbered(item: ChevronNumbered, number: Int) = "${item.chevrons} $number. ${item.content}"

/**
 * Pure: renumbers each list from [line] down, continuing from its last number
 * above [line]. Null unless [line] is a numbered item in a section.
 */
fun rebaseListFrom(lines: List<String>, line: Int): ListEdit? {
    if (line !in lines.indices || parseNumbered(lines[line]) == null) return null
    val header = headerAtOrAbove(lines, line) ?: return null
    val out = lines.toMutableList()
    val runs = NumberingRuns()
    val counters = HashMap<Int, Int>()
    var changed = 0
    for (i in header + 1..sectionLastLine(lines, header)) {
        val run = runs.visit(lines[i]) ?: continue
        val item = parseNumbered(lines[i])!!
        if (i < line) {
            counters[run] = item.num
            continue
        }
        val next = (counters[run] ?: 0) + 1
        counters[run] = next
        if (item.num != next) {
            out[i] = renumbered(item, next)
            changed++
        }
    }
    return ListEdit(out, changed)
}

/**
 * Pure: adds [offset] to every number in the section at [caretLine], or changes
 * nothing when any would go below 1. Null outside a section.
 */
fun offsetListNumbers(lines: List<String>, caretLine: Int, offset: Int): ListOffset? {
    if (caretLine !in lines.indices) return null
    val header = headerAtOrAbove(lines, caretLine) ?: return null
    val numbered = (header + 1..sectionLastLine(lines, header)).filter { parseNumbered(lines[it]) != null }
    val tooLow = numbered.count { parseNumbered(lines[it])!!.num + offset < 1 }
    if (tooLow > 0) return ListOffset(lines, 0, tooLow)
    val out = lines.toMutableList()
    for (i in numbered) {
        val item = parseNumbered(lines[i])!!
        out[i] = renumbered(item, item.num + offset)
    }
    return ListOffset(out, numbered.size, 0)
}

/**
 * Pure: renumbers the item at [line] and the rest of its own list from [start].
 * Items above it, nested lists and other lists are untouched. Null unless
 * [line] is a numbered item.
 */
fun setListStart(lines: List<String>, line: Int, start: Int): List<String>? {
    if (line !in lines.indices || parseNumbered(lines[line]) == null) return null
    val header = headerAtOrAbove(lines, line)
    val first = if (header == null) 0 else header + 1
    val last = if (header == null) lines.lastIndex else sectionLastLine(lines, header)
    val out = lines.toMutableList()
    val runs = NumberingRuns()
    var caretRun: Int? = null
    var next = start
    for (i in first..last) {
        val run = runs.visit(lines[i])
        if (i == line) caretRun = run
        if (run == null || i < line || run != caretRun) continue
        out[i] = renumbered(parseNumbered(lines[i])!!, next++)
    }
    return out
}

/** Pure: splits an item's words at [column] onto an indented continuation line; null unless words fall on both sides */
fun wrapItemAt(line: String, column: Int, listPrefix: String): ItemWrap? {
    val content = itemContent(line, listPrefix) ?: return null
    val chevrons = parseNumbered(line)?.chevrons ?: parseBullet(line, listPrefix)!!.chevrons
    val start = line.length - content.length
    val at = (column - start).coerceIn(0, content.length)
    val before = content.substring(0, at).trimEnd()
    val after = content.substring(at).trimStart()
    if (before.isEmpty() || after.isEmpty()) return null
    val continuation = "$chevrons   "
    return ItemWrap(line.substring(0, start) + before, continuation + after, continuation.length)
}
