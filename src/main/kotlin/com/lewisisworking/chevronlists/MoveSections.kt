/**
 * MoveSections.kt
 * Pure logic for swapping the `> Header` section at the caret with the one above
 * or below it. No IntelliJ Platform imports - fully testable with plain JUnit.
 *
 * A section is its header and everything up to its last non-blank line before
 * the next header, so notes under the items move with it. The blank lines
 * between the two sections stay between them. Ported from the VS Code
 * extension's Move Section Up / Down.
 */
package com.lewisisworking.chevronlists

/** The last non-blank line of the section headed at [header], or the header itself */
internal fun sectionContentEnd(lines: List<String>, header: Int): Int {
    val next = findHeaderBelow(lines, header) ?: lines.size
    return (next - 1 downTo header + 1).firstOrNull { lines[it].isNotBlank() } ?: header
}

/**
 * Pure: swaps the section holding [caretLine] with the previous one (or the next
 * when [up] is false). [MoveResult.newIndex] is the caret's line in the moved
 * section, at the same distance from its header. Null when there is no section
 * at the caret or nothing to swap with.
 */
fun computeMoveSection(lines: List<String>, caretLine: Int, up: Boolean): MoveResult? {
    if (caretLine !in lines.indices) return null
    val current = if (isHeader(lines[caretLine])) caretLine else findHeaderAbove(lines, caretLine) ?: return null
    val other = (if (up) findHeaderAbove(lines, current) else findHeaderBelow(lines, current)) ?: return null

    val first = minOf(current, other)
    val second = maxOf(current, other)
    val firstEnd = sectionContentEnd(lines, first)
    val secondEnd = sectionContentEnd(lines, second)
    val gap = lines.subList(firstEnd + 1, second)
    val result = lines.subList(0, first) + lines.subList(second, secondEnd + 1) + gap +
        lines.subList(first, firstEnd + 1) + lines.subList(secondEnd + 1, lines.size)

    val currentEnd = if (current == first) firstEnd else secondEnd
    val newHeader = if (current == first) first + (secondEnd - second + 1) + gap.size else first
    return MoveResult(result, newHeader + minOf(caretLine - current, currentEnd - current))
}
