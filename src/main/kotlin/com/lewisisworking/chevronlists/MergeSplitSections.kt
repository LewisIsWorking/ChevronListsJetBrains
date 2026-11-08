/**
 * MergeSplitSections.kt
 * Pure logic for merging the caret section with the one below it, and for
 * splitting it at the caret item. No IntelliJ Platform imports - fully testable
 * with plain JUnit. Ported from the VS Code extension's Merge Section Below and
 * Split Section Here.
 */
package com.lewisisworking.chevronlists

/**
 * Pure: the lines to delete to merge the caret section with the next one: the
 * blank lines after this section's content and the next header, so the next
 * section's items carry straight on. Null outside a section or in the last one.
 */
fun mergeSectionBelowRange(lines: List<String>, caretLine: Int): IntRange? {
    if (caretLine !in lines.indices) return null
    val header = if (isHeader(lines[caretLine])) caretLine else findHeaderAbove(lines, caretLine) ?: return null
    val next = findHeaderBelow(lines, header) ?: return null
    return sectionContentEnd(lines, header) + 1..next
}

/**
 * Pure: the name to offer for a new section starting at the item on [caretLine],
 * or null when the caret is not on an item line inside a section.
 */
fun splitSectionName(lines: List<String>, caretLine: Int): String? {
    if (caretLine !in lines.indices || !lines[caretLine].startsWith(">>")) return null
    val header = findHeaderAbove(lines, caretLine) ?: return null
    return "${parseHeader(lines[header])!!.content.trim()} (continued)"
}
